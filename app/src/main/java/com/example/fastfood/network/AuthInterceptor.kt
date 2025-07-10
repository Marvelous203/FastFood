package com.example.fastfood.network

import android.content.Context
import com.example.fastfood.utils.PreferencesManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(context: Context) : Interceptor {
    private val preferencesManager = PreferencesManager(context)

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Get the token from PreferencesManager
        val token = preferencesManager.authToken
        
        // If we have a token, add it to the request
        val newRequest = if (!token.isNullOrEmpty()) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }
        
        return chain.proceed(newRequest)
    }
} 