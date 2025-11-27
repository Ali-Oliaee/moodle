package com.example.moodle.utils

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * Helper class for managing runtime permissions across different Android versions
 * Handles Android 13+ specific permission requirements
 */
class PermissionManager(private val activity: AppCompatActivity) {
    
    private var onPermissionsGranted: (() -> Unit)? = null
    private var onPermissionsDenied: ((List<String>) -> Unit)? = null
    
    private val permissionLauncher: ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val deniedPermissions = permissions.filter { !it.value }.keys.toList()
            
            if (deniedPermissions.isEmpty()) {
                onPermissionsGranted?.invoke()
            } else {
                onPermissionsDenied?.invoke(deniedPermissions)
            }
        }
    
    /**
     * Request storage permissions based on Android version
     * Android 13+: READ_MEDIA_IMAGES, READ_MEDIA_VIDEO
     * Android 10-12: READ_EXTERNAL_STORAGE
     * Below Android 10: READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE
     */
    fun requestStoragePermissions(
        onGranted: () -> Unit,
        onDenied: (List<String>) -> Unit = {}
    ) {
        onPermissionsGranted = onGranted
        onPermissionsDenied = onDenied
        
        val permissions = getStoragePermissions()
        
        // Check if already granted
        if (hasStoragePermissions()) {
            onGranted()
            return
        }
        
        permissionLauncher.launch(permissions.toTypedArray())
    }
    
    /**
     * Request notification permission (required for Android 13+)
     */
    fun requestNotificationPermission(
        onGranted: () -> Unit,
        onDenied: () -> Unit = {}
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onPermissionsGranted = onGranted
            onPermissionsDenied = { onDenied() }
            
            if (hasNotificationPermission()) {
                onGranted()
                return
            }
            
            permissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
        } else {
            // No permission needed before Android 13
            onGranted()
        }
    }
    
    /**
     * Request audio recording permission
     */
    fun requestAudioPermission(
        onGranted: () -> Unit,
        onDenied: () -> Unit = {}
    ) {
        onPermissionsGranted = onGranted
        onPermissionsDenied = { onDenied() }
        
        if (hasAudioPermission()) {
            onGranted()
            return
        }
        
        permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
    }
    
    /**
     * Check if storage permissions are granted
     */
    fun hasStoragePermissions(): Boolean {
        val permissions = getStoragePermissions()
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(
                activity,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Check if notification permission is granted (Android 13+)
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not required before Android 13
        }
    }
    
    /**
     * Check if audio recording permission is granted
     */
    fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Get storage permissions based on Android version
     */
    private fun getStoragePermissions(): List<String> {
        return when {
            // Android 13+ (API 33+) - Scoped storage with granular permissions
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                listOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
                )
            }
            // Android 10-12 (API 29-32) - Scoped storage but different permissions
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            // Below Android 10 - Legacy storage
            else -> {
                listOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }
        }
    }
}

