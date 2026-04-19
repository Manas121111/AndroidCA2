package com.smarttour360.app.ui.chatbot

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.smarttour360.app.R
import com.smarttour360.app.dto.ChatMessage
import com.smarttour360.app.dto.ChatRole
import com.smarttour360.app.dto.QuickReply

class ChatbotAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val items = mutableListOf<ChatMessage>()

    fun submitList(data: List<ChatMessage>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position].role == ChatRole.USER) VIEW_USER else VIEW_BOT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutId = if (viewType == VIEW_USER) R.layout.item_chat_user else R.layout.item_chat_bot
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as MessageViewHolder).bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    private class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val text: TextView = itemView.findViewById(R.id.text_chat_message)

        fun bind(item: ChatMessage) {
            text.text = item.content
        }
    }

    private companion object {
        const val VIEW_BOT = 0
        const val VIEW_USER = 1
    }
}

class QuickReplyAdapter(
    private val onClick: (QuickReply) -> Unit
) : RecyclerView.Adapter<QuickReplyAdapter.QuickReplyViewHolder>() {
    private val items = mutableListOf<QuickReply>()

    fun submitList(data: List<QuickReply>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuickReplyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_quick_reply, parent, false)
        return QuickReplyViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: QuickReplyViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class QuickReplyViewHolder(
        itemView: View,
        private val onClick: (QuickReply) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val button: MaterialButton = itemView.findViewById(R.id.button_quick_reply)
        private var item: QuickReply? = null

        init {
            button.setOnClickListener { item?.let(onClick) }
        }

        fun bind(reply: QuickReply) {
            item = reply
            button.text = reply.label
        }
    }
}
