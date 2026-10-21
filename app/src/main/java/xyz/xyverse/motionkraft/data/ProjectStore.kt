package xyz.xyverse.motionkraft.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import xyz.xyverse.motionkraft.data.model.MographProject
import java.io.File

data class ProjectSummary(val id: String, val name: String, val modifiedAt: Long, val durationUs: Long)

/** Saves each project as one JSON file under filesDir/projects. */
class ProjectStore(context: Context) {

    private val dir = File(context.filesDir, "projects").apply { mkdirs() }
    private val gson: Gson = GsonBuilder().create()

    fun save(project: MographProject) {
        val target = File(dir, "${project.id}.json")
        val tmp = File(dir, "${project.id}.json.tmp")
        tmp.writeText(gson.toJson(project))
        if (!tmp.renameTo(target)) {
            target.writeText(tmp.readText())
            tmp.delete()
        }
    }

    fun load(id: String): MographProject? {
        val file = File(dir, "$id.json")
        if (!file.exists()) return null
        return runCatching { gson.fromJson(file.readText(), MographProject::class.java) }.getOrNull()
    }

    fun list(): List<ProjectSummary> =
        (dir.listFiles { f -> f.name.endsWith(".json") }?.toList() ?: emptyList())
            .mapNotNull { runCatching { gson.fromJson(it.readText(), MographProject::class.java) }.getOrNull() }
            .map { ProjectSummary(it.id, it.name, it.modifiedAt, it.timeline.durationUs) }
            .sortedByDescending { it.modifiedAt }

    fun delete(id: String) {
        File(dir, "$id.json").delete()
    }
}
