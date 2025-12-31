package com.example.moodle.ui.whiteboard.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.moodle.R
import com.example.moodle.ui.whiteboard.models.DrawingTool

/**
 * Shapes selection popup with Jetpack Compose
 * Displays a centered dialog with all available shapes organized in rows
 */
@Composable
fun ShapesPopup(
    isVisible: Boolean,
    currentTool: DrawingTool,
    onShapeSelected: (DrawingTool) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                modifier = modifier.width(320.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.98f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Row 1: Basic Shapes
                    ShapeRow(
                        shapes = listOf(
                            ShapeItem(DrawingTool.LINE, R.drawable.ic_line),
                            ShapeItem(DrawingTool.CIRCLE, R.drawable.ic_circle),
                            ShapeItem(DrawingTool.ELLIPSE, R.drawable.ic_ellipse),
                            ShapeItem(DrawingTool.RECTANGLE, R.drawable.ic_rectangle),
                            ShapeItem(DrawingTool.DIAMOND, R.drawable.ic_diamond)
                        ),
                        currentTool = currentTool,
                        onShapeSelected = { tool ->
                            onShapeSelected(tool)
                            onDismiss()
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 2: Triangles and More
                    ShapeRow(
                        shapes = listOf(
                            ShapeItem(DrawingTool.TRIANGLE, R.drawable.ic_triangle),
                            ShapeItem(DrawingTool.RIGHT_TRIANGLE, R.drawable.ic_right_triangle),
                            ShapeItem(DrawingTool.ISOSCELES_TRIANGLE, R.drawable.ic_isosceles_triangle),
                            ShapeItem(DrawingTool.STAR, R.drawable.ic_star),
                            ShapeItem(DrawingTool.ARROW, R.drawable.ic_arrow)
                        ),
                        currentTool = currentTool,
                        onShapeSelected = { tool ->
                            onShapeSelected(tool)
                            onDismiss()
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 3: Polygons
                    ShapeRow(
                        shapes = listOf(
                            ShapeItem(DrawingTool.PENTAGON, R.drawable.ic_pentagon),
                            ShapeItem(DrawingTool.HEXAGON, R.drawable.ic_hexagon),
                            ShapeItem(DrawingTool.PARALLELOGRAM, R.drawable.ic_parallelogram),
                            ShapeItem(DrawingTool.TRAPEZOID, R.drawable.ic_trapezoid)
                        ),
                        currentTool = currentTool,
                        onShapeSelected = { tool ->
                            onShapeSelected(tool)
                            onDismiss()
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFFCCCCCC))
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Row 4: 3D Shapes
                    ShapeRow(
                        shapes = listOf(
                            ShapeItem(DrawingTool.CUBE, R.drawable.ic_cube),
                            ShapeItem(DrawingTool.CYLINDER, R.drawable.ic_cylinder),
                            ShapeItem(DrawingTool.PYRAMID, R.drawable.ic_pyramid),
                            ShapeItem(DrawingTool.SPHERE, R.drawable.ic_sphere),
                            ShapeItem(DrawingTool.CONE, R.drawable.ic_cone)
                        ),
                        currentTool = currentTool,
                        onShapeSelected = { tool ->
                            onShapeSelected(tool)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

/**
 * A row of shape buttons
 */
@Composable
private fun ShapeRow(
    shapes: List<ShapeItem>,
    currentTool: DrawingTool,
    onShapeSelected: (DrawingTool) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        shapes.forEach { shape ->
            ShapeButton(
                shapeItem = shape,
                isSelected = shape.tool == currentTool,
                onClick = { onShapeSelected(shape.tool) }
            )
        }
    }
}

/**
 * Individual shape button
 */
@Composable
private fun ShapeButton(
    shapeItem: ShapeItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) Color(0xFFE8E3FF) else Color(0xFFF5F5F5)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(shapeItem.iconResId),
            contentDescription = null,
            tint = if (isSelected) Color(0xFF4338CA) else Color(0xFF666666),
            modifier = Modifier.size(28.dp)
        )
    }
}

/**
 * Data class for shape items
 */
private data class ShapeItem(
    val tool: DrawingTool,
    val iconResId: Int
)

