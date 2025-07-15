package com.example.fastfood.network

import com.example.fastfood.models.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    // Authentication - Updated endpoints
    @POST("api/v1/auth/email/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/v1/auth/email/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/v1/auth/email/confirm")
    suspend fun confirmEmail(@Body request: Map<String, String>): Response<AuthResponse>

    @POST("api/v1/auth/email/confirm/new")
    suspend fun resendConfirmation(@Body request: Map<String, String>): Response<AuthResponse>

    @POST("api/v1/auth/forgot/password")
    suspend fun forgotPassword(@Body request: Map<String, String>): Response<AuthResponse>

    @POST("api/v1/auth/reset/password")
    suspend fun resetPassword(@Body request: Map<String, String>): Response<AuthResponse>

    @GET("api/v1/auth/me")
    suspend fun getProfile(): Response<User>

    @PATCH("api/v1/auth/me")
    suspend fun updateProfile(
        @Body request: Map<String, Any>
    ): Response<User>

    @DELETE("api/v1/auth/me")
    suspend fun deleteAccount(): Response<Unit>

    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(
        @Body request: Map<String, String>
    ): Response<AuthResponse>

    @POST("api/v1/auth/logout")
    suspend fun logout(): Response<Unit>

    // Social Login
    @POST("api/v1/auth/facebook/login")
    suspend fun facebookLogin(@Body request: Map<String, String>): Response<AuthResponse>

    @POST("api/v1/auth/google/login")
    suspend fun googleLogin(@Body request: Map<String, String>): Response<AuthResponse>

    @POST("api/v1/auth/apple/login")
    suspend fun appleLogin(@Body request: Map<String, String>): Response<AuthResponse>

    // Food Categories - Updated to match API structure
    @GET("api/v1/categories")
    suspend fun getCategories(): Response<Map<String, Any>>

    // Products (Foods) - Updated to match actual API
    @GET("api/v1/products")
    suspend fun getFoods(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("filters") filters: String? = null,
        @Query("sort") sort: String? = null,
        @Header("X-custom-lang") language: String = "en"
    ): Response<Map<String, Any>>

    @GET("api/v1/products/popular")
    suspend fun getPopularFoods(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = 10
    ): Response<Map<String, Any>>

    @GET("api/v1/products/recommended")
    suspend fun getRecommendedFoods(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = 10
    ): Response<Map<String, Any>>

    @GET("api/v1/products/{id}")
    suspend fun getProductById(@Path("id") id: String): Response<Food>

    @GET("api/v1/products")
    suspend fun getFoodsByCategory(
        @Query("category") categoryId: String,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("sort") sort: String? = null
    ): Response<Map<String, Any>>

    @GET("api/v1/products")
    suspend fun searchFoods(
        @Query("filters") searchQuery: String,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): Response<Map<String, Any>>

    // Cart endpoints
    @GET("api/v1/carts/me")
    @Headers("Content-Type: application/json")
    suspend fun getMyCart(): Response<CartResponse>

    @POST("api/v1/carts/add/item")
    @Headers("Content-Type: application/json")
    suspend fun addToCart(
        @Body request: AddToCartRequest,
        @Header("x-custom-lang") language: String = "vi"
    ): Response<CartResponse>

    @PUT("api/v1/carts/{productId}")
    suspend fun updateCartItem(
        @Path("productId") productId: String,
        @Body request: UpdateCartItemRequest
    ): Response<CartResponse>

    @DELETE("api/v1/carts/{productId}")
    suspend fun removeItemFromCart(
        @Path("productId") productId: String
    ): Response<CartResponse>

    @DELETE("api/v1/carts/clear")
    suspend fun clearCart(): Response<CartResponse>

    // Orders - Updated to match backend endpoints
    @GET("api/v1/orders")
    suspend fun getOrders(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("filters") filters: String? = null,
        @Query("sort") sort: String? = null
    ): Response<Map<String, Any>>

    @GET("api/v1/orders/me")
    suspend fun getMyOrders(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("filters") filters: String? = null,
        @Query("sort") sort: String? = null
    ): Response<Map<String, Any>>

    @GET("api/v1/orders/{id}")
    suspend fun getOrderById(
        @Path("id") id: String
    ): Response<Order>

    @POST("api/v1/orders")
    suspend fun createOrder(
        @Body request: OrderRequest
    ): Response<Order>

    @POST("api/v1/orders/custom")
    suspend fun createCustomOrder(
        @Body request: CustomOrderRequest
    ): Response<Order>

    @POST("api/v1/orders/custom")
    suspend fun createCustomOrderFromCart(
        @Body request: CustomOrderFromCartRequest
    ): Response<Order>

    @POST("api/v1/orders/{id}/cancel")
    suspend fun cancelOrder(
        @Path("id") id: String
    ): Response<Order>

    // Payment endpoints - Updated to match backend
    @GET("api/v1/payments")
    suspend fun getPayments(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("filters") filters: String? = null,
        @Query("sort") sort: String? = null
    ): Response<Map<String, Any>>

    @GET("api/v1/payments/me")
    suspend fun getMyPayments(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("filters") filters: String? = null,
        @Query("sort") sort: String? = null
    ): Response<Map<String, Any>>

    @GET("api/v1/payments/order/{orderId}")
    suspend fun getPaymentsByOrder(
        @Path("orderId") orderId: String
    ): Response<List<Payment>>

    @GET("api/v1/payments/{id}")
    suspend fun getPaymentById(
        @Path("id") id: String
    ): Response<Payment>

    @POST("api/v1/payments/cash/{orderId}")
    suspend fun payByCash(
        @Path("orderId") orderId: String,
        @Body request: CashPaymentRequest
    ): Response<Payment>

    @POST("api/v1/payments/zalopay/mobile")
    suspend fun payByZaloPayMobile(
        @Body request: ZaloPaymentRequest
    ): Response<ZaloPaymentResponse>

    @POST("api/v1/payments/zalopay/callback")
    suspend fun handleZaloPayCallback(
        @Body request: PaymentCallbackRequest
    ): Response<Payment>

    @PATCH("api/v1/payments/{id}/status")
    suspend fun updatePaymentStatus(
        @Path("id") id: String,
        @Body request: PaymentStatusUpdateRequest
    ): Response<Payment>

    @PATCH("api/v1/payments/{id}/cancel")
    suspend fun cancelPayment(
        @Path("id") id: String
    ): Response<Payment>
}

// Base response type for all API responses
data class BaseResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null
)

// Specific response types
typealias ProductResponse = BaseResponse<Food>
typealias ProductsResponse = BaseResponse<List<Food>>
typealias OrderResponse = BaseResponse<Order>
typealias OrdersResponse = BaseResponse<List<Order>>
typealias CategoriesResponse = BaseResponse<List<Category>>
typealias ImageResponse = BaseResponse<String>
typealias FileUploadResponse = BaseResponse<String>
