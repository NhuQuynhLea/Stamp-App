package com.lea.stamp.ui.home

import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Grain
import com.lea.stamp.data.HealthData
import com.lea.stamp.data.DailyNutrients
import com.lea.stamp.data.DailyGoalPlan
import kotlinx.serialization.json.Json

fun generateKeyMetrics(data: HealthData): List<MetricUiModel> {
    // Try to parse dynamic goal plan
    val dynamicPlan = try {
        if (data.detailGoal.isNotBlank()) {
            Json.decodeFromString<DailyGoalPlan>(data.detailGoal)
        } else null
    } catch (e: Exception) {
        println("Error parsing detailGoal: ${e.message}")
        null
    }

    val metrics = mutableListOf<MetricUiModel>()

    if (dynamicPlan != null && dynamicPlan.metrics.isNotEmpty()) {
        dynamicPlan.metrics.take(3).forEach { goalMetric ->
            val label = goalMetric.label
            val targetText = goalMetric.value
            
            // Helper to extract first number from string (e.g. "150g" -> 150)
            val targetValue = Regex("(\\d+)(\\.\\d+)?").find(targetText)?.value?.toDoubleOrNull() ?: 100.0
            val unit = targetText.replace(Regex("[0-9.\\s]"), "") // clear numbers and spaces to get unit
            
            // Map label to actual data
            val (currentValue, status, finalUnit) = when {
                label.contains("Calor", ignoreCase = true) -> {
                    val v = data.currentCalories.toDouble()
                    val s = if (v > targetValue * 1.1) MetricStatus.WARNING else MetricStatus.GOOD
                    Triple(v, s, "")
                }
                label.contains("Protein", ignoreCase = true) -> {
                    val v = data.protein
                    Triple(v, MetricStatus.GOOD, "g")
                }
                label.contains("Fiber", ignoreCase = true) -> {
                    val v = data.fiber
                    Triple(v, if (v >= targetValue * 0.8) MetricStatus.GOOD else MetricStatus.WARNING, "g")
                }
                label.contains("Sugar", ignoreCase = true) -> {
                    val v = data.addedSugar
                    Triple(v, if (v > targetValue) MetricStatus.WARNING else MetricStatus.GOOD, "g")
                }
                label.contains("Fat", ignoreCase = true) && label.contains("Sat", ignoreCase = true) -> {
                    val v = data.saturatedFat
                    Triple(v, if (v < targetValue) MetricStatus.GOOD else MetricStatus.WARNING, "g")
                }
                 label.contains("Sodium", ignoreCase = true) -> {
                    val v = data.sodium
                     Triple(v, if (v > targetValue) MetricStatus.WARNING else MetricStatus.GOOD, "mg")
                }
                label.contains("Water", ignoreCase = true) || label.contains("Hydra", ignoreCase = true) -> {
                    val v = data.waterIntakeLiters.toDouble()
                    // Ensure targetValue is reasonable for water (liters), sometimes prompt returns ml
                    val t = if (targetValue > 10) targetValue / 1000.0 else targetValue
                    Triple(v, MetricStatus.GOOD, "L")
                }
                label.contains("Carb", ignoreCase = true) -> {
                    val v = data.carbs
                    Triple(v, MetricStatus.GOOD, "g")
                }
                else -> {
                    Triple(0.0, MetricStatus.NEUTRAL, unit)
                }
            }
            
            // --- Metric Config & Styling ---
            val goalKey = data.primaryGoal.trim().lowercase()
            
            // Default / Fallback Style
            var mIcon = Icons.Filled.LocalFireDepartment
            var mColor = Color(0xFF90CAF9) // Default Blue

            // Goal-specific Palettes
            when {
                goalKey.contains("weight") -> {
                    // Palette: Soft Red, Soft Blue, Soft Green
                    when {
                        label.contains("Calor", ignoreCase = true) -> {
                            mIcon = Icons.Filled.LocalFireDepartment
                            mColor = Color(0xFFEF9A9A) // Red 200
                        }
                        label.contains("Protein", ignoreCase = true) -> {
                            mIcon = Icons.Filled.FitnessCenter
                            mColor = Color(0xFF90CAF9) // Blue 200
                        }
                        label.contains("Fiber", ignoreCase = true) -> {
                            mIcon = Icons.Filled.Spa
                            mColor = Color(0xFFA5D6A7) // Green 200
                        }
                    }
                }
                goalKey.contains("muscle") -> {
                    // Palette: Deep Purple, Orange, Teal
                    when {
                        label.contains("Protein", ignoreCase = true) -> {
                            mIcon = Icons.Filled.FitnessCenter
                            mColor = Color(0xFFB39DDB) // Deep Purple 200
                        }
                        label.contains("Calor", ignoreCase = true) -> {
                            mIcon = Icons.Filled.LocalFireDepartment
                            mColor = Color(0xFFFFCC80) // Orange 200
                        }
                        label.contains("Time", ignoreCase = true) || label.contains("Timing", ignoreCase = true) -> {
                            mIcon = Icons.Filled.AccessTime
                            mColor = Color(0xFF80CBC4) // Teal 200
                        }
                    }
                }
                goalKey.contains("energy") -> {
                     // Palette: Amber, Light Blue, Blue
                    when {
                        label.contains("Carb", ignoreCase = true) -> {
                            mIcon = Icons.Filled.Bolt
                            mColor = Color(0xFFFFE082) // Amber 200
                        }
                        label.contains("Hydra", ignoreCase = true) || label.contains("Water", ignoreCase = true) -> {
                            mIcon = Icons.Filled.WaterDrop
                            mColor = Color(0xFF81D4FA) // Light Blue 200
                        }
                    }
                }
                 goalKey.contains("sugar") || goalKey.contains("blood") -> {
                     // Palette: Red, Green, Blue
                     when {
                         label.contains("Sug", ignoreCase = true) -> {
                             mIcon = Icons.Filled.Bolt
                             mColor = Color(0xFFEF9A9A) // Red 200
                         }
                         label.contains("Fib", ignoreCase = true) -> {
                             mIcon = Icons.Filled.Spa
                             mColor = Color(0xFFA5D6A7) // Green 200
                         }
                         label.contains("Tim", ignoreCase = true) -> {
                             mIcon = Icons.Filled.AccessTime
                             mColor = Color(0xFF90CAF9) // Blue 200
                         }
                     }
                }
                else -> {
                    // General mapping if not caught above
                    when {
                        label.contains("Calor", ignoreCase = true) -> mIcon = Icons.Filled.LocalFireDepartment
                        label.contains("Water", ignoreCase = true) -> mIcon = Icons.Filled.WaterDrop
                        label.contains("Prot", ignoreCase = true) -> mIcon = Icons.Filled.FitnessCenter
                        label.contains("Heart", ignoreCase = true) -> mIcon = Icons.Filled.Favorite
                        label.contains("Fat", ignoreCase = true) -> mIcon = Icons.Filled.Opacity
                        label.contains("Sod", ignoreCase = true) -> mIcon = Icons.Filled.Grain
                    }
                }
            }

            // If special logic for water (ml vs L) wasn't applied in map, apply target fix
            val finalTarget = if ((label.contains("Water", ignoreCase = true) || label.contains("Hydra", ignoreCase = true)) && targetValue > 10) targetValue / 1000.0 else targetValue
            
            metrics.add(MetricUiModel(
                title = label,
                current = currentValue,
                target = finalTarget,
                unit = finalUnit.ifBlank { unit },
                status = status,
                color = mColor,
                icon = mIcon
            ))
        }
    } else {
         // Fallback to old hardcoded logic if no dynamic plan
        val goal = data.primaryGoal.lowercase()
        if (goal.contains("weight")) {
             metrics.add(MetricUiModel("Calories", data.currentCalories.toDouble(), data.targetCalories.toDouble(), "", 
                if (data.currentCalories > data.targetCalories) MetricStatus.WARNING else MetricStatus.GOOD, 
                Color(0xFFEF9A9A), Icons.Filled.LocalFireDepartment))
            metrics.add(MetricUiModel("Protein", data.protein, 140.0, "g", MetricStatus.GOOD, 
                Color(0xFF90CAF9), Icons.Filled.FitnessCenter))
            metrics.add(MetricUiModel("Fiber", data.fiber, 25.0, "g", if (data.fiber > 20) MetricStatus.GOOD else MetricStatus.WARNING, 
                Color(0xFFA5D6A7), Icons.Filled.Spa))
        } else {
             metrics.add(MetricUiModel("Calories", data.currentCalories.toDouble(), data.targetCalories.toDouble(), "", MetricStatus.NEUTRAL, Color(0xFFEF9A9A), Icons.Filled.LocalFireDepartment))
             metrics.add(MetricUiModel("Water", data.waterIntakeLiters.toDouble(), data.waterTargetLiters.toDouble(), "L", MetricStatus.GOOD, Color(0xFF81D4FA), Icons.Filled.WaterDrop))
             metrics.add(MetricUiModel("Protein", data.protein, 100.0, "g", MetricStatus.GOOD, Color(0xFF90CAF9), Icons.Filled.FitnessCenter))
        }
    }
    
    return metrics
}

