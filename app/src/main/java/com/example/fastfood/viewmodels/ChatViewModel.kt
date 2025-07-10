package com.example.fastfood.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fastfood.models.ChatMessage
import com.example.fastfood.models.MessageType
import com.example.fastfood.models.SenderType
import com.example.fastfood.utils.ChatSocketManager
import com.example.fastfood.utils.PreferencesManager
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    
    private val chatSocketManager = ChatSocketManager.getInstance()
    private val preferencesManager = PreferencesManager(application)
    
    private val _messages = MutableLiveData<MutableList<ChatMessage>>()
    val messages: LiveData<MutableList<ChatMessage>> = _messages
    
    private val _isConnected = MutableLiveData<Boolean>()
    val isConnected: LiveData<Boolean> = _isConnected
    
    private val _isTyping = MutableLiveData<Boolean>()
    val isTyping: LiveData<Boolean> = _isTyping
    
    private val _connectionStatus = MutableLiveData<String>()
    val connectionStatus: LiveData<String> = _connectionStatus
    
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error
    
    private val _newMessageReceived = MutableLiveData<Boolean>()
    val newMessageReceived: LiveData<Boolean> = _newMessageReceived
    
    private val currentUser = preferencesManager.currentUser
    private val userId = currentUser?.id ?: "guest_${System.currentTimeMillis()}"
    private val userName = currentUser?.fullName ?: "Khách hàng"
    
    init {
        _messages.value = mutableListOf()
        setupSocketListeners()
        connectToChat()
        loadWelcomeMessage()
    }
    
    private fun setupSocketListeners() {
        chatSocketManager.onConnected = {
            _isConnected.value = true
            _connectionStatus.value = "Đã kết nối"
        }
        
        chatSocketManager.onDisconnected = {
            _isConnected.value = false
            _connectionStatus.value = "Mất kết nối"
        }
        
        chatSocketManager.onMessageReceived = { message ->
            viewModelScope.launch {
                addMessageToList(message)
                if (message.isFromStore) {
                    _newMessageReceived.value = true
                }
            }
        }
        
        chatSocketManager.onTyping = { isTyping ->
            _isTyping.value = isTyping
        }
        
        chatSocketManager.onError = { errorMessage ->
            _error.value = errorMessage
            _connectionStatus.value = "Lỗi kết nối"
        }
    }
    
    private fun connectToChat() {
        viewModelScope.launch {
            _connectionStatus.value = "Đang kết nối..."
            chatSocketManager.connect(userId, userName)
        }
    }
    
    private fun loadWelcomeMessage() {
        val welcomeMessage = ChatMessage(
            message = "Chào mừng bạn đến với dịch vụ hỗ trợ của chúng tôi! Chúng tôi sẽ phản hồi sớm nhất có thể.",
            senderId = "system",
            senderName = "Hệ thống",
            senderType = SenderType.STORE,
            messageType = MessageType.SYSTEM,
            timestamp = System.currentTimeMillis()
        )
        addMessageToList(welcomeMessage)
    }
    
    fun sendMessage(messageText: String) {
        if (messageText.trim().isEmpty()) return
        
        viewModelScope.launch {
            val message = ChatMessage(
                message = messageText.trim(),
                senderId = userId,
                senderName = userName,
                senderType = SenderType.CUSTOMER,
                messageType = MessageType.TEXT
            )
            
            // Add to local list immediately for better UX
            addMessageToList(message)
            
            // Send via socket
            chatSocketManager.sendMessage(message)
        }
    }
    
    fun sendTypingIndicator(isTyping: Boolean) {
        chatSocketManager.sendTyping(isTyping)
    }
    
    private fun addMessageToList(message: ChatMessage) {
        val currentList = _messages.value ?: mutableListOf()
        currentList.add(message)
        _messages.value = currentList
    }
    
    fun markMessagesAsRead() {
        viewModelScope.launch {
            val currentList = _messages.value ?: return@launch
            var hasUnreadMessages = false
            
            currentList.forEach { message ->
                if (!message.isRead && message.isFromStore) {
                    message.isRead = true
                    hasUnreadMessages = true
                }
            }
            
            if (hasUnreadMessages) {
                _messages.value = currentList
                chatSocketManager.markAsRead(userId)
            }
        }
    }
    
    fun reconnect() {
        viewModelScope.launch {
            _connectionStatus.value = "Đang kết nối lại..."
            chatSocketManager.disconnect()
            chatSocketManager.connect(userId, userName)
        }
    }
    
    fun getUnreadMessageCount(): Int {
        return _messages.value?.count { !it.isRead && it.isFromStore } ?: 0
    }
    
    fun clearNewMessageFlag() {
        _newMessageReceived.value = false
    }
    
    override fun onCleared() {
        super.onCleared()
        chatSocketManager.disconnect()
    }
} 