package com.monteirogames.nutriverse.ui.screens.mealtracker

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.monteirogames.nutriverse.domain.model.FoodItem
import com.monteirogames.nutriverse.domain.model.MealEntry
import com.monteirogames.nutriverse.domain.model.StructuredFoodEstimate
import com.monteirogames.nutriverse.ui.viewmodel.createViewModel
import java.util.UUID

@Composable
fun AiFoodRecognitionScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel = createViewModel { app ->
        AiFoodRecognitionViewModel(app.analyzeFoodImageUseCase, app.mealTrackerUseCases, app.authUseCases)
    }

    val uiState by viewModel.uiState.collectAsState()
    val selectedImage by viewModel.selectedImage.collectAsState()

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        viewModel.onImageSelected(bitmap)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, it))
            } else {
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            }
            // Make sure the bitmap is immutable before passing it around
            val immutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)
            viewModel.onImageSelected(immutableBitmap)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("AI Food Recognition", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if (selectedImage == null) {
            Text("Select an image to analyze", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = { cameraLauncher.launch() }) {
                    Text("Take Photo")
                }
                Button(onClick = {
                    galleryLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                }) {
                    Text("Choose from Gallery")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(onClick = onNavigateBack) {
                Text("Cancel")
            }
        } else {
            Image(
                bitmap = selectedImage!!.asImageBitmap(),
                contentDescription = "Selected food image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            when (val state = uiState) {
                is AiFoodRecognitionState.Idle -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedButton(onClick = { viewModel.resetState() }) {
                            Text("Retake")
                        }
                        Button(onClick = { viewModel.analyzeImage() }) {
                            Text("Analyze")
                        }
                    }
                }
                is AiFoodRecognitionState.Analyzing -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Analyzing image with Gemini...")
                }
                is AiFoodRecognitionState.Error -> {
                    Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedButton(onClick = { viewModel.resetState() }) {
                            Text("Cancel")
                        }
                        Button(onClick = { viewModel.analyzeImage() }) {
                            Text("Retry")
                        }
                    }
                }
                is AiFoodRecognitionState.Success -> {
                    ReviewEstimateForm(
                        estimate = state.estimate,
                        onSave = { updatedEstimate ->
                            viewModel.saveEstimateAsMeal(updatedEstimate)
                            onNavigateBack()
                        },
                        onCancel = { viewModel.resetState() }
                    )
                }
            }
        }
    }
}

@Composable
fun ReviewEstimateForm(
    estimate: StructuredFoodEstimate,
    onSave: (StructuredFoodEstimate) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(estimate.name) }
    var servingDescription by remember { mutableStateOf(estimate.servingDescription ?: "") }
    var calories by remember { mutableStateOf(estimate.calories?.toString() ?: "") }
    var protein by remember { mutableStateOf(estimate.protein?.toString() ?: "") }
    var carbs by remember { mutableStateOf(estimate.carbs?.toString() ?: "") }
    var fat by remember { mutableStateOf(estimate.fat?.toString() ?: "") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("AI Estimates (Review & Edit)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        
        if (estimate.confidenceScore != null) {
            Text("Confidence: ${(estimate.confidenceScore * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
        }
        
        if (estimate.notes != null) {
            Text("Notes: ${estimate.notes}", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Food Name") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = servingDescription,
            onValueChange = { servingDescription = it },
            label = { Text("Serving Description") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = calories,
                onValueChange = { calories = it },
                label = { Text("Calories") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = protein,
                onValueChange = { protein = it },
                label = { Text("Protein (g)") },
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = carbs,
                onValueChange = { carbs = it },
                label = { Text("Carbs (g)") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = fat,
                onValueChange = { fat = it },
                label = { Text("Fat (g)") },
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    val updatedEstimate = StructuredFoodEstimate(
                        name = name.ifBlank { "Unknown Food" },
                        servingDescription = servingDescription,
                        servingQuantity = estimate.servingQuantity ?: 1f,
                        calories = calories.toFloatOrNull(),
                        protein = protein.toFloatOrNull(),
                        carbs = carbs.toFloatOrNull(),
                        fat = fat.toFloatOrNull(),
                        confidenceScore = estimate.confidenceScore,
                        notes = estimate.notes
                    )
                    onSave(updatedEstimate)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Save as Meal")
            }
        }
    }
}
