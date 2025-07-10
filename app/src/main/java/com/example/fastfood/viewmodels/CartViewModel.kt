package com.example.fastfood.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fastfood.models.CartItem
import com.example.fastfood.models.Food
import com.example.fastfood.network.RetrofitClient
import com.example.fastfood.utils.CartManager
import com.example.fastfood.utils.Result
import kotlinx.coroutines.launch

class CartViewModel(application: Application) : AndroidViewModel(application) {
    
    private val cartManager = CartManager(application)
    private val apiService = RetrofitClient.getApiService()
    
    private val _addToCartResult = MutableLiveData<Result<CartItem>>()
    val addToCartResult: LiveData<Result<CartItem>> = _addToCartResult
    
    private val _cartItems = MutableLiveData<List<CartItem>>()
    val cartItems: LiveData<List<CartItem>> = _cartItems
    
    private val _totalPrice = MutableLiveData<Double>()
    val totalPrice: LiveData<Double> = _totalPrice
    
    private val _itemCount = MutableLiveData<Int>()
    val itemCount: LiveData<Int> = _itemCount
    
    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message
    
    private val _navigateToCheckout = MutableLiveData<Boolean>()
    val navigateToCheckout: LiveData<Boolean> = _navigateToCheckout
    
    init {
        loadCartItems()
    }

    suspend fun addToCart(productId: String, quantity: Int): Result<CartItem> {
        return try {
            val result = cartManager.addToCart(productId, quantity)
            when (result) {
                is Result.Success -> {
                    loadCartItems() // Reload cart after successful addition
                    _addToCartResult.value = result
                }
                is Result.Failure -> {
                    _addToCartResult.value = result
                }
            }
            result
        } catch (e: Exception) {
            val failureResult = Result.Failure(Exception("Lỗi thêm vào giỏ hàng: ${e.message}"))
            _addToCartResult.value = failureResult
            failureResult
        }
    }

    fun loadCartItems() {
        viewModelScope.launch {
            try {
                val result = cartManager.fetchCart()
                when (result) {
                    is Result.Success -> {
                        _cartItems.value = result.data
                        _totalPrice.value = cartManager.getTotalPrice()
                        _itemCount.value = cartManager.getItemCount()
                    }
                    is Result.Failure -> {
                        _message.value = result.exception.message
                    }
                }
            } catch (e: Exception) {
                _message.value = "Lỗi khi tải giỏ hàng: ${e.message}"
            }
        }
    }
    
    fun updateQuantity(foodId: String, quantity: Int) {
        viewModelScope.launch {
            try {
                val result = cartManager.updateQuantity(foodId, quantity)
                when (result) {
                    is Result.Success -> {
                        loadCartItems() // Reload cart after successful update
                    }
                    is Result.Failure -> {
                        _message.value = result.exception.message
                    }
                }
            } catch (e: Exception) {
                _message.value = "Lỗi khi cập nhật số lượng: ${e.message}"
            }
        }
    }
    
    fun removeFromCart(foodId: String) {
        viewModelScope.launch {
            try {
                val result = cartManager.removeFromCart(foodId)
                when (result) {
                    is Result.Success -> {
                        loadCartItems()
                        _message.value = "Đã xóa sản phẩm khỏi giỏ hàng"
                    }
                    is Result.Failure -> {
                        _message.value = result.exception.message
                    }
                }
            } catch (e: Exception) {
                _message.value = "Lỗi khi xóa sản phẩm: ${e.message}"
            }
        }
    }
    
    fun clearCart() {
        cartManager.clearCart()
        loadCartItems()
        _message.value = "Đã xóa toàn bộ giỏ hàng"
    }
    
    fun proceedToCheckout() {
        if (isCartEmpty()) {
            _message.value = "Giỏ hàng trống"
            return
        }
        _navigateToCheckout.value = true
    }
    
    fun onCheckoutNavigated() {
        _navigateToCheckout.value = false
    }
    
    private fun isCartEmpty(): Boolean {
        return cartItems.value.isNullOrEmpty()
    }

    suspend fun getProductDetails(productId: String): Food? {
        return try {
            val response = apiService.getProductById(productId)
            if (response.isSuccessful && response.body() != null) {
                response.body()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
} 