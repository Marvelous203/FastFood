package com.example.fastfood.utils

import android.content.Context
import android.util.Log
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.fastfood.R
import java.net.URLDecoder

object ImageUtils {
    
    /**
     * Clean and fix problematic S3 URLs
     */
    fun cleanImageUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        
        try {
            // Log original URL for debugging
            Log.d("ImageUtils", "Original URL: $url")
            
            // Check if URL is double-encoded
            if (url.contains("https%3A//")) {
                Log.d("ImageUtils", "URL appears to be double-encoded, attempting to decode...")
                
                // Try to extract the inner URL
                val decoded = URLDecoder.decode(url, "UTF-8")
                Log.d("ImageUtils", "Decoded URL: $decoded")
                
                // Look for the actual image URL pattern
                val s3Pattern = "https://prm392-aws-s3-cloud\\.s3\\.ap-southeast-2\\.amazonaws\\.com/[^?]+".toRegex()
                val match = s3Pattern.find(decoded)
                
                if (match != null) {
                    val cleanUrl = match.value
                    Log.d("ImageUtils", "Extracted clean URL: $cleanUrl")
                    return cleanUrl
                }
            }
            
            // If URL starts with S3 domain directly, check if it's valid
            if (url.startsWith("https://prm392-aws-s3-cloud.s3.ap-southeast-2.amazonaws.com/")) {
                // Check if it has AWS signature parameters that might be expired
                if (url.contains("X-Amz-Signature")) {
                    Log.w("ImageUtils", "URL contains AWS signature, might be expired")
                    // Try to use upload API to get fresh URL
                    return null // Will trigger upload API fallback
                }
                return url
            }
            
            // Return original URL if no issues detected
            return url
            
        } catch (e: Exception) {
            Log.e("ImageUtils", "Error cleaning URL: ${e.message}")
            return null
        }
    }
    
    /**
     * Load image with multiple fallback strategies
     */
    fun loadImageWithFallback(
        context: Context,
        imageView: ImageView,
        imageUrl: String?,
        productId: String? = null
    ) {
        val cleanUrl = cleanImageUrl(imageUrl)
        
        Log.d("ImageUtils", "Loading image for product: $productId")
        Log.d("ImageUtils", "Clean URL: $cleanUrl")
        
        // Primary strategy: Try to load cleaned URL
        if (!cleanUrl.isNullOrBlank()) {
            Glide.with(context)
                .load(cleanUrl)
                .apply(getOptimizedRequestOptions())
                .error(R.drawable.placeholder_food) // Fallback to placeholder on error
                .into(imageView)
        } else {
            // If URL is null or problematic, try to fetch fresh URL from upload API
            Log.d("ImageUtils", "URL is problematic, using placeholder")
            
            // For now, just show placeholder
            // TODO: Implement API call to get fresh image URL using productId
            imageView.setImageResource(R.drawable.placeholder_food)
            
            // Optional: Try to fetch fresh URL from upload API
            if (!productId.isNullOrBlank()) {
                fetchFreshImageUrl(context, imageView, productId)
            }
        }
    }
    
    /**
     * Get optimized Glide request options
     */
    private fun getOptimizedRequestOptions(): RequestOptions {
        return RequestOptions()
            .override(200, 200)
            .centerCrop()
            .diskCacheStrategy(DiskCacheStrategy.DATA)
            .timeout(5000)
            .placeholder(R.drawable.placeholder_food)
            .error(R.drawable.placeholder_food)
    }
    
    /**
     * Fetch fresh image URL from upload API (if needed)
     */
    private fun fetchFreshImageUrl(
        context: Context,
        imageView: ImageView,
        productId: String
    ) {
        Log.d("ImageUtils", "Attempting to fetch fresh URL for product: $productId")
        
        // For now, just use placeholder since this requires network call
        // In a real implementation, you would:
        // 1. Make API call to get fresh image URL
        // 2. Update the image if successful
        // 3. Cache the result
        
        // Example implementation (commented out):
        /*
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getProductImage(productId)
                if (response.isSuccessful) {
                    response.body()?.let { imageResponse ->
                        Glide.with(context)
                            .load(imageResponse.url)
                            .apply(getOptimizedRequestOptions())
                            .into(imageView)
                    }
                }
            } catch (e: Exception) {
                Log.e("ImageUtils", "Failed to fetch fresh image URL", e)
            }
        }
        */
        
        Log.d("ImageUtils", "Using placeholder for now - fresh URL fetch not implemented")
    }
    
    /**
     * Test if an image URL is likely to work
     */
    fun isImageUrlValid(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        
        return try {
            // Basic URL validation
            url.startsWith("http") && 
            !url.contains("X-Amz-Date=202506") && // Check for old dates
            !url.contains("https%3A//") // Check for double encoding
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Extract date from AWS URL to check if expired
     */
    fun extractAwsDate(url: String): String? {
        return try {
            val datePattern = "X-Amz-Date=([^&]+)".toRegex()
            datePattern.find(url)?.groupValues?.get(1)
        } catch (e: Exception) {
            null
        }
    }
} 