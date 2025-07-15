package com.example.fastfood.models

import com.google.gson.annotations.SerializedName

data class Cart(
    @SerializedName("id")
    val id: String,

    @SerializedName("userId")
    val userId: String,

    @SerializedName("isActive")
    val isActive: Boolean,

    @SerializedName("items")
    val items: List<CartItemResponse>,

    @SerializedName("createdAt")
    val createdAt: String,

    @SerializedName("updatedAt")
    val updatedAt: String
)



// Removed duplicate models - now using separate model files
