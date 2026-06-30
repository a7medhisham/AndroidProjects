package com.example.smartgarbage

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NearbyBinsAdapter(private val bins: List<BinItem>) :
    RecyclerView.Adapter<NearbyBinsAdapter.BinViewHolder>() {

    class BinViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvBinId: TextView = view.findViewById(R.id.tvBinId)
        val tvLocation: TextView = view.findViewById(R.id.tvLocation)
        val tvFillLevel: TextView = view.findViewById(R.id.tvFillLevel)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BinViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_nearby_bin, parent, false)
        return BinViewHolder(view)
    }

    override fun onBindViewHolder(holder: BinViewHolder, position: Int) {
        val bin = bins[position]
        val context = holder.itemView.context

        holder.tvBinId.text = context.getString(R.string.bin_number, bin.id)
        holder.tvLocation.text = "📍 ${bin.location}"
        holder.tvFillLevel.text = context.getString(R.string.fill_level, bin.fillLevel)

        when {
            bin.fillLevel >= 80 -> {
                holder.tvStatus.text = context.getString(R.string.bin_full_status)
                holder.tvStatus.setTextColor(Color.RED)
            }
            bin.fillLevel >= 50 -> {
                holder.tvStatus.text = context.getString(R.string.bin_partial_status)
                holder.tvStatus.setTextColor(Color.parseColor("#FF9800"))
            }
            else -> {
                holder.tvStatus.text = context.getString(R.string.bin_empty_status)
                holder.tvStatus.setTextColor(Color.GREEN)
            }
        }
    }

    override fun getItemCount() = bins.size
}