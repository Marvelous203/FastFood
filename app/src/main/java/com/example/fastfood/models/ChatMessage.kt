package com.example.fastfood.models

import com.google.gson.annotations.SerializedName
import java.util.*

data class ChatMessage(
    @SerializedName("id")
    val id: String = UUID.randomUUID().toString(),
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("senderId")
    val senderId: String,
    
    @SerializedName("senderName")
    val senderName: String,
    
    @SerializedName("senderType")
    val senderType: SenderType,
    
    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    
    @SerializedName("messageType")
    val messageType: MessageType = MessageType.TEXT,
    
    @SerializedName("isRead")
    var isRead: Boolean = false,
    
    @SerializedName("imageUrl")
    val imageUrl: String? = null
) {
    val formattedTime: String
        get() {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = timestamp
            
            val now = Calendar.getInstance()
            val isToday = calendar.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) &&
                         calendar.get(Calendar.YEAR) == now.get(Calendar.YEAR)
            
            return if (isToday) {
                String.format("%02d:%02d", 
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE))
            } else {
                String.format("%02d/%02d %02d:%02d",
                    calendar.get(Calendar.DAY_OF_MONTH),
                    calendar.get(Calendar.MONTH) + 1,
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE))
            }
        }
    
    val isFromCustomer: Boolean
        get() = senderType == SenderType.CUSTOMER
    
    val isFromStore: Boolean
        get() = senderType == SenderType.STORE
}

enum class SenderType(val value: String) {
    @SerializedName("customer")
    CUSTOMER("customer"),
    
    @SerializedName("store")
    STORE("store")
}

enum class MessageType(val value: String) {
    @SerializedName("text")
    TEXT("text"),
    
    @SerializedName("image")
    IMAGE("image"),
    
    @SerializedName("system")
    SYSTEM("system")
} 