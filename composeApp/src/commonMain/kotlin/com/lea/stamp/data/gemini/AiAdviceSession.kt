package com.lea.stamp.data.gemini

import com.lea.stamp.data.HealthData

object AiAdviceSession {
    var lastContextData: HealthData? = null
    var lastGenerationTime: Long = 0
    
    // Time blocks for meal suggestions
    enum class DayPhase { MORNING, LUNCH, AFTERNOON, EVENING, LATE, UNKNOWN }
    
    fun getPhase(hour: Int): DayPhase {
        return when (hour) {
            in 4..10 -> DayPhase.MORNING
            in 11..13 -> DayPhase.LUNCH
            in 14..16 -> DayPhase.AFTERNOON
            in 17..20 -> DayPhase.EVENING
            else -> DayPhase.LATE
        }
    }
}
