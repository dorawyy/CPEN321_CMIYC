package com.example.cmiyc.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "http://50.16.176.21/"

    // Create OkHttpClient with custom timeouts
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)  // Increased from default 10 seconds
        .readTimeout(30, TimeUnit.SECONDS)     // Increased from default 10 seconds
        .writeTimeout(30, TimeUnit.SECONDS)    // Increased from default 10 seconds
        .retryOnConnectionFailure(true)        // Retry on connection failures
        .build()

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}

object ApiClient {
    val apiService: ApiService by lazy {
        RetrofitClient.retrofit.create(ApiService::class.java)
    }
}

//// Mock API
//object ApiClient {
//    val apiService: MockApiService = MockApiService();
//}