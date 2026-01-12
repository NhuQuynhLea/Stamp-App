package com.lea.stamp

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import org.jetbrains.compose.ui.tooling.preview.Preview

import com.lea.stamp.ui.MainScreen

import kmp_hackathon.composeapp.generated.resources.Res
import kmp_hackathon.composeapp.generated.resources.compose_multiplatform


import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

@Composable
@Preview
fun App() {
    val colorScheme = lightColorScheme(
        background = Color(0xFFF8F8F8),
        surface = Color.White,
        onBackground = Color.Black,
        onSurface = Color.Black,
        primary = Color(0xFF006400 ),
    )

    MaterialTheme(
        colorScheme = colorScheme
    ) {
        MainScreen()
    }
}