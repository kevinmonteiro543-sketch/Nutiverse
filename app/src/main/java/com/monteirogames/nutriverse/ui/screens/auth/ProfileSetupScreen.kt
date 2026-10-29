package com.monteirogames.nutriverse.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.monteirogames.nutriverse.ui.viewmodel.createViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(onNavigateToMain: () -> Unit) {
    val viewModel = createViewModel { app -> 
        ProfileSetupViewModel(app.authUseCases, app.userProfileUseCases, app.profileSyncUseCases) 
    }
    
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is ProfileSetupUiState.Success) {
            onNavigateToMain()
        }
    }

    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    
    var goal by remember { mutableStateOf("") }
    var dietaryPreference by remember { mutableStateOf("") }
    var allergies by remember { mutableStateOf("") }
    
    var activityLevel by remember { mutableStateOf("") }
    var workoutFrequency by remember { mutableStateOf("") }
    var workoutIntensity by remember { mutableStateOf("") }

    val activityLevels = listOf("Sedentary", "Lightly Active", "Moderately Active", "Very Active")
    var activityExpanded by remember { mutableStateOf(false) }

    val workoutFrequencies = listOf("0 days", "1-3 days", "4-5 days", "6+ days")
    var frequencyExpanded by remember { mutableStateOf(false) }

    val workoutIntensities = listOf("Light", "Moderate", "High", "Very High")
    var intensityExpanded by remember { mutableStateOf(false) }

    val goals = listOf("Lose Weight", "Maintain Weight", "Gain Weight", "Build Muscle")
    var goalExpanded by remember { mutableStateOf(false) }

    val genders = listOf("Male", "Female", "Other")
    var genderExpanded by remember { mutableStateOf(false) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("Complete Your Profile", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("We need a few details to personalize your experience.", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Name") }, modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = age, onValueChange = { age = it },
                    label = { Text("Age") }, modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                ExposedDropdownMenuBox(
                    expanded = genderExpanded,
                    onExpandedChange = { genderExpanded = !genderExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = gender,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Gender") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = genderExpanded,
                        onDismissRequest = { genderExpanded = false }
                    ) {
                        genders.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    gender = option
                                    genderExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = height, onValueChange = { height = it },
                    label = { Text("Height (cm)") }, modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = weight, onValueChange = { weight = it },
                    label = { Text("Weight (kg)") }, modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            ExposedDropdownMenuBox(
                expanded = goalExpanded,
                onExpandedChange = { goalExpanded = !goalExpanded }
            ) {
                OutlinedTextField(
                    value = goal,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Goal") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = goalExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = goalExpanded,
                    onDismissRequest = { goalExpanded = false }
                ) {
                    goals.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                goal = option
                                goalExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = dietaryPreference, onValueChange = { dietaryPreference = it },
                label = { Text("Dietary Preference (Optional)") }, modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = allergies, onValueChange = { allergies = it },
                label = { Text("Allergies (Optional)") }, modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Activity & Workout", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = activityExpanded,
                onExpandedChange = { activityExpanded = !activityExpanded }
            ) {
                OutlinedTextField(
                    value = activityLevel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Base Activity Level") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = activityExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = activityExpanded,
                    onDismissRequest = { activityExpanded = false }
                ) {
                    activityLevels.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                activityLevel = option
                                activityExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = frequencyExpanded,
                onExpandedChange = { frequencyExpanded = !frequencyExpanded }
            ) {
                OutlinedTextField(
                    value = workoutFrequency,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Workout Frequency") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = frequencyExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = frequencyExpanded,
                    onDismissRequest = { frequencyExpanded = false }
                ) {
                    workoutFrequencies.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                workoutFrequency = option
                                frequencyExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = intensityExpanded,
                onExpandedChange = { intensityExpanded = !intensityExpanded }
            ) {
                OutlinedTextField(
                    value = workoutIntensity,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Workout Intensity") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = intensityExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = intensityExpanded,
                    onDismissRequest = { intensityExpanded = false }
                ) {
                    workoutIntensities.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                workoutIntensity = option
                                intensityExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            if (uiState is ProfileSetupUiState.Error) {
                Text(
                    text = (uiState as ProfileSetupUiState.Error).message, 
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            Button(
                onClick = { 
                    viewModel.saveProfile(
                        name, age, gender, height, weight, goal, dietaryPreference, allergies, activityLevel, workoutFrequency, workoutIntensity
                    )
                },
                enabled = uiState !is ProfileSetupUiState.Loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState is ProfileSetupUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Complete Setup")
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
