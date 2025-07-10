package com.example.fastfood.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fastfood.R
import com.example.fastfood.adapters.ChatAdapter
import com.example.fastfood.databinding.ActivityChatBinding
import com.example.fastfood.viewmodels.ChatViewModel

class ChatActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityChatBinding
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var chatAdapter: ChatAdapter
    
    private var typingHandler: Handler? = null
    private var typingRunnable: Runnable? = null
    private var isUserTyping = false
    
    companion object {
        fun start(context: Context) {
            val intent = Intent(context, ChatActivity::class.java)
            context.startActivity(intent)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        setupTypingIndicator()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "Chat hỗ trợ"
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }
    
    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter()
        
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true // Start from bottom
        
        binding.rvMessages.apply {
            adapter = chatAdapter
            this.layoutManager = layoutManager
        }
    }
    
    private fun setupObservers() {
        viewModel.messages.observe(this) { messages ->
            chatAdapter.submitList(messages.toList()) {
                // Scroll to bottom after new messages
                if (messages.isNotEmpty()) {
                    binding.rvMessages.scrollToPosition(messages.size - 1)
                }
            }
        }
        
        viewModel.isConnected.observe(this) { isConnected ->
            updateConnectionStatus(isConnected)
        }
        
        viewModel.connectionStatus.observe(this) { status ->
            binding.tvConnectionStatus.text = status
        }
        
        viewModel.isTyping.observe(this) { isTyping ->
            if (isTyping) {
                binding.tvTypingIndicator.text = "Nhân viên đang nhập..."
                binding.tvTypingIndicator.visibility = android.view.View.VISIBLE
            } else {
                binding.tvTypingIndicator.visibility = android.view.View.GONE
            }
        }
        
        viewModel.error.observe(this) { error ->
            if (error.isNotEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
            }
        }
        
        viewModel.newMessageReceived.observe(this) { hasNewMessage ->
            if (hasNewMessage) {
                // Mark messages as read when user is viewing chat
                viewModel.markMessagesAsRead()
                viewModel.clearNewMessageFlag()
            }
        }
    }
    
    private fun setupClickListeners() {
        binding.btnSend.setOnClickListener {
            sendMessage()
        }
        
        binding.etMessage.setOnEditorActionListener { _, _, _ ->
            sendMessage()
            true
        }
        
        binding.btnReconnect.setOnClickListener {
            viewModel.reconnect()
        }
    }
    
    private fun setupTypingIndicator() {
        typingHandler = Handler(Looper.getMainLooper())
        
        binding.etMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!isUserTyping && !s.isNullOrEmpty()) {
                    isUserTyping = true
                    viewModel.sendTypingIndicator(true)
                }
                
                // Remove previous typing stop callback
                typingRunnable?.let { typingHandler?.removeCallbacks(it) }
                
                // Set new callback to stop typing indicator after 1 second
                typingRunnable = Runnable {
                    if (isUserTyping) {
                        isUserTyping = false
                        viewModel.sendTypingIndicator(false)
                    }
                }
                typingHandler?.postDelayed(typingRunnable!!, 1000)
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
    }
    
    private fun sendMessage() {
        val messageText = binding.etMessage.text.toString().trim()
        
        if (messageText.isNotEmpty()) {
            viewModel.sendMessage(messageText)
            binding.etMessage.setText("")
            
            // Stop typing indicator
            if (isUserTyping) {
                isUserTyping = false
                viewModel.sendTypingIndicator(false)
            }
        }
    }
    
    private fun updateConnectionStatus(isConnected: Boolean) {
        if (isConnected) {
            binding.layoutConnectionStatus.visibility = android.view.View.GONE
            binding.layoutMessageInput.visibility = android.view.View.VISIBLE
            
            // Update toolbar subtitle
            supportActionBar?.subtitle = "Trực tuyến"
        } else {
            binding.layoutConnectionStatus.visibility = android.view.View.VISIBLE
            binding.layoutMessageInput.visibility = android.view.View.GONE
            
            // Update toolbar subtitle
            supportActionBar?.subtitle = "Ngoại tuyến"
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Mark messages as read when user returns to chat
        viewModel.markMessagesAsRead()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        typingHandler?.removeCallbacksAndMessages(null)
        
        // Stop typing indicator when leaving chat
        if (isUserTyping) {
            viewModel.sendTypingIndicator(false)
        }
    }
} 