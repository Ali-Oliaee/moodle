package com.example.moodle.data.api

import com.example.moodle.data.model.AttendanceSubmitRequest
import com.example.moodle.data.model.AttendanceSubmitResponse
import com.example.moodle.data.model.LoginResponse
import com.example.moodle.data.model.StudentDto
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {
    
    @FormUrlEncoded
    @POST("login/token.php")
    fun login(
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("service") service: String = "moodle_mobile_app"
    ): Call<LoginResponse>

    @GET("attendance/students")
    fun getStudents(
        @Header("Authorization") authorization: String
    ): Call<List<StudentDto>>

    @POST("attendance/submit")
    fun submitAttendance(
        @Header("Authorization") authorization: String,
        @Body request: AttendanceSubmitRequest
    ): Call<AttendanceSubmitResponse>
}
