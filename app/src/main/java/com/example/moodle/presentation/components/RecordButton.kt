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
    val buttonColor by animateFloatAsState(
        targetValue = if (isRecording) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "buttonColor"
    )
    
    IconButton(
        onClick = {
            if (isRecording) {
                onStopRecording()
            } else {
                onStartRecording()
            }
        },
        modifier = modifier
            .size(48.dp)
            .border(
                width = 1.dp,
                color = if (isRecording) Color.Red else Color.Gray,
                shape = RoundedCornerShape(25.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(
                    color = if (isRecording) Color.Red else Color.Gray,
                    shape = CircleShape
                )
        )
    }
}