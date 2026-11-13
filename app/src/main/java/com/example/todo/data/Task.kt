package com.example.todo.data

/** A single to-do item. Immutable — create changed copies with [copy]. */
data class Task(
    val id: String,
    val title: String,
    val createdAt: Long,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
)

enum class TaskFilter { ALL, ACTIVE, DONE }

/** Pure list logic, kept free of Android classes so it can be unit-tested on the JVM. */
object TaskList {

    /** Open tasks first (newest on top), then completed tasks (most recently finished on top). */
    fun sorted(tasks: List<Task>): List<Task> =
        tasks.sortedWith(
            compareBy<Task> { it.isCompleted }
                .thenByDescending { if (it.isCompleted) it.completedAt ?: it.createdAt else it.createdAt }
        )

    fun visible(tasks: List<Task>, filter: TaskFilter): List<Task> =
        sorted(tasks).filter {
            when (filter) {
                TaskFilter.ALL -> true
                TaskFilter.ACTIVE -> !it.isCompleted
                TaskFilter.DONE -> it.isCompleted
            }
        }

    fun setCompleted(tasks: List<Task>, id: String, completed: Boolean, now: Long): List<Task> =
        tasks.map {
            if (it.id == id && it.isCompleted != completed) {
                it.copy(isCompleted = completed, completedAt = if (completed) now else null)
            } else {
                it
            }
        }

    fun rename(tasks: List<Task>, id: String, title: String): List<Task> =
        tasks.map { if (it.id == id) it.copy(title = title) else it }
}
