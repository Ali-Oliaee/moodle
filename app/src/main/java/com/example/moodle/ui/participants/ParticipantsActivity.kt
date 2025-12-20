package com.example.moodle.ui.participants

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.moodle.R
import com.example.moodle.data.datastore.UserPreferences
import com.example.moodle.data.model.AttendanceStatus
import com.example.moodle.data.model.Participant
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ParticipantsActivity : AppCompatActivity() {

    private lateinit var rvParticipants: RecyclerView
    private lateinit var btnConfirm: Button
    private lateinit var btnBack: ImageButton
    private lateinit var loadingLayout: LinearLayout
    private lateinit var errorLayout: LinearLayout
    private lateinit var tvEmpty: TextView
    private lateinit var tvError: TextView
    private lateinit var btnRetry: Button
    
    private lateinit var participantAdapter: ParticipantAdapter
    private lateinit var userPreferences: UserPreferences
    private val viewModel: ParticipantsViewModel by viewModels()
    
    private val participants = mutableListOf<Participant>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_participants)

        userPreferences = UserPreferences(this)
        
        initViews()
        setupRecyclerView()
        setupButtons()
        observeViewModel()
        loadParticipants()
    }

    private fun initViews() {
        rvParticipants = findViewById(R.id.rvParticipants)
        btnConfirm = findViewById(R.id.btnConfirm)
        btnBack = findViewById(R.id.btnBack)
        loadingLayout = findViewById(R.id.loadingLayout)
        errorLayout = findViewById(R.id.errorLayout)
        tvEmpty = findViewById(R.id.tvEmpty)
        tvError = findViewById(R.id.tvError)
        btnRetry = findViewById(R.id.btnRetry)
    }

    private fun loadParticipants() {
        val token = userPreferences.getAuthToken()
        if (token != null) {
            viewModel.loadParticipants(token)
        } else {
            showError("توکن احراز هویت یافت نشد")
        }
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
        
        btnRetry.setOnClickListener {
            viewModel.clearError()
            loadParticipants()
        }
    }
    
    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            showLoading(isLoading)
        }
        
        viewModel.participants.observe(this) { participantsList ->
            updateParticipants(participantsList)
        }
        
        viewModel.error.observe(this) { error ->
            error?.let { showError(it) }
        }
    }
    
    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            loadingLayout.visibility = View.VISIBLE
            rvParticipants.visibility = View.GONE
            errorLayout.visibility = View.GONE
            tvEmpty.visibility = View.GONE
        } else {
            loadingLayout.visibility = View.GONE
        }
    }
    
    private fun updateParticipants(participantsList: List<Participant>) {
        participants.clear()
        participants.addAll(participantsList)
        participantAdapter.notifyDataSetChanged()
        
        if (participantsList.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rvParticipants.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            rvParticipants.visibility = View.VISIBLE
        }
        errorLayout.visibility = View.GONE
    }
    
    private fun showError(message: String) {
        errorLayout.visibility = View.VISIBLE
        tvError.text = message
        rvParticipants.visibility = View.GONE
        tvEmpty.visibility = View.GONE
        loadingLayout.visibility = View.GONE
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
        val notMarked = participants.count { it.attendanceStatus == AttendanceStatus.NONE || it.attendanceStatus == null }

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

