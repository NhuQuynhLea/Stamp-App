package com.lea.stamp.data

import kotlinx.serialization.Serializable

@Serializable
data class DailyGoalPlan(
    val title: String = "",
    val description: String = "",
    val metrics: List<GoalMetric> = emptyList()
)

@Serializable
data class GoalMetric(
    val label: String,
    val value: String,
    val rationale: String = ""
)
