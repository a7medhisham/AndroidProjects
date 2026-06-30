package com.example.smartgarbage

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AdapterSponsor(
    private val context: Context,
    private val sponsors: ArrayList<SponsorModel>
) : RecyclerView.Adapter<AdapterSponsor.ItemViewHolder>() {

    private var currentPlayer: MediaPlayer? = null
    private var playingPosition: Int = -1

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val sponsorLogo: ImageView = view.findViewById(R.id.image)
        val sponsorName: TextView = view.findViewById(R.id.text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.listextra, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val sponsor = sponsors[position]
        holder.sponsorLogo.setImageResource(sponsor.imageRes)
        holder.sponsorName.text = sponsor.text

        holder.itemView.setOnClickListener {
            if (sponsor.text == "PhoneKan") {
                if (currentPlayer != null) {
                    currentPlayer?.stop()
                    currentPlayer?.release()
                    currentPlayer = null
                    playingPosition = -1
                }
                val intent = Intent(context, RewardActivity::class.java)
                context.startActivity(intent)
            } else {
                if (playingPosition == position) {
                    currentPlayer?.stop()
                    currentPlayer?.release()
                    currentPlayer = null
                    playingPosition = -1
                } else {
                    currentPlayer?.stop()
                    currentPlayer?.release()
                    currentPlayer = MediaPlayer.create(context, sponsor.soundRes)
                    currentPlayer?.start()
                    playingPosition = position
                    currentPlayer?.setOnCompletionListener {
                        it.release()
                        currentPlayer = null
                        playingPosition = -1
                    }
                }
            }
        }
    }

    override fun getItemCount() = sponsors.size
}