package com.example.fastfood.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fastfood.models.CustomOrderRequest
import com.example.fastfood.models.Order
import com.example.fastfood.network.RetrofitClient
import kotlinx.coroutines.launch
import retrofit2.Response

class OrderDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = RetrofitClient.getApiService()

    // Order detail
    private val _order = MutableLiveData<Order?>()
    val order: LiveData<Order?> = _order

    // Loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Error message
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // Success message
    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage

    fun loadOrderDetail(orderId: String) {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val response = apiService.getOrderById(orderId)

                if (response.isSuccessful) {
                    _order.value = response.body()
                } else {
                    handleApiError(response, "Không thể tải thông tin đơn hàng")
                }
            } catch (e: Exception) {
                _errorMessage.value = "Lỗi kết nối: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun cancelOrder(order: Order) {
        val orderId = order.id
        if (orderId.isNullOrEmpty()) {
            _errorMessage.value = "ID đơn hàng không hợp lệ"
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                val response = apiService.cancelOrder(orderId)

                if (response.isSuccessful) {
                    _successMessage.value = "Đơn hàng đã được hủy thành công"
                    // Update the current order status
                    val updatedOrder = order.copy(status = "CANCELLED")
                    _order.value = updatedOrder
                } else {
                    handleApiError(response, "Không thể hủy đơn hàng")
                }
            } catch (e: Exception) {
                _errorMessage.value = "Lỗi khi hủy đơn hàng: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun reorder(order: Order) {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                // Create new order with same items
                val customOrderRequest = CustomOrderRequest(
                    cartId = "", // We'll need to add items to cart first
                    productIds = order.items?.map { it.foodId } ?: emptyList(),
                    notes = "Đặt lại từ đơn hàng #${(order.id ?: "UNKNOWN").take(8)}"
                )

                val response = apiService.createCustomOrder(customOrderRequest)

                if (response.isSuccessful) {
                    _successMessage.value = "Đã tạo đơn hàng mới thành công"
                } else {
                    handleApiError(response, "Không thể tạo đơn hàng mới")
                }
            } catch (e: Exception) {
                _errorMessage.value = "Lỗi khi đặt lại đơn hàng: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun handleApiError(response: Response<*>, defaultMessage: String) {
        val errorMessage = when (response.code()) {
            401 -> "Phiên đăng nhập đã hết hạn"
            403 -> "Không có quyền truy cập"
            404 -> "Không tìm thấy đơn hàng"
            500 -> "Lỗi máy chủ"
            else -> defaultMessage
        }
        _errorMessage.value = errorMessage
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}
