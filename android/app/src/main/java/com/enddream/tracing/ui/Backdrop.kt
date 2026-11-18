package com.enddream.tracing.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

internal fun DrawScope.paintBackdrop(dark: Boolean, scheme: ColorScheme) {
    val top = if (dark) Color(0xFF1C1E21) else Color(0xFFF4F5F7)
    val bottom = if (dark) Color(0xFF121316) else Color(0xFFE8EAEC)
    drawRect(Brush.verticalGradient(listOf(top, bottom)))
}

private class WindowBackdrop(
    private val top: Color,
    private val bottom: Color,
    private val heightPx: Float
) : Backdrop {

    override val isCoordinatesDependent: Boolean = true

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?
    ) {
        val y = coordinates?.positionInWindow()?.y ?: 0f
        translate(left = 0f, top = -y) {
            drawRect(Brush.verticalGradient(listOf(top, bottom), startY = 0f, endY = heightPx))
        }
    }
}

@Composable
fun BackdropHost(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val dark = LocalDark.current
    val scheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val height = LocalConfiguration.current.screenHeightDp
    val barBackdrop = rememberLayerBackdrop()
    val glassBackdrop = remember(dark, height) {
        val top = if (dark) Color(0xFF1C1E21) else Color(0xFFF4F5F7)
        val bottom = if (dark) Color(0xFF121316) else Color(0xFFE8EAEC)
        WindowBackdrop(top, bottom, with(density) { height.dp.toPx() })
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
