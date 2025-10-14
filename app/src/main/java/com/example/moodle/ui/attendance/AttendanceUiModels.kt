package com.example.moodle.ui.attendance

data class StudentAttendanceUiModel(
    val id: Long,
    val name: String,
    var status: AttendanceStatus? = null
)

enum class AttendanceStatus(val apiValue: String) {
    PRESENT("present"),
    ABSENT("absent")
}
