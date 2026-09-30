package com.movierecap.ai.data

import android.content.Context
import com.movierecap.ai.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppRepository(context: Context) {
    private val store = ProjectStore(context.applicationContext)
    private val exportStore = ExportStore(context.applicationContext)
    private val _projects = MutableStateFlow(store.load().ifEmpty { listOf(Project(name = "The Last Signal", sourceFileName = "last_signal.mp4", durationMs = 7 * 60_000L), Project(name = "Untitled recap", sourceFileName = "city_lights.mov", durationMs = 3 * 60_000L)) })
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()
    private val _exports = MutableStateFlow(exportStore.load())
    val exports: StateFlow<List<ExportedVideo>> = _exports.asStateFlow()
    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()
    fun upsertProject(project: Project) { val next = listOf(project) + _projects.value.filterNot { it.id == project.id }; _projects.value = next; store.save(next) }
    fun deleteProject(id: String) { val next = _projects.value.filterNot { it.id == id }; _projects.value = next; store.save(next) }
    fun renameProject(id: String, name: String) { val next = _projects.value.map { if (it.id == id) it.copy(name = name, updatedAt = System.currentTimeMillis()) else it }; _projects.value = next; store.save(next) }
    fun updateProject(project: Project) { upsertProject(project.copy(updatedAt = System.currentTimeMillis())) }
    fun addExport(export: ExportedVideo) { val next = listOf(export) + _exports.value; _exports.value = next; exportStore.save(next) }
    fun deleteExport(id: String) { val next = _exports.value.filterNot { it.id == id }; _exports.value = next; exportStore.save(next) }
    fun updateSettings(value: AppSettings) { _settings.value = value }
}
