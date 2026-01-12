package com.lea.stamp.data.gemini

import com.lea.stamp.data.FoodItem
import com.lea.stamp.data.FoodMetrics
import com.lea.stamp.data.MealAnalysis
import com.lea.stamp.data.SegmentationMask
import com.lea.stamp.data.SegmentationResponse
import com.lea.stamp.data.Strings
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

class FoodAnalysisService(
    private val geminiClient: GeminiClient = GeminiClientProvider.instance
) {
    private val json = Json { 
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    suspend fun analyzeFoodImageTwoPhase(
        imageData: ByteArray,
        apiKey: String
    ): GeminiResult<MealAnalysis> {
        return try {
            println("FoodAnalysisService: Starting two-phase analysis")
            
            val segmentationResult = performSegmentation(imageData, apiKey)
            if (segmentationResult is GeminiResult.Error) {
                return segmentationResult
            }
            
            val segmentations = (segmentationResult as GeminiResult.Success).data
            println("FoodAnalysisService: Found ${segmentations.size} food items")
            segmentations.forEachIndexed { index, seg ->
                println("FoodAnalysisService: Food ${index + 1}: ${seg.label} (box: ${seg.box_2d})")
            }
            
            if (segmentations.isEmpty()) {
                return GeminiResult.Error("No food items detected in the image")
            }
            
            val foodItems = analyzeFoodMetrics(segmentations, imageData, apiKey)
            
            val mealAnalysis = MealAnalysis.fromFoodItems(
                foodItems = foodItems,
                aiComment = generateMealComment(foodItems)
            )
            
            println("FoodAnalysisService: Analysis complete - ${foodItems.size} items, ${mealAnalysis.totalCalories} cal")
            GeminiResult.Success(mealAnalysis)
        } catch (e: Exception) {
            println("FoodAnalysisService: Exception: ${e.message}")
            e.printStackTrace()
            GeminiResult.Error("Analysis failed: ${e.message}", e)
        }
    }
    
    private suspend fun performSegmentation(
        imageData: ByteArray,
        apiKey: String
    ): GeminiResult<List<SegmentationMask>> {
        return try {
            println("FoodAnalysisService: Phase 1 - Requesting segmentation")
            
            val result = geminiClient.generateContent(
                prompt = Strings.GEMINI_SEGMENTATION_PROMPT,
                imageData = imageData,
                apiKey = apiKey,
                model = GeminiClient.DEFAULT_MODEL
            )
            
            if (result is GeminiResult.Error) {
                return result
            }
            
            val responseText = (result as GeminiResult.Success).data
            val segmentations = parseSegmentationResponse(responseText)
            
            GeminiResult.Success(segmentations)
        } catch (e: Exception) {
            println("FoodAnalysisService: Segmentation error: ${e.message}")
            GeminiResult.Error("Segmentation failed: ${e.message}", e)
        }
    }
    
    private suspend fun analyzeFoodMetrics(
        segmentations: List<SegmentationMask>,
        imageData: ByteArray,
        apiKey: String
    ): List<FoodItem> {
        println("FoodAnalysisService: Phase 2 - Batch analyzing metrics for ${segmentations.size} items")
        
        val metricsMap = getBatchFoodMetrics(segmentations.map { it.label }, imageData, apiKey)
        
        return segmentations.map { segmentation ->
            val metrics = metricsMap[segmentation.label]
            FoodItem(
                label = segmentation.label,
                box_2d = segmentation.box_2d,
                mask = segmentation.mask,
                metrics = metrics,
                isHealthy = metrics?.isHealthy ?: false
            )
        }
    }
    
    private suspend fun getBatchFoodMetrics(
        foodLabels: List<String>,
        imageData: ByteArray,
        apiKey: String
    ): Map<String, FoodMetrics?> {
        return try {
            val foodListText = foodLabels.joinToString(", ")
            println("FoodAnalysisService: Requesting batch metrics for: $foodListText")
            val prompt = Strings.GEMINI_BATCH_FOOD_METRICS_PROMPT.replace("{{FOOD_LIST}}", foodListText)
            
            val result = geminiClient.generateContent(
                prompt = prompt,
                imageData = imageData,
                apiKey = apiKey,
                model = GeminiClient.DEFAULT_MODEL
            )
            
            if (result is GeminiResult.Success) {
                parseBatchFoodMetricsResponse(result.data, foodLabels)
            } else {
                println("FoodAnalysisService: Failed to get batch metrics")
                foodLabels.associateWith { null }
            }
        } catch (e: Exception) {
            println("FoodAnalysisService: Batch metrics error: ${e.message}")
            foodLabels.associateWith { null }
        }
    }
    
    private fun parseSegmentationResponse(responseText: String): List<SegmentationMask> {
        try {
            val trimmed = responseText.trim()
            println("FoodAnalysisService: Parsing segmentation JSON (first 200 chars): ${trimmed.take(200)}")
            
            val masks = json.decodeFromString<List<SegmentationMask>>(trimmed)
            println("FoodAnalysisService: Parsed ${masks.size} segmentation masks")
            return masks
        } catch (e: Exception) {
            println("FoodAnalysisService: Segmentation parsing error: ${e.message}")
            throw e
        }
    }
    
    private fun parseBatchFoodMetricsResponse(responseText: String, foodLabels: List<String>): Map<String, FoodMetrics?> {
        try {
            val trimmed = responseText.trim()
            println("FoodAnalysisService: Parsing batch metrics JSON (first 300 chars): ${trimmed.take(300)}")
            
            // Expected format: {"foodName1": {...metrics...}, "foodName2": {...metrics...}}
            val metricsMap = json.decodeFromString<Map<String, FoodMetrics>>(trimmed)
            println("FoodAnalysisService: Parsed metrics for ${metricsMap.size} foods")
            
            // Map back to original labels (case-insensitive matching)
            val mappedMetrics = foodLabels.associateWith { label ->
                val matched = metricsMap.entries.find { it.key.equals(label, ignoreCase = true) }?.value
                println("FoodAnalysisService: Mapping '${label}' -> ${if (matched != null) "found" else "NOT FOUND"}")
                matched
            }
            println("FoodAnalysisService: Successfully mapped ${mappedMetrics.count { it.value != null }}/${foodLabels.size} foods")
            return mappedMetrics
        } catch (e: Exception) {
            println("FoodAnalysisService: Standard map parsing failed: ${e.message}. Trying list fallback...")
            return parseBatchFoodMetricsResponseAsList(responseText, foodLabels)
        }
    }
    
    private fun parseBatchFoodMetricsResponseAsList(responseText: String, foodLabels: List<String>): Map<String, FoodMetrics?> {
        try {
            val trimmed = responseText.trim()
            val listResponse = json.decodeFromString<FoodListResponse>(trimmed)
            val items = listResponse.theFoodItems ?: listResponse.items ?: listResponse.foodItems ?: emptyList()
            
            println("FoodAnalysisService: Parsed list fallback with ${items.size} items")
            
            val result = foodLabels.associateWith { label ->
                // Fuzzy match: check if item name contains label or label contains item name
                val matched = items.find { item -> 
                    val itemName = item.name ?: ""
                    itemName.contains(label, ignoreCase = true) || label.contains(itemName, ignoreCase = true)
                }
                
                if (matched != null) {
                    println("FoodAnalysisService: List Mapping '${label}' -> found '${matched.name}'")
                    FoodMetrics(
                        calories = matched.calories ?: 0,
                        protein = matched.protein ?: 0.0,
                        fiber = matched.fiber ?: 0.0,
                        addedSugar = matched.addedSugar ?: 0.0,
                        saturatedFat = matched.saturatedFat ?: 0.0,
                        sodium = matched.sodium ?: 0.0,
                        vegetableContent = matched.vegetableContent ?: 0.0,
                        water = matched.water ?: 0.0,
                        processingLevel = matched.processingLevel ?: 0,
                        isHealthy = matched.isHealthy ?: false
                    )
                } else {
                    println("FoodAnalysisService: List Mapping '${label}' -> NOT FOUND")
                    null
                }
            }
            return result
        } catch (e: Exception) {
             println("FoodAnalysisService: Batch metrics parsing error (both map and list failed): ${e.message}")
             return foodLabels.associateWith { null }
        }
    }

@Serializable
private data class FoodItemResponse(
    val name: String? = null,
    val calories: Int? = null,
    val protein: Double? = null,
    val fiber: Double? = null,
    val addedSugar: Double? = null,
    val saturatedFat: Double? = null,
    val sodium: Double? = null,
    val vegetableContent: Double? = null,
    val water: Double? = null,
    val processingLevel: Int? = null,
    val isHealthy: Boolean? = null
)

@Serializable
private data class FoodListResponse(
    @SerialName("the food items") val theFoodItems: List<FoodItemResponse>? = null,
    val items: List<FoodItemResponse>? = null,
    @SerialName("food_items") val foodItems: List<FoodItemResponse>? = null
)
    
    private fun generateMealComment(foodItems: List<FoodItem>): String {
        val healthyCount = foodItems.count { it.isHealthy }
        val totalCalories = foodItems.sumOf { it.metrics?.calories ?: 0 }
        val totalProtein = foodItems.sumOf { it.metrics?.protein ?: 0.0 }
        
        return when {
            healthyCount == foodItems.size -> 
                "Excellent meal choice! All items are nutritious and well-balanced."
            healthyCount >= foodItems.size * 0.67 -> 
                "Good meal overall with $healthyCount out of ${foodItems.size} healthy items. Total: ${totalCalories}cal, ${totalProtein}g protein."
            else -> 
                "This meal could be improved. Consider adding more vegetables and reducing processed foods. Total: ${totalCalories}cal."
        }
    }
}

object FoodAnalysisServiceProvider {
    val instance: FoodAnalysisService = FoodAnalysisService()
}
