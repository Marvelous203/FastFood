package com.example.fastfood.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fastfood.models.CartItem
import com.example.fastfood.models.Food
import com.example.fastfood.models.Order
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
        _selectedPaymentMethod.value = PaymentMethod.CASH_ON_DELIVERY
        
        // Initialize cart data
        updateCartData()
    }

    private fun updateCartData() {
        _cartItems.value = cartManager.getCartItems().toMutableList()
        _totalPrice.value = cartManager.getTotalPrice()
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
                    PaymentMethod.CASH_ON_DELIVERY -> {
                        processCashOnDelivery()
                    }
                    PaymentMethod.CREDIT_CARD -> {
                        processCreditCardPayment()
                    }
                    PaymentMethod.BANK_TRANSFER -> {
                        processBankTransfer()
                    }
                    PaymentMethod.E_WALLET -> {
                        processEWalletPayment()
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
            return false
        }
        
        if (_customerName.value.isNullOrBlank()) {
            _error.value = "Vui lòng nhập tên"
            return false
        }
        
        if (_phoneNumber.value.isNullOrBlank()) {
            _error.value = "Vui lòng nhập số điện thoại"
            return false
        }
        
        if (_deliveryAddress.value.isNullOrBlank()) {
            _error.value = "Vui lòng nhập địa chỉ giao hàng"
            return false
        }
        
        return true
    }
    
    private suspend fun processCashOnDelivery() {
        // Create order for cash on delivery
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
    
    private suspend fun processEWalletPayment() {
        // Simulate e-wallet payment
        delay(800)
        createOrder()
        _paymentSuccess.value = true
    }
    
    private fun createOrder() {
        // Clear cart after successful order
        cartManager.clearCart()
        updateCartData()
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
    
    enum class PaymentMethod(val displayName: String) {
        CASH_ON_DELIVERY("Thanh toán khi nhận hàng"),
        CREDIT_CARD("Thẻ tín dụng"),
        BANK_TRANSFER("Chuyển khoản ngân hàng"),
        E_WALLET("Ví điện tử")
    }
} 