package com.monteirogames.nutriverse.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
    darkColorScheme(
        primary = EmeraldPrimaryDark,
        onPrimary = OnEmeraldPrimaryDark,
        primaryContainer = EmeraldPrimaryContainerDark,
        onPrimaryContainer = OnEmeraldPrimaryContainerDark,
        secondary = MintSecondaryDark,
        onSecondary = OnMintSecondaryDark,
        secondaryContainer = MintSecondaryContainerDark,
        onSecondaryContainer = OnMintSecondaryContainerDark,
        tertiary = HarvestTertiaryDark,
        onTertiary = OnHarvestTertiaryDark,
        tertiaryContainer = HarvestTertiaryContainerDark,
        onTertiaryContainer = OnHarvestTertiaryContainerDark,
        background = BackgroundDark,
        onBackground = OnBackgroundDark,
        surface = SurfaceDark,
        onSurface = OnSurfaceDark,
        surfaceVariant = SurfaceVariantDark,
        onSurfaceVariant = OnSurfaceVariantDark,
        outline = OutlineDark,
        outlineVariant = OutlineVariantDark,
        error = ErrorDark,
        onError = OnErrorDark,
        errorContainer = ErrorContainerDark,
        onErrorContainer = OnErrorContainerDark,
    )

private val LightColorScheme =
    lightColorScheme(
        primary = EmeraldPrimaryLight,
        onPrimary = OnEmeraldPrimaryLight,
        primaryContainer = EmeraldPrimaryContainerLight,
        onPrimaryContainer = OnEmeraldPrimaryContainerLight,
        secondary = MintSecondaryLight,
        onSecondary = OnMintSecondaryLight,
        secondaryContainer = MintSecondaryContainerLight,
        onSecondaryContainer = OnMintSecondaryContainerLight,
        tertiary = HarvestTertiaryLight,
        onTertiary = OnHarvestTertiaryLight,
        tertiaryContainer = HarvestTertiaryContainerLight,
        onTertiaryContainer = OnHarvestTertiaryContainerLight,
        background = BackgroundLight,
        onBackground = OnBackgroundLight,
        surface = SurfaceLight,
        onSurface = OnSurfaceLight,
        surfaceVariant = SurfaceVariantLight,
        onSurfaceVariant = OnSurfaceVariantLight,
        outline = OutlineLight,
        outlineVariant = OutlineVariantLight,
        error = ErrorLight,
        onError = OnErrorLight,
        errorContainer = ErrorContainerLight,
        onErrorContainer = OnErrorContainerLight,
    )

@Composable
fun NutriVerseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep branded emerald/mint color scheme consistent across all devices by default
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
