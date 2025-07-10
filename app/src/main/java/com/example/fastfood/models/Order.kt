package com.example.fastfood.models

import com.google.gson.annotations.SerializedName

data class Order(
    @SerializedName("_id")
    val id: String? = null,
    
    @SerializedName("user_id")
    val userId: String,
    
    @SerializedName("items")
    val items: List<OrderItem>,
    
    @SerializedName("total_amount")
    val totalAmount: Double,
    
    @SerializedName("status")
    val status: String = "pending", // pending, confirmed, preparing, ready, delivered, cancelled
    
    @SerializedName("payment_method")
    val paymentMethod: String = "cash", // cash, card, online
    
    @SerializedName("delivery_address")
    val deliveryAddress: String? = null,
    
    @SerializedName("phone")
    val phone: String,
    
    @SerializedName("note")
    val note: String? = null,
    
    @SerializedName("created_at")
    val createdAt: String? = null,
    
    @SerializedName("estimated_delivery_time")
    val estimatedDeliveryTime: String? = null
)

data class OrderItem(
    @SerializedName("food_id")
    val foodId: String,
    
    @SerializedName("food_name")
    val foodName: String,
    
    @SerializedName("quantity")
    val quantity: Int,
    
    @SerializedName("price")
    val price: Double,
    
    @SerializedName("note")
    val note: String? = null
)

data class OrderRequest(
    @SerializedName("items")
    val items: List<OrderItem>,
    
    @SerializedName("delivery_address")
    val deliveryAddress: String? = null,
    
    @SerializedName("phone")
    val phone: String,
    
    @SerializedName("payment_method")
    val paymentMethod: String = "cash",
    
    @SerializedName("note")
    val note: String? = null
)

// Response type aliases for better readability
typealias OrderResponse = BaseResponse<Order>
typealias OrdersResponse = BaseResponse<List<Order>> 