package com.movierecap.ai.data

import android.content.Context
import com.movierecap.ai.domain.*
import org.json.JSONArray
import org.json.JSONObject

class ExportStore(context: Context) {
    private val prefs = context.getSharedPreferences("movie_recap_exports", Context.MODE_PRIVATE)
    fun load(): List<ExportedVideo> = runCatching { val a = JSONArray(prefs.getString("history", "[]")); buildList { for (i in 0 until a.length()) add(fromJson(a.getJSONObject(i))) } }.getOrDefault(emptyList())
    fun save(items: List<ExportedVideo>) { val a = JSONArray(); items.forEach { a.put(JSONObject().apply { put("id", it.id); put("name", it.fileName); put("uri", it.uri?.toString()); put("duration", it.durationMs); put("created", it.exportedAt); put("resolution", it.resolution.name); put("ratio", it.aspectRatio.name); put("quality", it.quality.name); put("saved", it.savedToGallery) }) }; prefs.edit().putString("history", a.toString()).apply() }
    fun loadSettings(): ExportSettings = runCatching { val j = JSONObject(prefs.getString("settings", "{}")); ExportSettings(runCatching { AspectRatio.valueOf(j.optString("ratio")) }.getOrDefault(AspectRatio.VERTICAL), runCatching { ExportQuality.valueOf(j.optString("quality")) }.getOrDefault(ExportQuality.HIGH), runCatching { ExportResolution.valueOf(j.optString("resolution")) }.getOrDefault(ExportResolution.AUTO), j.optDouble("narration", 1.0).toFloat(), j.optDouble("music", .2).toFloat(), j.optDouble("original", .15).toFloat(), j.optBoolean("subtitles", true)) }.getOrDefault(ExportSettings())
    fun saveSettings(s: ExportSettings) { prefs.edit().putString("settings", JSONObject().apply { put("ratio", s.aspectRatio.name); put("quality", s.quality.name); put("resolution", s.resolution.name); put("narration", s.narrationVolume); put("music", s.musicVolume); put("original", s.originalAudioVolume); put("subtitles", s.subtitlesEnabled) }.toString()).apply() }
    private fun fromJson(j: JSONObject) = ExportedVideo(j.optString("id"), j.optString("name"), j.optString("uri").takeIf { it.isNotBlank() }?.let(android.net.Uri::parse), j.optLong("duration"), j.optLong("created"), runCatching { ExportResolution.valueOf(j.optString("resolution")) }.getOrDefault(ExportResolution.AUTO), runCatching { AspectRatio.valueOf(j.optString("ratio")) }.getOrDefault(AspectRatio.VERTICAL), runCatching { ExportQuality.valueOf(j.optString("quality")) }.getOrDefault(ExportQuality.HIGH), j.optBoolean("saved"))
}
