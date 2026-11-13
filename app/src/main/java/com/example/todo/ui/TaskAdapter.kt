package com.example.todo.ui

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.todo.R
import com.example.todo.data.Task
import com.example.todo.databinding.ItemTaskBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskAdapter(
    private val onToggle: (Task, Boolean) -> Unit,
    private val onClick: (Task) -> Unit,
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(DIFF) {

    private val dateFormat = SimpleDateFormat("d MMM, h:mm a", Locale.getDefault())

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long = getItem(position).id.hashCode().toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(private val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                currentTask()?.let(onClick)
            }
        }

        /** Always resolve the task from the adapter at click time — never a captured position. */
        private fun currentTask(): Task? {
            val pos = bindingAdapterPosition
            return if (pos == RecyclerView.NO_POSITION) null else currentList.getOrNull(pos)
        }

        fun bind(task: Task) {
            val context = binding.root.context

            binding.cbDone.setOnCheckedChangeListener(null)
            binding.cbDone.isChecked = task.isCompleted
            binding.cbDone.setOnCheckedChangeListener { _, checked ->
                currentTask()?.let { onToggle(it, checked) }
            }

            binding.tvTitle.text = task.title
            binding.tvTitle.paintFlags = if (task.isCompleted) {
                binding.tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
            binding.tvTitle.alpha = if (task.isCompleted) 0.55f else 1f

            binding.tvMeta.text = if (task.isCompleted && task.completedAt != null) {
                context.getString(R.string.completed_on, dateFormat.format(Date(task.completedAt)))
            } else {
                context.getString(R.string.added_on, dateFormat.format(Date(task.createdAt)))
            }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<Task>() {
            override fun areItemsTheSame(oldItem: Task, newItem: Task) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Task, newItem: Task) = oldItem == newItem
        }
    }
}
