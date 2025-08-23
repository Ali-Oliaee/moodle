package com.example.moodle.data.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.example.moodle.data.network.models.*
import okhttp3.ResponseBody
import okhttp3.RequestBody
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

class LenientResponseConverter<T>(
    private val gson: Gson,
    private val type: Type
) : Converter<ResponseBody, T> {
    
    override fun convert(value: ResponseBody): T? {
        val stringValue = value.string()
        Log.d("Login", "convert: $stringValue")
        
        return try {
            gson.fromJson(stringValue, type)
        } catch (e: JsonSyntaxException) {
            // If JSON parsing fails, try to create a generic error response
            @Suppress("UNCHECKED_CAST")
            when (type) {
                LoginResponse::class.java -> {
                    createErrorLoginResponse(stringValue) as T
                }
                else -> {
                    // For unknown types, return null and let the caller handle it
                    null
                }
            }
        }
    }
    
    private fun createErrorLoginResponse(errorMessage: String): LoginResponse {
        return LoginResponse(
            success = false,
            message = "Server returned invalid response: $errorMessage",
            token = null,
            refreshToken = null,
            data = null
        )
    }
}

class LenientConverterFactory : Converter.Factory() {
    
    private val gson = Gson()
    
    override fun responseBodyConverter(
        type: Type,
        annotations: Array<Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *>? {

//         return LenientResponseConverter(gson = gson, type = type)
        return null
    }
}

class FormUrlEncodedConverter<T>(
    private val gson: Gson,
    private val type: Type
) : Converter<T, RequestBody> {
    
    override fun convert(value: T): RequestBody {
        return when (value) {
            is LoginRequest -> {
                val formBody = FormEncoder.encodeLoginCredentials(value.username, value.password, "moodle_mobile_app")
                Log.d("FormConverter", "Converting LoginRequest to form: username=${value.username}")
                formBody
            }
            else -> {
                // For other types, convert to JSON as fallback
                val jsonString = gson.toJson(value)
                Log.d("FormConverter", "Converting $type to JSON: $jsonString")
                RequestBody.create("application/json".toMediaTypeOrNull()!!, jsonString)
            }
        }
    }
}

class FormUrlEncodedConverterFactory : Converter.Factory() {
    
    private val gson = Gson()
    
    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<Annotation>,
        methodAnnotations: Array<Annotation>,
        retrofit: Retrofit
    ): Converter<*, RequestBody>? {
//        return FormUrlEncodedConverter(gson = gson, type = type)
        return null
    }
}