fun generateTrendData(
    goal: String, 
    targetCals: Int, 
    currentPro: Double, 
    trends: List<DailyNutrients>
): Pair<List<TrendBarData>, List<String>> {
    // Limit to last 5 days
    val recentTrends = trends.takeLast(5)
    
    // Ensure we always have 5 days of data for the chart
    val validTrends = if (recentTrends.size < 5) {
        val missingCount = 5 - recentTrends.size
        // padding
        val padding = (missingCount downTo 1).map { offset ->
             DailyNutrients(
                 dayLabel = "", // Will be set dynamically
                 date = 0L,
                 calories = 0,
                 carbs = 0.0,
                 protein = 0.0,
                 fat = 0.0,
                 fiber = 0.0,
                 addedSugar = 0.0,
                 sodium = 0.0,
                 saturatedFat = 0.0,
                 vegetableContent = 0.0,
                 water = 0.0
             )
        }
        padding + recentTrends
    } else {
        recentTrends
    }

    val g = goal.lowercase()
    // Determine targets for normalization
    val config = when {
        g.contains("weight") -> Triple(listOf("Cals", "Prot", "Fiber"), listOf(targetCals.toDouble(), 100.0, 25.0), listOf(Color.Unspecified, Color.Unspecified, Color.Unspecified))
        g.contains("muscle") -> Triple(listOf("Prot", "Cals", "Time"), listOf(150.0, targetCals.toDouble() + 200, 100.0), listOf(Color.Unspecified, Color.Unspecified, Color.Unspecified))
        g.contains("energy") -> Triple(listOf("CarbQ", "Hydr", "Water"), listOf(1.0, 2.5, 1.0), listOf(Color.Unspecified, Color.Unspecified, Color.Unspecified))
        g.contains("sugar") || g.contains("blood") -> Triple(listOf("Sug", "Fibr", "Time"), listOf(30.0, 25.0, 100.0), listOf(Color.Unspecified, Color.Unspecified, Color.Unspecified))
        g.contains("heart") -> Triple(listOf("Sod", "SatF", "Fibr"), listOf(2300.0, 20.0, 25.0), listOf(Color.Unspecified, Color.Unspecified, Color.Unspecified))
        else -> Triple(listOf("Cals", "Watr", "Prot"), listOf(targetCals.toDouble(), 2.5, 100.0), listOf(Color.Unspecified, Color.Unspecified, Color.Unspecified))
    }
    
    val (labels, targets, _) = config
    
    // Colors for status: Over (>110%), Good (80-110%), Low (<80%)
    val colorOver = Color(0xFFEF5350) // Red
    val colorGood = Color(0xFF66BB6A) // Green
    val colorLow = Color(0xFFFFCA28) // Yellow
    val colorEmpty = Color.LightGray

    val barData = validTrends.mapIndexed { index, day ->
        // Fix Day Label if empty
        val dayLabel = if (day.date == 0L) {
             // Simple fallback logic for 5 days (indexes 0..4)
             // 4 = Today, 3 = Yesterday
             when(index) {
                 4 -> "Today"
                 3 -> "Yest."
                 else -> "-"
             }
        } else {
            day.dayLabel
        }

        val vals = when {
            g.contains("weight") -> listOf(day.calories.toDouble(), day.protein, day.fiber)
            g.contains("muscle") -> listOf(day.protein, day.calories.toDouble(), day.mealTimingScore)
            g.contains("energy") -> listOf(if (day.carbs > 0) day.fiber / day.carbs else 0.0, day.water, day.water)
            g.contains("sugar") || g.contains("blood") -> listOf(day.addedSugar, day.fiber, day.mealTimingScore)
            g.contains("heart") -> listOf(day.sodium, day.saturatedFat, day.fiber)
            else -> listOf(day.calories.toDouble(), day.water, day.protein)
        }
        
        // Normalize to ratio
        val normalized = vals.mapIndexed { idx, v -> 
            val t = targets.getOrElse(idx) { 100.0 }
            if (t > 0) (v / t).toFloat() else 0f
        }
        
        // Determine Colors based on ratio (except timing scores or specific inverses, but sticking to general rule)
        val barColors = normalized.map { ratio ->
            if (ratio == 0f) colorEmpty 
            else if (ratio > 1.1f) colorOver 
            else if (ratio < 0.8f) colorLow 
            else colorGood
        }
        
        val valueLabels = vals.mapIndexed { idx, v -> 
             "${v.toInt()}"
        }

        TrendBarData(
            label = dayLabel,
            values = normalized,
            valueLabels = valueLabels,
            colors = barColors
        )
    }
    return barData to labels
}
