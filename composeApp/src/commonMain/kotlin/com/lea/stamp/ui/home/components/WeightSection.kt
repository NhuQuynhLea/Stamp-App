package com.lea.stamp.ui.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lea.stamp.ui.home.GoalStatus
import com.lea.stamp.ui.home.Trend

@Composable
fun WeightSection(
    currentWeight: Float,
    weightChange: Float,
    trend: Trend,
    status: GoalStatus,
    onEditClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                 Column(verticalArrangement = Arrangement.spacedBy((-4).dp)) {
                     Text(
                         text = "Current",
                         style = MaterialTheme.typography.labelMedium,
                         color = MaterialTheme.colorScheme.onSurfaceVariant
                     )
                     Row(verticalAlignment = Alignment.Bottom) {
                         Text(
                             text = "$currentWeight",
                             style = MaterialTheme.typography.headlineLarge.copy(
                                 fontWeight = FontWeight.Bold, 
                                 fontSize = 32.sp,
                                 lineHeight = 32.sp,
                                 letterSpacing = (-1).sp
                             ),
                         )
                         Text(
                             text = "kg",
                             style = MaterialTheme.typography.bodySmall,
                             color = MaterialTheme.colorScheme.onSurfaceVariant,
                             modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                         )
                     }
                 }

                 IconButton(
                     onClick = onEditClick,
                     modifier = Modifier.size(24.dp)
                 ) {
                     Icon(
                         imageVector = Icons.Filled.Edit,
                         contentDescription = "Edit Weight",
                         tint = MaterialTheme.colorScheme.primary,
                         modifier = Modifier.size(16.dp)
                     )
                 }
            }

            // Bottom Stats Row: Lost | Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Lost / Trend
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    Text(
                        text = "Lost",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // If weightChange is negative (loss), we show positive number.
                    val isLoss = weightChange < 0
                    val displayValue = if (isLoss) -weightChange else weightChange
                    val color = if (isLoss) Color(0xFF66BB6A) else if (weightChange > 0) Color(0xFFEF5350) else Color.Gray
                    
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${displayValue.toString().take(4)}",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, lineHeight = 24.sp),
                            color = color
                        )
                        Text(
                             text = "kg",
                             style = MaterialTheme.typography.bodySmall,
                             color = MaterialTheme.colorScheme.onSurfaceVariant,
                             modifier = Modifier.padding(bottom = 2.dp, start = 2.dp)
                         )
                    }
                }
                
                // Status
                Column(verticalArrangement = Arrangement.spacedBy(2.dp), horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Status",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        color = status.color.copy(alpha = 0.2f),
                        contentColor = status.color,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Canvas(modifier = Modifier.size(6.dp)) {
                                drawCircle(color = status.color)
                            }
                            Text(
                                text = status.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
