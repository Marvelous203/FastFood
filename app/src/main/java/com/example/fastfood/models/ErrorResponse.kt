package com.example.fastfood.models

import com.google.gson.annotations.SerializedName

data class ErrorResponse(
    @SerializedName("message")
    val message: String?,
    
    @SerializedName("error")
    val error: String?,
    
    @SerializedName("statusCode")
    val statusCode: Int?
) 