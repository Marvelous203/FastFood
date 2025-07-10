package com.example.fastfood.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fastfood.databinding.ItemChatMessageCustomerBinding
import com.example.fastfood.databinding.ItemChatMessageStoreBinding
import com.example.fastfood.databinding.ItemChatMessageSystemBinding
import com.example.fastfood.models.ChatMessage
import com.example.fastfood.models.MessageType
import com.example.fastfood.models.SenderType

class ChatAdapter : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(ChatDiffCallback()) {
    
    companion object {
        private const val VIEW_TYPE_CUSTOMER = 1
        private const val VIEW_TYPE_STORE = 2
        private const val VIEW_TYPE_SYSTEM = 3
    }
    
    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return when {
            message.messageType == MessageType.SYSTEM -> VIEW_TYPE_SYSTEM
            message.senderType == SenderType.CUSTOMER -> VIEW_TYPE_CUSTOMER
            else -> VIEW_TYPE_STORE
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_CUSTOMER -> {
                val binding = ItemChatMessageCustomerBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                CustomerMessageViewHolder(binding)
            }
            VIEW_TYPE_STORE -> {
                val binding = ItemChatMessageStoreBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                StoreMessageViewHolder(binding)
            }
            VIEW_TYPE_SYSTEM -> {
                val binding = ItemChatMessageSystemBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                SystemMessageViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is CustomerMessageViewHolder -> holder.bind(message)
            is StoreMessageViewHolder -> holder.bind(message)
            is SystemMessageViewHolder -> holder.bind(message)
        }
    }
    
    inner class CustomerMessageViewHolder(
        private val binding: ItemChatMessageCustomerBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(message: ChatMessage) {
            binding.apply {
                tvMessage.text = message.message
                tvTime.text = message.formattedTime
                
                // Show/hide read indicator
                if (message.isRead) {
                    tvReadStatus.text = "Đã xem"
                    tvReadStatus.visibility = android.view.View.VISIBLE
                } else {
                    tvReadStatus.text = "Đã gửi"
                    tvReadStatus.visibility = android.view.View.VISIBLE
                }
            }
        }
    }
    
    inner class StoreMessageViewHolder(
        private val binding: ItemChatMessageStoreBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(message: ChatMessage) {
            binding.apply {
                tvMessage.text = message.message
                tvTime.text = message.formattedTime
                tvSenderName.text = message.senderName
                
                // Show avatar letter (first letter of sender name)
                val firstLetter = message.senderName.firstOrNull()?.uppercase() ?: "S"
                tvAvatarLetter.text = firstLetter
            }
        }
    }
    
    inner class SystemMessageViewHolder(
        private val binding: ItemChatMessageSystemBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(message: ChatMessage) {
            binding.apply {
                tvMessage.text = message.message
                tvTime.text = message.formattedTime
            }
        }
    }
    
    private class ChatDiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem == newItem
        }
    }
} 