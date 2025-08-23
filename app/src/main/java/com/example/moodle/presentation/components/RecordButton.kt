package com.example.moodle.presentation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween

@Composable
fun RecordButton(
    isRecording: Boolean,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = {
            android.util.Log.d("RecordButton", "Button clicked, isRecording: $isRecording")
            if (isRecording) {
                onStopRecording()
            } else {
                onStartRecording()
            }
        },
        modifier = modifier
            .size(56.dp) // Make button bigger for better visibility
            .background(
                color = Color.White.copy(alpha = 0.9f), // Semi-transparent white background
                shape = RoundedCornerShape(28.dp)
            )
            .border(
                width = 2.dp, // Thicker border
                color = if (isRecording) Color.Red else Color.Blue,
                shape = RoundedCornerShape(28.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .size(24.dp) // Bigger inner circle
                .background(
                    color = if (isRecording) Color.Red else Color.Blue,
                    shape = CircleShape
                )
        )
    }
}