package com.enddream.tracing.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

internal fun DrawScope.paintBackdrop(dark: Boolean, scheme: ColorScheme) {
    val top = if (dark) Color(0xFF1C1E21) else Color(0xFFF4F5F7)
    val bottom = if (dark) Color(0xFF121316) else Color(0xFFE8EAEC)
    drawRect(Brush.verticalGradient(listOf(top, bottom)))
}

@Composable
fun BackdropHost(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val dark = LocalDark.current
    val scheme = MaterialTheme.colorScheme
    val barBackdrop = rememberLayerBackdrop()
    val glassBackdrop = rememberCanvasBackdrop {
        paintBackdrop(dark, scheme)
    }
    CompositionLocalProvider(
        LocalBarBackdrop provides barBackdrop,
        LocalGlassBackdrop provides glassBackdrop
    ) {
        Box(
            modifier
                .fillMaxSize()
                .drawBehind { paintBackdrop(dark, scheme) }
        ) {
            content()
        }
    }
}
