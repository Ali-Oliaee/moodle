package com.example.moodle.ui.whiteboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.moodle.R

/**
 * Pen options popup with color picker and stroke width selector
 * Built with Jetpack Compose for modern UI
 */
@Composable
fun PenOptionsPopup(
    isVisible: Boolean,
    currentColor: Color,
    currentStrokeWidth: Float,
    anchorX: Int,
    anchorY: Int,
    onColorSelected: (Color) -> Unit,
    onStrokeWidthChanged: (Float) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isVisible) {
        Popup(
            alignment = Alignment.TopStart,
            offset = IntOffset(anchorX, anchorY),
            onDismissRequest = onDismiss,
            properties = PopupProperties(
                focusable = true,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Card(
                modifier = modifier.width(240.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.98f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    ColorPickerGrid(
                        currentColor = currentColor,
                        onColorSelected = onColorSelected
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFFE0E0E0))
                            .padding(vertical = 12.dp)
                    )

                    // Stroke Width Section
                    Text(
                        text = stringResource(R.string.stroke_width),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF666666),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp).fillMaxWidth()
                    )

                    StrokeWidthSlider(
                        currentStrokeWidth = currentStrokeWidth,
                        onStrokeWidthChanged = onStrokeWidthChanged
                    )
                }
            }
        }
    }
}

/**
 * Color picker grid with common drawing colors
 */
@Composable
private fun ColorPickerGrid(
    currentColor: Color,
    onColorSelected: (Color) -> Unit
) {
    val colors = listOf(
        listOf(
            Color.Black,
            Color.White,
            Color.Red,
            Color.Blue,
            Color.Green,
            Color.LightGray
        ),
        listOf(
            Color.Yellow,
            Color.Cyan,
            Color.Magenta,
            Color(0xFFFF9800),
            Color(0xFF9C27B0)
        )
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        colors.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { color ->
                    ColorButton(
                        color = color,
                        isSelected = color == currentColor,
                        onClick = { onColorSelected(color) }
                    )
                }
            }
        }
    }
}

/**
 * Individual color button
 */
@Composable
private fun ColorButton(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color.Gray else Color(0xFFCCCCCC),
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
}

/**
 * Stroke width slider with labels
 */
@Composable
private fun StrokeWidthSlider(
    currentStrokeWidth: Float,
    onStrokeWidthChanged: (Float) -> Unit
) {
    var sliderValue by remember(currentStrokeWidth) {
        mutableStateOf(currentStrokeWidth)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "1",
            fontSize = 12.sp,
            color = Color.Black
        )

        Slider(
            value = sliderValue,
            onValueChange = {
                sliderValue = it
                onStrokeWidthChanged(it)
            },
            valueRange = 1f..21f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF4338CA),
                activeTrackColor = Color(0xFF4338CA),
                inactiveTrackColor = Color(0xFFE0E0E0)
            )
        )

        Text(
            text = "21",
            fontSize = 12.sp,
            color = Color.Black
        )
    }
}

