package com.lea.stamp.data

import com.lea.stamp.data.db.DatabaseProvider
import com.lea.stamp.data.db.MealEntity
import com.lea.stamp.data.db.DayEntity
import com.lea.stamp.data.db.DayWithMeals
import com.lea.stamp.data.db.FoodItemEntity
import com.lea.stamp.data.db.UserInfoEntity
import com.lea.stamp.data.db.WeightHistoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.firstOrNull

data class DailyNutrients(
    val dayLabel: String,
    val date: Long,
    val calories: Int,
    val carbs: Double,
    val protein: Double,
    val fat: Double,
    val fiber: Double,
    val addedSugar: Double,
    val sodium: Double,
    val saturatedFat: Double,
    val vegetableContent: Double,
    val water: Double,
    val mealTimingScore: Double = 100.0 // Mock score for chart
)

data class HealthData(
    val currentCalories: Int = 0,
    val targetCalories: Int = 2000,
    val currentWeight: Float = 60.0f,
    val weightProgress: List<Pair<String, Float>> = emptyList(), // Date -> Weight
    val waterIntakeLiters: Float = 0f,
    val waterTargetLiters: Float = 2.5f,
    val carbs: Double = 0.0,
    val protein: Double = 0.0,
    val fat: Double = 0.0,
    val fiber: Double = 0.0,
    val addedSugar: Double = 0.0,
    val sodium: Double = 0.0,
    val saturatedFat: Double = 0.0,
    val vegetableContent: Double = 0.0,
    val aiWarning: String? = null,
    val overallAdvice: String? = null,
    val nextMealSuggestion: String? = null,
    // User Profile
    val age: Int = 0,
    val sex: String = "",
    val height: Float = 0f,
    val activityLevel: ActivityLevel = ActivityLevel.LIGHTLY_ACTIVE, // Default enum is fine or add UNKNOWN
    val healthConditions: Set<String> = emptySet(),
    val dietaryPattern: String = "",
    val medications: String = "",
    val primaryGoal: String = "",
    val goalIntensity: String = "",
    val secondaryGoals: Set<String> = emptySet(),
    val detailGoal: String = "",
    val dailyMeals: List<Meal> = emptyList()
)


enum class ActivityLevel(val label: String) {
    SEDENTARY("Sedentary"),
    LIGHTLY_ACTIVE("Lightly Active"),
    ACTIVE("Active"),
    VERY_ACTIVE("Very Active")
}

enum class MealType { BREAKFAST, LUNCH, DINNER, SNACK }

data class Meal(
    val id: Long,
    val type: MealType,
    val date: Long,
    val dateLabel: String,
    val isCaptured: Boolean,
    val imageUrl: String? = null,
    val calories: Int? = null,
    val carbs: Double? = null,
    val protein: Double? = null,
    val fat: Double? = null,
    val aiComment: String? = null,
    val isHealthy: Boolean? = null,
    val foodItems: List<FoodItem> = emptyList(),
    val totalProtein: Double? = null,
    val totalFiber: Double? = null,
    val totalAddedSugar: Double? = null,
    val totalSaturatedFat: Double? = null,
    val totalSodium: Double? = null,
    val totalVegetableContent: Double? = null,
    val totalWater: Double? = null,
    val averageProcessingLevel: Int? = null
)

interface HealthRepository {
    fun getHealthData(): Flow<HealthData>
    fun addWater(amount: Float)
    fun updateWeight(newWeight: Float)
    fun logMeal(
        calories: Int,
        carbs: Double,
        protein: Double,
        fat: Double,
        fiber: Double,
        addedSugar: Double,
        sodium: Double,
        saturatedFat: Double,
        vegetableContent: Double
    )
    
    fun updateUserProfile(
        age: Int,
        sex: String,
        height: Float,
        weight: Float,
        activityLevel: ActivityLevel,
        healthConditions: Set<String>,
        dietaryPattern: String,
        medications: String,
        primaryGoal: String,
        goalIntensity: String,
        secondaryGoals: Set<String>,
        detailGoal: String = ""
    )
    
