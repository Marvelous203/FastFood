package com.example.fastfood.models

import com.google.gson.annotations.SerializedName

data class Food(
    val id: String,
    val name: String,
    val description: String?,
    val price: Double,
    val quantity: Int,
    val discount: Int,
    val categoryId: String,
    val images: List<ProductImage>,
    val brand: String?,
    val weight: String?,
    val ingredients: String?,
    @SerializedName("nutritionFacts")
    val nutrition: String?,
    val expiryDate: String?,
    val origin: String?,
    val packaging: String?,
    val storageInstructions: String?,
    val usageInstructions: String?,
    val isVegetarian: Boolean,
    val isVegan: Boolean,
    val allergens: String?,
    val servingSize: String?,
    val countryOfManufacture: String?,
    val rating: Double,
    val stock: Int,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String?
)

data class ProductImage(
    @SerializedName("id") val id: String,
    @SerializedName("path") val path: String
)

data class FoodCategory(
    val id: String,
    val name: String,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    // Backward compatibility properties
    val image: String? = null,
    val description: String? = null
)

// Image/File related responses
data class ImageResponse(
    val url: String,
    val id: String? = null,
    val path: String? = null
)

data class FileUploadResponse(
    val file: ImageResponse,
    val message: String? = null
)

// Response type aliases for better readability
typealias FoodResponse = BaseResponse<Food>
typealias FoodListResponse = BaseResponse<List<Food>>
typealias CategoryResponse = BaseResponse<FoodCategory>
typealias CategoryListResponse = BaseResponse<List<FoodCategory>> 