package com.lea.stamp.ui.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
actual fun PlatformCamera(
    modifier: Modifier,
    onControllerReady: (CameraCaptureController) -> Unit
) {
    // Stub for iOS
    Box(
        modifier = modifier
            .background(Color.DarkGray)
            .clickable {
                 // Simulate capture for testing on iOS (or non-supported platforms)
            },
        contentAlignment = Alignment.Center
    ) {
        Text("Camera not implemented for this platform", color = Color.White)
    }
    
    // Provide a mock controller immediately so the UI doesn't crash
    LaunchedEffect(Unit) {
        onControllerReady(object : CameraCaptureController {
            override fun capture(onResult: (String?) -> Unit) {
                // Simulate success with a dummy URL
                onResult("dummy_captured_image_url")
            }
        })
    }
}
