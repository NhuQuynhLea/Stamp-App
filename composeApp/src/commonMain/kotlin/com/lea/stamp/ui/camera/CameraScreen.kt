package com.lea.stamp.ui.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kmp_hackathon.composeapp.generated.resources.Res
import kmp_hackathon.composeapp.generated.resources.stamp
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun CameraScreen(
    onCapture: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var cameraController by remember { mutableStateOf<CameraCaptureController?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Camera Preview
        PlatformCamera(
            modifier = Modifier.fillMaxSize(),
            onControllerReady = { cameraController = it }
        )

        // Viewfinder frame
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(300.dp)
                .border(2.dp, Color.White.copy(alpha = 0.5f))
        )

        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopStart)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }

        // Bottom Controls
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 48.dp)
                .align(Alignment.BottomCenter)
        ) {
            // Capture Button
            // Capture Button (Stamp Styled)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.Center)
                    .clickable { 
                        cameraController?.capture { path ->
                            if (path != null) {
                                onCapture(path)
                            }
                        }
                    }
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color.White, CircleShape)
                        .border(4.dp, Color.White, CircleShape)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                     Icon(
                         painter = painterResource(Res.drawable.stamp),
                         contentDescription = "Stamp this Meal",
                         tint = MaterialTheme.colorScheme.primary,
                         modifier = Modifier.fillMaxSize()
                     )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Stamp this Meal",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.background(
                        color = Color.Black.copy(alpha = 0.5f), 
                        shape = RoundedCornerShape(8.dp)
                    ).padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
