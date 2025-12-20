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
        val TAG = "Moodle-URL"

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val urlLoggingInterceptor = Interceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)
            val url = request.url
          
            
            Log.d(TAG, "=== REQUEST URL DETAILS ===")
            Log.d(TAG, "Full URL: $url")
            Log.d(TAG, "Scheme: ${url.scheme}")
            Log.d(TAG, "Host: ${url.host}")
            Log.d(TAG, "Port: ${url.port}")
            Log.d(TAG, "Path: ${url.encodedPath}")
            Log.d(TAG, "Method: ${request.method}")
            Log.d(TAG, "=========================")
            
            Log.d(TAG, "=== REQUEST HEADERS ===")
            for (i in 0 until request.headers.size) {
                val name = request.headers.name(i)
                val value = request.headers.value(i)
                Log.d(TAG, "$name: $value")
            }
            Log.d(TAG, "=======================")
            
            val requestBody = request.body
            if (requestBody != null) {
                Log.d(TAG, "=== REQUEST BODY ===")
                Log.d(TAG, "Content-Type: ${requestBody.contentType()}")
                Log.d(TAG, "Content-Length: ${requestBody.contentLength()}")
                
                Log.d(TAG, "====================")
            }

            Log.d(TAG, "=== RESPONSE ===")
            Log.d(TAG, "Status: ${response.code}")
            Log.d(TAG, "Message: ${response.message}")
            Log.d(TAG, "Body: ${response.body?.string()}")
            Log.d(TAG, "=========================")
            
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
        val baseUrl = "http://10.0.2.2:8083/"
        
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(createOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
