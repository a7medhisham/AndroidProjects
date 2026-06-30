package com.example.smartgarbage

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AlertsAdapter(
    private val alerts: List<AlertItem>
) : RecyclerView.Adapter<AlertsAdapter.AlertViewHolder>() {

    class AlertViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_alert, parent, false)
        return AlertViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        val alert = alerts[position]
        val context = holder.itemView.context

        val wasteType = when (alert.wasteTypeId) {
            1 -> context.getString(R.string.glass)
            2 -> context.getString(R.string.metal)
            3 -> context.getString(R.string.paper)
            4 -> context.getString(R.string.plastic)
            5 -> context.getString(R.string.trach)
            else -> context.getString(R.string.unknown)
        }

        holder.tvMessage.text = context.getString(R.string.alert_message, alert.alertId, alert.binId, wasteType)
        holder.tvStatus.text = context.getString(R.string.status_label, alert.status)
        holder.tvTime.text = alert.createdAt.split("T")[0]

        when (alert.status) {
            "Resolved", "Completed" -> holder.tvStatus.setTextColor(Color.GREEN)
            "Pending" -> holder.tvStatus.setTextColor(Color.RED)
            else -> holder.tvStatus.setTextColor(Color.GRAY)
        }
    }

    override fun getItemCount() = alerts.size
}