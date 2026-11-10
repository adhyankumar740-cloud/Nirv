/**
 * Nirvana Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * "Glass & Blur" palette: deep slate base, frosted surfaces,
 * aqua accent with a warm peach highlight.
 */

package com.nirvana.music.ui.theme

import androidx.compose.ui.graphics.Color

// Base
val SlateDarkBackground = Color(0xFF12151B)
val SlateDarkSurface = Color(0xFF1A1E26)
val SlateDarkSurfaceVariant = Color(0xFF262C37)

// Surface containers (cards, sheets, nav bar) — layered like frosted glass
val GlassContainerLowest = Color(0xFF0E1116)
val GlassContainerLow = Color(0xFF181C24)
val GlassContainer = Color(0xFF1E232C)
val GlassContainerHigh = Color(0xFF252B35)
val GlassContainerHighest = Color(0xFF2D3440)

// Accents (names kept so existing code keeps compiling)
val NeonBluePrimary = Color(0xFF6FE3DD)      // aqua
val NeonPurpleSecondary = Color(0xFFFFC98F)  // warm peach
val ElectricPinkAccent = Color(0xFFFF8F7A)   // coral
val OnAccentDark = Color(0xFF0B1A1A)

val SlateOnBackground = Color(0xFFF3F6F9)
val SlateOnSurface = Color(0xFFFFFFFF)
val MutedTextGray = Color(0xFFA9B1BD)
val GlassOutline = Color(0x29FFFFFF)         // 16% white border
val GlassOutlineVariant = Color(0x1AFFFFFF)  // 10% white

// Glass fills (use with GlassSurface / Modifier.glass)
val GlassFill = Color(0x14FFFFFF)            // 8% white
val GlassFillStrong = Color(0x24FFFFFF)      // 14% white
val GlassSheenTop = Color(0x33FFFFFF)
val GlassSheenBottom = Color(0x05FFFFFF)
