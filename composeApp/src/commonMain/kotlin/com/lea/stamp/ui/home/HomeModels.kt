package com.lea.stamp.ui.home

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class Trend { UP, DOWN, STABLE }

enum class GoalStatus(val label: String, val color: Color) {
    ON_TRACK("On track", Color(0xFF66BB6A)), // Green
    SLIGHTLY_OFF("Slightly off", Color(0xFFFFA726)), // Orange
    NEEDS_ATTENTION("Needs attention", Color(0xFFEF5350)) // Red
}

enum class MetricStatus(val color: Color) {
    GOOD(Color(0xFF66BB6A)),
    WARNING(Color(0xFFFFA726)),
    ALERT(Color(0xFFEF5350)),
    NEUTRAL(Color.Gray)
}

data class MetricUiModel(
    val title: String,
    val current: Double,
    val target: Double,
    val unit: String,
    val status: MetricStatus,
    val color: Color = Color.Unspecified, // Goal-specific accent color
    val icon: ImageVector? = null
)

data class TrendBarData(
    val label: String,
    val values: List<Float>, // 3 values per day
    val valueLabels: List<String>, // Tooltip/Debugging info (e.g. "1500 kcal")
    val colors: List<Color>
)
