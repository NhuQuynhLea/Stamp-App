package com.lea.stamp.ui.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WeightLineChart(
    data: List<Pair<String, Float>>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val weights = data.map { it.second }
    val minWeight = (weights.minOrNull() ?: 0f) - 1f
    val maxWeight = (weights.maxOrNull() ?: 100f) + 1f
    val range = maxWeight - minWeight

    Box(modifier = modifier.fillMaxWidth().height(220.dp).padding(16.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val paddingLeft = 40.dp.toPx() // Increased space for Y-axis labels
            val paddingBottom = 24.dp.toPx() // Space for X-axis labels
            
            val width = size.width - paddingLeft
            val height = size.height - paddingBottom
            
            val xStep = width / (data.size - 1).coerceAtLeast(1)

            // Paint for text
             val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.GRAY
                textSize = 12.sp.toPx()
                textAlign = android.graphics.Paint.Align.RIGHT
            }
            
            // Draw Vertical Y-Axis Line
            drawLine(
                color = Color.LightGray,
                start = Offset(paddingLeft, 0f),
                end = Offset(paddingLeft, height),
                strokeWidth = 1.dp.toPx()
            )

            // Draw Y-Axis (Grid lines & Labels)
            val ySteps = 4
            for (i in 0..ySteps) {
                val ratio = i.toFloat() / ySteps
                val yVal = minWeight + (range * ratio)
                val y = height - (ratio * height)
                
                // Grid line
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.5f),
                    start = Offset(paddingLeft, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
                
                // Tick mark
                drawLine(
                    color = Color.LightGray,
                    start = Offset(paddingLeft - 4.dp.toPx(), y),
                    end = Offset(paddingLeft, y),
                    strokeWidth = 1.dp.toPx()
                )
                
                // Label
                drawContext.canvas.nativeCanvas.drawText(
                    "${yVal.toInt()}",
                    paddingLeft - 8.dp.toPx(),
                    y + 4.dp.toPx(), // Centered vertically roughly
                    textPaint
                )
            }

            // Draw Line Path
            val path = Path()
            data.forEachIndexed { index, pair ->
                val x = paddingLeft + (index * xStep)
                // Invert Y because canvas 0 is top
                val normalizedY = (pair.second - minWeight) / range
                val y = height - (normalizedY * height)
                
                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
                
                // Draw Points
                drawCircle(
                    color = Color(0xFF4285F4), // Blue color
                    radius = 5.dp.toPx(),
                    center = Offset(x, y)
                )
            }
            
            drawPath(
                path = path,
                color = Color(0xFF4285F4), // Blue color
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )
            
            // Draw Labels (X-axis)
            val xTextPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.GRAY
                textSize = 12.sp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
            }
            
            data.forEachIndexed { index, pair ->
                 // Simple logic to avoid overcrowding: show every 2nd if > 7 items
                 if (data.size <= 7 || index % 2 == 0 || index == data.size - 1) {
                     val x = paddingLeft + (index * xStep)
                     drawContext.canvas.nativeCanvas.drawText(
                         pair.first,
                         x,
                         size.height, // Bottom
                         xTextPaint
                     )
                 }
            }
        }
    }
}
