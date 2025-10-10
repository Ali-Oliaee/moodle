package com.example.moodle.data.model

import com.google.gson.annotations.SerializedName

/**
 * Network DTO representing a student returned from the attendance endpoint.
 */
data class StudentDto(
    @SerializedName("id")
    val id: Long,
    @SerializedName("fullName")
    val fullName: String,
    @SerializedName("present")
    val present: Boolean? = null
)

/**
 * Single attendance record payload that is sent back to the server per student.
 */
data class AttendanceRecordRequest(
    @SerializedName("studentId")
    val studentId: Long,
    @SerializedName("status")
    val status: String
)

/**
 * Request body posted to the attendance submission endpoint.
 */
data class AttendanceSubmitRequest(
    @SerializedName("records")
    val records: List<AttendanceRecordRequest>
)

/**
 * Generic response wrapper for attendance submission.
 */
data class AttendanceSubmitResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String? = null
)
