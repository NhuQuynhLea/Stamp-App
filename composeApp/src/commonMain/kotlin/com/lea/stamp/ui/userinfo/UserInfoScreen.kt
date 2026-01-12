package com.lea.stamp.ui.userinfo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lea.stamp.data.ActivityLevel

import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserInfoScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UserInfoViewModel = viewModel { UserInfoViewModel() }
) {
    val state by viewModel.uiState.collectAsState()

    // Removed auto-navigation on save as per requirement
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Personal Info") },
                actions = {
                    if (!state.isEditing) {
                        IconButton(onClick = { viewModel.onEvent(UserInfoEvent.ToggleEditMode) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                    }
                    // Removed duplicate Save icon
                }
            )
        },
        modifier = modifier
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (state.validationError != null) {
                    Text(
                        text = state.validationError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Section A: Core Personal Profile
                UserInfoSection(title = "Core Personal Profile (Required)") {
                     Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                         OutlinedTextField(
                            value = state.age,
                            onValueChange = { viewModel.onEvent(UserInfoEvent.UpdateAge(it)) },
                            label = { Text("Age") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            enabled = state.isEditing,
                            isError = state.validationError?.contains("age", ignoreCase = true) == true
                        )
                        
                        // Sex Selection
                        SelectionField(
                            label = "Sex",
                            value = state.sex,
                            options = listOf("Male", "Female"),
                            onOptionSelected = { viewModel.onEvent(UserInfoEvent.UpdateSex(it)) },
                            modifier = Modifier.weight(1f),
                            enabled = state.isEditing
                        )
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = state.height,
                            onValueChange = { viewModel.onEvent(UserInfoEvent.UpdateHeight(it)) },
                            label = { Text("Height (cm)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            enabled = state.isEditing,
                            isError = state.validationError?.contains("height", ignoreCase = true) == true
                        )
                        OutlinedTextField(
                            value = state.weight,
                            onValueChange = { viewModel.onEvent(UserInfoEvent.UpdateWeight(it)) },
                            label = { Text("Weight (kg)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            enabled = state.isEditing,
                            isError = state.validationError?.contains("weight", ignoreCase = true) == true
                        )
                    }
                    
                    Text("Activity Level", style = MaterialTheme.typography.labelLarge)
                    Column {
                        ActivityLevel.values().forEach { level ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = state.activityLevel == level,
                                    onClick = { viewModel.onEvent(UserInfoEvent.UpdateActivityLevel(level)) },
                                    enabled = state.isEditing
                                )
                                Text(
                                    text = level.label,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }

                // Section B: Health Context
                UserInfoSection(title = "Health Context (Optional)") {
                    Text("Health Conditions", style = MaterialTheme.typography.labelLarge)
                    // Multi-select using selection dialog
                    val conditions = listOf("Diabetes", "Prediabetes", "Hypertension", "High cholesterol", "Digestive issues", "Food allergies")
                    MultiSelectionField(
                        label = "Health Conditions",
                        selectedItems = state.healthConditions,
                        options = conditions,
                        onSelectionChanged = { viewModel.onEvent(UserInfoEvent.ToggleHealthCondition(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.isEditing
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.dietaryPattern,
                        onValueChange = { viewModel.onEvent(UserInfoEvent.UpdateDietaryPattern(it)) },
                        label = { Text("Dietary Pattern") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.isEditing
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.medications,
                        onValueChange = { viewModel.onEvent(UserInfoEvent.UpdateMedications(it)) },
                        label = { Text("Medications / Supplements") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.isEditing
                    )
                }

                // Section C: Goals
                UserInfoSection(title = "Health Goals Strategy") {
                    
                    // A. Primary Health Goal (Required - Single)
                    Text("Primary Goal (Pick 1)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    val primaryOptions = listOf(
                        "Weight loss", "Maintain weight", "Muscle gain", 
                        "Improve energy", "Blood sugar control", "Heart health", "Gut health"
                    )
                    SelectionField(
                        label = "Select Primary Goal",
                        value = state.primaryGoal,
                        options = primaryOptions,
                        onOptionSelected = { viewModel.onEvent(UserInfoEvent.UpdatePrimaryGoal(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.isEditing
                    )
                    
                    Spacer(Modifier.height(16.dp))

                    // B. Goal Style / Intensity
                    Text("Intensity / Style", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("How strict do you want to be?", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))
                    
                    val intensityOptions = if (state.primaryGoal in listOf("Weight loss", "Muscle gain")) {
                        listOf("Slow", "Moderate", "Fast")
                    } else {
                        listOf("Gentle", "Balanced", "Strict")
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                         intensityOptions.forEach { intensity ->
                            FilterChip(
                                selected = state.goalIntensity == intensity,
                                onClick = { viewModel.onEvent(UserInfoEvent.UpdateGoalIntensity(intensity)) },
                                label = { Text(intensity) },
                                enabled = state.isEditing
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))

                    // C. Secondary Focus
                    Text("Secondary Focus (Optional, Max 2)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    val secondaryOptions = listOf(
                        "Energy", "Digestion", "Athletic performance", "Sleep support"
                    )
                    MultiSelectionField(
                        label = "Select Secondary Focus",
                        selectedItems = state.secondaryGoals,
                        options = secondaryOptions,
                        onSelectionChanged = { viewModel.onEvent(UserInfoEvent.ToggleSecondaryGoal(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.isEditing
                    )
                }
                
                if (state.isEditing) {
                    Button(
                        onClick = { viewModel.onEvent(UserInfoEvent.Save) }, // This maps to triggerGeneration logic path in updated VM? No, I need to check VM "Save" event
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (state.generatedPlan.title.isEmpty()) "Generate Daily Target" else "Update Daily Target")
                    }
                }
                
                if (state.showGoalDialog) {
                    AlertDialog(
                        containerColor = androidx.compose.ui.graphics.Color.White,
                        onDismissRequest = { viewModel.onEvent(UserInfoEvent.DismissGoalDialog) },
                        title = { Text("Recommended Daily Target") },
                        text = {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                Text(
                                    text = state.generatedPlan.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                )
                                Text(
                                    text = state.generatedPlan.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                )
                                
                                Text("Daily Metrics", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                                
                                state.generatedPlan.metrics.forEachIndexed { index, metric ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = metric.label, 
                                            modifier = Modifier.width(80.dp),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        OutlinedTextField(
                                            value = metric.value,
                                            onValueChange = { viewModel.onEvent(UserInfoEvent.UpdateGeneratedPlanMetric(index, it)) },
                                            label = { Text("Value") },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { viewModel.onEvent(UserInfoEvent.SaveGeneratedGoal) }) {
                                Text("Save Plan")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { viewModel.onEvent(UserInfoEvent.DismissGoalDialog) }) {
                                Text("Cancel")
                            }
                        }
                    )
                } else if (state.isLoading) {
                    // Loading Overlay
                    AlertDialog(
                        onDismissRequest = {},
                        title = { Text("Creating Your Plan") },
                        text = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(16.dp))
                                Text("Consulting with Gemini...")
                            }
                        },
                        confirmButton = {},
                        dismissButton = {}
                    )
                }
                
                Spacer(Modifier.height(32.dp))
                
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun UserInfoSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
fun FlowWrap(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier) {
        content() 
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionField(
    label: String,
    value: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            enabled = enabled
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun MultiSelectionField(
    label: String,
    selectedItems: Set<String>,
    options: List<String>,
    onSelectionChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var showDialog by remember { mutableStateOf(false) }
    val displayValue = if (selectedItems.isEmpty()) "None" else selectedItems.joinToString(", ")

    androidx.compose.foundation.layout.Box(modifier = modifier) {
        OutlinedTextField(
            value = displayValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "Select") },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled
        )
        // Invisible overlay to catch clicks
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(enabled = enabled) { showDialog = true }
        )
    }

    if (showDialog) {
        AlertDialog(
            containerColor = androidx.compose.ui.graphics.Color.White,
            onDismissRequest = { showDialog = false },
            title = { Text("Select $label") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    options.forEach { option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectionChanged(option) }
                                .padding(vertical = 8.dp)
                        ) {
                            Checkbox(
                                checked = selectedItems.contains(option),
                                onCheckedChange = { onSelectionChanged(option) }
                            )
                            Text(
                                text = option,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}
