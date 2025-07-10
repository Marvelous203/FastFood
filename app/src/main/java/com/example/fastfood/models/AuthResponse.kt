package com.example.fastfood.models

import com.google.gson.annotations.SerializedName

data class AuthResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String? = null,
    
    @SerializedName("token")
    val token: String? = null,
    
    @SerializedName("user")
    val user: User? = null
) 