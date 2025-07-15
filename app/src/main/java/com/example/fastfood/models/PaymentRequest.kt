package com.example.fastfood.models

import com.google.gson.annotations.SerializedName

data class ZaloPaymentRequest(
    @SerializedName("orderId")
    val orderId: String,

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("returnUrl")
    val returnUrl: String? = null
)

data class CashPaymentRequest(
    @SerializedName("description")
    val description: String? = null
)

data class PaymentCallbackRequest(
    @SerializedName("data")
    val data: String,

    @SerializedName("mac")
    val mac: String,

    @SerializedName("type")
    val type: Int
)

data class ZaloPaymentResponse(
    @SerializedName("orderToken")
    val orderToken: String,

    @SerializedName("zpTransToken")
    val zpTransToken: String,

    @SerializedName("appTransId")
    val appTransId: String,

    @SerializedName("payment")
    val payment: Payment,

    @SerializedName("orderData")
    val orderData: Map<String, Any>
)

data class PaymentStatusUpdateRequest(
    @SerializedName("status")
    val status: String
)
