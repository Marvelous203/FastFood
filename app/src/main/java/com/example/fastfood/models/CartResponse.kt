package com.example.fastfood.models

import com.google.gson.annotations.SerializedName

data class CartResponse(
    @SerializedName("cart")
    val cart: Cart,
    
    @SerializedName("hasNextPage")
    val hasNextPage: Boolean,
    
    @SerializedName("totalItems")
    val totalItems: Int
)

data class CartItemResponse(
    @SerializedName("productId")
    val productId: String,
    
    @SerializedName("quantity")
    val quantity: Int
) 