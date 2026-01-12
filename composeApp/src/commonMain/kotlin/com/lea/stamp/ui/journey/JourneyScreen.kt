package com.lea.stamp.ui.journey

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Badge
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.Icon
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lea.stamp.data.Meal
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JourneyScreen(
    modifier: Modifier = Modifier,
    viewModel: JourneyViewModel = viewModel { JourneyViewModel() },
    onMealClick: (Meal) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    var showDatePicker by remember { androidx.compose.runtime.mutableStateOf(false) }
    
    if (state.showSettings) {
        SettingsDialog(
            apiKey = state.geminiApiKey,
            onApiKeyChange = viewModel::updateApiKey,
            onDismiss = viewModel::toggleSettings,
            onSave = viewModel::saveApiKey
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { dateMillis ->
                             viewModel.selectDate(dateMillis)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.selectDate(null) // Reset to all
                        showDatePicker = false
                    }
                ) {
                    Text("Clear")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val density = androidx.compose.ui.platform.LocalDensity.current
    
    LaunchedEffect(state.selectedDate, state.dayMealsList) {
        state.selectedDate?.let { date ->
            val index = state.dayMealsList.indexOfFirst { it.date == date }
            if (index >= 0) {
                // Center the item: offset it downwards by ~30% of screen height (heuristic)
                // Negative scrollOffset moves the item down
                val offset = with(density) { -250.dp.roundToPx() }
                listState.scrollToItem(index, offset)
                viewModel.clearSelection()
            }
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { 
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val firstVisible = layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0
            Triple(firstVisible, lastVisible, totalItems)
        }
            .distinctUntilChanged()
            .collect { (firstVisible, lastVisible, totalItems) ->
                // Load Previous (Top)
                if (firstVisible == 0 && state.canLoadPrevious && !state.isLoading) {
                    viewModel.loadPreviousMeals()
                }
                
                // Load More (Bottom)
                if (totalItems > 0 && 
                    lastVisible >= totalItems - 3 && 
                    state.hasMore && 
                    !state.isLoading) {
                    viewModel.loadMoreMeals()
                }
            }
    }

    Column(
        modifier = modifier.fillMaxSize()
            .padding(top = 16.dp) // Add some top padding
    ) {
        // Header Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My Stampbook",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Select Date",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = viewModel::toggleSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        JourneyContent(
            dayMealsList = state.dayMealsList,
            isLoading = state.isLoading,
            hasMore = state.hasMore,
            listState = listState,
            onMealClick = onMealClick
        )
    }
}

@Composable
fun JourneyContent(
    dayMealsList: List<DayMeals>,
    isLoading: Boolean,
    hasMore: Boolean,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onMealClick: (Meal) -> Unit
) {
    if (isLoading && dayMealsList.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            items(dayMealsList, key = { it.date }) { dayMeals ->
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dayMeals.dateLabel,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        dayMeals.status?.let { status ->
                            DayStatusBadge(status = status)
                        }
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        dayMeals.meals.forEach { meal ->
                            MealItem(
                                meal = meal,
                                onClick = { onMealClick(meal) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
fun DayStatusBadge(status: com.lea.stamp.data.DayStatus) {
    val (icon, color, text) = when {
        status.isHealthy -> Triple(
            Icons.Default.CheckCircle,
            Color(0xFF4CAF50),
            "Healthy"
        )
        status.isComplete -> Triple(
            Icons.Default.Warning,
            Color(0xFFFFA726),
            "Needs Improvement"
        )
        else -> Triple(
            Icons.Default.Warning,
            Color(0xFFEF5350),
            "Incomplete"
        )
    }
    
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = color,
            modifier = Modifier.height(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun MealItem(
    meal: Meal,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(if (meal.isCaptured) Color(0xFFFFCCBC) else Color(0xFFEEEEEE))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            if (meal.isCaptured && meal.imageUrl != null) {
                coil3.compose.AsyncImage(
                    model = meal.imageUrl,
                    contentDescription = "Meal Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Text("?", style = MaterialTheme.typography.headlineLarge, color = Color.Gray)
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = meal.type.name,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun SettingsDialog(
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gemini API Settings") },
        text = {
            Column {
                Text(
                    text = "Enter your Gemini API key to enable food analysis:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                TextField(
                    value = apiKey,
                    onValueChange = onApiKeyChange,
                    label = { Text("API Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
