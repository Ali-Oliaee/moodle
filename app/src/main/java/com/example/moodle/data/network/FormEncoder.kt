package com.example.moodle.data.network

import android.util.Log
import okhttp3.FormBody
import okhttp3.RequestBody

/**
 * Utility class for encoding form data
 * Provides reusable methods for converting data objects to x-www-form-urlencoded format
 * 
 * This class follows the DRY principle by centralizing form encoding logic
 * and provides a clean API for different encoding scenarios.
 */
object FormEncoder {
    
    /**
     * Encode login credentials to form data
     * @param username The user's username/email
     * @param password The user's password
     * @param service Mobile app
     * @return FormBody ready for HTTP request
     */
    fun encodeLoginCredentials(username: String, password: String, service: String): RequestBody {
        return FormBody.Builder()
            .add("username", username)
            .add("password", password)
            .add("service",service)
            .build()
    }
    
    /**
     * Encode generic key-value pairs to form data
     * @param pairs Variable number of key-value pairs
     * @return FormBody ready for HTTP request
     */
    fun encodeFormData(vararg pairs: Pair<String, String>): RequestBody {
        val builder = FormBody.Builder()
        pairs.forEach { (key, value) ->
            builder.add(key, value)
        }
        return builder.build()
    }
    
    /**
     * Encode a map of key-value pairs to form data
     * @param data Map containing form field names and values
     * @return FormBody ready for HTTP request
     */
    fun encodeFormData(data: Map<String, String>): RequestBody {
        val builder = FormBody.Builder()
        data.forEach { (key, value) ->
            builder.add(key, value)
        }
        return builder.build()
    }
    
    /**
     * Log form data for debugging purposes
     * @param formBody The FormBody to log
     * @param tag The log tag to use
     */
    fun logFormData(formBody: FormBody, tag: String = "FormEncoder") {
        val formString = buildString {
            for (i in 0 until formBody.size) {
                if (i > 0) append("&")
                append("${formBody.name(i)}=${formBody.value(i)}")
            }
        }
        Log.d(tag, "Form data: $formString")
    }
    
    /**
     * Validate that required form fields are not empty
     * @param pairs Key-value pairs to validate
     * @return true if all values are non-empty, false otherwise
     */
    fun validateFormFields(vararg pairs: Pair<String, String>): Boolean {
        return pairs.all { (_, value) -> value.isNotBlank() }
    }
}
