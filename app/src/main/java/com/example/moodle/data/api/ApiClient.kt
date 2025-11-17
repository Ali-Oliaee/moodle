package com.example.moodle.data.api

import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    
    private fun createOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val urlLoggingInterceptor = Interceptor { chain ->
            val request = chain.request()
            val url = request.url
            
            Log.d("Moodle-URL", "=== REQUEST URL DETAILS ===")
            Log.d("Moodle-URL", "Full URL: $url")
            Log.d("Moodle-URL", "Scheme: ${url.scheme}")
            Log.d("Moodle-URL", "Host: ${url.host}")
            Log.d("Moodle-URL", "Port: ${url.port}")
            Log.d("Moodle-URL", "Path: ${url.encodedPath}")
            Log.d("Moodle-URL", "Method: ${request.method}")
            Log.d("Moodle-URL", "=========================")
            
            Log.d("Moodle-HEADERS", "=== REQUEST HEADERS ===")
            for (i in 0 until request.headers.size) {
                val name = request.headers.name(i)
                val value = request.headers.value(i)
                Log.d("Moodle-HEADERS", "$name: $value")
            }
            Log.d("Moodle-HEADERS", "=======================")
            
            val requestBody = request.body
            if (requestBody != null) {
                Log.d("Moodle-BODY", "=== REQUEST BODY ===")
                Log.d("Moodle-BODY", "Content-Type: ${requestBody.contentType()}")
                Log.d("Moodle-BODY", "Content-Length: ${requestBody.contentLength()}")
                
                Log.d("Moodle-BODY", "====================")
            }
            
            chain.proceed(request)
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(urlLoggingInterceptor)
            .addInterceptor(loggingInterceptor)   
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    fun createApiService(): ApiService {
        // Use 10.0.2.2 for Android Emulator (maps to host's 127.0.0.1)
        // For physical device, replace with your computer's actual IP address
        val baseUrl = "http://10.0.2.2:8083/"
        
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(createOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
