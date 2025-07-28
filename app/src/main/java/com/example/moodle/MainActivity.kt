package com.example.moodle

import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.moodle.ui.theme.MoodleTheme
import com.example.moodle.presentation.screens.WhiteBoard
import com.example.moodle.presentation.components.RecordButton
import androidx.core.content.getSystemService

class MainActivity : ComponentActivity() {
    private val mediaProjectionManager by lazy {
        getSystemService<MediaProjectionManager>()!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MoodleTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        WhiteBoard(innerPadding)

                        RecordButton(
                            mediaProjectionManager = mediaProjectionManager,
                            modifier = Modifier.offset(x = 16.dp, y = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

