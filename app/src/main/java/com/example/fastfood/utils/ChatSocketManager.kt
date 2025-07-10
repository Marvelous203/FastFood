package com.example.fastfood.utils

import android.util.Log
import com.example.fastfood.models.ChatMessage
import com.example.fastfood.models.MessageType
import com.example.fastfood.models.SenderType
import com.google.gson.Gson
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONException
import org.json.JSONObject
import java.net.URISyntaxException

class ChatSocketManager private constructor() {
    
    private var socket: Socket? = null
    private val gson = Gson()
    
    companion object {
        private const val TAG = "ChatSocketManager"
        // TODO: Replace with actual server URL when API is provided
        private const val SERVER_URL = "https://your-chat-server.com"
        
        @Volatile
        private var INSTANCE: ChatSocketManager? = null
        
        fun getInstance(): ChatSocketManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ChatSocketManager().also { INSTANCE = it }
            }
        }
    }
    
    // Callback functions
    var onConnected: (() -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null
    var onMessageReceived: ((ChatMessage) -> Unit)? = null
    var onTyping: ((Boolean) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    
    private var currentUserId: String? = null
    private var currentUserName: String? = null
    private var isConnecting = false
    
    fun connect(userId: String, userName: String) {
        if (isConnecting || socket?.connected() == true) {
            Log.d(TAG, "Already connected or connecting")
            return
        }
        
        currentUserId = userId
        currentUserName = userName
        isConnecting = true
        
        try {
            Log.d(TAG, "Attempting to connect to chat server...")
            
            val options = IO.Options().apply {
                timeout = 10000
                reconnection = true
                reconnectionAttempts = 5
                reconnectionDelay = 1000
            }
            
            socket = IO.socket(SERVER_URL, options)
            
            setupSocketListeners()
            socket?.connect()
            
        } catch (e: URISyntaxException) {
            Log.e(TAG, "Invalid server URL", e)
            isConnecting = false
            // Fallback to mock mode for development
            simulateConnection()
        } catch (e: Exception) {
            Log.e(TAG, "Connection error", e)
            isConnecting = false
            // Fallback to mock mode for development
            simulateConnection()
        }
    }
    
    private fun setupSocketListeners() {
        socket?.apply {
            on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "Connected to chat server")
                isConnecting = false
                
                // Join user to their personal room
                val joinData = JSONObject().apply {
                    put("userId", currentUserId)
                    put("userName", currentUserName)
                    put("userType", "customer")
                }
                emit("join-room", joinData)
                
                onConnected?.invoke()
            }
            
            on(Socket.EVENT_DISCONNECT) {
                Log.d(TAG, "Disconnected from chat server")
                isConnecting = false
                onDisconnected?.invoke()
            }
            
            on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.e(TAG, "Connection error: ${args[0]}")
                isConnecting = false
                onError?.invoke("Không thể kết nối đến server chat")
                
                // Fallback to mock mode
                simulateConnection()
            }
            
            on("message") { args ->
                try {
                    val messageJson = args[0] as JSONObject
                    val message = parseJsonToMessage(messageJson)
                    message?.let { onMessageReceived?.invoke(it) }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing message", e)
                }
            }
            
            on("typing") { args ->
                try {
                    val data = args[0] as JSONObject
                    val isTyping = data.getBoolean("isTyping")
                    val senderId = data.getString("senderId")
                    
                    // Only show typing indicator for other users
                    if (senderId != currentUserId) {
                        onTyping?.invoke(isTyping)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing typing indicator", e)
                }
            }
            
            on("user-joined") { args ->
                try {
                    val data = args[0] as JSONObject
                    val userName = data.getString("userName")
                    Log.d(TAG, "User joined: $userName")
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing user-joined", e)
                }
            }
        }
    }
    
    private fun simulateConnection() {
        Log.d(TAG, "Using mock chat mode for development")
        
        // Simulate connection after a delay
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            onConnected?.invoke()
            
            // Simulate store response after customer sends message
            setupMockResponses()
        }, 1000)
    }
    
    private fun setupMockResponses() {
        // This will be used to simulate store responses in development mode
        Log.d(TAG, "Mock chat responses enabled")
    }
    
    fun sendMessage(message: ChatMessage) {
        if (socket?.connected() == true) {
            try {
                val messageJson = JSONObject().apply {
                    put("id", message.id)
                    put("message", message.message)
                    put("senderId", message.senderId)
                    put("senderName", message.senderName)
                    put("senderType", message.senderType.value)
                    put("messageType", message.messageType.value)
                    put("timestamp", message.timestamp)
                    put("roomId", "customer_${currentUserId}")
                }
                
                socket?.emit("send-message", messageJson)
                Log.d(TAG, "Message sent: ${message.message}")
                
            } catch (e: JSONException) {
                Log.e(TAG, "Error creating message JSON", e)
                onError?.invoke("Lỗi gửi tin nhắn")
            }
        } else {
            Log.w(TAG, "Socket not connected, using mock mode")
            // Simulate store auto-response in mock mode
            simulateStoreResponse(message)
        }
    }
    
    private fun simulateStoreResponse(customerMessage: ChatMessage) {
        // Simulate store response for development/testing
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            val responses = listOf(
                "Cảm ơn bạn đã liên hệ! Chúng tôi sẽ hỗ trợ bạn ngay.",
                "Chúng tôi đã nhận được yêu cầu của bạn. Vui lòng chờ trong giây lát.",
                "Có gì chúng tôi có thể giúp bạn thêm không?",
                "Cảm ơn bạn đã sử dụng dịch vụ của chúng tôi!"
            )
            
            val randomResponse = responses.random()
            val storeMessage = ChatMessage(
                message = randomResponse,
                senderId = "store_support",
                senderName = "Nhân viên hỗ trợ",
                senderType = SenderType.STORE,
                messageType = MessageType.TEXT,
                timestamp = System.currentTimeMillis()
            )
            
            onMessageReceived?.invoke(storeMessage)
        }, (2000..5000).random().toLong()) // Random delay 2-5 seconds
    }
    
    fun sendTyping(isTyping: Boolean) {
        if (socket?.connected() == true) {
            try {
                val typingData = JSONObject().apply {
                    put("isTyping", isTyping)
                    put("senderId", currentUserId)
                    put("roomId", "customer_${currentUserId}")
                }
                socket?.emit("typing", typingData)
            } catch (e: JSONException) {
                Log.e(TAG, "Error sending typing indicator", e)
            }
        }
    }
    
    fun markAsRead(userId: String) {
        if (socket?.connected() == true) {
            try {
                val readData = JSONObject().apply {
                    put("userId", userId)
                    put("roomId", "customer_${userId}")
                }
                socket?.emit("mark-read", readData)
            } catch (e: JSONException) {
                Log.e(TAG, "Error marking as read", e)
            }
        }
    }
    
    fun disconnect() {
        Log.d(TAG, "Disconnecting from chat server")
        socket?.disconnect()
        socket?.off()
        socket = null
        isConnecting = false
        currentUserId = null
        currentUserName = null
    }
    
    private fun parseJsonToMessage(json: JSONObject): ChatMessage? {
        return try {
            ChatMessage(
                id = json.getString("id"),
                message = json.getString("message"),
                senderId = json.getString("senderId"),
                senderName = json.getString("senderName"),
                senderType = SenderType.valueOf(json.getString("senderType").uppercase()),
                messageType = MessageType.valueOf(json.getString("messageType").uppercase()),
                timestamp = json.getLong("timestamp"),
                isRead = json.optBoolean("isRead", false)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing message JSON", e)
            null
        }
    }
    
    fun isConnected(): Boolean {
        return socket?.connected() == true
    }
} 