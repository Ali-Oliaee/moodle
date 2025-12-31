package com.example.moodle.ui.whiteboard.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.moodle.R
import com.example.moodle.ui.whiteboard.models.BackgroundColor as BgColor
import com.example.moodle.ui.whiteboard.models.BackgroundPattern

/**
 * Background pattern and color selection popup with Jetpack Compose
 * Displays a centered dialog with color picker, pattern grid, and upload button
 */
@Composable
fun BackgroundPatternPopup(
    isVisible: Boolean,
    currentPattern: BackgroundPattern,
    currentColor: BgColor,
    onPatternSelected: (BackgroundPattern) -> Unit,
    onColorSelected: (BgColor) -> Unit,
    onUploadImageRequested: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var color by remember { mutableStateOf(currentColor) }
    var pattern by remember { mutableStateOf(currentPattern) }

    if (isVisible) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            Card(
                modifier = modifier.width(360.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.98f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Background Colors Section
                    Text(
                        text = stringResource(R.string.background_colors),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )

                    BackgroundColorGrid(
                        currentColor = color,
                        onColorSelected = { c ->
                            run {
                                onColorSelected(c)
                                color = c
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFFE0E0E0))
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Background Patterns Section
                    Text(
                        text = stringResource(R.string.background_patterns),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )

                    BackgroundPatternGrid(
                        currentPattern = pattern,
                        onPatternSelected = { p -> run {
                            onPatternSelected(p)
                            pattern = p
                        }}
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFFE0E0E0))
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Upload Background Image Button
                    Button(
                        onClick = {
                            onUploadImageRequested()
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4338CA)
                        )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_image),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.upload_background_image),
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Grid of background color buttons
 */
@Composable
private fun BackgroundColorGrid(
    currentColor: BgColor,
    onColorSelected: (BgColor) -> Unit
) {
    val colorRows = listOf(
        listOf(
            BgColor.WHITE,
            BgColor.LIGHT_GRAY,
            BgColor.LIGHT_BLUE,
            BgColor.LIGHT_GREEN,
            BgColor.LIGHT_YELLOW,
            BgColor.LIGHT_PINK
        ),
        listOf(
            BgColor.DARK_BLUE,
            BgColor.DARK_GREEN,
            BgColor.BLACK
        )
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        colorRows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
            ) {
                row.forEach { bgColor ->
                    BackgroundColorButton(
                        color = Color(bgColor.colorValue),
                        isSelected = bgColor == currentColor,
                        onClick = { onColorSelected(bgColor) }
                    )
                }
            }
        }
    }
}

/**
 * Individual background color button
 */
@Composable
private fun BackgroundColorButton(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) Color(0xFF4338CA) else Color(0xFFCCCCCC),
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
}

/**
 * Grid of background pattern buttons
 */
@Composable
private fun BackgroundPatternGrid(
    currentPattern: BackgroundPattern,
    onPatternSelected: (BackgroundPattern) -> Unit
) {
    val patternRows = listOf(
        listOf(
            BackgroundPattern.PLAIN,
            BackgroundPattern.GRID_SMALL,
            BackgroundPattern.GRID_LARGE,
            BackgroundPattern.LINES_HORIZONTAL
        ),
        listOf(
            BackgroundPattern.LINES_VERTICAL,
            BackgroundPattern.LINES_BOTH,
            BackgroundPattern.DOT_GRID,
            BackgroundPattern.GRAPH_PAPER
        )
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        patternRows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
            ) {
                row.forEach { pattern ->
                    BackgroundPatternButton(
                        pattern = pattern,
                        isSelected = pattern == currentPattern,
                        onClick = { onPatternSelected(pattern) }
                    )
                }
            }
        }
    }
}

/**
 * Individual background pattern button with preview
 */
