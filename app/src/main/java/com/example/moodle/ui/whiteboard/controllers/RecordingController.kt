package com.example.moodle.ui.whiteboard.controllers

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.moodle.R
import com.example.moodle.ui.whiteboard.ScreenRecordService
import com.example.moodle.utils.PermissionManager

/**
 * Controller responsible for handling screen recording functionality
 * Updated for Android 13 compatibility with modern permission APIs
 */
class RecordingController(
    private val activity: AppCompatActivity
) {
    
    private var isRecording = false
    private var toggleRecordBtn: Button? = null
    private val permissionManager = PermissionManager(activity)
    
    private val mediaProjectionManager by lazy {
        activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }
    
    // Modern activity result launcher for screen capture
    private val screenCaptureLauncher: ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                startRecordingService(result.resultCode, result.data!!)
                Toast.makeText(activity, R.string.recording_started, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(activity, R.string.screen_capture_denied, Toast.LENGTH_SHORT).show()
            }
        }
    
    // Broadcast receiver for recording state changes
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
        if (isRecording) stopRecording()
        else startRecording()
    }
    
    /**
     * Start screen recording
     */
    private fun startRecording() {
        // Check and request permissions based on Android version
        checkAndRequestPermissions {
            requestScreenCapture()
        }
    }
    
    /**
     * Check and request all required permissions
     */
    private fun checkAndRequestPermissions(onGranted: () -> Unit) {
        // For Android 13+, we need notification permission for foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionManager.requestNotificationPermission(
                onGranted = {
                    // Then check storage permissions
                    checkStorageAndAudioPermissions(onGranted)
                },
                onDenied = {
                    Toast.makeText(
                        activity,
                        R.string.notification_permission_required,
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        } else checkStorageAndAudioPermissions(onGranted)
    }
    
    /**
     * Check storage and audio permissions
     */
    private fun checkStorageAndAudioPermissions(onGranted: () -> Unit) {
        // Check audio permission
        if (!permissionManager.hasAudioPermission()) {
            permissionManager.requestAudioPermission(
                onGranted = {
                    checkStoragePermission(onGranted)
                },
                onDenied = {
                    Toast.makeText(
                        activity,
                        R.string.audio_permission_required,
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        } else checkStoragePermission(onGranted)
    }
    
    /**
     * Check storage permission
     */
    private fun checkStoragePermission(onGranted: () -> Unit) {
        if (!permissionManager.hasStoragePermissions()) {
            permissionManager.requestStoragePermissions(
                onGranted = onGranted,
                onDenied = { deniedPermissions ->
                    Toast.makeText(
                        activity,
                        activity.getString(R.string.storage_permission_required),
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        } else onGranted()
    }
    
    /**
     * Stop screen recording
     */
    private fun stopRecording() {
        val intent = Intent(activity, ScreenRecordService::class.java).apply {
            action = ScreenRecordService.ACTION_STOP
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
            activity.startForegroundService(intent)
        else activity.startService(intent)
        
        Toast.makeText(activity, R.string.recording_stopped, Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Request screen capture permission
     */
    private fun requestScreenCapture() {
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        screenCaptureLauncher.launch(captureIntent)
    }
    
    /**
     * Start the recording service with the granted permissions
     */
    private fun startRecordingService(resultCode: Int, data: Intent) {
        val intent = Intent(activity, ScreenRecordService::class.java).apply {
            action = ScreenRecordService.ACTION_START
            putExtra(ScreenRecordService.EXTRA_RESULT_CODE, resultCode)
            putExtra(ScreenRecordService.EXTRA_DATA_INTENT, data)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
            activity.startForegroundService(intent)
        else activity.startService(intent)
    }
    
    /**
     * Register broadcast receiver for recording state updates
     */
    private fun registerRecordingStateReceiver() {
        val filter = IntentFilter().apply {
            addAction("RECORDING_STARTED")
            addAction("RECORDING_STOPPED")
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.registerReceiver(
                activity,
                recordingStateReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } else activity.registerReceiver(recordingStateReceiver, filter)
    }

    /**
     * Update button appearance based on recording state
     */
    private fun updateButtonAppearance() {
        toggleRecordBtn?.apply {
            if (!isRecording) setBackgroundResource(R.drawable.circle_button_idle)
            else setBackgroundResource(R.drawable.circle_button_recording) //TODO: Change to recording icon
        }
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        try {
            activity.unregisterReceiver(recordingStateReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver was not registered, ignore
        }
    }
}
