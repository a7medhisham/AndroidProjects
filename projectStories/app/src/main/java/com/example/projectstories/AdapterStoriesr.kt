package com.example.projectstories

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView


class AdapterSories(val activity:Activity,val Stories:ArrayList<Stories>, val onItemClick: (Int)->Unit) :
    RecyclerView.Adapter<AdapterSories.Item>() {
    class Item(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.image)
        val text: TextView = view.findViewById(R.id.text)
        val card: CardView = view.findViewById(R.id.card)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Item {
        val view = activity.layoutInflater.inflate(R.layout.list, parent, false)
        return Item(view)
    }

    override fun getItemCount() = Stories.size

    override fun onBindViewHolder(holder: Item, position: Int) {
        holder.image.setImageResource(Stories.get(position).picture)
        holder.text.setText(Stories.get(position).text1)
        holder.card.setOnClickListener {

            onItemClick(position)
        }
    }
}