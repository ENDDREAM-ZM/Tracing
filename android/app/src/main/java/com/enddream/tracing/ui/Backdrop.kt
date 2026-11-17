package com.enddream.tracing.ui

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer

class Backdrop(val layer: GraphicsLayer, val blurred: Boolean)

val LocalBackdrop = compositionLocalOf<Backdrop?> { null }

@Composable
fun rememberBackdrop(): Backdrop {
    val layer = rememberGraphicsLayer()
    val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    if (canBlur) {
        layer.renderEffect = BlurEffect(radiusX = 20f, radiusY = 20f, edgeTreatment = TileMode.Clamp)
    }
    return remember(layer, canBlur) { Backdrop(layer, canBlur) }
}

internal fun DrawScope.paintBackdrop(dark: Boolean, scheme: ColorScheme) {
    val top = if (dark) Color(0xFF1C1E21) else Color(0xFFF4F5F7)
    val bottom = if (dark) Color(0xFF121316) else Color(0xFFE8EAEC)
    drawRect(Brush.verticalGradient(listOf(top, bottom)))
}

internal fun DrawScope.paintGlassBackdrop(
    backdrop: Backdrop?,
    dark: Boolean,
    scheme: ColorScheme,
    originX: Float,
    originY: Float
) {
    if (backdrop != null && backdrop.blurred) {
        translate(-originX, -originY) {
            drawLayer(backdrop.layer)
        }
    } else {
        paintBackdrop(dark, scheme)
    }
}

@Composable
fun BackdropHost(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val backdrop = rememberBackdrop()
    val dark = LocalDark.current
    val scheme = MaterialTheme.colorScheme
    CompositionLocalProvider(LocalBackdrop provides backdrop) {
        Box(
            modifier
                .fillMaxSize()
                .drawBehind {
                    paintBackdrop(dark, scheme)
                    backdrop.layer.record { paintBackdrop(dark, scheme) }
                }
        ) {
            content()
        }
    }
}
