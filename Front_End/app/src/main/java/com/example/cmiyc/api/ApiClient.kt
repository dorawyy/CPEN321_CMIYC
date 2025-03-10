package com.example.cmiyc.api

import okhttp3.OkHttpClient
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.Cache
import okhttp3.Protocol
import okio.IOException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import android.content.Context
import java.io.File
import java.net.UnknownHostException

object RetrofitClient {
    private const val BASE_URL = "https://m32qmf20rf.execute-api.us-west-1.amazonaws.com/"
    private const val CACHE_SIZE = 10 * 1024 * 1024L // 10 MB

    private var okHttpClient: OkHttpClient? = null

    fun initialize(context: Context) {
        if (okHttpClient != null) return
        val cacheDir = File(context.cacheDir, "http-cache")
        val cache = Cache(cacheDir, CACHE_SIZE)
        val dispatcher = Dispatcher().apply {
            maxRequests = 32
            maxRequestsPerHost = 10
        }
        val connectionPool = ConnectionPool(
            5,
            30,
            TimeUnit.SECONDS
        )
        okHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .connectionPool(connectionPool)
            .dispatcher(dispatcher)
            .cache(cache)
            .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Connection", "keep-alive")
                    .build()
                try {
                    chain.proceed(request)
                } catch (e: Exception) {
                    if (e is UnknownHostException) {
                        println("DNS resolution error: ${e.message}, trying with fresh connection")
                        val freshRequest = request.newBuilder()
                            .removeHeader("Connection")
                            .addHeader("Connection", "close")
                            .cacheControl(okhttp3.CacheControl.FORCE_NETWORK)
                            .build()

                        return@addInterceptor chain.proceed(freshRequest)
                    }
                    if (e is IOException) {
                        val newRequest = request.newBuilder().build()
                        return@addInterceptor chain.proceed(newRequest)
                    }
                    throw e
                }
            }
            .build()
    }

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