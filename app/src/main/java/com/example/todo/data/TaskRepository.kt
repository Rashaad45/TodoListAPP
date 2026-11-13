package com.example.todo.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** Persists tasks on the device as JSON in SharedPreferences, so they survive app restarts. */
class TaskRepository(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): List<Task> {
        val raw = prefs.getString(KEY_TASKS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                Task(
                    id = o.getString("id"),
                    title = o.getString("title"),
                    createdAt = o.getLong("createdAt"),
                    isCompleted = o.optBoolean("completed", false),
                    completedAt = if (o.has("completedAt") && !o.isNull("completedAt")) {
                        o.getLong("completedAt")
                    } else {
                        null
                    },
                )
            }
        } catch (e: JSONException) {
            emptyList()
        }
    }

    fun save(tasks: List<Task>) {
        val array = JSONArray()
        tasks.forEach { t ->
            array.put(
                JSONObject().apply {
                    put("id", t.id)
                    put("title", t.title)
                    put("createdAt", t.createdAt)
                    put("completed", t.isCompleted)
                    t.completedAt?.let { put("completedAt", it) }
                }
            )
        }
        prefs.edit().putString(KEY_TASKS, array.toString()).apply()
    }

    private companion object {
        const val PREFS_NAME = "todo_tasks"
        const val KEY_TASKS = "tasks_v1"
    }
}
