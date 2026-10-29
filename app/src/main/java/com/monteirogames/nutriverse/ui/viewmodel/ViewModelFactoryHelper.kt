package com.monteirogames.nutriverse.ui.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.monteirogames.nutriverse.NutriVerseApplication

@Composable
inline fun <reified T : ViewModel> createViewModel(
    crossinline creator: (application: NutriVerseApplication) -> T
): T {
    val context = LocalContext.current
    val application = context.applicationContext as NutriVerseApplication
    
    return viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <VM : ViewModel> create(modelClass: Class<VM>): VM {
                return creator(application) as VM
            }
        }
    )
}
