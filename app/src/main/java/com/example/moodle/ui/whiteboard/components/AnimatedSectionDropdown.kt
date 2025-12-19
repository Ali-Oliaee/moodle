package com.example.moodle.ui.whiteboard.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodle.R

/**
 * Data class representing a section option in the dropdown
 */
data class SectionOption(
    val count: Int,
    val labelResId: Int
)

/**
 * Animated dropdown for selecting split section count
 * 
 * @param isExpanded Whether the dropdown is currently expanded
 * @param onItemSelected Callback when a section count is selected
 * @param onDismiss Callback when dropdown should be dismissed (clicking outside)
 * @param modifier Modifier for the dropdown container
 */
@Composable
fun AnimatedSectionDropdown(
    isExpanded: Boolean,
    onItemSelected: (Int) -> Unit,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val sectionOptions = remember {
        listOf(
            SectionOption(2, R.string.section_count_2),
            SectionOption(3, R.string.section_count_3),
            SectionOption(4, R.string.section_count_4),
            SectionOption(6, R.string.section_count_6)
        )
    }
    
    val expandedState = remember { MutableTransitionState(false) }
    expandedState.targetState = isExpanded
    
        // Animated Dropdown Content
        AnimatedVisibility(
            visibleState = expandedState,
            enter = fadeIn(animationSpec = tween(300)) + 
                    expandVertically(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)) + 
                   shrinkVertically(animationSpec = tween(300))
        ) {
            Card(
                modifier = Modifier
                    .width(40.dp)
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.95f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    sectionOptions.forEachIndexed { index, option ->
                        DropdownItem(
                            text = stringResource(id = option.labelResId),
                            onClick = { onItemSelected(option.count) }
                        )
                        
                        if (index < sectionOptions.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                thickness = 0.5.dp,
                                color = Color.LightGray.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
        }
    }
}

/**
 * Individual dropdown item with hover effect
 */
@Composable
private fun DropdownItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(100.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = text,
                fontSize = 13.sp,
                color = Color(0xFF1F2937),
                fontWeight = FontWeight.Normal
            )
        }
    }
}

