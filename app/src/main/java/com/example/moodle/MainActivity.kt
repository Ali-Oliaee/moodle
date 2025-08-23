package com.example.moodle

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.moodle.ui.theme.MoodleTheme
import com.example.moodle.presentation.screens.LoginScreen
import com.example.moodle.presentation.screens.WhiteBoard
import com.example.moodle.presentation.components.RecordButton
import com.example.moodle.di.NetworkModule
import com.example.moodle.domain.usecases.ScreenRecordConfig
import com.example.moodle.domain.usecases.ScreenRecordService
import androidx.core.content.getSystemService
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val mediaProjectionManager by lazy {
        getSystemService<MediaProjectionManager>()!!
    }
    
    // Screen recording permission launcher
    private val screenRecordingPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Permission granted, start recording
            startScreenRecording(result.resultCode, result.data!!)
        } else {
            // Permission denied
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MoodleTheme {
                val authRepository = remember { NetworkModule.provideAuthRepository(this@MainActivity) }
                val isLoggedIn by authRepository.isLoggedIn.collectAsStateWithLifecycle(initialValue = false)
                val scope = rememberCoroutineScope()
                
                // State for recording
                var isRecording by remember { mutableStateOf(false) }
                
                // Observe recording service status
                LaunchedEffect(Unit) {
                    ScreenRecordService.isServiceRunning.collect { serviceRunning ->
                        isRecording = serviceRunning
                    }
                }
                
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // Main content
                        if (isLoggedIn) {
                            WhiteBoard(
                                innerPadding = innerPadding,
                                onLogout = {
                                    scope.launch {
                                        authRepository.logout()
                                    }
                                }
                            )
                        } else {
                            LoginScreen(
                                onLoginSuccess = {
                                },
                            )
                        }
                        
                        if (isLoggedIn) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                            ) {
                                RecordButton(
                                    isRecording = isRecording,
                                    onStartRecording = {
                                        requestScreenRecordingPermission()
                                    },
                                    onStopRecording = {
                                        stopScreenRecording()
                                        isRecording = false
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    private fun requestScreenRecordingPermission() {
        val intent = mediaProjectionManager.createScreenCaptureIntent()
        screenRecordingPermissionLauncher.launch(intent)
    }
    
    private fun startScreenRecording(resultCode: Int, data: Intent) {
        val config = ScreenRecordConfig(resultCode, data)
        val intent = Intent(this, ScreenRecordService::class.java).apply {
            action = ScreenRecordService.START_RECORDING
            putExtra(ScreenRecordService.KEY_RECORDING_CONFIG, config)
        }
        startService(intent)
    }
    
    private fun stopScreenRecording() {
        val intent = Intent(this, ScreenRecordService::class.java).apply {
            action = ScreenRecordService.STOP_RECORDING
        }
        startService(intent)
    }
}

