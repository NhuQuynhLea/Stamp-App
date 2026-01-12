package com.lea.stamp.ui.camera

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface CameraCaptureController {
    fun capture(onResult: (String?) -> Unit)
}

@Composable
expect fun PlatformCamera(
    modifier: Modifier = Modifier,
    onControllerReady: (CameraCaptureController) -> Unit
)
