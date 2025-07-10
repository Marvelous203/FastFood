package com.example.fastfood.utils

import com.google.gson.annotations.SerializedName

/**
 * Lightweight models for performance testing
 * Only essential fields to reduce parsing overhead
 */

data class LightweightProductsResponse(
    val success: Boolean,
    val message: String,
    val data: List<LightweightProduct>
)

data class LightweightProduct(
    val id: String,
    val name: String,
    val description: String?,
    val price: Int,
    val categoryId: String,
    val images: List<LightweightImage>?,
    val rating: Float = 0f,
    val stock: Int = 0
    // Removed heavy fields: brand, weight, ingredients, nutritionFacts, etc.
)

data class LightweightImage(
    val id: String,
    val path: String
)

/**
 * Even more minimal model - just core display data
 */
data class MinimalProductsResponse(
    val success: Boolean,
    val message: String,
    val data: List<MinimalProduct>
)

data class MinimalProduct(
    val id: String,
    val name: String,
    val price: Int,
    @SerializedName("images")
    val imageUrls: List<Map<String, String>>? // Just get path as string
) {
    // Helper to get first image URL
    val mainImageUrl: String?
        get() = imageUrls?.firstOrNull()?.get("path")
}

/**
 * Ultra-minimal - for extreme performance testing
 */
data class UltraMinimalResponse(
    val success: Boolean,
    val message: String,
    val data: List<Map<String, Any>>
) 