@Composable
private fun BackgroundPatternButton(
    pattern: BackgroundPattern,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) Color(0xFF4338CA) else Color(0xFFCCCCCC),
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    ) {
        PatternPreview(
            pattern = pattern,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Preview of background pattern using Canvas
 */
@Composable
private fun PatternPreview(
    pattern: BackgroundPattern,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val gridColor = Color(0xFFE0E0E0)
        val lineColor = Color(0xFFD0D0D0)
        val dotColor = Color(0xFFC0C0C0)

        when (pattern) {
            BackgroundPattern.PLAIN -> {
                // No pattern to draw
            }
            BackgroundPattern.GRID_SMALL -> {
                drawGridPattern(width, height, 6f, gridColor)
            }
            BackgroundPattern.GRID_LARGE -> {
                drawGridPattern(width, height, 9f, gridColor)
            }
            BackgroundPattern.LINES_HORIZONTAL -> {
                drawHorizontalLines(width, height, 6f, lineColor)
            }
            BackgroundPattern.LINES_VERTICAL -> {
                drawVerticalLines(width, height, 6f, lineColor)
            }
            BackgroundPattern.LINES_BOTH -> {
                drawHorizontalLines(width, height, 6f, lineColor)
                drawVerticalLines(width, height, 6f, lineColor)
            }
            BackgroundPattern.DOT_GRID -> {
                drawDotGrid(width, height, 6f, dotColor)
            }
            BackgroundPattern.GRAPH_PAPER -> {
                drawGraphPaper(width, height)
            }
        }
    }
}

/**
 * Draw grid pattern
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGridPattern(
    width: Float,
    height: Float,
    spacing: Float,
    color: Color
) {
    // Vertical lines
    var x = spacing
    while (x < width) {
        drawLine(
            color = color,
            start = Offset(x, 0f),
            end = Offset(x, height),
            strokeWidth = 1f
        )
        x += spacing
    }

    // Horizontal lines
    var y = spacing
    while (y < height) {
        drawLine(
            color = color,
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 1f
        )
        y += spacing
    }
}

/**
 * Draw horizontal lines
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHorizontalLines(
    width: Float,
    height: Float,
    spacing: Float,
    color: Color
) {
    var y = spacing
    while (y < height) {
        drawLine(
            color = color,
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 1f
        )
        y += spacing
    }
}

/**
 * Draw vertical lines
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawVerticalLines(
    width: Float,
    height: Float,
    spacing: Float,
    color: Color
) {
    var x = spacing
    while (x < width) {
        drawLine(
            color = color,
            start = Offset(x, 0f),
            end = Offset(x, height),
            strokeWidth = 1f
        )
        x += spacing
    }
}

/**
 * Draw dot grid pattern
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDotGrid(
    width: Float,
    height: Float,
    spacing: Float,
    color: Color
) {
    var y = spacing
    while (y < height) {
        var x = spacing
        while (x < width) {
            drawCircle(
                color = color,
                radius = 0.8f,
                center = Offset(x, y)
            )
            x += spacing
        }
        y += spacing
    }
}

/**
 * Draw graph paper pattern
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGraphPaper(
    width: Float,
    height: Float
) {
    val smallSpacing = 3f
    val largeSpacing = 9f
    val lightColor = Color(0xFFF0F0F0)
    val darkColor = Color(0xFFD0D0D0)

    // Small grid
    var x = smallSpacing
    while (x < width) {
        drawLine(
            color = lightColor,
            start = Offset(x, 0f),
            end = Offset(x, height),
            strokeWidth = 0.5f
        )
        x += smallSpacing
    }

    var y = smallSpacing
    while (y < height) {
        drawLine(
            color = lightColor,
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 0.5f
        )
        y += smallSpacing
    }

    // Large grid
    x = largeSpacing
    while (x < width) {
        drawLine(
            color = darkColor,
            start = Offset(x, 0f),
            end = Offset(x, height),
            strokeWidth = 1f
        )
        x += largeSpacing
    }

    y = largeSpacing
    while (y < height) {
        drawLine(
            color = darkColor,
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 1f
        )
        y += largeSpacing
    }
}

