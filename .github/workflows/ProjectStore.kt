package com.movierecap.ai.data

import android.content.Context
import com.movierecap.ai.domain.*
import org.json.JSONArray
import org.json.JSONObject

class ProjectStore(context: Context) {
    private val prefs = context.getSharedPreferences("movie_recap_projects", Context.MODE_PRIVATE)
    private val key = "projects"
    fun load(): List<Project> = runCatching { val array = JSONArray(prefs.getString(key, "[]")); buildList { for (i in 0 until array.length()) add(fromJson(array.getJSONObject(i))) } }.getOrDefault(emptyList())
    fun save(projects: List<Project>) { val array = JSONArray(); projects.forEach { array.put(toJson(it)) }; prefs.edit().putString(key, array.toString()).apply() }

    private fun toJson(p: Project) = JSONObject().apply {
        put("id", p.id); put("name", p.name); put("uri", p.sourceUri?.toString()); put("file", p.sourceFileName); put("duration", p.durationMs); put("updated", p.updatedAt)
        put("complete", p.isComplete); put("language", p.language.name); put("voiceProfile", p.voiceProfile.name); put("length", p.length.name); put("customSeconds", p.customTargetSeconds ?: JSONObject.NULL); put("script", p.script)
        put("scenes", JSONArray().apply { p.scenes.forEach { put(JSONObject().apply { put("index", it.index); put("start", it.startMs); put("end", it.endMs); put("narration", it.narration) }) } })
        put("narration", JSONArray().apply { p.narrationSegments.forEach { put(JSONObject().apply { put("index", it.index); put("text", it.text); put("start", it.startMs); put("end", it.endMs); put("scene", it.sceneIndex); put("audio", it.audioUri?.toString()) }) } })
        put("subtitles", JSONArray().apply { p.subtitleCues.forEach { put(JSONObject().apply { put("index", it.index); put("text", it.text); put("start", it.startMs); put("end", it.endMs) }) } })
        put("subtitleEnabled", p.subtitleStyle.enabled); put("subtitleFont", p.subtitleStyle.fontSizeSp); put("subtitlePosition", p.subtitleStyle.position.name); put("subtitleOutline", p.subtitleStyle.outlined)
        put("musicEnabled", p.audioMix.musicEnabled); put("narrationVolume", p.audioMix.narrationVolume); put("musicVolume", p.audioMix.musicVolume); put("originalEnabled", p.audioMix.originalAudioEnabled); put("originalVolume", p.audioMix.originalAudioVolume); put("duckMusic", p.audioMix.duckMusicDuringNarration)
        put("syncOffset", p.syncOffsetMs); put("voiceAudio", p.voiceAudioUri?.toString()); put("voiceGenerated", p.voiceGenerated)
    }

    private fun fromJson(j: JSONObject): Project {
        val scenes = j.optJSONArray("scenes") ?: JSONArray(); val narration = j.optJSONArray("narration") ?: JSONArray(); val subtitles = j.optJSONArray("subtitles") ?: JSONArray()
        return Project(
            id = j.optString("id"), name = j.optString("name", "Untitled recap"), sourceUri = j.optString("uri").takeIf { it.isNotBlank() }?.let(android.net.Uri::parse), sourceFileName = j.optString("file").takeIf { it.isNotBlank() }, durationMs = j.optLong("duration"), updatedAt = j.optLong("updated", System.currentTimeMillis()), isComplete = j.optBoolean("complete"),
            language = runCatching { AppLanguage.valueOf(j.optString("language")) }.getOrDefault(AppLanguage.ENGLISH), voiceProfile = runCatching { VoiceProfile.valueOf(j.optString("voiceProfile")) }.getOrDefault(VoiceProfile.STORYTELLER_MALE), length = runCatching { RecapLength.valueOf(j.optString("length")) }.getOrDefault(RecapLength.MEDIUM), customTargetSeconds = if (j.isNull("customSeconds")) null else j.optInt("customSeconds"), script = j.optString("script"),
            scenes = buildList { for (i in 0 until scenes.length()) { val s = scenes.getJSONObject(i); add(SceneSegment(s.optInt("index"), s.optLong("start"), s.optLong("end"), s.optString("narration"))) } },
            narrationSegments = buildList { for (i in 0 until narration.length()) { val n = narration.getJSONObject(i); add(NarrationSegment(n.optInt("index"), n.optString("text"), n.optLong("start"), n.optLong("end"), n.optInt("scene"), n.optString("audio").takeIf { it.isNotBlank() }?.let(android.net.Uri::parse))) } },
            subtitleCues = buildList { for (i in 0 until subtitles.length()) { val s = subtitles.getJSONObject(i); add(SubtitleCue(s.optInt("index"), s.optString("text"), s.optLong("start"), s.optLong("end"))) } },
            subtitleStyle = SubtitleStyle(j.optBoolean("subtitleEnabled", true), j.optInt("subtitleFont", 22), runCatching { SubtitlePosition.valueOf(j.optString("subtitlePosition")) }.getOrDefault(SubtitlePosition.BOTTOM), j.optBoolean("subtitleOutline", true)),
            audioMix = AudioMixSettings(j.optBoolean("musicEnabled"), j.optDouble("narrationVolume", 1.0).toFloat(), j.optDouble("musicVolume", 0.2).toFloat(), j.optBoolean("originalEnabled"), j.optDouble("originalVolume", 0.15).toFloat(), j.optBoolean("duckMusic", true)),
            syncOffsetMs = j.optLong("syncOffset"), voiceAudioUri = j.optString("voiceAudio").takeIf { it.isNotBlank() }?.let(android.net.Uri::parse), voiceGenerated = j.optBoolean("voiceGenerated")
        )
    }
}
