package com.lea.stamp.ui.fooddetail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.lea.stamp.data.Meal
import com.lea.stamp.data.FoodItem
import com.lea.stamp.ui.components.SegmentedImageView
import com.lea.stamp.ui.components.ZoomableImage
import com.lea.stamp.getPlatform

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodDetailScreen(
    meal: Meal?,
    isLoading: Boolean = false,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showZoom by remember { mutableStateOf(false) }

    if (showZoom && meal?.imageUrl != null) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showZoom = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            com.lea.stamp.ui.components.ZoomableImage(
                imageUrl = meal.imageUrl,
                foodItems = meal.foodItems,
                onClose = { showZoom = false }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meal Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            if (meal != null) {
                // Image with segmentation overlay
                if (meal.imageUrl != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showZoom = true }
                    ) {
                        if (meal.foodItems.isNotEmpty()) {
                            // Show segmented image with overlays
                            SegmentedImageView(
                                imageUrl = meal.imageUrl,
                                foodItems = meal.foodItems,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1.5f)
                            )
                        } else {
                            // Show plain image if no segmentation data
                            AsyncImage(
                                model = meal.imageUrl,
                                contentDescription = "Meal Image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1.5f)
                                    .background(Color.LightGray),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                    }
                } else {
                     Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.5f)
                            .background(Color.LightGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No Image",
                            color = Color.DarkGray
                        )
                    }
                }

                // Content Section with Padding
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    text = "Analyzing food...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    } else {
                        // 1. Total Summary (Moved to Top)
                        if (meal.foodItems.isNotEmpty()) {
                            TotalNutritionCard(meal = meal)
                            Spacer(modifier = Modifier.height(16.dp))
                        } else {
                             // Basic Analysis Fallback
                             Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Basic Analysis",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                         Text("Calories: ${meal.calories ?: "--"} kcal")
                                         Text("Carbs: ${meal.carbs ?: "--"}g")
                                    }
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                         Text("Protein: ${meal.protein ?: "--"}g")
                                         Text("Fat: ${meal.fat ?: "--"}g")
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // 2. Comment (Moved to Top)
                        Text(
                            text = "Comment:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Card (
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha=0.5f))
                        ) {
                           Text(
                                text = meal.aiComment ?: "No comment available.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // 3. Expandable Food Items List
                        if (meal.foodItems.isNotEmpty()) {
                            var isFoodItemsExpanded by remember { mutableStateOf(false) }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isFoodItemsExpanded = !isFoodItemsExpanded }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Food Items Details (${meal.foodItems.size})",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = if (isFoodItemsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isFoodItemsExpanded) "Collapse" else "Expand"
                                )
                            }

                            AnimatedVisibility(
                                visible = isFoodItemsExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                    meal.foodItems.forEach { foodItem ->
                                        FoodItemCard(foodItem = foodItem)
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                }
                            }
                        }
                    }
                }

            } else {
                Text(
                    text = "No meal selected.",
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun FoodItemCard(foodItem: FoodItem) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (foodItem.isHealthy) 
                Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = foodItem.label.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = if (foodItem.isHealthy) 
                        Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (foodItem.isHealthy) 
                        Color(0xFF4CAF50) else Color(0xFFFF9800),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            if (foodItem.metrics != null) {
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                
                // Using a Column with Rows for 2-column grid layout with equal weights
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.weight(1f)) {
                            NutrientItem("Calories", "${foodItem.metrics.calories}", "kcal")
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            NutrientItem("Protein", "${foodItem.metrics.protein}", "g")
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.weight(1f)) {
                            NutrientItem("Fiber", "${foodItem.metrics.fiber}", "g")
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            NutrientItem("Sugar", "${foodItem.metrics.addedSugar}", "g")
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.weight(1f)) {
                            NutrientItem("Sat. Fat", "${foodItem.metrics.saturatedFat}", "g")
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            NutrientItem("Sodium", "${foodItem.metrics.sodium}", "mg")
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.weight(1f)) {
                            NutrientItem("Vegetables", "${foodItem.metrics.vegetableContent}", "g")
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            NutrientItem("Water", "${foodItem.metrics.water}", "ml")
                        }
                    }
                }
                
                Text(
                    text = "Processing Level: ${foodItem.metrics.processingLevel}/5",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp)
                    ).padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun TotalNutritionCard(meal: Meal) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (meal.isHealthy == true) 
                Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
        ),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Nutritional Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (meal.isHealthy == true) 
                            Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (meal.isHealthy == true) 
                            Color(0xFF4CAF50) else Color(0xFFFF9800),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (meal.isHealthy == true) "Healthy" else "Improve",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (meal.isHealthy == true) 
                            Color(0xFF4CAF50) else Color(0xFFFF9800)
                    )
                }
            }
            
            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            
             // Using similar grid layout as FoodItemCard for consistency
             Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f)) {
                        NutrientItem("Calories", "${meal.calories ?: 0}", "kcal")
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        NutrientItem("Protein", "${meal.totalProtein ?: 0}", "g")
                    }
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f)) {
                        NutrientItem("Fiber", "${meal.totalFiber ?: 0}", "g")
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        NutrientItem("Added Sugar", "${meal.totalAddedSugar ?: 0}", "g")
                    }
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f)) {
                        NutrientItem("Sat. Fat", "${meal.totalSaturatedFat ?: 0}", "g")
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        NutrientItem("Sodium", "${meal.totalSodium ?: 0}", "mg")
                    }
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f)) {
                        NutrientItem("Vegetables", "${meal.totalVegetableContent ?: 0}", "g")
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        NutrientItem("Water", "${meal.totalWater ?: 0}", "ml")
                    }
                }
            }
            
            if (meal.averageProcessingLevel != null) {
                Text(
                    text = "Avg. Processing Level: ${meal.averageProcessingLevel}/5",
                    style = MaterialTheme.typography.labelMedium,
                     modifier = Modifier.background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp)
                    ).padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun NutrientItem(label: String, value: String, unit: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$value $unit",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}
