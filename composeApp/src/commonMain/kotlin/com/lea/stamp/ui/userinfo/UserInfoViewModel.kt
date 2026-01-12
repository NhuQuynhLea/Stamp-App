package com.lea.stamp.ui.userinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lea.stamp.data.ActivityLevel
import com.lea.stamp.data.HealthRepository
import com.lea.stamp.data.RepositoryProvider
import com.lea.stamp.data.gemini.GeminiClient
import com.lea.stamp.data.gemini.GeminiClientProvider
import com.lea.stamp.data.gemini.GeminiResult
import com.lea.stamp.data.Strings
import kotlinx.serialization.json.Json
import com.lea.stamp.data.DailyGoalPlan
import com.lea.stamp.data.GoalMetric
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import com.lea.stamp.data.PreferencesProvider
import com.lea.stamp.data.PreferencesManager

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UserInfoUiState(
    val age: String = "",
    val sex: String = "Male",
    val height: String = "",
    val weight: String = "",
    val activityLevel: ActivityLevel = ActivityLevel.LIGHTLY_ACTIVE,
    val healthConditions: Set<String> = emptySet(),
    val dietaryPattern: String = "No Restriction",
    val medications: String = "",
    val primaryGoal: String = "",
    val goalIntensity: String = "",
    val secondaryGoals: Set<String> = emptySet(),
    val isSaved: Boolean = false,
    val isLoading: Boolean = true,
    val isEditing: Boolean = true, // Default to true until loaded
    val validationError: String? = null,
    val showGoalDialog: Boolean = false,
    val generatedPlan: DailyGoalPlan = DailyGoalPlan(),
    val rawGoalContent: String = "" // For fallback or storage
)


sealed interface UserInfoEvent {
    data class UpdateAge(val age: String) : UserInfoEvent
    data class UpdateSex(val sex: String) : UserInfoEvent
    data class UpdateHeight(val height: String) : UserInfoEvent
    data class UpdateWeight(val weight: String) : UserInfoEvent
    data class UpdateActivityLevel(val level: ActivityLevel) : UserInfoEvent
    data class ToggleHealthCondition(val condition: String) : UserInfoEvent
    data class UpdateDietaryPattern(val pattern: String) : UserInfoEvent
    data class UpdateMedications(val medications: String) : UserInfoEvent
    data class UpdatePrimaryGoal(val goal: String) : UserInfoEvent
    data class UpdateGoalIntensity(val intensity: String) : UserInfoEvent
    data class ToggleSecondaryGoal(val goal: String) : UserInfoEvent
    object Save : UserInfoEvent
    object ToggleEditMode : UserInfoEvent
    object ClearError : UserInfoEvent
    object GenerateDailyTarget : UserInfoEvent
    data class UpdateGeneratedPlanTitle(val title: String) : UserInfoEvent
    data class UpdateGeneratedPlanDescription(val desc: String) : UserInfoEvent
    data class UpdateGeneratedPlanMetric(val index: Int, val value: String) : UserInfoEvent
    object SaveGeneratedGoal : UserInfoEvent
    object DismissGoalDialog : UserInfoEvent
}

