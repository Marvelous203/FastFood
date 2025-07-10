package com.example.fastfood.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.fastfood.models.User
import com.google.gson.Gson

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        private const val PREFS_NAME = "fastfood_prefs"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER = "user_data"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_CART_ITEMS = "cart_items"
    }
    
    var authToken: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()
    
    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()
    
    var currentUser: User?
        get() {
            val userJson = prefs.getString(KEY_USER, null)
            return if (userJson != null) {
                try {
                    gson.fromJson(userJson, User::class.java)
                } catch (e: Exception) {
                    null
                }
            } else null
        }
        set(value) {
            if (value != null) {
                val userJson = gson.toJson(value)
                prefs.edit().putString(KEY_USER, userJson).apply()
            } else {
                prefs.edit().remove(KEY_USER).apply()
            }
        }
    
    fun getAuthHeader(): String? {
        return authToken?.let { "Bearer $it" }
    }
    
    fun logout() {
        prefs.edit().clear().apply()
    }
    
    fun saveCartItems(cartItems: String) {
        prefs.edit().putString(KEY_CART_ITEMS, cartItems).apply()
    }
    
    fun getCartItems(): String? {
        return prefs.getString(KEY_CART_ITEMS, null)
    }
    
    fun clearCart() {
        prefs.edit().remove(KEY_CART_ITEMS).apply()
    }
} 