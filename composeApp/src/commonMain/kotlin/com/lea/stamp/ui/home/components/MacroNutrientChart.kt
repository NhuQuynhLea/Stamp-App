package com.lea.stamp.ui.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.drawText
import com.lea.stamp.ui.home.TrendBarData

@Composable
fun DailyTrendChart(
    data: List<TrendBarData>,
    metricNames: List<String>,
    modifier: Modifier = Modifier
) {
    // We only render if we have data (HomeViewModel ensures we do)
    if (data.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val axisTextStyle = TextStyle(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)

    Surface(
        color = Color.White,
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Daily trend",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Legend
            Row(modifier = Modifier.padding(bottom = 12.dp)) {
                metricNames.forEachIndexed { index, name ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        // We use a generic grey dot or index-based color if needed, but bars are now status-colored.
                        // So legend is just to identify columns: Col 1, Col 2, Col 3
                        Text(
                            text = "${index + 1}: $name",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Legend for Colors logic check
            Row(modifier = Modifier.padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                 Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFEF5350)))
                 Spacer(Modifier.width(4.dp))
                 Text("High", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                 Spacer(Modifier.width(12.dp))
                 Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF66BB6A)))
                 Spacer(Modifier.width(4.dp))
                 Text("Good", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                 Spacer(Modifier.width(12.dp))
                 Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFFCA28)))
                 Spacer(Modifier.width(4.dp))
                 Text("Low", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Chart Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp) // Increased height for scaling
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    
                    // Margins
                    val leftMargin = 30.dp.toPx()
                    val bottomMargin = 20.dp.toPx()
                    val topMargin = 20.dp.toPx()
                    
                    val chartH = h - bottomMargin - topMargin
                    val chartW = w - leftMargin
                    
                    // define Y levels
                    val yZero = h - bottomMargin
                    val yTarget = topMargin + (chartH * 0.3f) // Target line at 70% from bottom (30% from top)
                    
                    // Actual height of "100%"
                    val targetHeight = yZero - yTarget
                    
                    // Draw Grid Lines
                    val gridColor = Color.LightGray.copy(alpha = 0.5f)
                    val dashed = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    
                    // 100% line (Target)
                    drawLine(gridColor, Offset(leftMargin, yTarget), Offset(w, yTarget), pathEffect = dashed)
                    // 50% line
                    val y50 = yZero - (targetHeight * 0.5f)
                    drawLine(gridColor, Offset(leftMargin, y50), Offset(w, y50), pathEffect = dashed)
                    // 0% line
                    drawLine(gridColor, Offset(leftMargin, yZero), Offset(w, yZero))

                    // Draw Y Axis Labels
                    drawText(textMeasurer, "Target", topLeft = Offset(0f, yTarget - 6.dp.toPx()), style = axisTextStyle)
                    drawText(textMeasurer, "50%", topLeft = Offset(0f, y50 - 6.dp.toPx()), style = axisTextStyle)
                    drawText(textMeasurer, "0", topLeft = Offset(0f, yZero - 6.dp.toPx()), style = axisTextStyle)

                    // Draw Bars
                    // We render from Oldest (Left) to Newest (Right)
                    // Repo returns "last 5 days" ordered by date, so no reverse needed.
                    val days = data 
                    // HomeViewModel creates (2 days ago, Yest, Today) order if filtered takeLast(3) on sorted list.
                    // So left to right is correct.
                    
                    val count = days.size
                    val slotWidth = chartW / count
                    
                    days.forEachIndexed { i, day ->
                        val slotLeft = leftMargin + (i * slotWidth)
                        val slotCenter = slotLeft + (slotWidth / 2)
                        
                        // X Axis Label
                        val labelLayout = textMeasurer.measure(day.label, style = axisTextStyle)
                        drawText(labelLayout, topLeft = Offset(slotCenter - (labelLayout.size.width / 2), h - bottomMargin + 4.dp.toPx()))

                        // Current day bars
                        // We have 3 metrics per day
                        val barsCount = day.values.size
                        // Total width for bars in slot = 60% of slot
                        val groupWidth = slotWidth * 0.7f
                        val barWidth = groupWidth / barsCount
                        val groupStart = slotCenter - (groupWidth / 2)
                        
                        day.values.forEachIndexed { j, ratio ->
                            // Calculate height relative to target line height
                            // Cap visually at maybe 150%? so it doesn't go off screen if huge outlier
                            val safeRatio = ratio.coerceAtMost(1.5f)
                            
                            val barH = targetHeight * safeRatio
                            val barTop = yZero - barH
                            
                            val barLeft = groupStart + (j * barWidth)
                            
                            val color = day.colors.getOrElse(j) { Color.Gray }
                            
                            drawRoundRect(
                                color = color,
                                topLeft = Offset(barLeft + 2.dp.toPx(), barTop), // 2dp spacing
                                size = Size(barWidth - 4.dp.toPx(), barH),
                                cornerRadius = CornerRadius(4.dp.toPx())
                            )
                        }
                    }
                }
            }
        }
    }
}
