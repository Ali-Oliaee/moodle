package com.example.moodle

import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.moodle.ui.theme.MoodleTheme
import com.example.moodle.presentation.screens.LoginScreen
import com.example.moodle.presentation.screens.WhiteBoard
import com.example.moodle.di.NetworkModule
import androidx.core.content.getSystemService
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val mediaProjectionManager by lazy {
        getSystemService<MediaProjectionManager>()!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MoodleTheme {
                val authRepository = remember { NetworkModule.provideAuthRepository(this@MainActivity) }
                val isLoggedIn by authRepository.isLoggedIn.collectAsStateWithLifecycle(initialValue = false)
                val scope = rememberCoroutineScope()
                
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
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
                                    // This callback is called when login is successful
                                    // The AuthRepository will automatically update the isLoggedIn state
                                    // and the UI will automatically switch to WhiteBoard screen
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

