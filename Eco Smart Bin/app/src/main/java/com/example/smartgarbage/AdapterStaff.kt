package com.example.smartgarbage

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AdapterStaff(
    val note: ArrayList<NoteStaff>,
    private val onItemClick: ((NoteStaff, Int) -> Unit)? = null
) : RecyclerView.Adapter<AdapterStaff.Item>() {

    class Item(view: View) : RecyclerView.ViewHolder(view) {
        val message: TextView = view.findViewById(R.id.tvMessage)
        val status: TextView = view.findViewById(R.id.tvStatus)
        val time: TextView = view.findViewById(R.id.tvTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Item {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.liststaff, parent, false)
        return Item(view)
    }

    override fun onBindViewHolder(holder: Item, position: Int) {
        val item = note[position]
        val context = holder.itemView.context

        holder.message.text = item.message
        holder.status.text = context.getString(R.string.status_label, item.status)
        holder.time.text = item.sent_at

        updateColors(holder, item)

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(item, position)
        }
    }

    private fun updateColors(holder: Item, item: NoteStaff) {
        when (item.status) {
            "Completed" -> {
                holder.status.setTextColor(Color.GRAY)
                holder.message.setTextColor(Color.GRAY)
            }
            "Active", "Pending" -> {
                holder.status.setTextColor(Color.RED)
                holder.message.setTextColor(Color.RED)
            }
            "Needs Emptying" -> {
                holder.message.setTextColor(Color.RED)
                holder.status.setTextColor(Color.RED)
            }
            else -> {
                holder.message.setTextColor(Color.parseColor("#2E7D32"))
                holder.status.setTextColor(Color.parseColor("#2E7D32"))
            }
        }
    }

    fun updateItem(position: Int, newStatus: String) {
        if (position in note.indices) {
            val oldItem = note[position]
            val newItem = oldItem.copy(status = newStatus)
            note[position] = newItem
            notifyItemChanged(position)
        }
    }

    override fun getItemCount() = note.size
}