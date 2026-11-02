// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.theme.material

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle as MaterialKolorPaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.material3.dynamicColorScheme
import com.rosan.installer.domain.settings.model.preferences.theme.PaletteStyle
import com.rosan.installer.domain.settings.model.preferences.theme.ThemeColorSpec

@Stable
fun dynamicColorScheme(
    keyColor: Color,
    isDark: Boolean,
    style: PaletteStyle = PaletteStyle.TonalSpot,
    contrastLevel: Double = 0.0,
    colorSpec: ThemeColorSpec = ThemeColorSpec.SPEC_2025,
): ColorScheme {
    // Map local domain PaletteStyle to materialkolor's PaletteStyle
    val mkStyle = when (style) {
        PaletteStyle.TonalSpot -> MaterialKolorPaletteStyle.TonalSpot
        PaletteStyle.Neutral -> MaterialKolorPaletteStyle.Neutral
        PaletteStyle.Vibrant -> MaterialKolorPaletteStyle.Vibrant
        PaletteStyle.Expressive -> MaterialKolorPaletteStyle.Expressive
        PaletteStyle.Rainbow -> MaterialKolorPaletteStyle.Rainbow
        PaletteStyle.FruitSalad -> MaterialKolorPaletteStyle.FruitSalad
        PaletteStyle.Monochrome -> MaterialKolorPaletteStyle.Monochrome
        PaletteStyle.Fidelity -> MaterialKolorPaletteStyle.Fidelity
        PaletteStyle.Content -> MaterialKolorPaletteStyle.Content
        PaletteStyle.Cmf -> MaterialKolorPaletteStyle.Cmf()
    }

    val specVersion = when (style.effectiveSpec(colorSpec)) {
        ThemeColorSpec.SPEC_2026 -> ColorSpec.SpecVersion.SPEC_2026
        ThemeColorSpec.SPEC_2025 -> ColorSpec.SpecVersion.SPEC_2025
        ThemeColorSpec.SPEC_2021 -> ColorSpec.SpecVersion.SPEC_2021
    }

    // Generating a palette is expensive and the install dialog regenerates the scheme for every
    // package icon colour. Reuse the result for repeated dialogs instead of blocking the frame.
    val cacheKey = DynamicColorSchemeCacheKey(keyColor, isDark, style, contrastLevel, colorSpec)
    DynamicColorSchemeCache[cacheKey]?.let { return it }

    val scheme = dynamicColorScheme(
        seedColor = keyColor,
        isDark = isDark,
        style = mkStyle,
        contrastLevel = contrastLevel,
        specVersion = specVersion,
    )
    DynamicColorSchemeCache[cacheKey] = scheme
    return scheme
}

private data class DynamicColorSchemeCacheKey(
    val keyColor: Color,
    val isDark: Boolean,
    val style: PaletteStyle,
    val contrastLevel: Double,
    val colorSpec: ThemeColorSpec,
)

/**
 * Small LRU of previously generated palettes.
 *
 * Palette generation walks several HCT tonal palettes and is far too slow to repeat on the main
 * thread every time the install dialog opens for the same app icon colour.
 */
private object DynamicColorSchemeCache {
    private const val MAX_ENTRIES = 8

    private val entries = object : LinkedHashMap<DynamicColorSchemeCacheKey, ColorScheme>(
        MAX_ENTRIES,
        0.75f,
        true,
    ) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<DynamicColorSchemeCacheKey, ColorScheme>?,
        ): Boolean = size > MAX_ENTRIES
    }

    @Synchronized
    operator fun get(key: DynamicColorSchemeCacheKey): ColorScheme? = entries[key]

    @Synchronized
    operator fun set(key: DynamicColorSchemeCacheKey, value: ColorScheme) {
        entries[key] = value
    }
}

/**
 * Extension function to animate all colors inside a ColorScheme smoothly,
 * including the newly added fixed container roles in Material 3.
 */
@Composable
fun ColorScheme.animateAsState(): ColorScheme {
    // Helper function to reduce boilerplate for state animation
    @Composable
    fun animateColor(color: Color): Color = animateColorAsState(
        targetValue = color,
        animationSpec = spring(),
        label = "theme_color_animation",
    ).value

    return ColorScheme(
        primary = animateColor(primary),
        onPrimary = animateColor(onPrimary),
        primaryContainer = animateColor(primaryContainer),
        onPrimaryContainer = animateColor(onPrimaryContainer),
        inversePrimary = animateColor(inversePrimary),
        secondary = animateColor(secondary),
        onSecondary = animateColor(onSecondary),
        secondaryContainer = animateColor(secondaryContainer),
        onSecondaryContainer = animateColor(onSecondaryContainer),
        tertiary = animateColor(tertiary),
        onTertiary = animateColor(onTertiary),
        tertiaryContainer = animateColor(tertiaryContainer),
        onTertiaryContainer = animateColor(onTertiaryContainer),
        background = animateColor(background),
        onBackground = animateColor(onBackground),
        surface = animateColor(surface),
        onSurface = animateColor(onSurface),
        surfaceVariant = animateColor(surfaceVariant),
        onSurfaceVariant = animateColor(onSurfaceVariant),
        surfaceTint = animateColor(surfaceTint),
        inverseSurface = animateColor(inverseSurface),
        inverseOnSurface = animateColor(inverseOnSurface),
        error = animateColor(error),
        onError = animateColor(onError),
        errorContainer = animateColor(errorContainer),
        onErrorContainer = animateColor(onErrorContainer),
        outline = animateColor(outline),
        outlineVariant = animateColor(outlineVariant),
        scrim = animateColor(scrim),
        surfaceBright = animateColor(surfaceBright),
        surfaceDim = animateColor(surfaceDim),
        surfaceContainer = animateColor(surfaceContainer),
        surfaceContainerHigh = animateColor(surfaceContainerHigh),
        surfaceContainerHighest = animateColor(surfaceContainerHighest),
        surfaceContainerLow = animateColor(surfaceContainerLow),
        surfaceContainerLowest = animateColor(surfaceContainerLowest),

        // New fixed color roles for Material 3
        primaryFixed = animateColor(primaryFixed),
        primaryFixedDim = animateColor(primaryFixedDim),
        onPrimaryFixed = animateColor(onPrimaryFixed),
        onPrimaryFixedVariant = animateColor(onPrimaryFixedVariant),
        secondaryFixed = animateColor(secondaryFixed),
        secondaryFixedDim = animateColor(secondaryFixedDim),
        onSecondaryFixed = animateColor(onSecondaryFixed),
        onSecondaryFixedVariant = animateColor(onSecondaryFixedVariant),
        tertiaryFixed = animateColor(tertiaryFixed),
        tertiaryFixedDim = animateColor(tertiaryFixedDim),
        onTertiaryFixed = animateColor(onTertiaryFixed),
        onTertiaryFixedVariant = animateColor(onTertiaryFixedVariant),
    )
}
