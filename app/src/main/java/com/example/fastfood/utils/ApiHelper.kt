package com.example.fastfood.utils

import com.example.fastfood.models.Food
import java.text.NumberFormat
import java.util.*

object ApiHelper {
    
    /**
     * Format price in Vietnamese Dong
     */
    fun formatPrice(price: Double): String {
        return try {
            val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
            formatter.format(price)
        } catch (e: Exception) {
            "${price.toInt()}₫"
        }
    }
    
    /**
     * Format rating display
     */
    fun formatRating(rating: Float): String {
        return if (rating > 0) String.format("%.1f", rating) else ""
    }
    
    /**
     * Get stock status message
     */
    fun getStockStatusMessage(stock: Int): String {
        return when {
            stock > 20 -> "Còn hàng"
            stock > 0 -> "Còn $stock sản phẩm"
            else -> "Hết hàng"
        }
    }
    
    /**
     * Get stock status color resource
     */
    fun getStockStatusColor(stock: Int): Int {
        return when {
            stock > 20 -> android.R.color.holo_green_dark
            stock > 0 -> android.R.color.holo_orange_dark
            else -> android.R.color.holo_red_dark
        }
    }
    
    /**
     * Check if product is available
     */
    fun isProductAvailable(food: Food): Boolean {
        return food.stock > 0 && food.deletedAt == null
    }
    
    /**
     * Get vegetarian/vegan indicator text
     */
    fun getVegIndicator(food: Food): String? {
        return when {
            food.isVegan == true -> "🌱 Thuần chay"
            food.isVegetarian == true -> "🥬 Chay"
            else -> null
        }
    }
    
    /**
     * Filter available products from list
     */
    fun filterAvailableProducts(products: List<Food>): List<Food> {
        return products.filter { isProductAvailable(it) }
    }
    
    /**
     * Log product information for debugging
     */
    fun logProductInfo(tag: String, food: Food) {
        android.util.Log.d(tag, "Product: ${food.name}")
        android.util.Log.d(tag, "Price: ${formatPrice(food.price)}")
        android.util.Log.d(tag, "Stock: ${food.stock}")
        android.util.Log.d(tag, "Images: ${food.images.size}")
        android.util.Log.d(tag, "Available: ${isProductAvailable(food)}")
    }
    
    /**
     * Get main image URL with fallback
     */
    fun getImageUrl(food: Food): String? {
        return food.images.firstOrNull()?.path?.takeIf { it.isNotBlank() }
    }
} 