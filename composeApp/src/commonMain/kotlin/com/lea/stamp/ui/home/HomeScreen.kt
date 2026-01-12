package com.lea.stamp.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lea.stamp.ui.home.components.DailyTrendChart
import com.lea.stamp.ui.home.components.AIAdviceSection
// import com.lea.stamp.ui.home.components.DailyProgressBar // Removed
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.lea.stamp.ui.home.components.HealthMetricTile
import com.lea.stamp.ui.home.components.WeightLineChart
import com.lea.stamp.ui.home.components.GoalHeader
import com.lea.stamp.ui.home.components.EditWeightDialog
import com.lea.stamp.ui.home.components.MealSuggestionSection
import com.lea.stamp.ui.home.components.KeyMetricsSection
import com.lea.stamp.ui.home.components.WeightSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel { HomeViewModel() }
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (state.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            HomeContent(
                state = state,
                onAddWater = { viewModel.onEvent(HomeEvent.AddWater(it)) },
                onUpdateWeight = { viewModel.onEvent(HomeEvent.UpdateWeight(it)) },
                modifier = Modifier.padding(padding)
            )
        }
    }
}



@Composable
fun HomeContent(
    state: HomeUiState,
    onAddWater: (Float) -> Unit,
    onUpdateWeight: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var showWeightDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    if (showWeightDialog) {
        EditWeightDialog(
            currentWeight = state.currentWeight,
            onDismiss = { showWeightDialog = false },
            onConfirm = onUpdateWeight
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 1. Main Goal Header
        // 1. Main Goal Header
        item {
            GoalHeader(
                goalName = state.mainGoalName
            )
        }

        // 2. Weight Section
        item {
            WeightSection(
                currentWeight = state.currentWeight,
                weightChange = state.weightChange,
                trend = state.weightTrend,
                status = state.mainGoalStatus,
                onEditClick = { showWeightDialog = true }
            )
           
        }
        
        // 3. AI Advice (Moved here)
        item {
            AIAdviceSection(
                warning = state.aiWarning,
                advice = state.overallAdvice,
                isLoading = state.isAiLoading
            )
        }

        // 4. Key Metrics (Goal Driven)
        item {
            KeyMetricsSection(metrics = state.keyMetrics)
        }

        // Weight Line Chart (New Section)
        if (state.weightGraphData.size > 1) {
             item {
                 androidx.compose.material3.Surface(
                     modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                     shape = RoundedCornerShape(12.dp),
                     color = MaterialTheme.colorScheme.surface,
                     shadowElevation = 2.dp
                 ) {
                     Column(modifier = Modifier.padding(12.dp)) {
                         Text(
                             text = "Weight Trend",
                             style = MaterialTheme.typography.titleSmall,
                             modifier = Modifier.padding(bottom = 8.dp)
                         )
                        WeightLineChart(data = state.weightGraphData)
                     }
                 }
            }
        }
        
        // 5. Meal Suggestion
        if (!state.nextMealSuggestion.isNullOrBlank()) {
             item {
                 MealSuggestionSection(
                     suggestion = state.nextMealSuggestion,
                     type = state.mealSuggestionType
                 )
             }
        }
        
        // 6. Daily Progress (Removed)
        // item {
        //      DailyProgressBar(progress = state.dailyProgress)
        // }

        // 7. Daily Trend Chart
        item {
            DailyTrendChart(
                data = state.dailyTrend,
                metricNames = state.trendMetricNames
            )
        }
        

        
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}








