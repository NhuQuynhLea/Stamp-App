package com.lea.stamp.ui.home

data class HomeUiState(
    // Main Goal Headers
    val mainGoalName: String = "Weight loss",
    val mainGoalStatus: GoalStatus = GoalStatus.ON_TRACK,
    val mainGoalTrend: Trend = Trend.STABLE,
    val mainGoalDescription: String = "Energy improving this week",

    // Goal-Driven Metrics
    val keyMetrics: List<MetricUiModel> = emptyList(),
    
    // Daily Trend Chart
    val dailyTrend: List<TrendBarData> = emptyList(),
    val trendMetricNames: List<String> = emptyList(), // Legend (e.g. ["Cals", "Prot", "Fiber"])

    // Existing metrics (some might be redundant but keeping for charts)
    val currentCalories: Int = 0,
    val targetCalories: Int = 0,
    val calorieProgress: Float = 0f,
    
    val currentWeight: Float = 0f,
    val weightHistory: List<Float> = emptyList(),
    val weightGraphData: List<Pair<String, Float>> = emptyList(), // Date -> Weight for Chart
    val weightChange: Float = 0f,
    val weightTrend: Trend = Trend.STABLE,
    
    val waterIntake: Float = 0f,
    val waterTarget: Float = 0f,
    val waterProgress: Float = 0f,
    
    val carbs: Double = 0.0,
    val protein: Double = 0.0,
    val fat: Double = 0.0,
    val totalMacros: Double = 1.0,
    
    val dailyProgress: Float = 0f,
    
    val aiWarning: String? = null,
    val overallAdvice: String? = null,
    val nextMealSuggestion: String? = null,
    val mealSuggestionType: String = "Lunch", // Default to Lunch or determine logic
    
    val isLoading: Boolean = false,
    val isAiLoading: Boolean = false
)
