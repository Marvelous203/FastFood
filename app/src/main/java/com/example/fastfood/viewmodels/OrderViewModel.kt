package com.example.fastfood.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fastfood.models.CustomOrderRequest
import com.example.fastfood.models.Order
import com.example.fastfood.network.RetrofitClient
import com.example.fastfood.utils.PreferencesManager
import kotlinx.coroutines.launch
import retrofit2.Response

class OrderViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = RetrofitClient.getApiService()
    private val preferencesManager = PreferencesManager(application)

    // Orders list
    private val _orders = MutableLiveData<List<Order>>()
    val orders: LiveData<List<Order>> = _orders

    // Filtered orders
    private val _filteredOrders = MutableLiveData<List<Order>>()
    val filteredOrders: LiveData<List<Order>> = _filteredOrders

    // Loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Error message
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    // Success message
    private val _successMessage = MutableLiveData<String>()
    val successMessage: LiveData<String> = _successMessage

    // Empty state
    private val _isEmpty = MutableLiveData<Boolean>()
    val isEmpty: LiveData<Boolean> = _isEmpty

    // Current filter
    private val _currentFilter = MutableLiveData<OrderFilter>()
    val currentFilter: LiveData<OrderFilter> = _currentFilter

    // No pagination needed since API returns all orders

    init {
        _currentFilter.value = OrderFilter.ALL
        // Don't auto-load orders, let Fragment control when to load
    }

    fun loadOrders(refresh: Boolean = false) {
        if (!preferencesManager.isLoggedIn) {
            _errorMessage.value = "Vui lòng đăng nhập để xem đơn hàng"
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                val response = apiService.getMyOrders()

                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null) {
                        val allOrders = responseBody.data
                        android.util.Log.d("OrderViewModel", "Loaded ${allOrders.size} orders from API")
                        _orders.value = allOrders

                        // Apply current filter
                        applyFilter(_currentFilter.value ?: OrderFilter.ALL)

                        // All orders loaded successfully
                        android.util.Log.d("OrderViewModel", "Orders set successfully, isEmpty: ${(_orders.value?.isEmpty() == true)}")
                    } else {
                        android.util.Log.e("OrderViewModel", "Response body is null")
                        _errorMessage.value = "Không thể tải danh sách đơn hàng"
                    }
                } else {
                    android.util.Log.e("OrderViewModel", "API call failed: ${response.code()}")
                    handleApiError(response)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Lỗi kết nối: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMoreOrders() {
        // No pagination needed since API returns all orders
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
                    loadOrders(refresh = true) // Refresh to get updated status
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
                    loadOrders(refresh = true)
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

    fun filterOrders(filter: OrderFilter) {
        _currentFilter.value = filter
        applyFilter(filter)
    }

    private fun applyFilter(filter: OrderFilter) {
        val allOrders = _orders.value ?: run {
            android.util.Log.w("OrderViewModel", "applyFilter called but _orders.value is null")
            return
        }

        android.util.Log.d("OrderViewModel", "Applying filter $filter to ${allOrders.size} orders")

        val filtered = when (filter) {
            OrderFilter.ALL -> allOrders
            OrderFilter.PENDING -> allOrders.filter {
                val status = it.status.uppercase()
                android.util.Log.d("OrderViewModel", "Checking order ${it.id} with status: '${it.status}' -> '${status}'")
                status in listOf("PENDING", "CONFIRMED")
            }
            OrderFilter.PROCESSING -> allOrders.filter {
                it.status.uppercase() in listOf("PREPARING", "READY", "DELIVERING")
            }
            OrderFilter.COMPLETED -> allOrders.filter {
                it.status.uppercase() in listOf("DELIVERED", "COMPLETED")
            }
            OrderFilter.CANCELLED -> allOrders.filter {
                it.status.uppercase() == "CANCELLED"
            }
        }

        android.util.Log.d("OrderViewModel", "Filter result: ${filtered.size} orders")
        _filteredOrders.value = filtered
        _isEmpty.value = filtered.isEmpty()
    }



    private fun handleApiError(response: Response<*>, defaultMessage: String = "Có lỗi xảy ra") {
        val errorMessage = when (response.code()) {
            401 -> "Phiên đăng nhập đã hết hạn"
            403 -> "Không có quyền truy cập"
            404 -> "Không tìm thấy dữ liệu"
            500 -> "Lỗi máy chủ"
            else -> defaultMessage
        }
        _errorMessage.value = errorMessage
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    enum class OrderFilter(val displayName: String) {
        ALL("Tất cả"),
        PENDING("Chờ xử lý"),
        PROCESSING("Đang xử lý"),
        COMPLETED("Hoàn thành"),
        CANCELLED("Đã hủy")
    }
}
