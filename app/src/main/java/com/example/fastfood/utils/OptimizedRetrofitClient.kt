package com.example.fastfood.utils

import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * Optimized Retrofit client for performance comparison
 * Minimal overhead, no caching, no interceptors
 */
object OptimizedRetrofitClient {
    private const val BASE_URL = "https://restapi-fast-food-shop-system-with-nestjs-mongod-production.up.railway.app/"
    
    // Minimal OkHttp client - no extras
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS) 
        .writeTimeout(5, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false) // Disable retry for fair comparison
        .build()
    
    // Minimal Retrofit instance
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val apiService: OptimizedApiService = retrofit.create(OptimizedApiService::class.java)
}

/**
 * Multiple API endpoints for performance testing
 */
interface OptimizedApiService {
    @GET("api/v1/products")
    suspend fun getProductsRaw(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): Response<Any> // Raw response - minimal parsing
    
    @GET("api/v1/products")
    suspend fun getProductsLightweight(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): Response<LightweightProductsResponse> // Lightweight model
    
    @GET("api/v1/products")
    suspend fun getProductsMinimal(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): Response<MinimalProductsResponse> // Minimal model
    
    @GET("api/v1/products")
    suspend fun getProductsUltraMinimal(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): Response<UltraMinimalResponse> // Ultra minimal
} 