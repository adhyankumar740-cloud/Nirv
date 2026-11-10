/**
 * Nirvana Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Reusable "Glass & Blur" building blocks.
 * Frosted look = translucent fill + light sheen gradient + thin white border.
 */

package com.nirvana.music.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.nirvana.music.ui.theme.GlassFill
import com.nirvana.music.ui.theme.GlassFillStrong
import com.nirvana.music.ui.theme.GlassOutline
import com.nirvana.music.ui.theme.GlassSheenBottom
import com.nirvana.music.ui.theme.GlassSheenTop

/** Frosted glass background + border. Put it after size/padding modifiers. */
fun Modifier.glass(
    shape: Shape = RoundedCornerShape(24.dp),
    strong: Boolean = false,
): Modifier = this
    .clip(shape)
    .background(if (strong) GlassFillStrong else GlassFill, shape)
    .background(
        Brush.verticalGradient(listOf(GlassSheenTop, GlassSheenBottom)),
        shape,
    )
    .border(1.dp, GlassOutline, shape)

/** A frosted card. Example: GlassCard { Text("Hello") } */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    strong: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .glass(shape, strong)
            .padding(contentPadding),
        content = content,
    )
}

/** Thin frosted border used by every glass surface. */
val GlassBorder = BorderStroke(1.dp, GlassOutline)

/** Card colors for the glass look. Use with `border = GlassBorder`. */
@Composable
fun glassCardColors(strong: Boolean = false): CardColors =
    CardDefaults.cardColors(
        containerColor = if (strong) GlassFillStrong else GlassFill,
        contentColor = MaterialTheme.colorScheme.onSurface,
    )

/** Dialogs and bottom sheets: frosted but still mostly opaque so text stays readable. */
@Composable
fun glassSheetColor(): Color =
    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f)

/** Top bars: see-through at rest, frosted once the list scrolls under them. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun glassTopAppBarColors(): TopAppBarColors =
    TopAppBarDefaults.topAppBarColors(
        containerColor = Color.Transparent,
        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f),
    )

/**
 * Soft "aurora" backdrop that sits behind the whole app so the glass layers
 * have something to frost. Uses theme colors, so custom seed colors still work.
 */
fun Modifier.auroraBackground(
    base: Color,
    glowA: Color,
    glowB: Color,
): Modifier = this
    .background(base)
    .drawBehind {
        drawRect(
            Brush.radialGradient(
                colors = listOf(glowA.copy(alpha = 0.20f), Color.Transparent),
                center = Offset(size.width * 0.12f, size.height * 0.06f),
                radius = size.maxDimension * 0.65f,
            ),
        )
        drawRect(
            Brush.radialGradient(
                colors = listOf(glowB.copy(alpha = 0.13f), Color.Transparent),
                center = Offset(size.width * 0.92f, size.height * 0.88f),
                radius = size.maxDimension * 0.6f,
            ),
        )
    }
