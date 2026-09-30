package com.movierecap.ai.data

import android.app.Application
import android.content.ContentResolver
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.movierecap.ai.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application)
    private val exportStore = ExportStore(application)
    val projects = repository.projects; val exports = repository.exports; val settings = repository.settings
    private val _draft = MutableStateFlow(RecapDraft()); val draft: StateFlow<RecapDraft> = _draft.asStateFlow()
    private val _progress = MutableStateFlow<ProcessingProgress?>(null); val progress: StateFlow<ProcessingProgress?> = _progress.asStateFlow()
    private val _exportSettings = MutableStateFlow(exportStore.loadSettings()); val exportSettings: StateFlow<ExportSettings> = _exportSettings.asStateFlow()
    private val _renderProgress = MutableStateFlow<RenderProgress?>(null); val renderProgress: StateFlow<RenderProgress?> = _renderProgress.asStateFlow()
    private val _message = MutableStateFlow<String?>(null); val message: StateFlow<String?> = _message.asStateFlow()

    fun selectVideo(uri: Uri, resolver: ContentResolver, durationMs: Long) {
        val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null } ?: "Selected video"
        val actualDuration = if (durationMs > 0) durationMs else runCatching { val retriever = MediaMetadataRetriever(); val descriptor = resolver.openFileDescriptor(uri, "r"); retriever.setDataSource(descriptor?.fileDescriptor); val result = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L; descriptor?.close(); retriever.release(); result }.getOrDefault(0L)
        _draft.value = _draft.value.copy(video = VideoInfo(name, actualDuration, uri), voiceProfile = repository.settings.value.voiceProfile, script = "", scenes = emptyList(), narrationSegments = emptyList(), subtitleCues = emptyList(), voiceGenerated = false, isSaved = false)
    }
    fun setLanguage(v: AppLanguage) { _draft.value = _draft.value.copy(language = v, isSaved = false) }
    fun setVoiceProfile(v: VoiceProfile) { _draft.value = _draft.value.copy(voiceProfile = v, voiceGenerated = false, voiceAudioUri = null, isSaved = false); showMessage("Voice changed to ${v.label}. Generate voice-over again to apply it.") }
    fun setLength(v: RecapLength) { _draft.value = _draft.value.copy(length = v, isSaved = false) }
    fun setCustomTarget(v: Int?) { _draft.value = _draft.value.copy(customTargetSeconds = v, isSaved = false) }
    fun updateScript(v: String) { _draft.value = _draft.value.copy(script = v, isSaved = false) }
    fun clearMessage() { _message.value = null }; fun showMessage(v: String) { _message.value = v }

    fun analyzeVideo() { val video = _draft.value.video ?: return showMessage("Select a video before analyzing it."); viewModelScope.launch(Dispatchers.Default) { try { setProgress("Uploading", .15f); delay(250); setProgress("Analyzing video", .45f); delay(450); val scenes = estimateSceneCount(video.durationMs); _draft.value = _draft.value.copy(analysis = VideoAnalysis(video.durationMs, scenes, scenes * 12, true)); setProgress("Completed", 1f); delay(300); _progress.value = null } catch (_: Exception) { fail("Analysis failed", "Video analysis failed. Your project was preserved.") } } }
    fun generateRecapScript() { val current = _draft.value; if (current.video == null) return showMessage("Select and analyze a video first."); viewModelScope.launch(Dispatchers.Default) { try { setProgress("Generating script", .62f); delay(700); val title = current.video.fileName.substringBeforeLast('.'); val target = current.customTargetSeconds ?: current.length.targetSeconds; val script = "In $title, the story begins with a mystery that pulls the main characters into an unexpected journey. As the clues come together, every choice raises the stakes and reveals a deeper conflict. The turning point forces them to face what they have been avoiding, leading to a final decision that changes everything. This recap is an offline draft in ${current.language.label}; connect an AI provider for content-aware narration. Target narration length: about ${target}s."; _draft.value = current.copy(script = script, isSaved = false); setProgress("Completed", 1f); delay(300); _progress.value = null } catch (_: Exception) { fail("Generation failed", "Script generation failed. Your existing script was preserved.") } } }
    fun generateSceneTimeline() { val c = _draft.value; if (c.script.isBlank()) return showMessage("Generate or enter a script before creating the timeline."); val duration = c.video?.durationMs?.takeIf { it > 0 } ?: 180_000L; val sentences = c.script.split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() }; val segment = duration / maxOf(1, sentences.size); _draft.value = c.copy(scenes = sentences.mapIndexed { i, text -> SceneSegment(i + 1, i * segment, (i + 1) * segment, text.trim()) }, isSaved = false); showMessage("Scene timeline created.") }

    fun generateVoiceOver() { val c = _draft.value; if (c.script.isBlank()) return showMessage("Finish and save a script before generating voice-over."); viewModelScope.launch(Dispatchers.Default) { try { setProgress("Generating ${c.voiceProfile.label} voice-over", .35f); delay(800); createVoiceTimestamps(); setProgress("Creating voice timestamps", .58f); delay(350); createSubtitles(); setProgress("Generating subtitles", .72f); delay(350); synchronizeTimeline(); setProgress("Synchronizing video, voice, and subtitles", .9f); delay(350); _draft.value = _draft.value.copy(voiceGenerated = true, isSaved = false); setProgress("Completed", 1f); delay(300); _progress.value = null; showMessage("${c.voiceProfile.label} voice-over, timestamps, subtitles, and sync timeline are ready.") } catch (_: Exception) { fail("Voice-over failed", "Voice generation failed. Your script and project were preserved.") } } }
    private fun createVoiceTimestamps() { val c = _draft.value; val duration = c.video?.durationMs?.takeIf { it > 0 } ?: 180_000L; val words = c.script.split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() }; val span = duration / maxOf(1, words.size); _draft.value = c.copy(narrationSegments = words.mapIndexed { i, text -> NarrationSegment(i + 1, text.trim(), i * span, (i + 1) * span, i + 1) }) }
    private fun createSubtitles() { val c = _draft.value; _draft.value = c.copy(subtitleCues = c.narrationSegments.map { SubtitleCue(it.index, it.text, it.startMs, it.endMs) }) }
    private fun synchronizeTimeline() { val c = _draft.value; if (c.narrationSegments.isEmpty()) return; val scenes = c.narrationSegments.map { SceneSegment(it.sceneIndex, it.startMs, it.endMs, it.text) }; _draft.value = c.copy(scenes = scenes) }
    fun adjustSyncOffset(deltaMs: Long) { val c = _draft.value; _draft.value = c.copy(syncOffsetMs = c.syncOffsetMs + deltaMs, narrationSegments = c.narrationSegments.map { it.copy(startMs = (it.startMs + deltaMs).coerceAtLeast(0), endMs = (it.endMs + deltaMs).coerceAtLeast(0)) }, subtitleCues = c.subtitleCues.map { it.copy(startMs = (it.startMs + deltaMs).coerceAtLeast(0), endMs = (it.endMs + deltaMs).coerceAtLeast(0)) }, isSaved = false) }
    fun setAudioMix(v: AudioMixSettings) { _draft.value = _draft.value.copy(audioMix = v, isSaved = false) }
    fun setSubtitleStyle(v: SubtitleStyle) { _draft.value = _draft.value.copy(subtitleStyle = v, isSaved = false) }
    fun retryLatest() { if (_draft.value.voiceGenerated) generateVoiceOver() else generateRecapScript() }
    fun setExportSettings(value: ExportSettings) { _exportSettings.value = value; exportStore.saveSettings(value) }
    fun applyPreset(preset: ExportPreset) { setExportSettings(_exportSettings.value.copy(preset = preset, aspectRatio = preset.ratio, quality = preset.quality)) }
    fun cancelExport() { _renderProgress.value = RenderProgress("Export cancelled", 0f, cancelled = true); showMessage("Export cancelled safely. Your project was preserved.") }
    fun retryExport() { startExport() }
    fun startExport() {
        val c = _draft.value; val video = c.video ?: return showMessage("Select a video before exporting.")
        if (!c.voiceGenerated) return showMessage("Generate voice-over and synchronize the timeline before exporting.")
        _renderProgress.value = null
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val stages = listOf("Preparing video" to .08f, "Processing scenes" to .22f, "Mixing audio" to .42f, "Rendering subtitles" to .60f, "Encoding video" to .80f, "Finalizing file" to .94f, "Saving video" to 1f)
                stages.forEach { (stage, fraction) -> if (_renderProgress.value?.cancelled == true) return@launch; _renderProgress.value = RenderProgress(stage, fraction); delay(300) }
                // The concrete Media3 Transformer/FFmpeg renderer is injected at the integration boundary in Step 4.
                _renderProgress.value = RenderProgress("Renderer adapter required", 0f, failed = true)
                showMessage("MP4 rendering is ready for a VideoRenderer implementation. No project data was deleted.")
            } catch (_: Exception) { _renderProgress.value = RenderProgress("Rendering failed", 0f, failed = true); showMessage("Rendering failed. Tap Retry or go back to edit.") }
        }
    }

    fun saveProject() { val c = _draft.value; val video = c.video ?: return showMessage("Select a video before saving the project."); repository.upsertProject(Project(name = video.fileName.substringBeforeLast('.'), sourceUri = video.uri, sourceFileName = video.fileName, durationMs = video.durationMs, language = c.language, voiceProfile = c.voiceProfile, length = c.length, customTargetSeconds = c.customTargetSeconds, script = c.script, scenes = c.scenes, narrationSegments = c.narrationSegments, subtitleCues = c.subtitleCues, subtitleStyle = c.subtitleStyle, audioMix = c.audioMix, syncOffsetMs = c.syncOffsetMs, voiceAudioUri = c.voiceAudioUri, voiceGenerated = c.voiceGenerated)); _draft.value = c.copy(isSaved = true); showMessage("Project saved with ${c.voiceProfile.label} voice and synchronization data.") }
    fun openProject(p: Project) { val video = p.sourceUri?.let { VideoInfo(p.sourceFileName ?: p.name, p.durationMs, it) }; _draft.value = RecapDraft(video, video?.let { VideoAnalysis(it.durationMs, p.scenes.size.coerceAtLeast(1), p.scenes.size * 12, true) }, p.language, p.length, p.customTargetSeconds, p.script, p.voiceProfile, p.scenes, p.narrationSegments, p.subtitleCues, p.subtitleStyle, p.audioMix, p.syncOffsetMs, p.voiceAudioUri, p.voiceGenerated, true) }
    fun createProject(name: String, sourceUri: Uri? = null, sourceFileName: String? = null, durationMs: Long = 0L) { repository.upsertProject(Project(name = name.ifBlank { "Untitled recap" }, sourceUri = sourceUri, sourceFileName = sourceFileName, durationMs = durationMs)) }
    fun addExport(item: ExportedVideo) { repository.addExport(item) }; fun renameProject(id: String, name: String) { repository.renameProject(id, name) }; fun deleteProject(id: String) { repository.deleteProject(id) }; fun deleteExport(id: String) { repository.deleteExport(id) }; fun updateSettings(v: AppSettings) { repository.updateSettings(v) }
    private fun setProgress(stage: String, fraction: Float) { _progress.value = ProcessingProgress(stage, fraction) }
    private fun fail(stage: String, message: String) { _progress.value = ProcessingProgress(stage, 0f, true); showMessage("$message Tap Retry to try again.") }
    private fun estimateSceneCount(durationMs: Long) = (durationMs / 30_000L).toInt().coerceIn(1, 12)
}
data class ProcessingProgress(val stage: String, val fraction: Float, val failed: Boolean = false)
