package com.movierecap.ai.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.movierecap.ai.data.AppViewModel
import com.movierecap.ai.domain.*
import java.text.SimpleDateFormat
import java.util.*

private enum class Route { HOME, CREATE, PROJECTS, EXPORTS, EXPORT, SETTINGS }

@Composable fun MovieRecapApp(model: AppViewModel) {
    var route by remember { mutableStateOf(Route.HOME) }
    val projects by model.projects.collectAsStateWithLifecycle()
    val exports by model.exports.collectAsStateWithLifecycle()
    val settings by model.settings.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val message by model.message.collectAsStateWithLifecycle()
    LaunchedEffect(message) { message?.let { snackbar.showSnackbar(it); model.clearMessage() } }
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, bottomBar = {
        NavigationBar { listOf(Route.HOME to Icons.Default.Home, Route.PROJECTS to Icons.Default.Folder, Route.EXPORTS to Icons.Default.VideoLibrary, Route.SETTINGS to Icons.Default.Settings).forEach { (r, icon) -> NavigationBarItem(selected = route == r, onClick = { route = r }, icon = { Icon(icon, null) }, label = { Text(r.name.lowercase().replaceFirstChar { it.uppercase() }) }) } }
    }) { padding ->
        Box(Modifier.padding(padding)) {
            when (route) {
                Route.HOME -> HomeScreen({ route = Route.CREATE }, { route = Route.PROJECTS }, { route = Route.EXPORTS }, { route = Route.SETTINGS })
                Route.CREATE -> CreateRecapScreen(model, onBack = { route = Route.HOME }, onContinue = { model.saveProject(); route = Route.EXPORT })
                Route.PROJECTS -> ProjectsScreen(projects, model, onBack = { route = Route.HOME }, onCreate = { route = Route.CREATE })
                Route.EXPORTS -> ExportsScreen(exports, model, onBack = { route = Route.HOME })
                Route.EXPORT -> ExportScreen(model, onBack = { route = Route.CREATE }, onDone = { route = Route.EXPORTS })
                Route.SETTINGS -> SettingsScreen(settings, model, onBack = { route = Route.HOME })
            }
        }
    }
}

@Composable private fun Header(title: String, onBack: (() -> Unit)? = null) { Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) { if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }; Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) } }
@Composable private fun PrimaryButton(text: String, icon: ImageVector? = null, enabled: Boolean = true, onClick: () -> Unit) { Button(onClick, Modifier.fillMaxWidth().height(56.dp), enabled = enabled, shape = RoundedCornerShape(18.dp)) { if (icon != null) { Icon(icon, null); Spacer(Modifier.width(10.dp)) }; Text(text, fontWeight = FontWeight.SemiBold) } }
@Composable private fun SecondaryButton(text: String, icon: ImageVector? = null, onClick: () -> Unit) { OutlinedButton(onClick, Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) { if (icon != null) { Icon(icon, null); Spacer(Modifier.width(10.dp)) }; Text(text) } }

