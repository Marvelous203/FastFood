package com.example.fastfood.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fastfood.models.CartItem
import com.example.fastfood.models.Food
import com.example.fastfood.models.Order
import com.example.fastfood.models.PaymentMethod
import com.example.fastfood.network.RetrofitClient
import com.example.fastfood.utils.CartManager
import com.example.fastfood.utils.PreferencesManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BillingViewModel(application: Application) : AndroidViewModel(application) {

    private val cartManager = CartManager(application)
    private val preferencesManager = PreferencesManager(application)
    private val apiService = RetrofitClient.getApiService()

    private val _cartItems = MutableLiveData<MutableList<CartItem>>()
    val cartItems: LiveData<MutableList<CartItem>> = _cartItems

    private val _totalPrice = MutableLiveData<Double>()
    val totalPrice: LiveData<Double> = _totalPrice

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _paymentSuccess = MutableLiveData<Boolean>()
    val paymentSuccess: LiveData<Boolean> = _paymentSuccess

    // ZaloPay QR dialog
    private val _showZaloPayQR = MutableLiveData<Boolean>()
    val showZaloPayQR: LiveData<Boolean> = _showZaloPayQR

    private val _selectedPaymentMethod = MutableLiveData<PaymentMethod>()
    val selectedPaymentMethod: LiveData<PaymentMethod> = _selectedPaymentMethod

    private val _deliveryAddress = MutableLiveData<String>()
    val deliveryAddress: LiveData<String> = _deliveryAddress

    private val _deliveryNote = MutableLiveData<String>()
    val deliveryNote: LiveData<String> = _deliveryNote

    private val _phoneNumber = MutableLiveData<String>()
    val phoneNumber: LiveData<String> = _phoneNumber

    private val _customerName = MutableLiveData<String>()
    val customerName: LiveData<String> = _customerName

    init {
        // Initialize with user data
        val currentUser = preferencesManager.currentUser
        _customerName.value = currentUser?.fullName ?: ""
        _phoneNumber.value = currentUser?.phone ?: ""
        _selectedPaymentMethod.value = PaymentMethod.CASH

        // Initialize cart data
        updateCartData()
    }

        private fun updateCartData() {
        viewModelScope.launch {
            try {
                // Refresh thông tin sản phẩm để đảm bảo có giá
                cartManager.refreshCartItemsInfo()

                _cartItems.value = cartManager.getCartItems().toMutableList()

                // Tính tổng tiền từ API (suspend function)
                val totalPrice = cartManager.getTotalPrice()
                _totalPrice.value = totalPrice
            } catch (e: Exception) {
                _error.value = "Không thể tính tổng tiền: ${e.message}"
            }
        }
    }

    fun setPaymentMethod(method: PaymentMethod) {
        _selectedPaymentMethod.value = method
    }

    fun setDeliveryAddress(address: String) {
        _deliveryAddress.value = address
    }

    fun setDeliveryNote(note: String) {
        _deliveryNote.value = note
    }

    fun setPhoneNumber(phone: String) {
        _phoneNumber.value = phone
    }

    fun setCustomerName(name: String) {
        _customerName.value = name
    }

    fun processPayment() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // Validate order
                if (!validateOrder()) {
                    _isLoading.value = false
                    return@launch
                }

                // Simulate payment processing
                delay(2000)

                when (_selectedPaymentMethod.value) {
                    PaymentMethod.CASH -> {
                        processCashPayment()
                    }
                    PaymentMethod.CREDIT_CARD -> {
                        processCreditCardPayment()
                    }
                    PaymentMethod.BANK_TRANSFER -> {
                        processBankTransfer()
                    }
                    PaymentMethod.ZALOPAY -> {
                        processZaloPayPayment()
                    }
                    else -> {
                        _error.value = "Phương thức thanh toán không hợp lệ"
                    }
                }

            } catch (e: Exception) {
                _error.value = "Lỗi xử lý thanh toán: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun validateOrder(): Boolean {
        val items = cartItems.value
        if (items.isNullOrEmpty()) {
            _error.value = "Giỏ hàng trống"
            android.util.Log.d("BillingViewModel", "Validation failed: Cart is empty")
            return false
        }

        if (_customerName.value.isNullOrBlank()) {
            _error.value = "Vui lòng nhập tên"
            android.util.Log.d("BillingViewModel", "Validation failed: Customer name is blank")
            return false
        }

        if (_phoneNumber.value.isNullOrBlank()) {
            _error.value = "Vui lòng nhập số điện thoại"
            android.util.Log.d("BillingViewModel", "Validation failed: Phone number is blank")
            return false
        }

        if (_deliveryAddress.value.isNullOrBlank()) {
            _error.value = "Vui lòng nhập địa chỉ giao hàng"
            android.util.Log.d("BillingViewModel", "Validation failed: Delivery address is blank")
            return false
        }

        android.util.Log.d("BillingViewModel", "Order validation passed successfully")
        return true
    }

    private suspend fun processCashPayment() {
        // Create order for cash payment
        createOrder()
        _paymentSuccess.value = true
    }

    private suspend fun processCreditCardPayment() {
        // Simulate credit card processing
        delay(1000)
        // In real app, integrate with payment gateway
        createOrder()
        _paymentSuccess.value = true
    }

    private suspend fun processBankTransfer() {
        // Simulate bank transfer
        delay(1500)
        createOrder()
        _paymentSuccess.value = true
    }

    private suspend fun processZaloPayPayment() {
        // Show ZaloPay QR dialog instead of creating order immediately
        _showZaloPayQR.value = true
    }

    fun completeZaloPayPayment() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                delay(500) // Simulate processing
                createOrder()
                _paymentSuccess.value = true
            } catch (e: Exception) {
                _error.value = "Lỗi xử lý thanh toán: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun cancelZaloPayPayment() {
        _showZaloPayQR.value = false
    }

            private suspend fun createOrder() {
        try {
            android.util.Log.d("BillingViewModel", "Starting order creation...")

            // Get cartId from local storage
            val cartId = cartManager.getCartId()
            android.util.Log.d("BillingViewModel", "CartId from storage: $cartId")
            if (cartId == null) {
                _error.value = "Không tìm thấy giỏ hàng"
                return
            }

            // Get current cart items to extract productIds
            val cartItems = cartManager.getCartItems()
            if (cartItems.isEmpty()) {
                _error.value = "Giỏ hàng trống"
                return
            }

            val productIds = cartItems.map { it.foodId }
            android.util.Log.d("BillingViewModel", "ProductIds from cart: $productIds")

            // Prepare order request from cart
            val orderRequest = com.example.fastfood.models.CustomOrderFromCartRequest(
                cartId = cartId,
                productIds = productIds,
                notes = "Đặt hàng từ ứng dụng - ${_customerName.value} - ${_phoneNumber.value} - ${_deliveryAddress.value}"
            )
            android.util.Log.d("BillingViewModel", "Order request prepared: $orderRequest")

            // Call API to create order from cart
            android.util.Log.d("BillingViewModel", "Calling API to create order from cart...")
            val response = apiService.createCustomOrderFromCart(orderRequest)
            android.util.Log.d("BillingViewModel", "API response received: ${response.code()}")

            if (response.isSuccessful) {
                val createdOrder = response.body()
                if (createdOrder != null) {
                    android.util.Log.d("BillingViewModel", "Order created successfully: ${createdOrder.id}")
                    // Order created successfully
                    // Clear cart after successful order
                    cartManager.clearCart()
                    updateCartData()
                } else {
                    android.util.Log.d("BillingViewModel", "Order creation failed: response body is null")
                    _error.value = "Không thể tạo đơn hàng"
                }
            } else {
                android.util.Log.d("BillingViewModel", "Order creation failed: ${response.code()} - ${response.message()}")
                handleApiError(response)
            }
        } catch (e: Exception) {
            android.util.Log.e("BillingViewModel", "Order creation error: ${e.message}", e)
            _error.value = "Lỗi kết nối: ${e.message}"
        }
    }



    private fun handleApiError(response: retrofit2.Response<*>) {
        val errorMessage = when (response.code()) {
            400 -> "Dữ liệu không hợp lệ"
            401 -> "Phiên đăng nhập đã hết hạn"
            403 -> "Không có quyền truy cập"
            500 -> "Lỗi máy chủ"
            else -> "Lỗi tạo đơn hàng (${response.code()})"
        }
        _error.value = errorMessage
    }

    fun getDeliveryFee(): Double {
        return 15000.0 // Fixed delivery fee
    }

    fun getTaxAmount(): Double {
        val total = totalPrice.value ?: 0.0
        return total * 0.1 // 10% tax
    }

    fun getFinalTotal(): Double {
        val subtotal = totalPrice.value ?: 0.0
        return subtotal + getDeliveryFee() + getTaxAmount()
    }

    fun getFormattedFinalTotal(): String {
        return "${String.format("%,.0f", getFinalTotal())} ₫"
    }

    fun getFormattedDeliveryFee(): String {
        return "${String.format("%,.0f", getDeliveryFee())} ₫"
    }

    fun getFormattedTax(): String {
        return "${String.format("%,.0f", getTaxAmount())} ₫"
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

    // PaymentMethod enum is now imported from Payment model
}
