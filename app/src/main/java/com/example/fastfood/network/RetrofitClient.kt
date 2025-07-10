package com.example.fastfood.network

import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.ConnectionPool
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import android.app.Application

object RetrofitClient {
    private const val BASE_URL = "https://restapi-fast-food-shop-system-with-nestjs-mongod-production.up.railway.app/"
    private const val CACHE_SIZE = 10 * 1024 * 1024L // 10 MB
    private var cache: Cache? = null
    private var instance: Retrofit? = null
    private var apiService: ApiService? = null
    private var application: Application? = null

    fun initialize(app: Application) {
        application = app
        cache = Cache(File(app.cacheDir, "http-cache"), CACHE_SIZE)
        instance = null // Force rebuild if needed
        apiService = null
    }
    
    // Disable logging in production for better performance
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY // Set to BODY for debugging, NONE for production
    }
    
    // Cache interceptor for offline support and faster loading
    private val cacheInterceptor = Interceptor { chain ->
        val request = chain.request()
        val cacheControl = CacheControl.Builder()
            .maxAge(3, TimeUnit.MINUTES) // Cache for 3 minutes
            .build()
        
        val response = chain.proceed(request)
        response.newBuilder()
            .header("Cache-Control", cacheControl.toString())
            .build()
    }
    
    // Connection pool for reusing connections
    private val connectionPool = ConnectionPool(
        maxIdleConnections = 5,
        keepAliveDuration = 5,
        timeUnit = TimeUnit.MINUTES
    )
    
    private val okHttpClient by lazy {
        val app = application ?: throw IllegalStateException("RetrofitClient must be initialized with an Application context")
        OkHttpClient.Builder()
            .connectionPool(connectionPool)
            .addInterceptor(AuthInterceptor(app))
            .addInterceptor(cacheInterceptor)
            .addInterceptor(loggingInterceptor)
            .cache(cache)
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private fun buildRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Synchronized
    fun <T> create(serviceClass: Class<T>): T {
        if (instance == null) {
            instance = buildRetrofit()
        }
        return instance!!.create(serviceClass)
    }
    
    @Synchronized
    fun getApiService(): ApiService {
        if (application == null) {
            throw IllegalStateException("RetrofitClient must be initialized before use. Call RetrofitClient.initialize(application) in your Application class.")
        }
        if (apiService == null) {
            apiService = create(ApiService::class.java)
        }
        return apiService!!
    }
} 