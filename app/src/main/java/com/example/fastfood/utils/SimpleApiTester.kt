package com.example.fastfood.utils

import android.util.Log
import com.example.fastfood.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Utility class for testing API connectivity and endpoints
 */
object SimpleApiTester {
    private const val TAG = "API_TEST"
    private const val BASE_URL = "https://restapi-fast-food-shop-system-with-nestjs-mongod-production.up.railway.app/api/v1/"
    private const val CONNECTIVITY_TEST_URL = "https://www.google.com"
    private const val TIMEOUT = 5000 // 5 seconds

    /**
     * Test basic internet connectivity
     * @return true if internet is available, false otherwise
     */
    suspend fun basicInternetTest(): Boolean {
        return try {
            val response = withContext(Dispatchers.IO) {
                val url = URL(CONNECTIVITY_TEST_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "HEAD"  // Use HEAD request for faster response
                    connectTimeout = TIMEOUT
                    readTimeout = TIMEOUT
                    instanceFollowRedirects = true
                    useCaches = false
                }
                val responseCode = connection.responseCode
                connection.disconnect()
                responseCode
            }
            Log.d(TAG, "🌐 Internet connectivity test response code: $response")
            response in 200..399
        } catch (e: Exception) {
            Log.e(TAG, "❌ Internet connectivity test failed", e)
            false
        }
    }

    /**
     * Test API server connectivity
     * @return true if API server is reachable, false otherwise
     */
    suspend fun pingTest(): Boolean {
        return try {
            val response = withContext(Dispatchers.IO) {
                val url = URL("${BASE_URL}products?limit=1")
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "GET"
                    connectTimeout = TIMEOUT
                    readTimeout = TIMEOUT
                }
                val responseCode = connection.responseCode
                connection.disconnect()
                responseCode
            }
            Log.d(TAG, "🏓 Ping test response code: $response")
            response in 200..299
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ping test failed", e)
            false
        }
    }

    /**
     * Perform a quick test of essential API endpoints
     */
    suspend fun quickTest() {
        try {
            Log.d(TAG, "🚀 Starting quick API test...")
            
            // Test products endpoint
            val apiService = RetrofitClient.create(com.example.fastfood.network.ApiService::class.java)
            val productsResponse = apiService.getFoods(limit = 1)
            Log.d(TAG, "📦 Products test: ${productsResponse.code()}")
            
            // Test categories endpoint (if available)
            try {
                val categoriesResponse = apiService.getCategories()
                Log.d(TAG, "🏷️ Categories test: ${categoriesResponse.code()}")
            } catch (e: Exception) {
                Log.e(TAG, "⚠️ Categories test failed", e)
            }
            
            Log.d(TAG, "✅ Quick test completed")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Quick test failed", e)
        }
    }

    /**
     * Perform a comprehensive test of all major API endpoints
     * @return String containing test results
     */
    suspend fun comprehensiveTest(): String {
        val results = mutableListOf<String>()
        
        try {
            val apiService = RetrofitClient.create(com.example.fastfood.network.ApiService::class.java)
            
            // Test 1: Products
            try {
                val response = apiService.getFoods(limit = 5)
                results.add("✅ Products API: ${response.code()}")
            } catch (e: Exception) {
                results.add("❌ Products API: ${e.message}")
            }
            
            // Test 2: Categories
            try {
                val response = apiService.getCategories()
                results.add("✅ Categories API: ${response.code()}")
            } catch (e: Exception) {
                results.add("❌ Categories API: ${e.message}")
            }
            
            // Test 3: Search
            try {
                val response = apiService.searchFoods("burger")
                results.add("✅ Search API: ${response.code()}")
            } catch (e: Exception) {
                results.add("❌ Search API: ${e.message}")
            }
            
        } catch (e: Exception) {
            results.add("❌ Test failed: ${e.message}")
        }
        
        return results.joinToString("\n")
    }
} 