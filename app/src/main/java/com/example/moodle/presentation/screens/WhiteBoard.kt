package com.example.moodle.presentation.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.moodle.presentation.components.CanvasControls
import com.example.moodle.presentation.components.DrawingCanvas
import com.example.moodle.domain.usecases.DrawingViewModel
import com.example.moodle.domain.usecases.DrawingAction
import com.example.moodle.domain.usecases.allColors

@Composable
fun WhiteBoard() {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        val viewModel = viewModel<DrawingViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
}