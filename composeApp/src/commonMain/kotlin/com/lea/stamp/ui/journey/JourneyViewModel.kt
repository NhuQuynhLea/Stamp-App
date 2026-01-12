package com.lea.stamp.ui.journey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lea.stamp.data.HealthRepository
import com.lea.stamp.data.HealthRepositoryImpl
import com.lea.stamp.data.Meal
import com.lea.stamp.data.RepositoryProvider
import com.lea.stamp.data.DayStatus
import com.lea.stamp.data.DateUtils
import com.lea.stamp.data.MealType
import com.lea.stamp.data.toMeal
import com.lea.stamp.data.PreferencesManager
import com.lea.stamp.data.PreferencesProvider
import kotlinx.datetime.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.time.Clock

data class DayMeals(
    val dateLabel: String,
    val date: Long,
    val meals: List<Meal>,
    val status: DayStatus?
)

data class JourneyUiState(
    val dayMealsList: List<DayMeals> = emptyList(),
    val isLoading: Boolean = false,
    val hasMore: Boolean = true,
    val showSettings: Boolean = false,
    val geminiApiKey: String = "",
    val selectedDate: Long? = null,
    val canLoadPrevious: Boolean = false
)

data class UiControlState(
    val showSettings: Boolean,
    val apiKey: String,
    val selectedDate: Long?,
    val offset: Int,
    val isLoading: Boolean
)

class JourneyViewModel(
    private val repository: HealthRepository = RepositoryProvider.instance,
    private val preferencesManager: PreferencesManager = PreferencesProvider.instance
) : ViewModel() {

    private val _daysToLoad = MutableStateFlow(10)
    private val _showSettings = MutableStateFlow(false)
    private val _apiKeyInput = MutableStateFlow("")
    private val _selectedDate = MutableStateFlow<Long?>(null)
    private val _currentOffset = MutableStateFlow(0)
    private val _isLoading = MutableStateFlow(false)
    
    init {
        viewModelScope.launch {
            preferencesManager.getGeminiApiKey().collect { apiKey ->
                _apiKeyInput.value = apiKey ?: ""
            }
        }
    }
    
    private val _baseUiState = combine(_daysToLoad, _currentOffset) { daysCount, offset ->
        _isLoading.value = true
        
        val today = Clock.System.now()
        val timeZone = TimeZone.currentSystemDefault()
        
        val totalNeeded = offset + daysCount
        for (i in 0 until totalNeeded) {
            val targetDate = today.minus(i, DateTimeUnit.DAY, timeZone)
            val dayStartTimestamp = DateUtils.getStartOfDay(targetDate.toEpochMilliseconds())
            val dayId = DateUtils.generateDayId(dayStartTimestamp)
            repository.getOrCreateDay(dayId, dayStartTimestamp)
        }
        
        repository.getDaysWithMealsPaginated(daysCount, offset)
    }
    .flatMapLatest { it }
    .onEach { _isLoading.value = false }
    .map { daysWithMeals ->
        val database = com.lea.stamp.data.db.DatabaseProvider.getDatabase()
        val foodItemDao = database.foodItemDao()
        val dayMealsList = daysWithMeals.map { dayWithMeals ->
            val meals = dayWithMeals.meals.map { kotlinx.coroutines.runBlocking { it.toMeal(dayWithMeals.day.dateTimestamp, foodItemDao) } }
            val status = repository.getDayStatus(dayWithMeals.day.id)
            
            DayMeals(
                dateLabel = DateUtils.formatDateLabel(dayWithMeals.day.dateTimestamp),
                date = dayWithMeals.day.dateTimestamp,
                meals = meals,
                status = status
            )
        }
        
        JourneyUiState(
            dayMealsList = dayMealsList,
            isLoading = false,
            hasMore = true,
            showSettings = false,
            geminiApiKey = ""
        )
    }
    
    private val _uiControlState = combine(
        _showSettings,
        _apiKeyInput,
        _selectedDate,
        _currentOffset,
        _isLoading
    ) { showSettings, apiKey, selectedDate, offset, isLoading ->
        UiControlState(showSettings, apiKey, selectedDate, offset, isLoading)
    }

    val uiState: StateFlow<JourneyUiState> = combine(
        _baseUiState,
        _uiControlState
    ) { base, control ->
        base.copy(
            showSettings = control.showSettings,
            geminiApiKey = control.apiKey,
            selectedDate = control.selectedDate,
            canLoadPrevious = control.offset > 0,
            isLoading = control.isLoading
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = JourneyUiState(isLoading = true)
    )
    
    fun loadMoreMeals() {
        _daysToLoad.update { it + 10 }
    }
    
    fun loadPreviousMeals() {
        val currentOffset = _currentOffset.value
        if (currentOffset > 0) {
            val delta = minOf(currentOffset, 10)
            _currentOffset.update { it - delta }
            _daysToLoad.update { it + delta }
        }
    }
    
    fun toggleSettings() {
        _showSettings.update { !it }
    }
    
    fun updateApiKey(apiKey: String) {
        _apiKeyInput.value = apiKey
    }
    
    fun saveApiKey() {
        viewModelScope.launch {
            preferencesManager.setGeminiApiKey(_apiKeyInput.value)
            _showSettings.value = false
        }
    }
    
    fun selectDate(date: Long?) {
        viewModelScope.launch {
            if (date != null) {
                val normalizedDate = DateUtils.getStartOfDay(date)
                val newerDays = repository.getDaysCountNewerThan(normalizedDate)
                val buffer = 5
                val newOffset = (newerDays - buffer).coerceAtLeast(0)
                
                _currentOffset.value = newOffset
                _daysToLoad.value = 15
                _selectedDate.value = normalizedDate 
            } else {
                _currentOffset.value = 0
                _selectedDate.value = null
            }
        }
    }
    
    fun clearSelection() {
        _selectedDate.value = null
    }
}
