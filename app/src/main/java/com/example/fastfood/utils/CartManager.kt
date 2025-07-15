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

                        // Lưu cartId từ response
                        saveCartId(cartResponse.cart.id)

                        val addedItem = cartResponse.cart.items.find { it.productId == productId }
                            ?: return@withContext Result.failure(Exception("Không thể thêm sản phẩm vào giỏ hàng"))

                        // Lấy thông tin sản phẩm từ API với retry mechanism
                        var price = 0.0
                        var name = "Sản phẩm"
                        var image = ""
                        
                        // Thử lấy thông tin sản phẩm với retry
                        var retryCount = 0
                        val maxRetries = 2
                        
                        while (retryCount <= maxRetries) {
                            try {
                                val productResponse = apiService.getProductById(addedItem.productId)
                                if (productResponse.isSuccessful && productResponse.body() != null) {
                                    val product = productResponse.body()!!
                                    price = product.price
                                    name = product.name
                                    image = if (product.images.isNotEmpty()) {
                                        product.images[0].path
                                    } else ""
                                    break // Thành công, thoát khỏi loop
                                } else {
                                    retryCount++
                                    if (retryCount <= maxRetries) {
                                        kotlinx.coroutines.delay(1000) // Đợi 1 giây trước khi retry
                                    }
                                }
                            } catch (e: Exception) {
                                retryCount++
                                if (retryCount <= maxRetries) {
                                    kotlinx.coroutines.delay(1000) // Đợi 1 giây trước khi retry
                                } else {
                                    // Nếu không lấy được thông tin sản phẩm sau nhiều lần thử,
                                    // vẫn trả về thành công nhưng với thông tin cơ bản
                                    android.util.Log.w("CartManager", "Không thể lấy thông tin sản phẩm: ${e.message}")
                                }
                            }
                        }

                        val cartItem = CartItem(
                            foodId = addedItem.productId,
                            quantity = addedItem.quantity,
                            price = price,
                            name = name,
                            image = image
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

                        // Lưu cartId từ response
                        saveCartId(cartResponse.cart.id)

                        val cartItems = cartResponse.cart.items.map { item ->
                            // Lấy thông tin sản phẩm từ API
                            var price = 0.0
                            var name = ""
                            var image = ""
                            try {
                                val productResponse = apiService.getProductById(item.productId)
                                if (productResponse.isSuccessful && productResponse.body() != null) {
                                    val product = productResponse.body()!!
                                    price = product.price
                                    name = product.name
                                    image = if (product.images.isNotEmpty()) {
                                        product.images[0].path
                                    } else ""
                                }
                            } catch (e: Exception) {
                                // Nếu không lấy được thông tin sản phẩm, vẫn tiếp tục
                            }

                            CartItem(
                                foodId = item.productId,
                                quantity = item.quantity,
                                price = price,
                                name = name,
                                image = image
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

                        // Lưu cartId từ response
                        saveCartId(cartResponse.cart.id)

                        val updatedItem = cartResponse.cart.items.find { it.productId == foodId }
                            ?: return@withContext Result.failure(Exception("Không thể cập nhật số lượng"))

                        // Lấy thông tin sản phẩm từ API hoặc từ cache
                        val existingItem = getCartItems().find { it.foodId == foodId }
                        var price = existingItem?.price ?: 0.0
                        var name = existingItem?.name ?: ""
                        var image = existingItem?.image ?: ""

                        // Nếu chưa có thông tin sản phẩm, fetch từ API
                        if (price == 0.0 || name.isEmpty()) {
                            try {
                                val productResponse = apiService.getProductById(updatedItem.productId)
                                if (productResponse.isSuccessful && productResponse.body() != null) {
                                    val product = productResponse.body()!!
                                    price = product.price
                                    name = product.name
                                    image = if (product.images.isNotEmpty()) {
                                        product.images[0].path
                                    } else ""
                                }
                            } catch (e: Exception) {
                                // Nếu không lấy được thông tin sản phẩm, vẫn tiếp tục
                            }
                        }

                        val cartItem = CartItem(
                            foodId = updatedItem.productId,
                            quantity = updatedItem.quantity,
                            price = price,
                            name = name,
                            image = image
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
        // Xóa cartId khi clear cart
        sharedPreferences.edit().remove("CART_ID").apply()
    }

    fun getItemCount(): Int {
        return getCartItems().sumOf { it.quantity }
    }

    fun getCartId(): String? {
        return sharedPreferences.getString("CART_ID", null)
    }

    private fun saveCartId(cartId: String) {
        sharedPreferences.edit().putString("CART_ID", cartId).apply()
    }

    suspend fun refreshCartItemsInfo(): Result<List<CartItem>> {
        return withContext(Dispatchers.IO) {
            try {
                val cartItems = getCartItems().toMutableList()

                for (i in cartItems.indices) {
                    val item = cartItems[i]
                    try {
                        val productResponse = apiService.getProductById(item.foodId)
                        if (productResponse.isSuccessful && productResponse.body() != null) {
                            val product = productResponse.body()!!
                            val price = product.price
                            val name = product.name
                            val imageUrl = if (product.images.isNotEmpty()) {
                                product.images[0].path
                            } else ""

                            cartItems[i] = item.copy(
                                price = price,
                                name = name,
                                image = imageUrl
                            )
                        }
                    } catch (e: Exception) {
                        // Nếu không lấy được thông tin sản phẩm, giữ nguyên item cũ
                        continue
                    }
                }

                saveCartItems(cartItems)
                Result.success(cartItems)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getTotalPrice(): Double {
        return withContext(Dispatchers.IO) {
            val cartItems = getCartItems().toMutableList()
            var total = 0.0
            var needToSave = false

            for (i in cartItems.indices) {
                val item = cartItems[i]

                if (item.price > 0) {
                    // Nếu đã có giá trong cache, sử dụng luôn
                    total += item.price * item.quantity
                } else {
                    // Nếu chưa có giá, fetch từ API
                    try {
                        val productResponse = apiService.getProductById(item.foodId)
                        if (productResponse.isSuccessful && productResponse.body() != null) {
                            val product = productResponse.body()!!
                            val price = product.price
                            val name = product.name
                            val imageUrl = if (product.images.isNotEmpty()) {
                                product.images[0].path
                            } else ""

                            // Cập nhật thông tin sản phẩm vào cache
                            cartItems[i] = item.copy(
                                price = price,
                                name = name,
                                image = imageUrl
                            )
                            needToSave = true

                            total += price * item.quantity
                        }
                    } catch (e: Exception) {
                        // Nếu có lỗi khi fetch API, bỏ qua item này
                        continue
                    }
                }
            }

            // Lưu lại cart items đã được cập nhật thông tin nếu cần
            if (needToSave) {
                saveCartItems(cartItems)
            }

            // Debug: Log tổng tiền
            android.util.Log.d("CartManager", "📊 Tính tổng tiền:")
            cartItems.forEach { item ->
                android.util.Log.d("CartManager", "   • ${item.name} (${item.foodId}): ${item.price} x ${item.quantity} = ${item.price * item.quantity}")
            }
            android.util.Log.d("CartManager", "   💰 Tổng cộng: $total")

            total
        }
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
