package com.example.moodle.data.network

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import com.example.moodle.data.network.LenientConverterFactory
import com.example.moodle.data.network.FormUrlEncodedConverterFactory

object NetworkConfig {
    private const val BASE_URL = "http://62.60.210.162/"
    
    private val gson: Gson = GsonBuilder()
        .setLenient()
        .create()
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(FormUrlEncodedConverterFactory())
        .addConverterFactory(LenientConverterFactory())
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
}
