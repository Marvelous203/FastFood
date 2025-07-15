package com.example.fastfood.models

import com.google.gson.annotations.SerializedName

data class CustomOrderRequest(
    @SerializedName("cartId")
    val cartId: String,

    @SerializedName("productIds")
    val productIds: List<String>,

    @SerializedName("notes")
    val notes: String? = null
)

data class CustomOrderFromCartRequest(
    @SerializedName("cartId")
    val cartId: String,

    @SerializedName("productIds")
    val productIds: List<String>,

    @SerializedName("notes")
    val notes: String? = null
)

data class CustomProduct(
    @SerializedName("name")
    val name: String,

    @SerializedName("price")
    val price: Double,

    @SerializedName("quantity")
    val quantity: Int,

    @SerializedName("description")
    val description: String? = null
)
