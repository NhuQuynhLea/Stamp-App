package com.lea.stamp.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lea.stamp.ui.home.MetricUiModel
import androidx.compose.ui.graphics.StrokeCap

@Composable
fun KeyMetricsSection(metrics: List<MetricUiModel>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Today’s Key Metrics",
            style = MaterialTheme.typography.titleMedium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            metrics.forEach { metric ->
                KeyMetricCard(
                    metric = metric,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun KeyMetricCard(metric: MetricUiModel, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: Icon + Percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon based on metric.icon (dynamically assigned) or fallback
                val icon = metric.icon ?: when {
                    metric.title.contains("Calor", ignoreCase = true) -> Icons.Filled.LocalFireDepartment
                    metric.title.contains("Water", ignoreCase = true) -> Icons.Filled.WaterDrop
                    metric.title.contains("Protein", ignoreCase = true) -> Icons.Filled.FitnessCenter
                    metric.title.contains("Heart", ignoreCase = true) -> Icons.Filled.Favorite
                    else -> Icons.Filled.ShowChart
                }
                
                // Use metric.color if specified, else status color
                val accentColor = if (metric.color != Color.Unspecified) metric.color else metric.status.color
                
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )

                // Percentage
                val percentage = if (metric.target > 0) (metric.current / metric.target * 100).toInt() else 0
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Body: Title
            Text(
                text = metric.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Values: Current (Big)
            Text(
                text = metric.current.toInt().toString(),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            
            // Target + Unit (Small)
            Row(verticalAlignment = Alignment.CenterVertically) {
                 if (metric.target > 0) {
                    Text(
                        text = "/ ${metric.target.toInt()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (metric.unit.isNotBlank()) {
                     Text(
                        text = " ${metric.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }
            }

            // Footer: Progress Bar
            val progress = if (metric.target > 0) (metric.current / metric.target).toFloat().coerceIn(0f, 1f) else 0f
            
            // Use accentColor for progress bar as well
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = if (metric.color != Color.Unspecified) metric.color else metric.status.color,
                trackColor = (if (metric.color != Color.Unspecified) metric.color else metric.status.color).copy(alpha = 0.2f),
                strokeCap = StrokeCap.Round,
            )
        }
    }
}
