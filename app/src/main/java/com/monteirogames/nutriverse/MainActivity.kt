package com.monteirogames.nutriverse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.monteirogames.nutriverse.ui.navigation.RootNavHost
import com.monteirogames.nutriverse.ui.theme.NutriVerseTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      NutriVerseTheme {
        RootNavHost()
      }
    }
  }
}