class UserInfoViewModel(
    private val repository: HealthRepository = RepositoryProvider.instance,
    private val geminiClient: GeminiClient = GeminiClientProvider.instance,
    private val preferencesManager: PreferencesManager = PreferencesProvider.instance
) : ViewModel() {

    // Internal mutable state for the form
    private val _formState = MutableStateFlow(UserInfoUiState(isLoading = true))
    
    // We observe repository data to initialize the form, but only once or if explicit sync is needed.
    // Actually, distinct form state is needed to allow editing without saving immediately.
    // So we initialize _formState from repository once.
    
    init {
        viewModelScope.launch {
            repository.getHealthData().collect { data ->
                // Only update if we are loading, to avoid overwriting user edits if repo updates from elsewhere
                // Or better, we only take initial value.
                if (_formState.value.isLoading) {
                    val hasData = data.age > 0 && data.height > 0f // Simple heuristic if user exists
                    _formState.update {
                        it.copy(
                            age = if (data.age == 0) "" else data.age.toString(),
                            sex = if (data.sex.isNotEmpty()) data.sex else "Male",
                            height = if (data.height == 0f) "" else data.height.toString(),
                            weight = data.currentWeight.toString(),
                            activityLevel = data.activityLevel,
                            healthConditions = data.healthConditions,
                            dietaryPattern = data.dietaryPattern,
                            medications = data.medications,
                            primaryGoal = data.primaryGoal,
                            goalIntensity = data.goalIntensity,
                            secondaryGoals = data.secondaryGoals,
                            // Ideally parse detailGoal if it was JSON, for now clear or leave empty to regen
                            generatedPlan = try {
                                if (data.detailGoal.isNotBlank()) Json.decodeFromString(data.detailGoal) else DailyGoalPlan()
                            } catch (e: Exception) { DailyGoalPlan() },
                            isLoading = false,
                            isEditing = !hasData // Edit mode if no data, View mode if data exists
                        )
                    }
                }
            }
        }
    }

    val uiState: StateFlow<UserInfoUiState> = _formState.asStateFlow()

    fun onEvent(event: UserInfoEvent) {
        when (event) {
            is UserInfoEvent.UpdateAge -> _formState.update { it.copy(age = event.age, validationError = null) }
            is UserInfoEvent.UpdateSex -> _formState.update { it.copy(sex = event.sex) }
            is UserInfoEvent.UpdateHeight -> _formState.update { it.copy(height = event.height, validationError = null) }
            is UserInfoEvent.UpdateWeight -> _formState.update { it.copy(weight = event.weight, validationError = null) }
            is UserInfoEvent.UpdateActivityLevel -> _formState.update { it.copy(activityLevel = event.level) }
            is UserInfoEvent.ToggleHealthCondition -> _formState.update {
                val current = it.healthConditions
                val newconds = if (current.contains(event.condition)) current - event.condition else current + event.condition
                it.copy(healthConditions = newconds)
            }
            is UserInfoEvent.UpdateDietaryPattern -> _formState.update { it.copy(dietaryPattern = event.pattern) }
            is UserInfoEvent.UpdateMedications -> _formState.update { it.copy(medications = event.medications) }
            is UserInfoEvent.UpdatePrimaryGoal -> _formState.update { it.copy(primaryGoal = event.goal, validationError = null) }
            is UserInfoEvent.UpdateGoalIntensity -> _formState.update { it.copy(goalIntensity = event.intensity) }
            is UserInfoEvent.ToggleSecondaryGoal -> _formState.update {
                val current = it.secondaryGoals
                // Max 2 goals
                val newGoals = if (current.contains(event.goal)) {
                    current - event.goal
                } else {
                    if (current.size < 2) current + event.goal else current
                }
                it.copy(secondaryGoals = newGoals)
            }
            is UserInfoEvent.Save -> triggerGeneration()
            is UserInfoEvent.ToggleEditMode -> _formState.update { it.copy(isEditing = !it.isEditing) }
            is UserInfoEvent.ClearError -> _formState.update { it.copy(validationError = null) }
            is UserInfoEvent.GenerateDailyTarget -> generateDailyTarget()
            is UserInfoEvent.UpdateGeneratedPlanTitle -> _formState.update { 
                it.copy(generatedPlan = it.generatedPlan.copy(title = event.title)) 
            }
            is UserInfoEvent.UpdateGeneratedPlanDescription -> _formState.update { 
                 it.copy(generatedPlan = it.generatedPlan.copy(description = event.desc)) 
            }
            is UserInfoEvent.UpdateGeneratedPlanMetric -> _formState.update {
                val newMetrics = it.generatedPlan.metrics.toMutableList()
                if (event.index in newMetrics.indices) {
                    newMetrics[event.index] = newMetrics[event.index].copy(value = event.value)
                }
                it.copy(generatedPlan = it.generatedPlan.copy(metrics = newMetrics))
            }
            is UserInfoEvent.SaveGeneratedGoal -> save() // Now this actually saves to DB
            is UserInfoEvent.DismissGoalDialog -> _formState.update { it.copy(showGoalDialog = false) }
        }
    }

    private fun triggerGeneration() {
        val state = _formState.value
        
        // Validation
        val age = state.age.toIntOrNull()
        val height = state.height.toFloatOrNull()
        val weight = state.weight.toFloatOrNull()
        
        if (age == null || age <= 0) {
            _formState.update { it.copy(validationError = "Please enter a valid age") }
            return
        }
        if (height == null || height <= 0) {
            _formState.update { it.copy(validationError = "Please enter a valid height") }
            return
        }
        if (weight == null || weight <= 0) {
            _formState.update { it.copy(validationError = "Please enter a valid weight") }
            return
        }
        if (state.primaryGoal.isBlank()) {
             _formState.update { it.copy(validationError = "Please select a primary goal") }
             return
        }

        // Proceed to generate
        generateDailyTarget()
    }

    private fun save() {
        val state = _formState.value
        // Actually save to DB now
         val jsonString = try {
            Json.encodeToString(DailyGoalPlan.serializer(), state.generatedPlan)
        } catch (e: Exception) { "" }
        
        repository.updateUserProfile(
            age = state.age.toInt(),
            sex = state.sex,
            height = state.height.toFloat(),
            weight = state.weight.toFloat(),
            activityLevel = state.activityLevel,
            healthConditions = state.healthConditions,
            dietaryPattern = state.dietaryPattern,
            medications = state.medications,
            primaryGoal = state.primaryGoal,
            goalIntensity = state.goalIntensity,
            secondaryGoals = state.secondaryGoals,
            detailGoal = jsonString
        )
        // Disable edit mode on success
        _formState.update { it.copy(isSaved = true, isEditing = false, validationError = null, showGoalDialog = false) }
    }

    private fun generateDailyTarget() {
        val state = _formState.value
        // Don't regenerate if we already have one unless forced? For now, generate if empty or just overwrite.
        // Actually, user might want to refresh. Let's do it on Save.
        
        viewModelScope.launch {
            _formState.update { it.copy(isLoading = true) }
            
            // Determine metrics based on goal
            val metricsMap = mapOf(
                "Weight loss" to listOf("Calories", "Protein", "Fiber"),
                "Maintain weight" to listOf("Calories", "Protein", "Fiber"),
                "Muscle gain" to listOf("Protein", "Calories", "Meal Timing"),
                "Improve energy" to listOf("Carb Quality", "Hydration", "Water"),
                "Blood sugar control" to listOf("Added Sugar", "Fiber", "Meal Timing"),
                "Heart health" to listOf("Sodium", "Saturated Fat", "Fiber"),
                "Gut health" to listOf("Calories", "Water", "Protein") // From README "Gut Health / Other"
            )
            
            val targetMetrics = metricsMap[state.primaryGoal] ?: listOf("Calories", "Protein", "Fiber")
            
            // Format for prompt injection
            val metricsJsonMock = targetMetrics.joinToString(",\n") { label ->
                """{ "label": "$label", "value": "Target Value", "rationale": "Brief reason" }"""
            }

            val prompt = Strings.GEMINI_DAILY_TARGET_PROMPT
                .replace("{{SEX}}", state.sex)
                .replace("{{AGE}}", state.age)
                .replace("{{HEIGHT}}", state.height)
                .replace("{{WEIGHT}}", state.weight)
                .replace("{{ACTIVITY_LEVEL}}", state.activityLevel.label)
                .replace("{{PRIMARY_GOAL}}", state.primaryGoal)
                .replace("{{INTENSITY}}", state.goalIntensity)
                .replace("{{SECONDARY_FOCUS}}", state.secondaryGoals.joinToString(", "))
                .replace("{{HEALTH_CONDITIONS}}", state.healthConditions.joinToString(", "))
                .replace("{{DIETARY_PATTERN}}", state.dietaryPattern)
                .replace("{{MEDICATIONS}}", state.medications)
                .replace("{{DYNAMIC_METRICS}}", metricsJsonMock)
                .replace("{{INSTRUCTIONS}}", "CRITICAL: For the 'value' field in metrics, you MUST provide a specific numeric target with units based on the user's profile. \n" +
                        "Examples: '150g', '2000 kcal', '2.5 L', 'Before 9am', 'Low'. \n" +
                        "Do NOT use generic text like 'Target Value'. Calculate or estimate a distinct number.")
            
            // Retrieve API Key from Preferences
            val apiKey = preferencesManager.getGeminiApiKey().firstOrNull()
            
            if (apiKey.isNullOrBlank()) {
                 _formState.update { 
                    it.copy(
                        isLoading = false,
                        validationError = "Gemini API Key missing. Please set it in Journey settings." 
                    ) 
                }
                return@launch
            }
            
            val result = geminiClient.generateContent(
                prompt = prompt,
                apiKey = apiKey,
                model = GeminiClient.DEFAULT_MODEL
            )
            
            
            when (result) {
                is GeminiResult.Success -> {
                    try {
                        val cleanedJson = result.data.trim().removePrefix("```json").removeSuffix("```").trim()
                        val plan = Json.decodeFromString<DailyGoalPlan>(cleanedJson)
                        _formState.update { 
                            it.copy(
                                isLoading = false, 
                                showGoalDialog = true, 
                                generatedPlan = plan
                            ) 
                        }
                    } catch (e: Exception) {
                        // Fallback manual parse if simple JSON fails or structure mismatch?
                        // For now just error or try to salvage.
                         _formState.update { 
                            it.copy(
                                isLoading = false,
                                validationError = "Failed to parse goal format. Please try again." 
                            ) 
                        }
                    }
                }
                is GeminiResult.Error -> {
                    _formState.update { 
                        it.copy(
                            isLoading = false,
                            validationError = "Failed to generate goal: ${result.message}" 
                        ) 
                    }
                }
            }
        }
    }
}

