package com.enddream.tracing.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow

val LocalBarBackdrop = compositionLocalOf<LayerBackdrop?> { null }
val LocalGlassBackdrop = compositionLocalOf<Backdrop?> { null }

@Composable
internal fun glassSurface(
    modifier: Modifier,
    shape: Shape,
    backdrop: Backdrop?,
    shadow: Boolean,
    highlightAlpha: Float? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val dark = LocalDark.current
    val tint = if (dark) Color(0xFF16181B).copy(alpha = 0.52f)
    else Color(0xFFFFFFFF).copy(alpha = 0.52f)
    val shadowBlock: (() -> Shadow?)? = if (shadow) {
        {
            Shadow(
                radius = 16.dp,
                color = if (dark) Color.Black.copy(alpha = 0.42f)
                else Color.Black.copy(alpha = 0.10f)
            )
        }
    } else null
    val base = if (backdrop != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                blur(24.dp.toPx())
            },
            highlight = highlightAlpha?.let { a -> { Highlight.Default.copy(alpha = a) } }
                ?: {
                    Highlight.Default.copy(alpha = if (dark) 0.30f else 0.62f)
                },
            shadow = shadowBlock,
            onDrawSurface = {
                drawRect(tint)
            }
        )
    } else {
        modifier
            .then(if (shadow) Modifier.shadow(14.dp, shape) else Modifier)
            .clip(shape)
            .background(
                if (dark) Color(0xFF23262A).copy(alpha = 0.96f)
                else Color(0xFFFFFFFF).copy(alpha = 0.96f)
            )
    }
    Box(base, content = content)
}

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    shadow: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    glassSurface(modifier, shape, LocalGlassBackdrop.current, shadow, content = content)
}

@Composable
fun GlassFrosted(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    content: @Composable BoxScope.() -> Unit
) {
    glassSurface(modifier, shape, LocalGlassBackdrop.current, true, content = content)
}

@Composable
fun GlassPill(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    glassSurface(modifier, RoundedCornerShape(22.dp), LocalGlassBackdrop.current, false, content = content)
}

@Composable
fun GlassBar(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(30.dp),
    shadow: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    glassSurface(modifier, shape, LocalBarBackdrop.current, shadow, content = content)
}
