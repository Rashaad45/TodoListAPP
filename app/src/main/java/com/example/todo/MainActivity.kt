package com.example.todo

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var todoAdapter: TodoAdapter
    private val todoItems = mutableListOf<TodoItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupRecyclerView()
        setupInputField()
        setupButtons()
        updateEmptyState()
    }

    private fun setupRecyclerView() {
        val rvTodoItems = findViewById<RecyclerView>(R.id.rvTodoItems)
        todoAdapter = TodoAdapter(todoItems)
        rvTodoItems.adapter = todoAdapter
        rvTodoItems.layoutManager = LinearLayoutManager(this)

        // Add item click listener if needed
        todoAdapter.setOnItemClickListener(object : TodoAdapter.OnItemClickListener {
            override fun onCheckBoxClicked(position: Int, isChecked: Boolean) {
                todoItems[position].isCompleted = isChecked
                todoAdapter.notifyItemChanged(position)
            }
        })
    }

    private fun setupInputField() {
        val etTodoTitle = findViewById<TextInputEditText>(R.id.etTodoTitle)

        // Handle keyboard "Done" action
        etTodoTitle.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addTodoItem()
                true
            } else {
                false
            }
        }
    }

    private fun setupButtons() {
        // Add button
        findViewById<View>(R.id.btnAddTodo).setOnClickListener {
            addTodoItem()
        }

        // Delete completed button
        findViewById<View>(R.id.btnDeleteTodo).setOnClickListener {
            deleteCompletedItems()
        }
    }

    private fun addTodoItem() {
        val etTodoTitle = findViewById<TextInputEditText>(R.id.etTodoTitle)
        val todoText = etTodoTitle.text.toString().trim()

        if (todoText.isNotEmpty()) {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val currentDate = dateFormat.format(Date())

            val newTodo = TodoItem(
                id = UUID.randomUUID().toString(),
                title = todoText,
                dateAdded = "Added: $currentDate",
                isCompleted = false
            )

            todoItems.add(0, newTodo) // Add to the top of the list
            todoAdapter.notifyItemInserted(0)

            // Clear the input field
            etTodoTitle.text = null

            // Update empty state
            updateEmptyState()

            // Scroll to the top
            findViewById<RecyclerView>(R.id.rvTodoItems).smoothScrollToPosition(0)
        } else {
            Toast.makeText(this, "Please enter a task", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteCompletedItems() {
        val completedItems = todoItems.filter { it.isCompleted }

        if (completedItems.isEmpty()) {
            Toast.makeText(this, "No completed tasks to delete", Toast.LENGTH_SHORT).show()
            return
        }

        todoItems.removeAll { it.isCompleted }
        todoAdapter.notifyDataSetChanged()
        updateEmptyState()

        val count = completedItems.size
        Toast.makeText(
            this,
            "Deleted $count ${if (count == 1) "task" else "tasks"}",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun updateEmptyState() {
        val emptyStateLayout = findViewById<View>(R.id.emptyStateLayout)
        if (todoItems.isEmpty()) {
            emptyStateLayout.visibility = View.VISIBLE
        } else {
            emptyStateLayout.visibility = View.GONE
        }
    }
}

// Data class for Todo items
data class TodoItem(
    val id: String,
    val title: String,
    val dateAdded: String,
    var isCompleted: Boolean
)

// Adapter for the RecyclerView
class TodoAdapter(
    private val todoItems: List<TodoItem>
) : RecyclerView.Adapter<TodoAdapter.TodoViewHolder>() {

    private var onItemClickListener: OnItemClickListener? = null

    interface OnItemClickListener {
        fun onCheckBoxClicked(position: Int, isChecked: Boolean)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.onItemClickListener = listener
    }

    inner class TodoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkBox: CheckBox = itemView.findViewById(R.id.cbDone)
        val titleTextView: TextView = itemView.findViewById(R.id.tvTodoTitle)
        val dateTextView: TextView = itemView.findViewById(R.id.tvDateAdded)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): TodoViewHolder {
        val itemView = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_todo, parent, false)
        return TodoViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        val currentItem = todoItems[position]

        holder.titleTextView.text = currentItem.title
        holder.dateTextView.text = currentItem.dateAdded

        // Set checkbox without triggering listener
        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = currentItem.isCompleted

        // Apply strike-through text if completed
        if (currentItem.isCompleted) {
            holder.titleTextView.paintFlags = holder.titleTextView.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            holder.titleTextView.paintFlags = holder.titleTextView.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }

        // Set checkbox listener
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            onItemClickListener?.onCheckBoxClicked(position, isChecked)

            // Update text appearance
            if (isChecked) {
                holder.titleTextView.paintFlags = holder.titleTextView.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                holder.titleTextView.paintFlags = holder.titleTextView.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
        }
    }

    override fun getItemCount(): Int = todoItems.size
}