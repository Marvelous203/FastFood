package com.example.fastfood.models

import com.google.gson.annotations.SerializedName

data class Order(
    @SerializedName("id")
    val id: String? = null,

    @SerializedName("userId")
    val userId: String? = null,

    @SerializedName("cartId")
    val cartId: String? = null,

    @SerializedName("items")
    val items: List<OrderItem>? = null,

    @SerializedName("total")
    val total: Double? = null,

    @SerializedName("status")
    val status: String = "pending", // pending, confirmed, preparing, ready, delivered, cancelled

    @SerializedName("notes")
    val notes: String? = null,

    @SerializedName("createdAt")
    val createdAt: String? = null,

    @SerializedName("updatedAt")
    val updatedAt: String? = null,

    @SerializedName("deletedAt")
    val deletedAt: String? = null,

    // Legacy fields - keeping for backward compatibility
    @SerializedName("payment_method")
    val paymentMethod: String = "cash", // cash, card, online

    @SerializedName("delivery_address")
    val deliveryAddress: String? = null,

    @SerializedName("phone")
    val phone: String? = null,

    @SerializedName("note")
    val note: String? = null,

    @SerializedName("estimated_delivery_time")
    val estimatedDeliveryTime: String? = null
) {
    // Getter for totalAmount to maintain backward compatibility
    val totalAmount: Double?
        get() = total
}

data class OrderItem(
    @SerializedName("productId")
    val productId: String,

    @SerializedName("productName")
    val productName: String,

    @SerializedName("quantity")
    val quantity: Int,

    @SerializedName("price")
    val price: Double,

    @SerializedName("note")
    val note: String? = null
) {
    // Legacy getters for backward compatibility
    val foodId: String
        get() = productId

    val foodName: String
        get() = productName
}

data class OrdersApiResponse(
    @SerializedName("data")
    val data: List<Order>,

    @SerializedName("hasNextPage")
    val hasNextPage: Boolean = false
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
