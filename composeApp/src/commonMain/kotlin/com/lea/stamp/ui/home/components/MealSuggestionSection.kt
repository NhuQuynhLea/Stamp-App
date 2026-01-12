package com.lea.stamp.ui.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
// import androidx.compose.material.icons.filled.Restaurant // Safe
// import androidx.compose.material.icons.filled.FreeBreakfast // Risk
// import androidx.compose.material.icons.filled.DinnerDining // Risk
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp


import androidx.compose.material.icons.filled.Restaurant

@Composable
fun MealSuggestionSection(
    suggestion: String, 
    type: String
) {
    // Colors matching the image (Cream/Orange theme)
    val cardBgColor = Color(0xFFffd6a8) // Light cream/beige
    val contentColor = Color(0xFF2C3E50) // Slate
    // val titleColor = Color(0xFFE65100) // Dark Orange
    val titleColor = Color(0xFF000000)
    val accentColor = Color(0xFFFF9800) // Orange

    val iconVector = when (type) {
        "Breakfast" -> Icons.Filled.Star // Fallback for Breakfast
        "Lunch" -> Icons.Filled.Restaurant
        "Dinner" -> Icons.Filled.Restaurant // Fallback for Dinner
        else -> Icons.Filled.Star
    }

    // Try to parse "Breakfast" -> "FreeBreakfast" if I could.

    Card(
        colors = CardDefaults.cardColors(
            containerColor = cardBgColor
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = "$type Suggestion",
                    tint = accentColor,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Column {
                    Text(
                         text = type, // e.g. "Dinner"
                         style = MaterialTheme.typography.labelSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                         color = titleColor
                    )
                    Text(
                        text = "Suggestion",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                        color = titleColor
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            // Format content: Detect bullets and paragraphs
            val lines = suggestion.split("\n")
            lines.forEach { line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ")) {
                    val cleanText = trimmed.removePrefix("- ").removePrefix("* ").removePrefix("• ").trim()
                    
                    Row(
                        modifier = Modifier.padding(start = 0.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp).padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = cleanText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = contentColor
                        )
                    }
                } else if (trimmed.isNotBlank()) {
                     Text(
                        text = trimmed,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
        }
    }
}
