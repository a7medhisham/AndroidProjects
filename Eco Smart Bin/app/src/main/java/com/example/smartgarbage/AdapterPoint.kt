package com.example.smartgarbage

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AdapterPoint(val transactions: ArrayList<PointItem>) :
    RecyclerView.Adapter<AdapterPoint.Item>() {

    class Item(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvPoints: TextView = view.findViewById(R.id.tvPoints)
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Item {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.listpoint, parent, false)
        return Item(view)
    }

    override fun getItemCount() = transactions.size

    override fun onBindViewHolder(holder: Item, position: Int) {
        val item = transactions[position]
        val context = holder.itemView.context

        val formattedDate = item.date.split("T")[0]
        val calculatedAmount = item.pointsRedeemed * 0.25

        holder.tvDate.text = context.getString(R.string.date_label, formattedDate)
        holder.tvPoints.text = context.getString(R.string.points_redeemed_label, item.pointsRedeemed)
        holder.tvAmount.text = context.getString(R.string.amount_label, calculatedAmount)
    }
}