package com.lea.stamp.data.gemini

import com.lea.stamp.data.Strings
import com.lea.stamp.data.HealthData
import com.lea.stamp.data.MealType
import com.lea.stamp.ui.home.MetricUiModel
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class AICoachService(
    private val geminiClient: GeminiClient = GeminiClientProvider.instance
) {
    suspend fun getAdvice(
        data: HealthData,
        keyMetrics: List<MetricUiModel>,
        apiKey: String
    ): GeminiResult<String> {
        return try {
            val contextString = buildContext(data, keyMetrics)
            println("AICoachService: Getting advice...")
            
            val finalPrompt = Strings.GEMINI_ADVICE_PROMPT.replace("%1\$s", contextString)

            val result = geminiClient.generateContent(
                prompt = finalPrompt,
                apiKey = apiKey,
                model = GeminiClient.DEFAULT_MODEL
            )
            
            if (result is GeminiResult.Success) {
                println("AICoachService: Advice Response: ${result.data}")
                GeminiResult.Success(result.data.trim())
            } else {
                 GeminiResult.Error("Failed to generate advice", null)
            }
        } catch (e: Exception) {
            println("AICoachService: Error generating advice: ${e.message}")
            GeminiResult.Error("Error generating advice: ${e.message}", e)
        }
    }

    suspend fun getMealSuggestion(
        data: HealthData,
        keyMetrics: List<MetricUiModel>,
        apiKey: String
    ): GeminiResult<String> {
        return try {
            val contextString = buildContext(data, keyMetrics)
            println("AICoachService: Getting meal suggestion...")
            
            val finalPrompt = Strings.GEMINI_MEAL_SUGGESTION_PROMPT.replace("%1\$s", contextString)

            val result = geminiClient.generateContent(
                prompt = finalPrompt,
                apiKey = apiKey,
                model = GeminiClient.DEFAULT_MODEL
            )
            
            if (result is GeminiResult.Success) {
                println("AICoachService: Meal Suggestion Response: ${result.data}")
                GeminiResult.Success(cleanResponse(result.data))
            } else {
                 GeminiResult.Error("Failed to generate meal suggestion", null)
            }
        } catch (e: Exception) {
            println("AICoachService: Error generating meal suggestion: ${e.message}")
            GeminiResult.Error("Error generating meal suggestion: ${e.message}", e)
        }
    }

    private fun cleanResponse(input: String): String {
        var text = input.trim()
        
        // Remove wrapping quotes if present (common if model outputs JSON string value)
        if (text.startsWith("\"") && text.endsWith("\"")) {
            text = text.removeSurrounding("\"")
        }
        
        // Unescape common sequences
        text = text.replace("\\n", "\n")
            .replace("\\\"", "\"")
            .replace("\\u003c", "<")
            .replace("\\u003e", ">")
            .replace("\\u0026", "&")
            .replace("<b>", "") // Remove HTML bold
            .replace("</b>", "")
            .replace("<br>", "\n") // Replace breaks with newlines
            .replace("* **", "• ") // Fix markdown bold list
            .replace("**", "") // Remove generic bolding
            
        return text
    }

    private fun buildContext(data: HealthData, keyMetrics: List<MetricUiModel>): String {
        val sb = StringBuilder()
        
        // 1. Comparison of Key Metrics
        sb.appendLine("--- Key Metrics Status ---")
        if (keyMetrics.isNotEmpty()) {
            keyMetrics.forEach { metric ->
                 sb.appendLine("${metric.title}: ${metric.current.toInt()} / ${metric.target.toInt()}${metric.unit} (Status: ${metric.status})")
            }
        } else {
             // Fallback if no specific key metrics
             sb.appendLine("Calories: ${data.currentCalories} / ${data.targetCalories} (Target)")
             sb.appendLine("Water: ${data.waterIntakeLiters}L / ${data.waterTargetLiters}L (Target)")
        }
        
        // 2. Meal Completeness & Timing
        sb.appendLine("\n--- Meals Captured ---")
        val meals = data.dailyMeals
        if (meals.isEmpty()) {
            sb.appendLine("No meals captured yet today.")
        } else {
            meals.sortedBy { it.date }.forEach { meal ->
                val time = Instant.fromEpochMilliseconds(meal.date)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                val timeString = "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
                sb.appendLine("- ${meal.type.name} at $timeString : ${meal.calories ?: 0} kcal")
            }
        }
        
        // 3. User Goal context
        sb.appendLine("\n--- User Context ---")
        sb.appendLine("Goal: ${data.primaryGoal}")
        
        // 4. Current Time
        val now = Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
        val timeString = "${now.hour.toString().padStart(2, '0')}:${now.minute.toString().padStart(2, '0')}"
        sb.appendLine("\nCurrent Time: $timeString")

        return sb.toString()
    }
}

object AICoachServiceProvider {
    val instance: AICoachService = AICoachService()
}
