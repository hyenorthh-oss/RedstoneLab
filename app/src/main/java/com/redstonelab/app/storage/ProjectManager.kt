package com.redstonelab.app.storage

import android.content.Context
import com.redstonelab.app.model.CircuitGrid
import org.json.JSONObject
import java.io.File

data class CircuitProjectMetadata(
    val id: String,
    val name: String,
    val modifiedAt: Long,
    val blockCount: Int
)

/**
 * 100% Offline Local Storage for RedstoneLab Projects using JSON in internal app storage.
 */
object ProjectManager {

    private fun getProjectsDir(context: Context): File {
        val dir = File(context.filesDir, "projects")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun listProjects(context: Context): List<CircuitProjectMetadata> {
        val dir = getProjectsDir(context)
        val files = dir.listFiles { f -> f.extension == "json" } ?: return emptyList()

        val list = mutableListOf<CircuitProjectMetadata>()
        for (file in files) {
            try {
                val content = file.readText()
                val json = JSONObject(content)
                val id = json.optString("id", file.nameWithoutExtension)
                val name = json.optString("name", file.nameWithoutExtension)
                val modifiedAt = json.optLong("modifiedAt", file.lastModified())
                val blocks = json.optJSONObject("grid")?.optJSONArray("blocks")?.length() ?: 0
                list.add(CircuitProjectMetadata(id, name, modifiedAt, blocks))
            } catch (_: Exception) {}
        }
        return list.sortedByDescending { it.modifiedAt }
    }

    fun saveProject(context: Context, id: String, name: String, grid: CircuitGrid) {
        val dir = getProjectsDir(context)
        val file = File(dir, "$id.json")
        val json = JSONObject()
        json.put("id", id)
        json.put("name", name)
        json.put("modifiedAt", System.currentTimeMillis())
        json.put("grid", JSONObject(grid.toJson()))
        file.writeText(json.toString(2))
    }

    fun loadProject(context: Context, id: String): Pair<String, CircuitGrid>? {
        val dir = getProjectsDir(context)
        val file = File(dir, "$id.json")
        if (!file.exists()) return null

        return try {
            val content = file.readText()
            val json = JSONObject(content)
            val name = json.optString("name", "Circuito")
            val gridJson = json.getJSONObject("grid").toString()
            val grid = CircuitGrid.fromJson(gridJson)
            Pair(name, grid)
        } catch (_: Exception) {
            null
        }
    }

    fun deleteProject(context: Context, id: String): Boolean {
        val dir = getProjectsDir(context)
        val file = File(dir, "$id.json")
        return if (file.exists()) file.delete() else false
    }
}