    fun getDaysWithMealsPaginated(limit: Int, offset: Int): Flow<List<DayWithMeals>>
    suspend fun getOrCreateDay(dayId: String, dateTimestamp: Long): DayEntity
    suspend fun saveMeal(meal: Meal): Long
    suspend fun updateMeal(meal: Meal)
    suspend fun saveMealWithFoodItems(meal: Meal, mealAnalysis: MealAnalysis): Long
    suspend fun getFoodItemsForMeal(mealId: Long): List<FoodItem>
    suspend fun getDayStatus(dayId: String): DayStatus
    fun getDayWithMeals(dayId: String): Flow<DayWithMeals?>
    suspend fun getDaysCountNewerThan(dateTimestamp: Long): Int
    
    fun getWeeklyTrends(): Flow<List<DailyNutrients>>
    @Deprecated("Use updateAdvice and updateMealSuggestion instead")
    fun updateOverallAdvice(advice: String, mealSuggestion: String?)
    
    fun updateAdvice(advice: String)
    fun updateMealSuggestion(suggestion: String)
}

data class DayStatus(
    val totalMeals: Int,
    val healthyMeals: Int,
    val isComplete: Boolean
) {
    val isHealthy: Boolean
        get() = totalMeals > 0 && healthyMeals >= totalMeals * 0.67
}

