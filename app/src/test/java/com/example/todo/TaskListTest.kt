package com.example.todo

import com.example.todo.data.Task
import com.example.todo.data.TaskFilter
import com.example.todo.data.TaskList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskListTest {

    private val a = Task(id = "a", title = "Old open", createdAt = 100)
    private val b = Task(id = "b", title = "New open", createdAt = 300)
    private val c = Task(id = "c", title = "Done early", createdAt = 50, isCompleted = true, completedAt = 400)
    private val d = Task(id = "d", title = "Done late", createdAt = 60, isCompleted = true, completedAt = 500)

    @Test
    fun openTasksComeFirstNewestOnTop_thenMostRecentlyCompleted() {
        val result = TaskList.sorted(listOf(a, c, b, d)).map { it.id }
        assertEquals(listOf("b", "a", "d", "c"), result)
    }

    @Test
    fun filtersSelectTheRightTasks() {
        val all = listOf(a, b, c, d)
        assertEquals(4, TaskList.visible(all, TaskFilter.ALL).size)
        assertEquals(listOf("b", "a"), TaskList.visible(all, TaskFilter.ACTIVE).map { it.id })
        assertEquals(listOf("d", "c"), TaskList.visible(all, TaskFilter.DONE).map { it.id })
    }

    @Test
    fun completingATaskStampsTimeAndUncompletingClearsIt() {
        val done = TaskList.setCompleted(listOf(a, b), "a", true, now = 999)
        assertTrue(done.first { it.id == "a" }.isCompleted)
        assertEquals(999L, done.first { it.id == "a" }.completedAt)
        assertEquals(b, done.first { it.id == "b" })

        val undone = TaskList.setCompleted(done, "a", false, now = 1000)
        assertNull(undone.first { it.id == "a" }.completedAt)
    }

    @Test
    fun renameOnlyChangesTheTargetTask() {
        val result = TaskList.rename(listOf(a, b), "b", "Renamed")
        assertEquals("Renamed", result.first { it.id == "b" }.title)
        assertEquals(a, result.first { it.id == "a" })
    }
}
