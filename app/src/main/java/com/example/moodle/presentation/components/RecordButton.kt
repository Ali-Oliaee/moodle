package com.example.moodle.presentation.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.moodle.domain.usecases.ScreenRecordConfig
import com.example.moodle.domain.usecases.ScreenRecordService
import com.example.moodle.domain.usecases.ScreenRecordService.Companion.KEY_RECORDING_CONFIG
import com.example.moodle.domain.usecases.ScreenRecordService.Companion.START_RECORDING
import com.example.moodle.domain.usecases.ScreenRecordService.Companion.STOP_RECORDING

@Composable
fun RecordButton(
    mediaProjectionManager: MediaProjectionManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isServiceRunning by ScreenRecordService.isServiceRunning.collectAsStateWithLifecycle()
    
    val buttonColor by animateFloatAsState(
        targetValue = if (isServiceRunning) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "buttonColor"
    )
    
    var hasNotificationPermission by remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mutableStateOf(
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            )
        } else mutableStateOf(true)
    }
    
    val screenRecordLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val intent = result.data ?: return@rememberLauncherForActivityResult
            val config = ScreenRecordConfig(
                resultCode = result.resultCode,
                data = intent
            )

            val serviceIntent = Intent(context, ScreenRecordService::class.java).apply {
                action = START_RECORDING
                putExtra(KEY_RECORDING_CONFIG, config)
            }
            context.startForegroundService(serviceIntent)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (hasNotificationPermission && !isServiceRunning) {
            screenRecordLauncher.launch(
                mediaProjectionManager.createScreenCaptureIntent()
            )
        }
    }

    fun startRecording() {
        if (!hasNotificationPermission &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            screenRecordLauncher.launch(
                mediaProjectionManager.createScreenCaptureIntent()
            )
        }
    }
    
    IconButton(
        onClick = {
            if (isServiceRunning) {
                Intent(context, ScreenRecordService::class.java).also {
                    it.action = STOP_RECORDING
                    context.startForegroundService(it)
                }
            } else {
                startRecording()
            }
        },
        modifier = modifier
            .size(48.dp)
            .border(
                width = 1.dp,
                color = if (isServiceRunning) Color.Red else Color.Gray,
                shape = RoundedCornerShape(25.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(
                    color = if (isServiceRunning) Color.Red else Color.Gray,
                    shape = CircleShape
                )
        )
    }
}