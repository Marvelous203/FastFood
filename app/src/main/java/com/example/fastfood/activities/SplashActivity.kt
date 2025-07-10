package com.example.fastfood.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.fastfood.R
import com.example.fastfood.utils.PreferencesManager
import com.example.fastfood.utils.NotificationHelper

class SplashActivity : AppCompatActivity() {
    
    private lateinit var preferencesManager: PreferencesManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        // Set status bar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = ContextCompat.getColor(this, R.color.primary_dark)
        }
        
        preferencesManager = PreferencesManager(this)
        
        // Delay for 2.5 seconds then navigate
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToNextActivity()
        }, 2500)
    }
    
    private fun navigateToNextActivity() {
        val intent = if (preferencesManager.isLoggedIn) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, AuthActivity::class.java)
        }
        
        // Check for cart items and show notification if user is logged in
        if (preferencesManager.isLoggedIn) {
            val notificationHelper = NotificationHelper(this)
            notificationHelper.checkAndShowCartNotification()
        }
        
        startActivity(intent)
        finish()
        
        // Add transition animation
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
} 