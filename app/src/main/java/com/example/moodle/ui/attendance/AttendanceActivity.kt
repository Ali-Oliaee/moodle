package com.example.moodle.ui.attendance

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.example.moodle.R
import com.example.moodle.data.datastore.UserPreferences
import com.example.moodle.data.model.AttendanceRecordRequest
import com.example.moodle.data.repository.AttendanceRepository
import com.example.moodle.utils.FontHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AttendanceActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var btnSubmit: Button
    private lateinit var btnRetry: Button

    private lateinit var attendanceRepository: AttendanceRepository
    private lateinit var adapter: AttendanceAdapter

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance)

        attendanceRepository = AttendanceRepository(UserPreferences(this))
        adapter = AttendanceAdapter(mutableListOf()) { _, _ ->
            // No-op for now; could be used for analytics or real-time updates.
        }

        initializeViews()
        applyFonts()
        fetchStudents()
    }

    private fun initializeViews() {
        val btnBack: ImageButton = findViewById(R.id.btnBack)
        val tvTitle: TextView = findViewById(R.id.tvTitle)
        recyclerView = findViewById(R.id.rvStudents)
        progressBar = findViewById(R.id.progressBar)
        tvError = findViewById(R.id.tvError)
        tvEmpty = findViewById(R.id.tvEmpty)
        btnSubmit = findViewById(R.id.btnSubmitAttendance)
        btnRetry = findViewById(R.id.btnRetry)

        tvTitle.text = getString(R.string.attendance_title)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        btnBack.setOnClickListener { finish() }
        btnSubmit.setOnClickListener { submitAttendance() }
        btnRetry.setOnClickListener { fetchStudents() }
    }

    private fun applyFonts() {
        val root = findViewById<ViewGroup>(android.R.id.content)
        FontHelper.applyFontToViewGroup(this, root)
    }

    private fun fetchStudents() {
        showLoading(true)
        hideError()
        scope.launch {
            val result = attendanceRepository.fetchStudents()
            result.fold(
                onSuccess = { students ->
                    val uiItems = students.map {
                        StudentAttendanceUiModel(
                            id = it.id,
                            name = it.fullName,
                            status = when (it.present) {
                                true -> AttendanceStatus.PRESENT
                                false -> AttendanceStatus.ABSENT
                                else -> null
                            }
                        )
                    }
                    adapter.updateItems(uiItems)
                    showLoading(false)
                    updateEmptyState(uiItems.isEmpty())
                },
                onFailure = { error ->
                    showLoading(false)
                    showError(error.message ?: getString(R.string.attendance_generic_error))
                }
            )
        }
    }

    private fun submitAttendance() {
        val snapshot = adapter.snapshot()
        if (snapshot.isEmpty()) {
            Toast.makeText(this, R.string.attendance_empty_submit, Toast.LENGTH_SHORT).show()
            return
        }

        val pending = snapshot.filter { it.status == null }
        if (pending.isNotEmpty()) {
            Toast.makeText(
                this,
                getString(R.string.attendance_incomplete, pending.size),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        setSubmitInProgress(true)
        val payload = snapshot.map {
            AttendanceRecordRequest(
                studentId = it.id,
                status = it.status?.apiValue ?: AttendanceStatus.ABSENT.apiValue
            )
        }

        scope.launch {
            val result = attendanceRepository.submitAttendance(payload)
            result.fold(
                onSuccess = { response ->
                    setSubmitInProgress(false)
                    val message = response.message ?: getString(R.string.attendance_submit_success)
                    Toast.makeText(this@AttendanceActivity, message, Toast.LENGTH_LONG).show()
                    finish()
                },
                onFailure = { error ->
                    setSubmitInProgress(false)
                    Toast.makeText(
                        this@AttendanceActivity,
                        error.message ?: getString(R.string.attendance_submit_failure),
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        recyclerView.visibility = if (show) View.GONE else View.VISIBLE
        if (show) {
            tvEmpty.visibility = View.GONE
            hideError()
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun hideError() {
        tvError.visibility = View.GONE
        btnRetry.visibility = View.GONE
    }

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
        btnRetry.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        tvEmpty.visibility = View.GONE
    }

    private fun setSubmitInProgress(isLoading: Boolean) {
        btnSubmit.isEnabled = !isLoading
        btnSubmit.text = if (isLoading) {
            getString(R.string.attendance_submitting)
        } else {
            getString(R.string.attendance_submit)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
