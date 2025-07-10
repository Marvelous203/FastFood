package com.example.fastfood

import android.app.Application
import com.example.fastfood.network.RetrofitClient

class FastFoodApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize RetrofitClient
        RetrofitClient.initialize(this)
    }
} 