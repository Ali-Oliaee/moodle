package com.example.moodle.data.model

data class Participant(
    val id: Int,
    val fullName: String,
    val username: String,
    val nationalCode: String,
    var attendanceStatus: AttendanceStatus? = AttendanceStatus.NONE
)

enum class AttendanceStatus {
    NONE,
    PRESENT,
    ABSENT,
    LATE,
    EXCUSED
}

data class ParticipantResponse(
    val items: List<Participant>,
    val pageNumber: Int,
    val pageSize: Int,
    val totalElements: Int,
    val totalPages: Int
)

data class GetParticipantsRequest(
    val page: Int,
    val limit: Int
)