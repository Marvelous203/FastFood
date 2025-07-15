package com.example.fastfood.models

import com.google.gson.annotations.SerializedName
import java.util.Date

data class Payment(
    @SerializedName("id")
    val id: String,

    @SerializedName("orderId")
    val orderId: String,

    @SerializedName("userId")
    val userId: String,

    @SerializedName("method")
    val method: PaymentMethod,

    @SerializedName("status")
    val status: PaymentStatus,

    @SerializedName("amount")
    val amount: Double,

    @SerializedName("currency")
    val currency: String = "VND",

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("transactionId")
    val transactionId: String? = null,

    @SerializedName("gatewayReference")
    val gatewayReference: String? = null,

    @SerializedName("gatewayResponse")
    val gatewayResponse: Map<String, Any>? = null,

    @SerializedName("createdAt")
    val createdAt: Date,

    @SerializedName("updatedAt")
    val updatedAt: Date,

    @SerializedName("paidAt")
    val paidAt: Date? = null
)

enum class PaymentMethod(val value: String, val displayName: String) {
    @SerializedName("CASH")
    CASH("CASH", "Thanh toán tiền mặt"),

    @SerializedName("ZALOPAY")
    ZALOPAY("ZALOPAY", "ZaloPay"),

    @SerializedName("CREDIT_CARD")
    CREDIT_CARD("CREDIT_CARD", "Thẻ tín dụng"),

    @SerializedName("BANK_TRANSFER")
    BANK_TRANSFER("BANK_TRANSFER", "Chuyển khoản ngân hàng");

    companion object {
        fun fromString(value: String): PaymentMethod {
            return values().find { it.value == value } ?: CASH
        }
    }
}

enum class PaymentStatus(val value: String, val displayName: String) {
    @SerializedName("PENDING")
    PENDING("PENDING", "Đang chờ xử lý"),

    @SerializedName("PROCESSING")
    PROCESSING("PROCESSING", "Đang xử lý"),

    @SerializedName("COMPLETED")
    COMPLETED("COMPLETED", "Hoàn thành"),

    @SerializedName("FAILED")
    FAILED("FAILED", "Thất bại"),

    @SerializedName("CANCELLED")
    CANCELLED("CANCELLED", "Đã hủy"),

    @SerializedName("REFUNDED")
    REFUNDED("REFUNDED", "Đã hoàn tiền");

    companion object {
        fun fromString(value: String): PaymentStatus {
            return values().find { it.value == value } ?: PENDING
        }
    }
}