class HealthRepositoryImpl(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : HealthRepository {
    private val _data = MutableStateFlow(
        HealthData(
            currentCalories = 1250,
            targetCalories = 2200,
            currentWeight = 60.0f,
            weightProgress = emptyList(), // Will be updated from DB
            waterIntakeLiters = 1.2f,
            waterTargetLiters = 2.5f,
            carbs = 150.0,
            protein = 80.0,
            fat = 45.0,
            fiber = 15.0,
            addedSugar = 20.0,
            sodium = 1500.0,
            saturatedFat = 10.0,
            vegetableContent = 0.0,
            aiWarning = null,
            overallAdvice = null,
            nextMealSuggestion = null,
            dailyMeals = emptyList()
        )
    )

    private val database = DatabaseProvider.getDatabase()
    private val dayDao = database.dayDao()
    private val mealDao = database.mealDao()
    private val foodItemDao = database.foodItemDao()
    private val userInfoDao = database.userInfoDao()

    init {
        scope.launch {
            userInfoDao.getUserInfo().collect { entity ->
                if (entity != null) {
                    _data.update { current ->
                        current.copy(
                            age = entity.age,
                            sex = entity.sex,
                            height = entity.height,
                            currentWeight = entity.weight,
                            activityLevel = try { ActivityLevel.valueOf(entity.activityLevel) } catch (e: Exception) { ActivityLevel.LIGHTLY_ACTIVE },
                            healthConditions = if (entity.healthConditions.isBlank()) emptySet() else entity.healthConditions.split(",").toSet(),
                            dietaryPattern = entity.dietaryPattern,
                            medications = entity.medications,
                            primaryGoal = entity.primaryGoal,
                            goalIntensity = entity.goalIntensity,
                            secondaryGoals = if (entity.secondaryGoals.isBlank()) emptySet() else entity.secondaryGoals.split(",").toSet(),
                            detailGoal = entity.detailGoal
                        )
                    }
                } else {
                    // Initialize with default if not exists
                    val defaultUser = UserInfoEntity(
                        id = 0,
                        age = 0,
                        sex = "",
                        height = 0f,
                        weight = 60f, // Use a safe default for weight calculation to avoid div/0 elsewhere, or 0
                        activityLevel = ActivityLevel.LIGHTLY_ACTIVE.name,
                        healthConditions = "",
                        dietaryPattern = "",
                        medications = "",
                        primaryGoal = "",
                        goalIntensity = "",
                        secondaryGoals = "",
                        detailGoal = ""
                    )
                    userInfoDao.insertUserInfo(defaultUser)
                }
            }
        }
    }

    override fun getHealthData(): Flow<HealthData> {
        val todayId = DateUtils.generateDayId(kotlin.time.Clock.System.now().toEpochMilliseconds())
        
        return combine(
            _data,
            userInfoDao.getUserInfo(),
            userInfoDao.getWeightHistory(),
            dayDao.getDayWithMealsFlow(todayId)
        ) { appState, userInfo, weightHistory, dayWithMeals ->
            val dailyMealEntities = dayWithMeals?.meals ?: emptyList()
            
            val dbCalories = dailyMealEntities.sumOf { it.calories ?: 0 }
            val dbCarbs = dailyMealEntities.sumOf { it.carbs ?: 0.0 } 
            val dbProtein = dailyMealEntities.sumOf { it.protein ?: 0.0 }
            val dbFat = dailyMealEntities.sumOf { it.fat ?: 0.0 }
            
            val dbFiber = dailyMealEntities.sumOf { it.fiber ?: 0.0 }
            val dbSugar = dailyMealEntities.sumOf { it.addedSugar ?: 0.0 }
            val dbSodium = dailyMealEntities.sumOf { it.sodium ?: 0.0 }
            val dbSatFat = dailyMealEntities.sumOf { it.saturatedFat ?: 0.0 }
            val dbVeg = dailyMealEntities.sumOf { it.vegetableContent ?: 0.0 }
            
            val domainMeals = mutableListOf<Meal>()
            if (dayWithMeals != null) {
                dailyMealEntities.forEach { 
                    domainMeals.add(it.toMeal(dayWithMeals.day.dateTimestamp, foodItemDao))
                }
            }

            HealthData(
                currentCalories = dbCalories,
                targetCalories = appState.targetCalories,
                currentWeight = if (weightHistory.isNotEmpty()) weightHistory.last().weight else (userInfo?.weight ?: appState.currentWeight),
                weightProgress = weightHistory.map { DateUtils.formatDateLabel(it.dateTimestamp) to it.weight },
                waterIntakeLiters = appState.waterIntakeLiters,
                waterTargetLiters = appState.waterTargetLiters,
                
                carbs = dbCarbs,
                protein = dbProtein,
                fat = dbFat,
                fiber = dbFiber,
                addedSugar = dbSugar,
                sodium = dbSodium,
                saturatedFat = dbSatFat,
                vegetableContent = dbVeg,
                
                aiWarning = appState.aiWarning,
                overallAdvice = appState.overallAdvice,
                nextMealSuggestion = appState.nextMealSuggestion,
                
                age = userInfo?.age ?: 0,
                sex = userInfo?.sex ?: "",
                height = userInfo?.height ?: 0f,
                activityLevel = try { ActivityLevel.valueOf(userInfo?.activityLevel ?: "LIGHTLY_ACTIVE") } catch (e: Exception) { ActivityLevel.LIGHTLY_ACTIVE },
                healthConditions = if (userInfo?.healthConditions.isNullOrBlank()) emptySet() else userInfo!!.healthConditions.split(",").toSet(),
                dietaryPattern = userInfo?.dietaryPattern ?: "",
                medications = userInfo?.medications ?: "",
                primaryGoal = userInfo?.primaryGoal ?: "",
                goalIntensity = userInfo?.goalIntensity ?: "",
                secondaryGoals = if (userInfo?.secondaryGoals.isNullOrBlank()) emptySet() else userInfo!!.secondaryGoals.split(",").toSet(),
                detailGoal = userInfo?.detailGoal ?: "",
                dailyMeals = domainMeals
            )
        }
    }
    
    override fun getDaysWithMealsPaginated(limit: Int, offset: Int): Flow<List<DayWithMeals>> {
        return dayDao.getDaysWithMealsPaginated(limit, offset)
    }

    override fun getDayWithMeals(dayId: String): Flow<DayWithMeals?> {
        return dayDao.getDayWithMealsFlow(dayId)
    }

    override suspend fun getDaysCountNewerThan(dateTimestamp: Long): Int {
        return dayDao.getDaysNewerThan(dateTimestamp)
    }

    override fun getWeeklyTrends(): Flow<List<DailyNutrients>> {
        return dayDao.getDaysWithMealsPaginated(7, 0)
            .transformLatest { days ->
                val result = days.map { dayWithMeals ->
                    val mealIds = dayWithMeals.meals.map { it.id }
                    // Batch fetch food items
                    val foodItems = if (mealIds.isNotEmpty()) {
                        foodItemDao.getFoodItemsByMealIds(mealIds)
                    } else {
                        emptyList()
                    }
                    
                    // Aggregate Daily
                    var cal = 0
                    var carb = 0.0
                    var pro = 0.0
                    var fat = 0.0
                    var fib = 0.0
                    var sug = 0.0
                    var sod = 0.0
                    var sat = 0.0
                    var veg = 0.0
                    var wtr = 0.0
                    
                    // We sum from meals first for basic macros if available
                    // But our structure relies on foodItems for granular data
                    // However, HealthRepository.logMeal updates memory, not DB for granular.
                    // But saveMealWithFoodItems updates DB foodItems.
                    // So relying on foodItems IS correct for granular.
                    
                    // For basic macros, MealEntity has them too.
                    dayWithMeals.meals.forEach { m -> 
                         cal += m.calories ?: 0
                         carb += m.carbs ?: 0.0
                         pro += m.protein ?: 0.0
                         fat += m.fat ?: 0.0
                    }

                    // For granular, sum from foodItems
                    foodItems.forEach { fi ->
                         fib += fi.fiber ?: 0.0
                         sug += fi.addedSugar ?: 0.0
                         sod += fi.sodium ?: 0.0
                         sat += fi.saturatedFat ?: 0.0
                         veg += fi.vegetableContent ?: 0.0
                         wtr += fi.water ?: 0.0
                    }
                    
                    DailyNutrients(
                        dayLabel = DateUtils.formatDateLabel(dayWithMeals.day.dateTimestamp),
                        date = dayWithMeals.day.dateTimestamp,
                        calories = cal,
                        carbs = carb,
                        protein = pro,
                        fat = fat,
                        fiber = fib,
                        addedSugar = sug,
                        sodium = sod,
                        saturatedFat = sat,
                        vegetableContent = veg,
                        water = wtr
                    )
                }.sortedBy { it.date } // Ensure chronological order for chart
                emit(result)
            }
            .flowOn(Dispatchers.IO)
    }
    
    override suspend fun getOrCreateDay(dayId: String, dateTimestamp: Long): DayEntity {
        var day = dayDao.getDayById(dayId)
        if (day == null) {
            val newDay = DayEntity(id = dayId, dateTimestamp = dateTimestamp)
            dayDao.insertDay(newDay)
            
            MealType.values().filter { it != MealType.SNACK }.forEach { mealType ->
                mealDao.insertMeal(
                    MealEntity(
                        id = 0,
                        dayId = dayId,
                        capturedDate = null,
                        mealType = mealType.name,
                        imageUri = null,
                        calories = null,
                        carbs = null,
                        protein = null,
                        fat = null,
                        fiber = null,
                        addedSugar = null,
                        sodium = null,
                        saturatedFat = null,
                        vegetableContent = null,
                        water = null,
                        aiComment = null,
                        isHealthy = null
                    )
                )
            }
            day = newDay
        }
        return day
    }
    
    override suspend fun saveMeal(meal: Meal): Long {
        val dayId = DateUtils.generateDayId(meal.date)
        return mealDao.insertMeal(meal.toEntity(dayId))
    }

    override fun addWater(amount: Float) {
        _data.update { current ->
            val newWater = (current.waterIntakeLiters + amount).coerceAtMost(current.waterTargetLiters * 2) // Cap reasonable max
            checkWarnings(current.copy(waterIntakeLiters = newWater))
        }
    }

    override fun updateWeight(newWeight: Float) {
        scope.launch {
            val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
            userInfoDao.insertWeight(
                WeightHistoryEntity(
                    weight = newWeight,
                    dateTimestamp = now
                )
            )
            // Also update UserInfo singleton for consistency if needed, but history is source of truth for charts
            // Actually, we should update UserInfo.weight too as it's the "current" profile weight
            // Perform a single-shot read to update the user info
            val user = userInfoDao.getUserInfo().firstOrNull()
            if (user != null) {
                 userInfoDao.insertUserInfo(user.copy(weight = newWeight))
            }
        }
    }

    override fun logMeal(
        calories: Int,
        carbs: Double,
        protein: Double,
        fat: Double,
        fiber: Double,
        addedSugar: Double,
        sodium: Double,
        saturatedFat: Double,
        vegetableContent: Double
    ) {
        _data.update { current ->
            checkWarnings(
                current.copy(
                    currentCalories = current.currentCalories + calories,
                    carbs = current.carbs + carbs,
                    protein = current.protein + protein,
                    fat = current.fat + fat,
                    fiber = current.fiber + fiber,
                    addedSugar = current.addedSugar + addedSugar,
                    sodium = current.sodium + sodium,
                    saturatedFat = current.saturatedFat + saturatedFat,
                    vegetableContent = current.vegetableContent + vegetableContent
                )
            )
        }
    }

    override fun updateOverallAdvice(advice: String, mealSuggestion: String?) {
        _data.update { current ->
            current.copy(
                overallAdvice = advice,
                nextMealSuggestion = mealSuggestion
            )
        }
    }

    override fun updateAdvice(advice: String) {
        _data.update { current ->
            current.copy(overallAdvice = advice)
        }
    }

    override fun updateMealSuggestion(suggestion: String) {
        _data.update { current ->
            current.copy(nextMealSuggestion = suggestion)
        }
    }

    override fun updateUserProfile(
        age: Int,
        sex: String,
        height: Float,
        weight: Float,
        activityLevel: ActivityLevel,
        healthConditions: Set<String>,
        dietaryPattern: String,
        medications: String,
        primaryGoal: String,
        goalIntensity: String,
        secondaryGoals: Set<String>,
        detailGoal: String
    ) {
        scope.launch {
            val entity = UserInfoEntity(
                id = 0,
                age = age,
                sex = sex,
                height = height,
                weight = weight,
                activityLevel = activityLevel.name,
                healthConditions = healthConditions.joinToString(","),
                dietaryPattern = dietaryPattern,
                medications = medications,
                primaryGoal = primaryGoal,
                goalIntensity = goalIntensity,
                secondaryGoals = secondaryGoals.joinToString(","),
                detailGoal = detailGoal
            )
            userInfoDao.insertUserInfo(entity)
        }
    }

    override suspend fun updateMeal(meal: Meal) {
        val dayId = DateUtils.generateDayId(meal.date)
        mealDao.updateMeal(meal.toEntity(dayId))
    }
    
    override suspend fun saveMealWithFoodItems(meal: Meal, mealAnalysis: MealAnalysis): Long {
        val dayId = DateUtils.generateDayId(meal.date)
        val startOfDay = DateUtils.getStartOfDay(meal.date)
        
        // Ensure day exists to satisfy Foreign Key and enable Flow updates
        getOrCreateDay(dayId, startOfDay)
        
        val mealEntity = MealEntity(
            id = if (meal.id == 0L) 0 else meal.id,
            dayId = dayId,
            capturedDate = meal.date,
            mealType = meal.type.name,
            imageUri = meal.imageUrl,
            calories = mealAnalysis.totalCalories,
            carbs = null,
            protein = mealAnalysis.totalProtein,
            fat = mealAnalysis.totalSaturatedFat, // Fallback/Legacy
            fiber = mealAnalysis.totalFiber,
            addedSugar = mealAnalysis.totalAddedSugar,
            sodium = mealAnalysis.totalSodium,
            saturatedFat = mealAnalysis.totalSaturatedFat,
            vegetableContent = mealAnalysis.totalVegetableContent,
            water = mealAnalysis.totalWater,
            aiComment = mealAnalysis.aiComment,
            isHealthy = mealAnalysis.isHealthy
        )
        
        val mealId = mealDao.insertMeal(mealEntity)
        
        val foodItemEntities = mealAnalysis.foodItems.map { foodItem ->
            FoodItemEntity(
                id = 0,
                mealId = mealId,
                label = foodItem.label,
                box2dY0 = foodItem.box_2d.getOrNull(0) ?: 0,
                box2dX0 = foodItem.box_2d.getOrNull(1) ?: 0,
                box2dY1 = foodItem.box_2d.getOrNull(2) ?: 0,
                box2dX1 = foodItem.box_2d.getOrNull(3) ?: 0,
                maskBase64 = foodItem.mask,
                calories = foodItem.metrics?.calories,
                protein = foodItem.metrics?.protein,
                fiber = foodItem.metrics?.fiber,
                addedSugar = foodItem.metrics?.addedSugar,
                saturatedFat = foodItem.metrics?.saturatedFat,
                sodium = foodItem.metrics?.sodium,
                vegetableContent = foodItem.metrics?.vegetableContent,
                water = foodItem.metrics?.water,
                processingLevel = foodItem.metrics?.processingLevel,
                isHealthy = foodItem.isHealthy
            )
        }
        
        foodItemDao.insertFoodItems(foodItemEntities)
        
        // Update in-memory health data
        logMeal(
            calories = mealAnalysis.totalCalories ?: 0,
            carbs = 0.0, // NOTE: Analysis doesn't seem to return total carbs?
            protein = mealAnalysis.totalProtein ?: 0.0,
            fat = mealAnalysis.totalSaturatedFat ?: 0.0, // Using saturated fat as 'fat' proxy if total fat missing?
            fiber = foodItemEntities.sumOf { it.fiber ?: 0.0 },
            addedSugar = foodItemEntities.sumOf { it.addedSugar ?: 0.0 },
            sodium = foodItemEntities.sumOf { it.sodium ?: 0.0 },
            saturatedFat = foodItemEntities.sumOf { it.saturatedFat ?: 0.0 },
            vegetableContent = foodItemEntities.sumOf { it.vegetableContent ?: 0.0 }
        )
        
        return mealId
    }
    
    override suspend fun getFoodItemsForMeal(mealId: Long): List<FoodItem> {
        val foodItemEntities = foodItemDao.getFoodItemsByMealId(mealId)
        return foodItemEntities.map { entity ->
            FoodItem(
                label = entity.label,
                box_2d = listOf(entity.box2dY0, entity.box2dX0, entity.box2dY1, entity.box2dX1),
                mask = entity.maskBase64,
                metrics = if (entity.calories != null) {
                    FoodMetrics(
                        calories = entity.calories ?: 0,
                        protein = entity.protein ?: 0.0,
                        fiber = entity.fiber ?: 0.0,
                        addedSugar = entity.addedSugar ?: 0.0,
                        saturatedFat = entity.saturatedFat ?: 0.0,
                        sodium = entity.sodium ?: 0.0,
                        vegetableContent = entity.vegetableContent ?: 0.0,
                        water = entity.water ?: 0.0,
                        processingLevel = entity.processingLevel ?: 0,
                        isHealthy = entity.isHealthy ?: false
                    )
                } else null,
                isHealthy = entity.isHealthy ?: false
            )
        }
    }
    
    override suspend fun getDayStatus(dayId: String): DayStatus {
        val capturedMeals = mealDao.getCapturedMealCountForDay(dayId)
        val healthyMeals = mealDao.getHealthyMealCountForDay(dayId)
        return DayStatus(
            totalMeals = capturedMeals,
            healthyMeals = healthyMeals,
            isComplete = capturedMeals >= 3
        )
    }

    private fun checkWarnings(data: HealthData): HealthData {
        var warning: String? = null
        if (data.waterIntakeLiters < 0.5f && data.currentCalories > 1000) {
            warning = "Water intake too low for current consumption."
        } else if (data.fat > 100) { // Arbitrary threshold
            warning = "High fat intake detected."
        }
        
        return data.copy(aiWarning = warning)
    }
}

suspend fun MealEntity.toMeal(dayTimestamp: Long, foodItemDao: com.lea.stamp.data.db.FoodItemDao): Meal {
    val foodItems = foodItemDao.getFoodItemsByMealId(id).map { entity ->
        FoodItem(
            label = entity.label,
            box_2d = listOf(entity.box2dY0, entity.box2dX0, entity.box2dY1, entity.box2dX1),
            mask = entity.maskBase64,
            metrics = if (entity.calories != null) {
                FoodMetrics(
                    calories = entity.calories ?: 0,
                    protein = entity.protein ?: 0.0,
                    fiber = entity.fiber ?: 0.0,
                    addedSugar = entity.addedSugar ?: 0.0,
                    saturatedFat = entity.saturatedFat ?: 0.0,
                    sodium = entity.sodium ?: 0.0,
                    vegetableContent = entity.vegetableContent ?: 0.0,
                    water = entity.water ?: 0.0,
                    processingLevel = entity.processingLevel ?: 0,
                    isHealthy = entity.isHealthy ?: false
                )
            } else null,
            isHealthy = entity.isHealthy ?: false
        )
    }
    
    val totalProtein = foodItems.sumOf { it.metrics?.protein ?: 0.0 }
    val totalFiber = foodItems.sumOf { it.metrics?.fiber ?: 0.0 }
    val totalAddedSugar = foodItems.sumOf { it.metrics?.addedSugar ?: 0.0 }
    val totalSaturatedFat = foodItems.sumOf { it.metrics?.saturatedFat ?: 0.0 }
    val totalSodium = foodItems.sumOf { it.metrics?.sodium ?: 0.0 }
    val totalVegetableContent = foodItems.sumOf { it.metrics?.vegetableContent ?: 0.0 }
    val totalWater = foodItems.sumOf { it.metrics?.water ?: 0.0 }
    val averageProcessingLevel = if (foodItems.isNotEmpty()) {
        foodItems.sumOf { it.metrics?.processingLevel ?: 0 } / foodItems.size
    } else null
    
    return Meal(
        id = id,
        type = MealType.valueOf(mealType),
        date = dayTimestamp,
        dateLabel = DateUtils.formatDateLabel(dayTimestamp),
        isCaptured = imageUri != null,
        imageUrl = imageUri,
        calories = calories,
        carbs = carbs,
        protein = protein,
        fat = fat,
        aiComment = aiComment,
        isHealthy = isHealthy,
        foodItems = foodItems,
        totalProtein = protein, // Use Entity data if available
        totalFiber = fiber ?: totalFiber,
        totalAddedSugar = addedSugar ?: totalAddedSugar,
        totalSaturatedFat = saturatedFat ?: totalSaturatedFat,
        totalSodium = sodium ?: totalSodium,
        totalVegetableContent = vegetableContent ?: totalVegetableContent,
        totalWater = water ?: totalWater,
        averageProcessingLevel = averageProcessingLevel
    )
}

fun Meal.toEntity(dayId: String): MealEntity {
    return MealEntity(
        id = if (id == 0L) 0 else id,
        dayId = dayId,
        capturedDate = if (imageUrl != null) date else null,
        mealType = type.name,
        imageUri = imageUrl,
        calories = calories,
        carbs = carbs,
        protein = protein,
        fat = fat,
        fiber = totalFiber,
        addedSugar = totalAddedSugar,
        sodium = totalSodium,
        saturatedFat = totalSaturatedFat,
        vegetableContent = totalVegetableContent,
        water = totalWater,
        aiComment = aiComment,
        isHealthy = isHealthy
    )
}

object RepositoryProvider {
    val instance: HealthRepository = HealthRepositoryImpl()
}

