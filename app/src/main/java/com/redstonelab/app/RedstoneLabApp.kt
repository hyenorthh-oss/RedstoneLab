package com.redstonelab.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.redstonelab.app.theme.CyberBlack
import com.redstonelab.app.theme.RedstoneLabTheme
import com.redstonelab.app.ui.EditorScreen
import com.redstonelab.app.viewmodel.EditorViewModel

@Composable
fun RedstoneLabApp(
    viewModel: EditorViewModel = viewModel()
) {
    RedstoneLabTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = CyberBlack
        ) {
            EditorScreen(viewModel = viewModel)
        }
    }
}
