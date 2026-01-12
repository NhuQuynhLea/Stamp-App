package com.lea.stamp.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import org.jetbrains.compose.resources.painterResource
import kmp_hackathon.composeapp.generated.resources.Res
import kmp_hackathon.composeapp.generated.resources.flame
import kmp_hackathon.composeapp.generated.resources.leaf
import kmp_hackathon.composeapp.generated.resources.stamp
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lea.stamp.data.Meal
import com.lea.stamp.data.RepositoryProvider
import com.lea.stamp.data.PreferencesProvider
import com.lea.stamp.data.gemini.FoodAnalysisServiceProvider
import com.lea.stamp.data.gemini.GeminiResult
import com.lea.stamp.data.readImageBytes
import com.lea.stamp.ui.camera.CameraScreen
import com.lea.stamp.ui.fooddetail.FoodDetailScreen
import com.lea.stamp.ui.home.HomeScreen
import com.lea.stamp.ui.journey.JourneyScreen
import com.lea.stamp.ui.userinfo.UserInfoScreen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import kotlinx.coroutines.launch

import com.lea.stamp.ui.splash.SplashScreen

enum class AppScreen {
    Splash, Home, Journey, Camera, FoodDetail, UserInfo
}

@Composable
fun MainScreen() {
    var currentScreen by remember { mutableStateOf(AppScreen.Splash) }
    var selectedMeal by remember { mutableStateOf<Meal?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val foodAnalysisService = remember { FoodAnalysisServiceProvider.instance }
    val preferencesManager = remember { PreferencesProvider.instance }
    val apiKey by preferencesManager.getGeminiApiKey().collectAsState(initial = null)

    Scaffold(
        bottomBar = {
            if (currentScreen == AppScreen.Home || currentScreen == AppScreen.Journey || currentScreen == AppScreen.UserInfo) {
                NavigationBar(
                    containerColor = androidx.compose.ui.graphics.Color.White
                ) {
                    val navItemColors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = androidx.compose.ui.graphics.Color.Transparent
                    )

                    NavigationBarItem(
                        selected = currentScreen == AppScreen.Journey,
                        onClick = { currentScreen = AppScreen.Journey },
                        icon = { Icon(painterResource(Res.drawable.stamp), contentDescription = "Stampbook") },
                        label = { Text("Stampbook") },
                        colors = navItemColors
                    )
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.Home,
                        onClick = { currentScreen = AppScreen.Home },
                        icon = { Icon(painterResource(Res.drawable.flame), contentDescription = "Home") },
                        label = { Text("Home") },
                        colors = navItemColors
                    )
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.UserInfo,
                        onClick = { currentScreen = AppScreen.UserInfo },
                        icon = { Icon(painterResource(Res.drawable.leaf), contentDescription = "Profile") },
                        label = { Text("Profile") },
                        colors = navItemColors
                    )
                }
            }
        }
    ) { padding ->
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
            },
            label = "Screen Transition"
        ) { targetScreen ->
            when (targetScreen) {
                AppScreen.Splash -> SplashScreen(
                    onTimeout = { currentScreen = AppScreen.Home }
                )

                AppScreen.Home -> HomeScreen(
                    modifier = Modifier.padding(padding)
                )

                AppScreen.UserInfo -> UserInfoScreen(
                    modifier = Modifier.padding(padding),
                    onBack = { currentScreen = AppScreen.Home }
                )

                AppScreen.Journey -> JourneyScreen(
                    modifier = Modifier.padding(padding),
                    onMealClick = { meal ->
                        selectedMeal = meal
                        if (meal.isCaptured) {
                            currentScreen = AppScreen.FoodDetail
                        } else {
                            currentScreen = AppScreen.Camera
                        }
                    }
                )

                AppScreen.Camera -> {
                    CameraScreen(
                        modifier = Modifier.padding(padding),
                        onCapture = { imagePath ->
                            selectedMeal?.let { meal ->
                                val tempMeal = meal.copy(imageUrl = imagePath)
                                selectedMeal = tempMeal
                                isProcessing = true
                                currentScreen = AppScreen.FoodDetail

                                scope.launch {
                                    try {
                                        val imageBytes = readImageBytes(imagePath)
                                        if (imageBytes == null) {
                                            errorMessage = "Failed to read image data"
                                            isProcessing = false
                                            return@launch
                                        }

                                        val currentApiKey = apiKey
                                        if (currentApiKey.isNullOrBlank()) {
                                            errorMessage = "Gemini API key not configured. Please set it in Journey settings."
                                            isProcessing = false
                                            currentScreen = AppScreen.Journey
                                            return@launch
                                        }

                                        when (val result = foodAnalysisService.analyzeFoodImageTwoPhase(imageBytes, currentApiKey)) {
                                            is GeminiResult.Success -> {
                                                val mealAnalysis = result.data
                                                val updatedMeal = tempMeal.copy(
                                                    aiComment = mealAnalysis.aiComment,
                                                    calories = mealAnalysis.totalCalories,
                                                    protein = mealAnalysis.totalProtein,
                                                    fat = mealAnalysis.totalSaturatedFat,
                                                    isHealthy = mealAnalysis.isHealthy,
                                                    foodItems = mealAnalysis.foodItems,
                                                    totalProtein = mealAnalysis.totalProtein,
                                                    totalFiber = mealAnalysis.totalFiber,
                                                    totalAddedSugar = mealAnalysis.totalAddedSugar,
                                                    totalSaturatedFat = mealAnalysis.totalSaturatedFat,
                                                    totalSodium = mealAnalysis.totalSodium,
                                                    totalVegetableContent = mealAnalysis.totalVegetableContent,
                                                    totalWater = mealAnalysis.totalWater,
                                                    averageProcessingLevel = mealAnalysis.averageProcessingLevel
                                                )
                                                selectedMeal = updatedMeal
                                                RepositoryProvider.instance.saveMealWithFoodItems(updatedMeal, mealAnalysis)
                                                isProcessing = false
                                            }
                                            is GeminiResult.Error -> {
                                                errorMessage = "Failed to analyze food: ${result.message}"
                                                isProcessing = false
                                            }
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = "Error processing image: ${e.message}"
                                        isProcessing = false
                                    }
                                }
                            }
                        },
                        onBack = { currentScreen = AppScreen.Journey }
                    )

                    errorMessage?.let { message ->
                        AlertDialog(
                            onDismissRequest = {
                                errorMessage = null
                                currentScreen = AppScreen.Journey
                            },
                            title = { Text("Processing Error") },
                            text = { Text(message) },
                            confirmButton = {
                                TextButton(onClick = {
                                    errorMessage = null
                                    currentScreen = AppScreen.Journey
                                }) {
                                    Text("OK")
                                }
                            }
                        )
                    }
                }

                AppScreen.FoodDetail -> FoodDetailScreen(
                    modifier = Modifier.padding(padding),
                    meal = selectedMeal,
                    isLoading = isProcessing,
                    onBack = { currentScreen = AppScreen.Journey }
                )
            }
        }
    }
}