@Composable private fun HomeScreen(onCreate: () -> Unit, onProjects: () -> Unit, onExports: () -> Unit, onSettings: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(top = 36.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Movie Recap AI", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
        Text("Turn your videos into engaging recaps.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(24.dp)) { Column(Modifier.padding(22.dp)) { Text("Ready to create?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("Select a video you own and start your first project.", Modifier.padding(top = 6.dp, bottom = 18.dp)); PrimaryButton("Create New Recap", Icons.Default.Add, onClick = onCreate) } }
        SecondaryButton("My Projects", Icons.Default.Folder, onProjects)
        SecondaryButton("Exported Videos", Icons.Default.VideoLibrary, onExports)
        SecondaryButton("Settings", Icons.Default.Settings, onSettings)
        Spacer(Modifier.weight(1f)); Text("Step 4 • Export foundation", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun CreateRecapScreen(model: AppViewModel, onBack: () -> Unit, onContinue: () -> Unit) {
    val context = LocalContext.current
    val draft by model.draft.collectAsStateWithLifecycle()
    val progress by model.progress.collectAsStateWithLifecycle()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? -> uri?.let { runCatching { context.contentResolver.takePersistableUriPermission(it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }; model.selectVideo(it, context.contentResolver, 0L) } }
    var customSeconds by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        Header("Create New Recap", onBack)
        LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Text("1. Select and analyze", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("Use only videos you own or have permission to process.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            item { if (draft.video == null) Box(Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Movie, null, Modifier.size(48.dp)); Text("No video selected", Modifier.padding(top = 8.dp)) } } else VideoPreview(draft.video.uri) }
            item { PrimaryButton("Select Video", Icons.Default.FileOpen) { launcher.launch(arrayOf("video/*")) } }
            draft.video?.let { info ->
                item { VideoInfoCard(info, draft.analysis) }
                item { PrimaryButton("Analyze Video", Icons.Default.Analytics, enabled = progress == null) { model.analyzeVideo() } }
            }
            progress?.let { item { ProgressCard(it) { if (it.stage.contains("Analysis")) model.analyzeVideo() else if (it.stage.contains("Voice") || it.stage.contains("Synchron")) model.generateVoiceOver() else model.generateRecapScript() } } }
            item { Text("2. Choose recap format", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            item { SettingDropdown("Narration language", draft.language, AppLanguage.entries.toList(), model::setLanguage) }
            item { SettingDropdown("Recap voice", draft.voiceProfile, VoiceProfile.entries.toList(), model::setVoiceProfile) }
            item { Text(draft.voiceProfile.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            item { SettingDropdown("Recap length", draft.length, RecapLength.entries.toList(), model::setLength) }
            if (draft.length == RecapLength.CUSTOM) item { OutlinedTextField(customSeconds, { customSeconds = it.filter(Char::isDigit); model.setCustomTarget(customSeconds.toIntOrNull()) }, Modifier.fillMaxWidth(), label = { Text("Target duration in seconds") }, singleLine = true) }
            item { PrimaryButton("Generate Recap Script", Icons.Default.AutoAwesome, enabled = draft.video != null && progress == null) { model.generateRecapScript() } }
            if (draft.script.isNotBlank()) {
                item { Text("3. Edit your script", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                item { OutlinedTextField(draft.script, model::updateScript, Modifier.fillMaxWidth().heightIn(min = 220.dp), label = { Text("Recap narration") }) }
                item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) { OutlinedButton({ model.generateRecapScript() }, Modifier.weight(1f), enabled = progress == null) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(6.dp)); Text("Regenerate") }; OutlinedButton({ model.showMessage("Script copied when clipboard integration is added.") }, Modifier.weight(1f)) { Icon(Icons.Default.ContentCopy, null); Spacer(Modifier.width(6.dp)); Text("Copy") } } }
                item { PrimaryButton("Generate Scene Timeline", Icons.Default.Timeline) { model.generateSceneTimeline() } }
            }
            if (draft.scenes.isNotEmpty()) {
                item { Text("4. Scene timeline", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                items(draft.scenes, key = { it.index }) { SceneRow(it) }
                item { PrimaryButton("Generate Voice-over", Icons.Default.RecordVoiceOver, enabled = progress == null) { model.generateVoiceOver() } }
            }
            if (draft.voiceGenerated) {
                item { Text("5. Voice, subtitles, and synchronization", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                items(draft.narrationSegments, key = { it.index }) { NarrationRow(it) }
                item { SubtitleControls(draft.subtitleStyle, model) }
                item { AudioMixControls(draft.audioMix, model) }
                item { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) { Text("Manual sync offset: ${draft.syncOffsetMs} ms", fontWeight = FontWeight.SemiBold); Row { OutlinedButton({ model.adjustSyncOffset(-250) }) { Text("−250") }; Spacer(Modifier.width(8.dp)); OutlinedButton({ model.adjustSyncOffset(250) }) { Text("+250") } } } }
                item { SynchronizedPreview(draft.video?.uri, draft.subtitleStyle.enabled) }
                item { PrimaryButton("Save Project", Icons.Default.Save, enabled = !draft.isSaved, onClick = { model.saveProject() }) }
                item { SecondaryButton("Continue to Step 4 Export", Icons.Default.ArrowForward) { onContinue() } }
            } else if (draft.scenes.isNotEmpty()) {
                item { PrimaryButton(if (draft.isSaved) "Saved" else "Save Project", Icons.Default.Save, enabled = !draft.isSaved, onClick = onContinue) }
            }
            item { Text("AI providers are optional. Without a configured key, the app uses an offline draft generator and never crashes.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable private fun ProgressCard(progress: com.movierecap.ai.data.ProcessingProgress, onRetry: () -> Unit) { Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (progress.failed) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer)) { Column(Modifier.padding(16.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(progress.stage, fontWeight = FontWeight.Bold); Text(if (progress.failed) "Error" else "${(progress.fraction * 100).toInt()}%") }; if (!progress.failed) LinearProgressIndicator(progress.fraction, Modifier.fillMaxWidth().padding(top = 12.dp)) else OutlinedButton(onRetry, Modifier.padding(top = 10.dp)) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(6.dp)); Text("Retry") } } } }
@Composable private fun VideoPreview(uri: Uri) { val context = LocalContext.current; val player = remember(uri) { ExoPlayer.Builder(context).build().apply { setMediaItem(MediaItem.fromUri(uri)); prepare(); playWhenReady = false } }; DisposableEffect(player) { onDispose { player.release() } }; AndroidView({ PlayerView(it).apply { this.player = player; useController = true } }, Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(24.dp))) }
@Composable private fun VideoInfoCard(info: VideoInfo, analysis: VideoAnalysis?) { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) { Text(info.fileName, fontWeight = FontWeight.Bold); Text(if (info.durationMs > 0) formatDuration(info.durationMs) else "Duration unavailable", color = MaterialTheme.colorScheme.onSurfaceVariant); analysis?.let { Text("${it.sceneCount} scenes • ${it.frameCount} sampled frames • audio ${if (it.hasAudio) "available" else "not detected"}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp)) } } } }
@Composable private fun SceneRow(scene: SceneSegment) { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("Scene ${scene.index}  •  ${formatDuration(scene.startMs)} — ${formatDuration(scene.endMs)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary); Text(scene.narration, Modifier.padding(top = 8.dp)) } } }
@Composable private fun NarrationRow(segment: NarrationSegment) { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("Narration ${segment.index}  •  Scene ${segment.sceneIndex}  •  ${formatDuration(segment.startMs)} — ${formatDuration(segment.endMs)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary); Text(segment.text, Modifier.padding(top = 8.dp)); Text("Duration ${formatDuration(segment.durationMs)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
@Composable private fun SubtitleControls(style: SubtitleStyle, model: AppViewModel) { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Text("Automatic subtitles", Modifier.weight(1f), fontWeight = FontWeight.SemiBold); Switch(style.enabled, { model.setSubtitleStyle(style.copy(enabled = it)) }) }; Text("Font size: ${style.fontSizeSp}sp"); Slider(style.fontSizeSp.toFloat(), { model.setSubtitleStyle(style.copy(fontSizeSp = it.toInt())) }, valueRange = 12f..48f); SettingDropdown("Subtitle position", style.position, SubtitlePosition.entries.toList()) { model.setSubtitleStyle(style.copy(position = it)) }; Row(verticalAlignment = Alignment.CenterVertically) { Text("Outlined appearance", Modifier.weight(1f)); Checkbox(style.outlined, { model.setSubtitleStyle(style.copy(outlined = it)) }) } } } }
@Composable private fun AudioMixControls(mix: AudioMixSettings, model: AppViewModel) { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text("Audio mix", fontWeight = FontWeight.Bold); Row(verticalAlignment = Alignment.CenterVertically) { Text("Background music", Modifier.weight(1f)); Switch(mix.musicEnabled, { model.setAudioMix(mix.copy(musicEnabled = it)) }) }; Text("Narration volume ${(mix.narrationVolume * 100).toInt()}%"); Slider(mix.narrationVolume, { model.setAudioMix(mix.copy(narrationVolume = it)) }); Text("Music volume ${(mix.musicVolume * 100).toInt()}%"); Slider(mix.musicVolume, { model.setAudioMix(mix.copy(musicVolume = it)) }); Row(verticalAlignment = Alignment.CenterVertically) { Text("Original video audio", Modifier.weight(1f)); Switch(mix.originalAudioEnabled, { model.setAudioMix(mix.copy(originalAudioEnabled = it)) }) }; if (mix.musicEnabled) Row(verticalAlignment = Alignment.CenterVertically) { Text("Duck music under narration", Modifier.weight(1f)); Switch(mix.duckMusicDuringNarration, { model.setAudioMix(mix.copy(duckMusicDuringNarration = it)) }) } } } }
@Composable private fun SynchronizedPreview(uri: Uri?, subtitlesEnabled: Boolean) { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) { Text("Synchronized preview", fontWeight = FontWeight.Bold); if (uri != null) VideoPreview(uri); Text(if (subtitlesEnabled) "Video + narration timing + subtitles prepared" else "Video + narration timing prepared; subtitles disabled", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
@Composable private fun ExportScreen(model: AppViewModel, onBack: () -> Unit, onDone: () -> Unit) {
    val draft by model.draft.collectAsStateWithLifecycle(); val settings by model.exportSettings.collectAsStateWithLifecycle(); val progress by model.renderProgress.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) { Header("Export MP4", onBack); LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("Final preview", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        item { if (draft.video != null) SynchronizedPreview(draft.video.uri, settings.subtitlesEnabled) else EmptyState("No video selected", "Go back to edit and select a video.") }
        item { Text("Export presets", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        items(ExportPreset.entries.toList()) { preset -> OutlinedButton({ model.applyPreset(preset) }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text("${preset.label} • ${preset.ratio.value} • ${preset.quality.label}") } }
        item { SettingDropdown("Aspect ratio", settings.aspectRatio, AspectRatio.entries.toList()) { model.setExportSettings(settings.copy(aspectRatio = it, preset = null)) } }
        item { SettingDropdown("Quality", settings.quality, ExportQuality.entries.toList()) { model.setExportSettings(settings.copy(quality = it, preset = null)) } }
        item { SettingDropdown("Resolution", settings.resolution, ExportResolution.entries.toList()) { model.setExportSettings(settings.copy(resolution = it)) } }
        item { Text("Selected output: ${settings.aspectRatio.value} • ${settings.quality.label} • ${settings.resolution.label}", fontWeight = FontWeight.SemiBold) }
        item { Text("Subtitle track", fontWeight = FontWeight.SemiBold); SwitchRow("Include subtitles", settings.subtitlesEnabled) { model.setExportSettings(settings.copy(subtitlesEnabled = it)) } }
        item { Text("Audio", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        item { VolumeControl("Narration", settings.narrationVolume) { model.setExportSettings(settings.copy(narrationVolume = it)) } }
        item { VolumeControl("Background music", settings.musicVolume) { model.setExportSettings(settings.copy(musicVolume = it)) } }
        item { VolumeControl("Original audio", settings.originalAudioVolume) { model.setExportSettings(settings.copy(originalAudioVolume = it)) } }
        item { Text("Estimated duration: ${formatDuration(draft.video?.durationMs ?: 0L)}", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        progress?.let { item { RenderProgressCard(it, { model.retryExport() }, { model.cancelExport() }) } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) { OutlinedButton(onBack, Modifier.weight(1f)) { Text("Back to Edit") }; Button({ model.startExport() }, Modifier.weight(1f), enabled = progress == null || progress.failed) { Icon(Icons.Default.FileUpload, null); Spacer(Modifier.width(6.dp)); Text("Export MP4") } } }
        item { Text("The renderer uses the synchronized timeline and is designed to save through MediaStore Movies/Movie Recap AI after a concrete VideoRenderer is connected.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    } }
}
@Composable private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(label, Modifier.weight(1f)); Switch(checked, onChange) } }
@Composable private fun VolumeControl(label: String, value: Float, onChange: (Float) -> Unit) { Text("$label ${(value * 100).toInt()}%", fontWeight = FontWeight.SemiBold); Slider(value, onChange) }
@Composable private fun RenderProgressCard(progress: com.movierecap.ai.domain.RenderProgress, onRetry: () -> Unit, onCancel: () -> Unit) { Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (progress.failed) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer)) { Column(Modifier.padding(16.dp)) { Text(progress.stage, fontWeight = FontWeight.Bold); if (!progress.failed && !progress.cancelled) { LinearProgressIndicator(progress.fraction, Modifier.fillMaxWidth().padding(top = 10.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("${(progress.fraction * 100).toInt()}%", style = MaterialTheme.typography.labelSmall); TextButton(onCancel) { Text("Cancel") } } } else if (progress.failed) { Text("Your project was preserved."); OutlinedButton(onRetry) { Text("Retry") } } else Text("Temporary files will be cleaned safely.") } } }

@Composable private fun ProjectsScreen(projects: List<Project>, model: AppViewModel, onBack: () -> Unit, onCreate: () -> Unit) { var rename by remember { mutableStateOf<Project?>(null) }; var name by remember { mutableStateOf("") }; Column(Modifier.fillMaxSize()) { Header("My Projects", onBack); LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { item { PrimaryButton("Create Project", Icons.Default.Add, onClick = onCreate) }; if (projects.isEmpty()) item { EmptyState("No projects yet", "Create a recap to see it here.") }; items(projects, key = { it.id }) { project -> ProjectRow(project, { model.openProject(project); onCreate() }, { rename = project; name = project.name }, { model.deleteProject(project.id) }) } } }; rename?.let { project -> AlertDialog(onDismissRequest = { rename = null }, title = { Text("Rename project") }, text = { OutlinedTextField(name, { name = it }, singleLine = true, label = { Text("Project name") }) }, confirmButton = { TextButton({ model.renameProject(project.id, name); rename = null }) { Text("Save") } }, dismissButton = { TextButton({ rename = null }) { Text("Cancel") } }) } }
@Composable private fun ProjectRow(project: Project, onOpen: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit) { Card(Modifier.fillMaxWidth()) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onOpen) { Icon(Icons.Default.PlayArrow, "Open project") }; Column(Modifier.weight(1f).padding(horizontal = 6.dp)) { Text(project.name, fontWeight = FontWeight.Bold); Text(project.sourceFileName ?: "No source video yet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); if (project.script.isNotBlank()) Text("Script saved • ${project.scenes.size} scenes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }; IconButton(onRename) { Icon(Icons.Default.Edit, "Rename") }; IconButton(onDelete) { Icon(Icons.Default.DeleteOutline, "Delete") } } } }

@Composable private fun ExportsScreen(exports: List<ExportedVideo>, model: AppViewModel, onBack: () -> Unit) { Column(Modifier.fillMaxSize()) { Header("Exported Videos", onBack); LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { if (exports.isEmpty()) item { EmptyState("No exported videos", "Your finished MP4 videos will appear here.") }; items(exports, key = { it.id }) { ExportRow(it, { model.showMessage("Playback is ready when an exported URI is available.") }, { model.showMessage("Share is ready for the exported MP4.") }, { model.deleteExport(it.id) }) } } } }
@Composable private fun ExportRow(video: ExportedVideo, onPlay: () -> Unit, onShare: () -> Unit, onDelete: () -> Unit) { Card(Modifier.fillMaxWidth()) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(68.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) { Icon(Icons.Default.PlayArrow, "Play") }; Column(Modifier.weight(1f).padding(horizontal = 12.dp)) { Text(video.fileName, fontWeight = FontWeight.Bold); Text("${formatDuration(video.durationMs)} • ${video.resolution.label} • ${video.aspectRatio.value}", style = MaterialTheme.typography.bodySmall); Text("${video.quality.label} • ${SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(video.exportedAt))}${if (video.savedToGallery) " • Saved" else ""}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; IconButton(onPlay) { Icon(Icons.Default.PlayCircle, "Play") }; IconButton(onShare) { Icon(Icons.Default.Share, "Share") }; IconButton(onDelete) { Icon(Icons.Default.DeleteOutline, "Delete") } } } }

@Composable private fun SettingsScreen(settings: AppSettings, model: AppViewModel, onBack: () -> Unit) { Column(Modifier.fillMaxSize()) { Header("Settings", onBack); LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { item { SettingDropdown("Default language", settings.defaultLanguage, AppLanguage.entries.toList()) { model.updateSettings(settings.copy(defaultLanguage = it)) } }; item { SettingDropdown("Voice language", settings.voiceLanguage, AppLanguage.entries.toList()) { model.updateSettings(settings.copy(voiceLanguage = it)) } }; item { SettingDropdown("Recap voice", settings.voiceProfile, VoiceProfile.entries.toList()) { model.updateSettings(settings.copy(voiceProfile = it)); model.showMessage("Default recap voice set to ${it.label}.") } }; item { Text(settings.voiceProfile.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; item { SettingDropdown("Subtitle language", settings.subtitleLanguage, AppLanguage.entries.toList()) { model.updateSettings(settings.copy(subtitleLanguage = it)) } }; item { SettingDropdown("Video quality", settings.quality, listOf("720p", "1080p", "4K")) { model.updateSettings(settings.copy(quality = it)) } }; item { SettingDropdown("Aspect ratio", settings.aspectRatio, AspectRatio.entries.toList()) { model.updateSettings(settings.copy(aspectRatio = it)) } }; item { SettingDropdown("AI provider", settings.aiProvider, AiProvider.entries.toList()) { model.updateSettings(settings.copy(aiProvider = it)) } }; item { Text("Background music volume", fontWeight = FontWeight.SemiBold); Slider(settings.musicVolume, { model.updateSettings(settings.copy(musicVolume = it)) }); Text("${(settings.musicVolume * 100).toInt()}%", style = MaterialTheme.typography.labelMedium) }; item { Card { Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) { Text("Dark theme", Modifier.weight(1f), fontWeight = FontWeight.SemiBold); Switch(settings.darkTheme, { model.updateSettings(settings.copy(darkTheme = it)) }) } } }; item { Text("API keys are never stored here. Configure OPENAI_API_KEY or GEMINI_API_KEY on your secure backend/developer environment. AI providers may incur charges; the offline draft generator remains available without a key.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } } }

@Composable private fun <T> SettingDropdown(label: String, selected: T, options: List<T>, onSelected: (T) -> Unit) { var open by remember { mutableStateOf(false) }; Box { OutlinedButton(onClick = { open = true }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) { Text(label, style = MaterialTheme.typography.labelMedium); Text(selected.toString(), fontWeight = FontWeight.SemiBold) } }; DropdownMenu(open, { open = false }) { options.forEach { option -> DropdownMenuItem({ Text(option.toString()) }, { onSelected(option); open = false }) } } } }
@Composable private fun EmptyState(title: String, message: String) { Column(Modifier.fillMaxWidth().padding(vertical = 60.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.VideoLibrary, null, Modifier.size(52.dp), tint = MaterialTheme.colorScheme.outline); Text(title, Modifier.padding(top = 14.dp), fontWeight = FontWeight.Bold); Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
private fun formatDuration(ms: Long): String { val total = ms / 1000; return "%02d:%02d".format(total / 60, total % 60) }
