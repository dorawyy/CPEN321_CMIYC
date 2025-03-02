package com.example.cmiyc.api

import okhttp3.OkHttpClient
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.Cache
import okhttp3.Protocol
import okio.IOException
import okhttp3.Interceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import android.content.Context
import java.io.File

object RetrofitClient {
    private const val BASE_URL = "https://m32qmf20rf.execute-api.us-west-1.amazonaws.com/"
    private const val CACHE_SIZE = 10 * 1024 * 1024L // 10 MB

    private var okHttpClient: OkHttpClient? = null

    fun initialize(context: Context) {
        if (okHttpClient != null) return

        // Create cache directory
        val cacheDir = File(context.cacheDir, "http-cache")
        val cache = Cache(cacheDir, CACHE_SIZE)

        // Configure Dispatcher for better concurrency handling
        val dispatcher = Dispatcher().apply {
            maxRequests = 32 // Increased from default 64 to reduce resources
            maxRequestsPerHost = 10 // Increased from default 5 for API communications
        }

        // Configure connection pool for connection reuse
        val connectionPool = ConnectionPool(
            5, // Max idle connections
            30, // Keep alive duration
            TimeUnit.SECONDS
        )

        // Create OkHttpClient with optimized settings
        okHttpClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .connectionPool(connectionPool)
            .dispatcher(dispatcher)
            .cache(cache)
            .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1)) // Prefer HTTP/2
            .addInterceptor { chain ->
                // Add headers for all requests
                val request = chain.request().newBuilder()
                    .addHeader("Connection", "keep-alive")
                    .build()

                try {
                    chain.proceed(request)
                } catch (e: IOException) {
                    // Force a new connection on network errors
                    val newRequest = request.newBuilder().build()
                    chain.proceed(newRequest)
                }
            }
            .build()
    }

    // Optimize JSON parsing
    private val gson: Gson = GsonBuilder()
        .serializeNulls()
        .setLenient()
        .create()

    val retrofit: Retrofit by lazy {
        if (okHttpClient == null) {
            throw IllegalStateException("OkHttpClient must be initialized before using Retrofit")
        }

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient!!)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
}

object ApiClient {
    val apiService: ApiService by lazy {
        RetrofitClient.retrofit.create(ApiService::class.java)
    }

    fun initialize(context: Context) {
        RetrofitClient.initialize(context)
    }
}