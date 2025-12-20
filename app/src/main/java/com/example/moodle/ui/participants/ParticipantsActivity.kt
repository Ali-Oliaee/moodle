package com.example.moodle.ui.participants

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.moodle.R
import com.example.moodle.data.model.AttendanceStatus
import com.example.moodle.data.model.Participant

class ParticipantsActivity : AppCompatActivity() {

    private lateinit var rvParticipants: RecyclerView
    private lateinit var btnConfirm: Button
    private lateinit var btnBack: ImageButton
    private lateinit var participantAdapter: ParticipantAdapter
    private val participants = mutableListOf<Participant>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_participants)

        initViews()
        loadMockData()
        setupRecyclerView()
        setupButtons()
    }

    private fun initViews() {
        rvParticipants = findViewById(R.id.rvParticipants)
        btnConfirm = findViewById(R.id.btnConfirm)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun loadMockData() {
        participants.addAll(
            listOf(
                Participant("98012345678", "علی علیایی"),
                Participant("98023456789", "فاطمه محمدی"),
                Participant("98034567890", "حسین احمدی"),
                Participant("98045678901", "مریم رضایی"),
                Participant("98056789012", "رضا کریمی"),
                Participant("98067890123", "زهرا حسینی"),
                Participant("98078901234", "محمد جعفری"),
                Participant("98089012345", "سارا نوری"),
                Participant("98090123456", "امیر صادقی"),
                Participant("98101234567", "لیلا موسوی"),
                Participant("98112345678", "مهدی باقری"),
                Participant("98123456789", "نرگس حیدری"),
                Participant("98134567890", "سعید رحیمی"),
                Participant("98145678901", "پریسا عباسی"),
                Participant("98156789012", "احمد میرزایی"),
                Participant("98167890123", "سمیرا قاسمی"),
                Participant("98178901234", "بهروز سلیمانی"),
                Participant("98189012345", "نسترن فتحی"),
                Participant("98190123456", "مسعود آقایی"),
                Participant("98201234567", "شیما نجفی")
            )
        )
    }

    private fun setupRecyclerView() {
        participantAdapter = ParticipantAdapter(participants) { participant, status ->
            handleAttendanceChange(participant, status)
        }

        rvParticipants.apply {
            layoutManager = LinearLayoutManager(this@ParticipantsActivity)
            adapter = participantAdapter
        }
    }

    private fun setupButtons() {
        btnBack.setOnClickListener {
            finish()
        }
        
        btnConfirm.setOnClickListener {
            confirmAttendance()
        }
    }

    private fun confirmAttendance() {
        val summary = buildAttendanceSummary()

        // Log summary for debugging/future use
        android.util.Log.d("ParticipantsActivity", "Attendance Summary:\n$summary")
        
        // TODO: Save attendance data to server/database
        
        // Navigate back to whiteboard
        finish()
    }

    private fun buildAttendanceSummary(): String {
        val present = participants.count { it.attendanceStatus == AttendanceStatus.PRESENT }
        val absent = participants.count { it.attendanceStatus == AttendanceStatus.ABSENT }
        val late = participants.count { it.attendanceStatus == AttendanceStatus.LATE }
        val excused = participants.count { it.attendanceStatus == AttendanceStatus.EXCUSED }
        val notMarked = participants.count { it.attendanceStatus == AttendanceStatus.NONE }

        return """
            ${getString(R.string.attendance_present)}: $present
            ${getString(R.string.attendance_absent)}: $absent
            ${getString(R.string.attendance_late)}: $late
            ${getString(R.string.attendance_excused)}: $excused
            ثبت نشده: $notMarked
        """.trimIndent()
    }

    private fun handleAttendanceChange(participant: Participant, status: AttendanceStatus) {
        // Attendance status updated silently
    }
}

