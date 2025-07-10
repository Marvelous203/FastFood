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



data class CustomOrderRequest(
    @SerializedName("items")
    val items: List<CustomOrderItem>,
    
    @SerializedName("delivery_address")
    val deliveryAddress: String? = null,
    
    @SerializedName("phone")
    val phone: String,
    
    @SerializedName("payment_method")
    val paymentMethod: String = "cash",
    
    @SerializedName("note")
    val note: String? = null
)

data class CustomOrderItem(
    @SerializedName("food_id")
    val foodId: String,
    
    @SerializedName("quantity")
    val quantity: Int,
    
    @SerializedName("note")
    val note: String? = null
)

data class ZaloPayRequest(
    @SerializedName("order_id")
    val orderId: String,
    
    @SerializedName("amount")
    val amount: Double,
    
    @SerializedName("description")
    val description: String? = null,
    
    @SerializedName("bank_code")
    val bankCode: String? = null
)

data class ZaloPayResponse(
    @SerializedName("order_url")
    val orderUrl: String,
    
    @SerializedName("order_token")
    val orderToken: String,
    
    @SerializedName("return_url")
    val returnUrl: String? = null,
    
    @SerializedName("zp_trans_token")
    val zpTransToken: String? = null
)

data class Payment(
    @SerializedName("_id")
    val id: String,
    
    @SerializedName("order_id")
    val orderId: String,
    
    @SerializedName("user_id")
    val userId: String,
    
    @SerializedName("amount")
    val amount: Double,
    
    @SerializedName("payment_method")
    val paymentMethod: String,
    
    @SerializedName("status")
    val status: String,
    
    @SerializedName("transaction_id")
    val transactionId: String? = null,
    
    @SerializedName("created_at")
    val createdAt: String? = null,
    
    @SerializedName("updated_at")
    val updatedAt: String? = null
) 