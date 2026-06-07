package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = GlowingGold,
    secondary = WarmAmberAccent,
    tertiary = DeepSkyNavy,
    background = SacredNightBlue,
    surface = CathedralChamber,
    onPrimary = SacredNightBlue,
    onSecondary = SacredNightBlue,
    onTertiary = SacredNightBlue,
    onBackground = CelestialAlabaster,
    onSurface = CelestialAlabaster,
    outline = BlueGoldBorder
)

private val LightColorScheme = lightColorScheme(
    primary = RoyalBlueBrand,
    secondary = GoldCross,
    tertiary = DarkGoldHighlight,
    background = SereneWhitePages,
    surface = CathedralIvory,
    onPrimary = SereneWhitePages,
    onSecondary = SereneWhitePages,
    onTertiary = SereneWhitePages,
    onBackground = DeepNavyText,
    onSurface = DeepNavyText,
    outline = LightIvoryBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Turn off by default to strictly enforce our beautiful design theme!
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
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
