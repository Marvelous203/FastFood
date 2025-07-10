package com.example.fastfood.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.fastfood.models.Food
import com.example.fastfood.network.ApiService
import com.example.fastfood.network.RetrofitClient
import com.google.gson.Gson
import com.google.gson.JsonObject
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ProductDetailViewModel : BaseViewModel() {
    private val _product = MutableLiveData<Food>()
    val product: LiveData<Food> = _product

    private val apiService = RetrofitClient.getApiService()
    private val gson = Gson()
    private val TAG = "ProductDetailViewModel"

    fun loadProduct(productId: String) {
        executeAsync {
            try {
                Log.d(TAG, "Loading product with ID: $productId")
                val response = apiService.getProductById(productId)
                Log.d(TAG, "Response code: ${response.code()}")
                Log.d(TAG, "Raw response body: ${response.body()}")
                
                if (response.isSuccessful) {
                    val food = response.body()
                    if (food != null) {
                        _product.value = food
                        Log.d(TAG, "Product loaded successfully: ${food.name}")
                    } else {
                        setError("Không thể tải thông tin sản phẩm")
                    }
                } else {
                    // Try to parse error message from response
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Error response: $errorBody")
                    val errorMessage = try {
                        val errorJson = gson.fromJson(errorBody, JsonObject::class.java)
                        errorJson.get("message")?.asString ?: "Không thể tải thông tin sản phẩm"
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing error response", e)
                        when (response.code()) {
                            404 -> "Không tìm thấy sản phẩm"
                            else -> "Không thể tải thông tin sản phẩm (${response.code()})"
                        }
                    }
                    setError(errorMessage)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during API call", e)
                val errorMessage = when (e) {
                    is HttpException -> "Lỗi kết nối: ${e.code()}"
                    is SocketTimeoutException -> "Kết nối bị gián đoạn"
                    is UnknownHostException -> "Không thể kết nối đến máy chủ"
                    else -> "Đã xảy ra lỗi: ${e.message}"
                }
                setError(errorMessage)
            }
        }
    }
} 