package com.lea.stamp.data

import kotlinx.serialization.Serializable

@Serializable
data class SegmentationMask(
    val box_2d: List<Int>,
    val mask: String,
    val label: String
) {
    /**
     * Extracts pure base64 data from mask field.
     * Handles both raw base64 and data URI format (data:image/png;base64,...)
     */
    fun getBase64Data(): String {
        return if (mask.startsWith("data:")) {
            // Extract base64 part after the comma
            mask.substringAfter(",", mask)
        } else {
            mask
        }
    }
    
    /**
     * Get denormalized pixel coordinates from normalized box_2d values (0-1000 scale)
     * @param imageWidth The actual image width in pixels
     * @param imageHeight The actual image height in pixels
     * @return Array of [pixelY0, pixelX0, pixelY1, pixelX1]
     */
    fun getDenormalizedBox(imageWidth: Int, imageHeight: Int): IntArray {
        val y0 = (box_2d[0] * imageHeight) / 1000
        val x0 = (box_2d[1] * imageWidth) / 1000
        val y1 = (box_2d[2] * imageHeight) / 1000
        val x1 = (box_2d[3] * imageWidth) / 1000
        return intArrayOf(y0, x0, y1, x1)
    }
    
    /**
     * Get bounding box dimensions in pixels
     * @param imageWidth The actual image width in pixels
     * @param imageHeight The actual image height in pixels
     * @return Pair of (boxWidth, boxHeight)
     */
    fun getBoxDimensions(imageWidth: Int, imageHeight: Int): Pair<Int, Int> {
        val denormalized = getDenormalizedBox(imageWidth, imageHeight)
        val boxWidth = denormalized[3] - denormalized[1]  // x1 - x0
        val boxHeight = denormalized[0] - denormalized[2]  // y1 - y0 (note: might be negative)
        return Pair(boxWidth, kotlin.math.abs(boxHeight))
    }
}

@Serializable
data class SegmentationResponse(
    val detections: List<SegmentationMask>? = null
)

@Serializable
data class FoodMetrics(
    val calories: Int,
    val protein: Double,
    val fiber: Double,
    val addedSugar: Double,
    val saturatedFat: Double,
    val sodium: Double,
    val vegetableContent: Double,
    val water: Double,
    val processingLevel: Int,
    val isHealthy: Boolean
)

data class FoodItem(
    val label: String,
    val box_2d: List<Int>,
    val mask: String,
    val metrics: FoodMetrics?,
    val isHealthy: Boolean
)

data class MealAnalysis(
    val foodItems: List<FoodItem>,
    val totalCalories: Int,
    val totalProtein: Double,
    val totalFiber: Double,
    val totalAddedSugar: Double,
    val totalSaturatedFat: Double,
    val totalSodium: Double,
    val totalVegetableContent: Double,
    val totalWater: Double,
    val averageProcessingLevel: Int,
    val isHealthy: Boolean,
    val aiComment: String
) {
    companion object {
        fun fromFoodItems(foodItems: List<FoodItem>, aiComment: String = ""): MealAnalysis {
            val totalCalories = foodItems.sumOf { it.metrics?.calories ?: 0 }
            val totalProtein = foodItems.sumOf { it.metrics?.protein ?: 0.0 }
            val totalFiber = foodItems.sumOf { it.metrics?.fiber ?: 0.0 }
            val totalAddedSugar = foodItems.sumOf { it.metrics?.addedSugar ?: 0.0 }
            val totalSaturatedFat = foodItems.sumOf { it.metrics?.saturatedFat ?: 0.0 }
            val totalSodium = foodItems.sumOf { it.metrics?.sodium ?: 0.0 }
            val totalVegetableContent = foodItems.sumOf { it.metrics?.vegetableContent ?: 0.0 }
            val totalWater = foodItems.sumOf { it.metrics?.water ?: 0.0 }
            val averageProcessingLevel = if (foodItems.isNotEmpty()) {
                foodItems.sumOf { it.metrics?.processingLevel ?: 0 } / foodItems.size
            } else 0
            val healthyCount = foodItems.count { it.isHealthy }
            val isHealthy = foodItems.isNotEmpty() && healthyCount >= foodItems.size * 0.67
            
            return MealAnalysis(
                foodItems = foodItems,
                totalCalories = totalCalories,
                totalProtein = totalProtein,
                totalFiber = totalFiber,
                totalAddedSugar = totalAddedSugar,
                totalSaturatedFat = totalSaturatedFat,
                totalSodium = totalSodium,
                totalVegetableContent = totalVegetableContent,
                totalWater = totalWater,
                averageProcessingLevel = averageProcessingLevel,
                isHealthy = isHealthy,
                aiComment = aiComment
            )
        }
    }
}
