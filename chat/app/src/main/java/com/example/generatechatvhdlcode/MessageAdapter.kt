package com.example.generatechatvhdlcode

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.generatechatvhdlcode.databinding.ItemMessageBotBinding
import com.example.generatechatvhdlcode.databinding.ItemMessageUserBinding

class MessageAdapter(private val messages: List<Message>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_BOT  = 0
        private const val VIEW_USER = 1
    }

    override fun getItemViewType(position: Int) =
        if (messages[position].role == Role.BOT) VIEW_BOT else VIEW_USER

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_BOT) {
            BotViewHolder(ItemMessageBotBinding.inflate(inflater, parent, false))
        } else {
            UserViewHolder(ItemMessageUserBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = messages[position]
        when (holder) {
            is BotViewHolder  -> holder.bind(msg)
            is UserViewHolder -> holder.bind(msg)
        }
    }

    override fun getItemCount() = messages.size

    class BotViewHolder(private val b: ItemMessageBotBinding) :
        RecyclerView.ViewHolder(b.root) {
        fun bind(msg: Message) { b.messageBotText.text = msg.text }
    }

    class UserViewHolder(private val b: ItemMessageUserBinding) :
        RecyclerView.ViewHolder(b.root) {
        fun bind(msg: Message) { b.messageUserText.text = msg.text }
    }
}