package com.example.todo

import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.todo.data.Task
import com.example.todo.data.TaskFilter
import com.example.todo.data.TaskList
import com.example.todo.data.TaskRepository
import com.example.todo.databinding.ActivityMainBinding
import com.example.todo.databinding.DialogTaskInputBinding
import com.example.todo.ui.TaskAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.util.UUID
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: TaskRepository
    private lateinit var adapter: TaskAdapter

    private var tasks: List<Task> = emptyList()
    private var filter: TaskFilter = TaskFilter.ALL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        repository = TaskRepository(this)
        tasks = repository.load()
        filter = savedInstanceState?.getString(KEY_FILTER)
            ?.let { name -> TaskFilter.values().firstOrNull { it.name == name } }
            ?: TaskFilter.ALL

        setupList()
        setupFilters()
        binding.fabAdd.setOnClickListener { showTaskDialog(null) }

        render()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_FILTER, filter.name)
    }

    // ---------------------------------------------------------------- menu

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val hasDone = tasks.any { it.isCompleted }
        menu.findItem(R.id.action_clear_done)?.let {
            it.isEnabled = hasDone
            it.icon?.alpha = if (hasDone) 255 else 90
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_clear_done -> {
            confirmClearCompleted()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    // ---------------------------------------------------------------- setup

    private fun setupList() {
        adapter = TaskAdapter(
            onToggle = { task, checked -> toggleTask(task, checked) },
            onClick = { task -> showTaskDialog(task) },
        )
        binding.rvTasks.layoutManager = LinearLayoutManager(this)
        binding.rvTasks.adapter = adapter

        // Shrink the "New task" button while scrolling down, expand when scrolling up.
        binding.rvTasks.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0) binding.fabAdd.shrink() else if (dy < 0) binding.fabAdd.extend()
            }
        })

        ItemTouchHelper(SwipeToDeleteCallback()).attachToRecyclerView(binding.rvTasks)
    }

    private fun setupFilters() {
        val initialChip = when (filter) {
            TaskFilter.ALL -> R.id.chipAll
            TaskFilter.ACTIVE -> R.id.chipActive
            TaskFilter.DONE -> R.id.chipDone
        }
        binding.chipGroup.check(initialChip)
        binding.chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            filter = when (checkedIds.firstOrNull()) {
                R.id.chipActive -> TaskFilter.ACTIVE
                R.id.chipDone -> TaskFilter.DONE
                else -> TaskFilter.ALL
            }
            render()
            binding.rvTasks.scrollToPosition(0)
        }
    }

    // ---------------------------------------------------------------- state changes

    private fun updateTasks(newTasks: List<Task>) {
        tasks = newTasks
        repository.save(tasks)
        render()
    }

    private fun addTask(title: String) {
        val task = Task(
            id = UUID.randomUUID().toString(),
            title = title,
            createdAt = System.currentTimeMillis(),
        )
        updateTasks(tasks + task)
        // A new task is open, so make sure it is visible.
        if (filter == TaskFilter.DONE) binding.chipGroup.check(R.id.chipAll)
        binding.rvTasks.post { binding.rvTasks.smoothScrollToPosition(0) }
    }

    private fun toggleTask(task: Task, completed: Boolean) {
        updateTasks(TaskList.setCompleted(tasks, task.id, completed, System.currentTimeMillis()))
    }

    private fun renameTask(task: Task, title: String) {
        updateTasks(TaskList.rename(tasks, task.id, title))
    }

    private fun deleteTask(task: Task) {
        if (tasks.none { it.id == task.id }) return
        updateTasks(tasks.filterNot { it.id == task.id })
        Snackbar.make(binding.root, R.string.task_deleted, Snackbar.LENGTH_LONG)
            .setAnchorView(binding.fabAdd)
            .setAction(R.string.undo) {
                if (tasks.none { it.id == task.id }) updateTasks(tasks + task)
            }
            .show()
    }

    private fun confirmClearCompleted() {
        val done = tasks.filter { it.isCompleted }
        if (done.isEmpty()) return
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.clear_completed_title)
            .setMessage(resources.getQuantityString(R.plurals.clear_completed_message, done.size, done.size))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.clear) { _, _ ->
                updateTasks(tasks.filterNot { it.isCompleted })
                Snackbar.make(
                    binding.root,
                    resources.getQuantityString(R.plurals.tasks_cleared, done.size, done.size),
                    Snackbar.LENGTH_LONG,
                )
                    .setAnchorView(binding.fabAdd)
                    .setAction(R.string.undo) {
                        val existing = tasks.map { it.id }.toSet()
                        updateTasks(tasks + done.filterNot { it.id in existing })
                    }
                    .show()
            }
            .show()
    }

    // ---------------------------------------------------------------- rendering

    private fun render() {
        val visible = TaskList.visible(tasks, filter)
        adapter.submitList(visible)

        val total = tasks.size
        val done = tasks.count { it.isCompleted }
        val active = total - done

        binding.tvProgress.text = when {
            total == 0 -> getString(R.string.progress_empty)
            done == total -> getString(R.string.progress_all_done, total)
            else -> getString(R.string.progress_format, done, total)
        }
        binding.progressIndicator.max = maxOf(total, 1)
        binding.progressIndicator.setProgressCompat(done, true)

        binding.chipAll.text = getString(R.string.filter_all, total)
        binding.chipActive.text = getString(R.string.filter_active, active)
        binding.chipDone.text = getString(R.string.filter_done, done)

        binding.emptyState.isVisible = visible.isEmpty()
        if (visible.isEmpty()) {
            val (title, subtitle) = when {
                total == 0 -> R.string.empty_title_none to R.string.empty_subtitle_none
                filter == TaskFilter.ACTIVE -> R.string.empty_title_active to R.string.empty_subtitle_active
                else -> R.string.empty_title_done to R.string.empty_subtitle_done
            }
            binding.tvEmptyTitle.setText(title)
            binding.tvEmptySubtitle.setText(subtitle)
        }

        invalidateOptionsMenu()
    }

    // ---------------------------------------------------------------- add / edit dialog

    private fun showTaskDialog(existing: Task?) {
        val dialogBinding = DialogTaskInputBinding.inflate(layoutInflater)
        existing?.let {
            dialogBinding.etTask.setText(it.title)
            dialogBinding.etTask.setSelection(it.title.length)
        }

        val builder = MaterialAlertDialogBuilder(this)
            .setTitle(if (existing == null) R.string.new_task else R.string.edit_task)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(if (existing == null) R.string.add else R.string.save, null)
        if (existing != null) {
            builder.setNeutralButton(R.string.delete) { _, _ -> deleteTask(existing) }
        }
        val dialog = builder.create()

        fun submit() {
            val text = dialogBinding.etTask.text?.toString()?.trim().orEmpty()
            if (text.isEmpty()) {
                dialogBinding.tilTask.error = getString(R.string.error_empty_task)
                return
            }
            if (existing == null) addTask(text) else if (text != existing.title) renameTask(existing, text)
            dialog.dismiss()
        }

        dialogBinding.etTask.doAfterTextChanged { dialogBinding.tilTask.error = null }
        dialogBinding.etTask.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit()
                true
            } else {
                false
            }
        }

        dialog.setOnShowListener {
            // Override the default click so an empty input keeps the dialog open.
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { submit() }
            dialogBinding.etTask.requestFocus()
        }
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        dialog.show()
    }

    // ---------------------------------------------------------------- swipe to delete

    private inner class SwipeToDeleteCallback :
        ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

        private val background = ColorDrawable(ContextCompat.getColor(this@MainActivity, R.color.swipe_delete_background))
        private val icon = ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_delete)?.mutate()?.apply {
            setTint(ContextCompat.getColor(this@MainActivity, R.color.swipe_delete_foreground))
        }
        private val iconMargin = (24 * resources.displayMetrics.density).toInt()

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder,
        ): Boolean = false

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.bindingAdapterPosition
            if (position == RecyclerView.NO_POSITION) return
            deleteTask(adapter.currentList[position])
        }

        override fun onChildDraw(
            c: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean,
        ) {
            val item = viewHolder.itemView
            if (dX != 0f) {
                val swipeRight = dX > 0
                if (swipeRight) {
                    background.setBounds(item.left, item.top, item.left + dX.toInt(), item.bottom)
                } else {
                    background.setBounds(item.right + dX.toInt(), item.top, item.right, item.bottom)
                }
                background.draw(c)

                icon?.let {
                    val w = it.intrinsicWidth
                    val h = it.intrinsicHeight
                    val top = item.top + (item.height - h) / 2
                    if (swipeRight) {
                        val left = item.left + iconMargin
                        it.setBounds(left, top, left + w, top + h)
                    } else {
                        val right = item.right - iconMargin
                        it.setBounds(right - w, top, right, top + h)
                    }
                    if (abs(dX) > iconMargin + w) it.draw(c)
                }
            }
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }
    }

    private companion object {
        const val KEY_FILTER = "filter"
    }
}
