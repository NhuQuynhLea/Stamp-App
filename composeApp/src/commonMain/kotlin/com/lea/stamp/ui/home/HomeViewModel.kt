package com.lea.stamp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lea.stamp.data.HealthRepository
import com.lea.stamp.data.RepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.lea.stamp.data.gemini.AICoachService
import com.lea.stamp.data.gemini.AICoachServiceProvider
import com.lea.stamp.data.gemini.GeminiResult
import com.lea.stamp.data.HealthData
import kotlinx.coroutines.joinAll

import com.lea.stamp.data.PreferencesManager
import com.lea.stamp.data.PreferencesProvider
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.toLocalDateTime

class HomeViewModel(
    private val repository: HealthRepository = RepositoryProvider.instance,
    private val aiCoachService: AICoachService = AICoachServiceProvider.instance,
    private val preferencesManager: PreferencesManager = PreferencesProvider.instance
) : ViewModel() {

    private val _isAiLoading = MutableStateFlow(false)
    
    val uiState: StateFlow<HomeUiState> = kotlinx.coroutines.flow.combine(
        repository.getHealthData(),
        repository.getWeeklyTrends(),
        _isAiLoading
    ) { data, trends, isAiLoading ->
            val calProgress = if (data.targetCalories > 0) data.currentCalories.toFloat() / data.targetCalories else 0f
            val waterPrg = if (data.waterTargetLiters > 0) data.waterIntakeLiters / data.waterTargetLiters else 0f
            
            val totalMacros = data.carbs + data.protein + data.fat
            val safeTotalMacros = if (totalMacros > 0) totalMacros else 1.0

            // Simple heuristic for daily progress
            val dailyPrg = ((calProgress + waterPrg) / 2).coerceAtMost(1.0f)

            // Weight Trend Logic
            val history = data.weightProgress
            val currentW = data.currentWeight
            val prevW = if (history.size > 1) history[history.size - 2].second else currentW
            val weightChange = currentW - prevW
            val weightTrend = when {
                weightChange > 0.1 -> Trend.UP
                weightChange < -0.1 -> Trend.DOWN
                else -> Trend.STABLE
            }

            // Goal Logic
            val goalStatus = if (data.aiWarning != null) GoalStatus.NEEDS_ATTENTION else GoalStatus.ON_TRACK
            val goalTrend = if (data.primaryGoal.contains("weight", ignoreCase = true)) {
                if (weightChange < 0) Trend.UP else Trend.DOWN // Weight loss logic
                weightTrend
            } else {
                Trend.STABLE
            }

            // Key Metrics (using Factory)
            val keyMetrics = generateKeyMetrics(data)
            
            // Chart Data Generation (using Factory)
            val (trendData, metricNames) = generateTrendData(data.primaryGoal, data.targetCalories, data.protein, trends)

            HomeUiState(
                mainGoalName = data.primaryGoal.ifBlank { "Live Healthily" },
                mainGoalStatus = goalStatus,
                mainGoalTrend = goalTrend,
                mainGoalDescription = data.overallAdvice ?: "Keep going!",
                
                keyMetrics = keyMetrics,
                
                dailyTrend = trendData,
                trendMetricNames = metricNames,

                currentCalories = data.currentCalories,
                targetCalories = data.targetCalories,
                calorieProgress = calProgress.coerceAtMost(1.0f),
                
                currentWeight = data.currentWeight,
                weightHistory = data.weightProgress.map { it.second },
                weightGraphData = data.weightProgress,
                weightChange = weightChange,
                weightTrend = weightTrend,
                
                waterIntake = data.waterIntakeLiters,
                waterTarget = data.waterTargetLiters,
                waterProgress = waterPrg.coerceAtMost(1.0f),
                
                carbs = data.carbs,
                protein = data.protein,
                fat = data.fat,
                totalMacros = safeTotalMacros,
                
                dailyProgress = dailyPrg,
                
                aiWarning = data.aiWarning,
                overallAdvice = data.overallAdvice,
                nextMealSuggestion = data.nextMealSuggestion.also { println("HomeViewModel UI State: nextMealSuggestion=$it") },
                mealSuggestionType = determineMealType(),
                
                isAiLoading = isAiLoading
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeUiState(isLoading = true)
        )
    
    init {
        // trigger the AI Coach whenever there is update in health data (metrics or meals)
        viewModelScope.launch {
            repository.getHealthData()
                .collect { data ->
                     // Check trigger conditions
                     if (shouldTriggerAi(data)) {
                         println("HomeViewModel: Triggering AI Advice (Condition Met)")
                         generateAiAdvice(data)
                     } else {
                         // Even if we don't regen, we might rely on cached advice
                     }
                }
        }
    }
    
    private fun shouldTriggerAi(newData: HealthData): Boolean {
        val session = com.lea.stamp.data.gemini.AiAdviceSession
        val lastData = session.lastContextData
        
        // 1. First Run?
        if (lastData == null) {
            println("[WeightDebug] First run - No previous data")
            return true
        }
        
        // 2. Data Changed?
        // Use Factory to generate metrics for comparison
        val currentMetrics = generateKeyMetrics(newData)
        val lastMetrics = generateKeyMetrics(lastData)
        
        val weightChanged = newData.currentWeight != lastData.currentWeight
        val mealsChanged = newData.dailyMeals.size != lastData.dailyMeals.size
        val metricsChanged = currentMetrics != lastMetrics
        
        if (weightChanged) {
            println("[WeightDebug] Weight changed from ${lastData.currentWeight} to ${newData.currentWeight}")
        }
        
        if (mealsChanged) {
            println("[WeightDebug] Meals count changed from ${lastData.dailyMeals.size} to ${newData.dailyMeals.size}")
        }
        
        if (metricsChanged) {
            println("[WeightDebug] Key Metrics changed")
            // Optional: Print diff if needed, but might be too verbose
        }
        
        val dataChanged = metricsChanged || mealsChanged || weightChanged
        
        if (dataChanged) {
            println("HomeViewModel: API Trigger - Data Changed (Metrics or Meals/Weight)")
            return true
        }

        // 3. Time Phase Changed? (For Meal Suggestions)
        val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
        val currentHour = kotlinx.datetime.Instant.fromEpochMilliseconds(now)
            .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).hour
            
        val lastGenTime = session.lastGenerationTime
        val lastGenHour = kotlinx.datetime.Instant.fromEpochMilliseconds(lastGenTime)
            .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).hour
            
        val currentPhase = session.getPhase(currentHour)
        val lastPhase = session.getPhase(lastGenHour)
        
        if (currentPhase != lastPhase) {
             println("[WeightDebug] Time Phase Changed ($lastPhase -> $currentPhase)")
             println("HomeViewModel: API Trigger - Time Phase Changed ($lastPhase -> $currentPhase)")
             return true
        }
        
        return false
    }

    private fun generateAiAdvice(data: HealthData) {
        println("HomeViewModel: generateAiAdvice called for Goal: ${data.primaryGoal}")
        
        // CRITICAL FIX: Update Session Cache SYNCHRONOUSLY to prevent race conditions
        // If we wait for launch, a rapid 2nd emit from repository could trigger another advice gen
        com.lea.stamp.data.gemini.AiAdviceSession.lastContextData = data
        com.lea.stamp.data.gemini.AiAdviceSession.lastGenerationTime = kotlin.time.Clock.System.now().toEpochMilliseconds()
        
        viewModelScope.launch {
            println("HomeViewModel: Starting API call coroutines")
            try {
                _isAiLoading.value = true
                val apiKey = preferencesManager.getGeminiApiKey().value
                
                if (apiKey.isNullOrBlank()) {
                   println("HomeViewModel: API Key not configured in settings, skipping AI advice")
                   _isAiLoading.value = false
                   return@launch
                }

                val keyMetrics = generateKeyMetrics(data)

                val adviceJob = launch {
                    when (val result = aiCoachService.getAdvice(data, keyMetrics, apiKey)) {
                        is GeminiResult.Success -> {
                            repository.updateAdvice(result.data)
                            println("HomeViewModel: Advice updated: ${result.data.take(20)}...")
                        }
                        is GeminiResult.Error -> {
                             println("HomeViewModel: Failed to generate advice: ${result.message}")
                        }
                    }
                }

                val mealJob = launch {
                    when (val result = aiCoachService.getMealSuggestion(data, keyMetrics, apiKey)) {
                        is GeminiResult.Success -> {
                            repository.updateMealSuggestion(result.data)
                            println("HomeViewModel: Meal Suggestion updated: ${result.data.take(20)}...")
                        }
                        is GeminiResult.Error -> {
                             println("HomeViewModel: Failed to generate meal suggestion: ${result.message}")
                        }
                    }
                }
                
                // Wait for both to likely complete for loading state
                joinAll(adviceJob, mealJob)

            } catch (e: Exception) {
                println("HomeViewModel: Error generating advice: ${e.message}")
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    private fun determineMealType(): String {
        val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
        val currentHour = kotlinx.datetime.Instant.fromEpochMilliseconds(now)
            .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).hour
        
        val phase = com.lea.stamp.data.gemini.AiAdviceSession.getPhase(currentHour)
        return when (phase) {
            com.lea.stamp.data.gemini.AiAdviceSession.DayPhase.MORNING -> "Breakfast"
            com.lea.stamp.data.gemini.AiAdviceSession.DayPhase.LUNCH -> "Lunch"
            else -> "Dinner"
        }
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.AddWater -> {
                repository.addWater(event.amount)
            }
            is HomeEvent.UpdateWeight -> {
                repository.updateWeight(event.weight)
            }
        }
    }
}
