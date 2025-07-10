package com.example.fastfood.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.fastfood.models.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.fastfood.network.RetrofitClient
import com.example.fastfood.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CartManager(private val context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val apiService: ApiService = RetrofitClient.getApiService()
    private val preferencesManager = PreferencesManager(context)
    
    suspend fun addToCart(productId: String, quantity: Int): Result<CartItem> {
        return withContext(Dispatchers.IO) {
            try {
                if (!preferencesManager.isLoggedIn) {
                    return@withContext Result.failure(Exception("Vui lòng đăng nhập để thêm vào giỏ hàng"))
                }

                val request = AddToCartRequest(productId, quantity)
                val response = apiService.addToCart(request)
                
                when {
                    response.isSuccessful && response.body() != null -> {
                        val cartResponse = response.body()!!
                        val addedItem = cartResponse.cart.items.find { it.productId == productId }
                            ?: return@withContext Result.failure(Exception("Không thể thêm sản phẩm vào giỏ hàng"))
                        
                        val cartItem = CartItem(
                            foodId = addedItem.productId,
                            quantity = addedItem.quantity
                        )
                        
                        // Update local storage
                        val currentItems = getCartItems().toMutableList()
                        val existingItemIndex = currentItems.indexOfFirst { it.foodId == productId }
                        if (existingItemIndex != -1) {
                            currentItems[existingItemIndex] = cartItem
                        } else {
                            currentItems.add(cartItem)
                        }
                        saveCartItems(currentItems)
                        
                        return@withContext Result.success(cartItem)
                    }
                    response.code() == 401 -> {
                        preferencesManager.logout()
                        return@withContext Result.failure(Exception("Phiên đăng nhập đã hết hạn"))
                    }
                    else -> {
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = if (!errorBody.isNullOrEmpty()) {
                            try {
                                gson.fromJson(errorBody, ErrorResponse::class.java)?.message
                                    ?: "Lỗi thêm vào giỏ hàng"
                            } catch (e: Exception) {
                                "Lỗi thêm vào giỏ hàng"
                            }
                        } else "Lỗi thêm vào giỏ hàng: ${response.message()}"
                        
                        return@withContext Result.failure(Exception(errorMessage))
                    }
                }
            } catch (e: Exception) {
                return@withContext Result.failure(Exception("Lỗi kết nối: ${e.message}"))
            }
        }
    }

    suspend fun fetchCart(): Result<List<CartItem>> {
        return withContext(Dispatchers.IO) {
            try {
                if (!preferencesManager.isLoggedIn) {
                    return@withContext Result.failure(Exception("Vui lòng đăng nhập để xem giỏ hàng"))
                }

                val response = apiService.getMyCart()
                
                when {
                    response.isSuccessful && response.body() != null -> {
                        val cartResponse = response.body()!!
                        val cartItems = cartResponse.cart.items.map { item ->
                            CartItem(
                                foodId = item.productId,
                                quantity = item.quantity
                            )
                        }
                        
                        // Save to local storage
                        saveCartItems(cartItems)
                        return@withContext Result.success(cartItems)
                    }
                    response.code() == 401 -> {
                        preferencesManager.logout()
                        return@withContext Result.failure(Exception("Phiên đăng nhập đã hết hạn"))
                    }
                    else -> {
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = if (!errorBody.isNullOrEmpty()) {
                            try {
                                gson.fromJson(errorBody, ErrorResponse::class.java)?.message
                                    ?: "Lỗi tải giỏ hàng"
                            } catch (e: Exception) {
                                "Lỗi tải giỏ hàng"
                            }
                        } else "Lỗi tải giỏ hàng: ${response.message()}"
                        
                        return@withContext Result.failure(Exception(errorMessage))
                    }
                }
            } catch (e: Exception) {
                return@withContext Result.failure(Exception("Lỗi kết nối: ${e.message}"))
            }
        }
    }

    suspend fun removeFromCart(foodId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (!preferencesManager.isLoggedIn) {
                    return@withContext Result.failure(Exception("Vui lòng đăng nhập để xóa sản phẩm"))
                }

                val response = apiService.removeItemFromCart(foodId)
                
                when {
                    response.isSuccessful -> {
                        // Remove from local storage
                        val cartItems = getCartItems().toMutableList()
                        cartItems.removeAll { it.foodId == foodId }
                        saveCartItems(cartItems)
                        return@withContext Result.success(Unit)
                    }
                    response.code() == 401 -> {
                        preferencesManager.logout()
                        return@withContext Result.failure(Exception("Phiên đăng nhập đã hết hạn"))
                    }
                    else -> {
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = if (!errorBody.isNullOrEmpty()) {
                            try {
                                gson.fromJson(errorBody, ErrorResponse::class.java)?.message
                                    ?: "Lỗi xóa sản phẩm"
                            } catch (e: Exception) {
                                "Lỗi xóa sản phẩm"
                            }
                        } else "Lỗi xóa sản phẩm: ${response.message()}"
                        
                        return@withContext Result.failure(Exception(errorMessage))
                    }
                }
            } catch (e: Exception) {
                return@withContext Result.failure(Exception("Lỗi kết nối: ${e.message}"))
            }
        }
    }

    suspend fun updateQuantity(foodId: String, quantity: Int): Result<CartItem> {
        return withContext(Dispatchers.IO) {
            try {
                if (!preferencesManager.isLoggedIn) {
                    return@withContext Result.failure(Exception("Vui lòng đăng nhập để cập nhật giỏ hàng"))
                }

                val request = UpdateCartItemRequest(quantity)
                val response = apiService.updateCartItem(foodId, request)
                
                when {
                    response.isSuccessful && response.body() != null -> {
                        val cartResponse = response.body()!!
                        val updatedItem = cartResponse.cart.items.find { it.productId == foodId }
                            ?: return@withContext Result.failure(Exception("Không thể cập nhật số lượng"))
                        
                        val cartItem = CartItem(
                            foodId = updatedItem.productId,
                            quantity = updatedItem.quantity
                        )
                        
                        // Update local storage
                        val currentItems = getCartItems().toMutableList()
                        val existingItemIndex = currentItems.indexOfFirst { it.foodId == foodId }
                        if (existingItemIndex != -1) {
                            currentItems[existingItemIndex] = cartItem
                        }
                        saveCartItems(currentItems)
                        
                        return@withContext Result.success(cartItem)
                    }
                    response.code() == 401 -> {
                        preferencesManager.logout()
                        return@withContext Result.failure(Exception("Phiên đăng nhập đã hết hạn"))
                    }
                    else -> {
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = if (!errorBody.isNullOrEmpty()) {
                            try {
                                gson.fromJson(errorBody, ErrorResponse::class.java)?.message
                                    ?: "Lỗi cập nhật số lượng"
                            } catch (e: Exception) {
                                "Lỗi cập nhật số lượng"
                            }
                        } else "Lỗi cập nhật số lượng: ${response.message()}"
                        
                        return@withContext Result.failure(Exception(errorMessage))
                    }
                }
            } catch (e: Exception) {
                return@withContext Result.failure(Exception("Lỗi kết nối: ${e.message}"))
            }
        }
    }

    fun getCartItems(): List<CartItem> {
        val json = sharedPreferences.getString(KEY_CART_ITEMS, "[]")
        val type = object : TypeToken<List<CartItem>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun clearCart() {
        saveCartItems(emptyList())
    }

    fun getItemCount(): Int {
        return getCartItems().sumOf { it.quantity }
    }

    fun getTotalPrice(): Double {
        return getCartItems().sumOf { it.quantity.toDouble() }  // Temporary fix - will need to fetch actual prices from API
    }

    private fun saveCartItems(items: List<CartItem>) {
        val json = gson.toJson(items)
        sharedPreferences.edit().putString(KEY_CART_ITEMS, json).apply()
    }

    companion object {
        private const val PREF_NAME = "cart_preferences"
        private const val KEY_CART_ITEMS = "cart_items"
    }
} 