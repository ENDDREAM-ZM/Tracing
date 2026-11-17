package com.enddream.tracing.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.dp

@Composable
internal fun glassContainer(
    modifier: Modifier,
    shape: Shape,
    topAlpha: Float,
    bottomAlpha: Float,
    rimAlpha: Float,
    content: @Composable BoxScope.() -> Unit
) {
    val dark = LocalDark.current
    val scheme = MaterialTheme.colorScheme
    val backdrop = LocalBackdrop.current
    var origin by remember { mutableStateOf(Offset.Zero) }
    val frosted = backdrop != null && backdrop.blurred

    val a1 = if (frosted) topAlpha else 0.95f
    val a2 = if (frosted) bottomAlpha else 0.91f
    val tintTop = if (dark) Color(0xFF2F3841).copy(alpha = a1)
    else Color(0xFFFFFFFF).copy(alpha = a1)
    val tintBottom = if (dark) Color(0xFF141A20).copy(alpha = a2)
    else Color(0xFFFFFFFF).copy(alpha = a2 * 0.90f)

    val sheen = if (dark) 0.16f else 0.55f
    val shade = if (dark) 0.26f else 0.08f
    val rimTop = if (dark) Color.White.copy(alpha = rimAlpha * 0.70f)
    else Color.White.copy(alpha = 1f)
    val rimSide = if (dark) Color.White.copy(alpha = rimAlpha * 0.30f)
    else Color.White.copy(alpha = 0.72f)
    val rimBottom = if (dark) Color.White.copy(alpha = rimAlpha * 0.10f)
    else Color.White.copy(alpha = 0.30f)

    Box(
        modifier = modifier
            .onGloballyPositioned { origin = it.positionInWindow() }
            .shadow(
                elevation = if (dark) 14.dp else 16.dp,
                shape = shape,
                ambientColor = if (dark) Color.Black.copy(alpha = 0.55f)
                else scheme.primary.copy(alpha = 0.18f),
                spotColor = if (dark) Color.Black.copy(alpha = 0.75f)
                else scheme.primary.copy(alpha = 0.24f)
            )
            .clip(shape)
            .drawBehind {
                paintGlassBackdrop(backdrop, dark, scheme, origin.x, origin.y)

                drawRect(Brush.verticalGradient(listOf(tintTop, tintBottom)))

                drawRect(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = sheen),
                        0.18f to Color.White.copy(alpha = sheen * 0.36f),
                        0.46f to Color.Transparent,
                        0.80f to Color.Transparent,
                        1f to Color.Black.copy(alpha = shade)
                    )
                )

                drawRect(
                    Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = sheen * 0.95f), Color.Transparent),
                        center = Offset(size.width * 0.12f, -size.height * 0.10f),
                        radius = size.width * 0.62f
                    )
                )

                drawRect(
                    Brush.horizontalGradient(
                        0f to Color.White.copy(alpha = sheen * 0.30f),
                        0.12f to Color.Transparent,
                        0.88f to Color.Transparent,
                        1f to Color.White.copy(alpha = sheen * 0.22f)
                    )
                )

                val edge = 2.dp.toPx()
                drawRect(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = if (dark) 0.10f else 0.28f), Color.Transparent),
                        startY = 0f,
                        endY = edge * 2.4f
                    ),
                    size = Size(size.width, edge * 2.4f)
                )
            }
            .border(
                BorderStroke(
                    1.dp,
                    Brush.verticalGradient(listOf(rimTop, rimSide, rimBottom))
                ),
                shape
            ),
        content = content
    )
}

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    content: @Composable BoxScope.() -> Unit
) {
    glassContainer(
        modifier = modifier,
        shape = shape,
        topAlpha = 0.30f,
        bottomAlpha = 0.22f,
        rimAlpha = 0.78f,
        content = content
    )
}

@Composable
fun GlassCapsule(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    glassContainer(
        modifier = modifier,
        shape = RoundedCornerShape(32.dp),
        topAlpha = 0.36f,
        bottomAlpha = 0.26f,
        rimAlpha = 0.95f,
        content = content
    )
}

@Composable
fun GlassFrosted(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    content: @Composable BoxScope.() -> Unit
) {
    glassContainer(
        modifier = modifier,
        shape = shape,
        topAlpha = 0.90f,
        bottomAlpha = 0.86f,
        rimAlpha = 0.55f,
        content = content
    )
}

@Composable
fun GlassPill(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    glassContainer(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        topAlpha = 0.34f,
        bottomAlpha = 0.24f,
        rimAlpha = 0.88f,
        content = content
    )
}
