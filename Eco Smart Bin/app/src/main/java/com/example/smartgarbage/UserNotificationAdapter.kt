package com.example.smartgarbage

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UserNotificationAdapter(
    private val notifications: MutableList<UserNotificationItem>,
    private val onItemClick: (UserNotificationItem, Int) -> Unit
) : RecyclerView.Adapter<UserNotificationAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val item = notifications[position]
        val context = holder.itemView.context

        when {
            item.type.contains("Transaction", ignoreCase = true) -> {
                holder.tvMessage.text = "⭐ ${item.message}"
                holder.tvMessage.setTextColor(Color.parseColor("#FF9800"))
            }
            item.type.contains("Full", ignoreCase = true) -> {
                holder.tvMessage.text = "🔴 ${item.message}"
                holder.tvMessage.setTextColor(Color.RED)
            }
            item.type.contains("Empty", ignoreCase = true) -> {
                holder.tvMessage.text = "🟢 ${item.message}"
                holder.tvMessage.setTextColor(Color.GREEN)
            }
            else -> {
                holder.tvMessage.text = "📢 ${item.message}"
                holder.tvMessage.setTextColor(Color.BLACK)
            }
        }

        val statusText = context.getString(R.string.status_label, item.status)
        holder.tvStatus.text = statusText
        holder.tvTime.text = item.sentAt.split("T")[0]

        if (item.status == "Seen") {
            holder.tvStatus.setTextColor(Color.GRAY)
            holder.itemView.setBackgroundColor(Color.parseColor("#F5F5F5"))
        } else {
            holder.tvStatus.setTextColor(Color.RED)
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
        }

        holder.itemView.setOnClickListener {
            if (item.status != "Seen") {
                onItemClick(item, position)
            }
        }
    }

    fun updateItemStatus(position: Int, newStatus: String) {
        if (position in notifications.indices) {
            val updatedItem = notifications[position].copy(status = newStatus)
            notifications[position] = updatedItem
            notifyItemChanged(position)
        }
    }

    override fun getItemCount() = notifications.size
}