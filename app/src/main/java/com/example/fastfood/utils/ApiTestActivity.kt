package com.example.fastfood.utils

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fastfood.R
import com.example.fastfood.network.RetrofitClient
import com.example.fastfood.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ApiTestActivity : AppCompatActivity() {
    
    private lateinit var btnPingTest: Button
    private lateinit var btnFullTest: Button
    private lateinit var btnCurlTest: Button
    private lateinit var tvResults: TextView
    private lateinit var apiService: ApiService
    
    companion object {
        private const val TAG = "API_TEST"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize RetrofitClient with application context
        RetrofitClient.initialize(application)
        
        // Initialize API service
        apiService = RetrofitClient.create(ApiService::class.java)
        
        // Simple layout programmatically
        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(32, 32, 32, 32)
        
        // Title
        val title = TextView(this)
        title.text = "🧪 API Test Tools"
        title.textSize = 20f
        title.setPadding(0, 0, 0, 32)
        layout.addView(title)
        
        // Ping Test Button
        btnPingTest = Button(this)
        btnPingTest.text = "🏓 Ping Test (Quick)"
        btnPingTest.setOnClickListener { performPingTest() }
        layout.addView(btnPingTest)
        
        // Full Test Button
        btnFullTest = Button(this)
        btnFullTest.text = "🚀 Full API Test"
        btnFullTest.setOnClickListener { performFullTest() }
        layout.addView(btnFullTest)
        
        // cURL Test Button  
        btnCurlTest = Button(this)
        btnCurlTest.text = "📋 Copy cURL Command"
        btnCurlTest.setOnClickListener { showCurlCommand() }
        layout.addView(btnCurlTest)
        
        // Results TextView
        tvResults = TextView(this)
        tvResults.text = "Nhấn button để test API\n\nKiểm tra Logcat để xem kết quả chi tiết (filter: API_TEST)"
        tvResults.setPadding(0, 32, 0, 0)
        tvResults.setBackgroundColor(0xFFF5F5F5.toInt())
        tvResults.setPadding(16, 16, 16, 16)
        layout.addView(tvResults)
        
        setContentView(layout)
        
        Log.d(TAG, "🧪 API Test Activity started")
    }
    
    private fun performPingTest() {
        tvResults.text = "🏓 Ping testing...\nCheck Logcat (filter: API_TEST)"
        btnPingTest.isEnabled = false
        
        lifecycleScope.launch {
            val startTime = System.currentTimeMillis()
            val success = SimpleApiTester.pingTest()
            val endTime = System.currentTimeMillis()
            
            runOnUiThread {
                val result = if (success) {
                    "✅ PING SUCCESS\nTime: ${endTime - startTime}ms"
                } else {
                    "❌ PING FAILED\nTime: ${endTime - startTime}ms"
                }
                tvResults.text = result + "\n\nCheck Logcat for details"
                btnPingTest.isEnabled = true
            }
        }
    }
    
    private fun performFullTest() {
        tvResults.text = "🚀 Full API testing...\nCheck Logcat (filter: API_TEST)"
        btnFullTest.isEnabled = false
        
        lifecycleScope.launch {
            try {
                // Test basic internet first
                if (!SimpleApiTester.basicInternetTest()) {
                    tvResults.text = "❌ No internet connection"
                    btnFullTest.isEnabled = true
                    return@launch
                }
                
                // Then test API
                if (!SimpleApiTester.pingTest()) {
                    tvResults.text = "❌ API server not reachable"
                    btnFullTest.isEnabled = true
                    return@launch
                }
                
                // Run comprehensive test
                val results = SimpleApiTester.comprehensiveTest()
                
                // Display results
                withContext(Dispatchers.Main) {
                    tvResults.text = "✅ Test Results:\n\n$results"
                    btnFullTest.isEnabled = true
                }
                
            } catch (e: Exception) {
                tvResults.text = "❌ Test failed: ${e.message}"
                Log.e(TAG, "Test failed", e)
                btnFullTest.isEnabled = true
            }
        }
    }
    
    private fun showCurlCommand() {
        val curlCommand = """
            curl -w "Response time: %{time_total}s\n" \
                 -H "Accept: application/json" \
                 -H "X-custom-lang: vi" \
                 -H "User-Agent: FastFoodApp/1.0" \
                 "https://restapi-fast-food-shop-system-with-nestjs-mongod-production.up.railway.app/api/v1/products"
        """.trimIndent()
        
        // Copy to clipboard
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("cURL Command", curlCommand)
        clipboard.setPrimaryClip(clip)
        
        tvResults.text = "📋 cURL command copied to clipboard!\n\n$curlCommand"
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "🧪 API Test Activity destroyed")
    }
} 