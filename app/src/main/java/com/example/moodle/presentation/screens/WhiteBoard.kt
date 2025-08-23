package com.example.moodle.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.moodle.presentation.components.CanvasControls
import com.example.moodle.presentation.components.DrawingCanvas
import com.example.moodle.domain.usecases.DrawingViewModel
import com.example.moodle.domain.usecases.DrawingAction
import com.example.moodle.domain.usecases.allColors

@Composable
fun WhiteBoard(
    innerPadding: PaddingValues,
    onLogout: () -> Unit = {}
) {
    val viewModel = viewModel<DrawingViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header with logout button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
           
            IconButton(
                onClick = onLogout,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Logout",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        DrawingCanvas(
            paths = state.paths,
            currentPath = state.currentPath,
            onAction = viewModel::onAction,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
        CanvasControls(
            selectedColor = state.selectedColor,
            colors = allColors,
            onSelectColor = {
                viewModel.onAction(DrawingAction.OnSelectColor(it))
            },
            onClearCanvas = {
                viewModel.onAction(DrawingAction.OnClearCanvasClick)
            }
        )
    }
}