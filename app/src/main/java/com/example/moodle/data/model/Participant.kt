package com.example.moodle.data.model

data class Participant(
    val id: String,
    val name: String,
    var attendanceStatus: AttendanceStatus = AttendanceStatus.NONE
)

enum class AttendanceStatus {
    NONE,
    PRESENT,
    ABSENT,
    LATE,
    EXCUSED
}

