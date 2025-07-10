package com.example.fastfood.models

import com.google.gson.annotations.SerializedName

data class UpdateCartItemRequest(
    @SerializedName("quantity")
    val quantity: Int,
    
    @SerializedName("note")
    val note: String? = null
) 