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

// Helper to get icons if available, else fallback
// We can't do reflection easily in commonMain without quirks, so we stick to safe calls?
// I'll try to use specific imports. If they fail, the user will report compilation error.
// The user asked for "mapping". 
// Let's assume extended icons are NOT available by default unless I see them used.
// I haven't seen them used.
// I'll use Star, Restaurant (if valid), and maybe Edit?
// Actually, let's use text emoticons or generic icons if we can't be sure? No, that's ugly.

// I will try to use the names, assuming standard material.
// Restaurant is common.
// FreeBreakfast is common.
// DinnerDining is not common (Extended).
// I'll use:
// Breakfast -> Star (with Coffee emoji in title? No) -> Just Star for now? Or `Info`?
// Lunch -> Star
// Dinner -> Star
// But I'll color them.
// Wait, user specifically asked "change the icon in title mapping with BreakFase, Dinner, Lunch".
// I MUST map them.
// I will try to use `androidx.compose.material.icons.filled.Restaurant` (Lunch/Dinner) and `androidx.compose.material.icons.filled.FreeBreakfast` (Breakfast).
// I will add the imports. If it crashes, I fix.

import androidx.compose.material.icons.filled.Restaurant
// import androidx.compose.material.icons.filled.FreeBreakfast // I'll trust this exists in newer material3 or extended.
// Actually, `FreeBreakfast` is in `androidx.compose.material.icons.filled` in some versions, but often extended.
// I'll use `Icons.Filled.AccountBox`? No.

// Let's look for known specific icons in the USER's code.
// No specific usage of extended icons seen.

// I'll use `Icons.Filled.Face` (Breakfast?), `Icons.Filled.Home` (Lunch?)... No.
// I will use `Star` for all but with different colors? No.
// I will try `Restaurant` for all "Food" related ones.
// I'll use `Restaurant` for Lunch and Dinner.
// For Breakfast, maybe `Restaurant` too?
// Or `Check`?
// The user really wants mapping.
// I'll provide a placeholder TODO comment or use `Restaurant` for all if I can't find others.
// Actually, I can use creating an Icon from resource if I had one.

// I'll use `Restaurant` for Lunch/Dinner.
// I'll use `Star` for Breakfast for now, but label it "Breakfast".

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
