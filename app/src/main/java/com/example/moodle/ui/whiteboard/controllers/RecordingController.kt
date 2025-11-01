package com.example.moodle.ui.whiteboard.controllers

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.Toast
import com.example.moodle.R
import com.example.moodle.ui.whiteboard.ScreenRecordService

/**
 * Controller responsible for handling screen recording functionality
 */
class RecordingController(
    private val activity: AppCompatActivity
) {
    
    companion object {
        const val REQUEST_CODE_SCREEN_CAPTURE = 1000
        const val REQUEST_CODE_PERMISSIONS = 1001
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
        )
    }
    
    private var isRecording = false
    private var toggleRecordBtn: Button? = null
    
    private val mediaProjectionManager by lazy {
        activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }
    
    private val recordingStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "RECORDING_STARTED" -> {
                    isRecording = true
                    updateButtonAppearance()
                }
                "RECORDING_STOPPED" -> {
                    isRecording = false
                    updateButtonAppearance()
                }
            }
        }
    }
    
    /**
     * Initialize the recording controller with the record button
     */
    fun initialize(recordButton: Button) {
        toggleRecordBtn = recordButton
        registerRecordingStateReceiver()
        updateButtonAppearance()
    }
    
    /**
     * Toggle recording on/off
     */
    fun toggleRecording() {
        if (isRecording) {
            stopRecording()
        } else {
            startRecording()
        }
    }
    
    /**
     * Start screen recording
     */
    private fun startRecording() {
        if (checkPermissions()) {
            requestScreenCapture()
        } else {
            requestPermissions()
        }
    }
    
    /**
     * Stop screen recording
     */
    private fun stopRecording() {
        val intent = Intent(activity, ScreenRecordService::class.java).apply {
            action = ScreenRecordService.ACTION_STOP
        }
        activity.startService(intent)
        Toast.makeText(activity, activity.getString(R.string.recording_stopped), Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Request screen capture permission
     */
    private fun requestScreenCapture() {
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        activity.startActivityForResult(captureIntent, REQUEST_CODE_SCREEN_CAPTURE)
    }
    
    /**
     * Check if all required permissions are granted
     */
    private fun checkPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Request recording permissions
     */
    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }
    
    /**
     * Handle activity result for screen capture
     */
    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                startRecordingService(resultCode, data)
                Toast.makeText(activity, activity.getString(R.string.recording_started), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(activity, activity.getString(R.string.screen_capture_denied), Toast.LENGTH_SHORT).show()
            }
            return true
        }
        return false
    }
    
    /**
     * Handle permission request results
     */
    fun handlePermissionsResult(
        requestCode: Int,
        grantResults: IntArray
    ): Boolean {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(activity, activity.getString(R.string.permissions_granted), Toast.LENGTH_SHORT).show()
                startRecording()
            } else {
                Toast.makeText(activity, activity.getString(R.string.permissions_denied), Toast.LENGTH_LONG).show()
            }
            return true
        }
        return false
    }
    
    /**
     * Start the recording service
     */
    private fun startRecordingService(resultCode: Int, data: Intent) {
        val intent = Intent(activity, ScreenRecordService::class.java).apply {
            action = ScreenRecordService.ACTION_START
            putExtra(ScreenRecordService.EXTRA_RESULT_CODE, resultCode)
            putExtra(ScreenRecordService.EXTRA_DATA_INTENT, data)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity.startForegroundService(intent)
        } else {
            activity.startService(intent)
        }
    }
    
    /**
     * Update the record button appearance based on recording state
     */
    private fun updateButtonAppearance() {
        toggleRecordBtn?.let { button ->
            if (isRecording) {
                button.setBackgroundResource(R.drawable.square_button)
            } else {
                button.setBackgroundResource(R.drawable.circle_button)
            }
        }
    }
    
    /**
     * Register the broadcast receiver for recording state changes
     */
    private fun registerRecordingStateReceiver() {
        val filter = IntentFilter().apply {
            addAction("RECORDING_STARTED")
            addAction("RECORDING_STOPPED")
        }
        activity.registerReceiver(recordingStateReceiver, filter)
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        try {
            activity.unregisterReceiver(recordingStateReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
        }
    }
    
    /**
     * Check if currently recording
     */
    fun isRecording(): Boolean = isRecording
}

