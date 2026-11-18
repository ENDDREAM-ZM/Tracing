package com.enddream.tracing.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
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
    blur: Boolean = false,
    blurRadius: Dp = 14.dp,
    tint: Color = glassTint(),
    content: @Composable BoxScope.() -> Unit
) {
    val dark = LocalDark.current
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
                if (blur) blur(blurRadius.toPx())
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
            .background(if (dark) Color(0xFF23262A) else Color(0xFFFFFFFF))
            .background(tint)
    }
    Box(base, content = content)
}

@Composable
internal fun glassTint(): Color {
    val dark = LocalDark.current
    return if (dark) Color(0xFF16181B).copy(alpha = 0.44f) else Color(0xFFFFFFFF).copy(alpha = 0.40f)
}

@Composable
internal fun glassStroke(): Color {
    val dark = LocalDark.current
    return if (dark) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.70f)
}

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    shadow: Boolean = true,
    tint: Color = glassTint(),
    content: @Composable BoxScope.() -> Unit
) {
    glassSurface(modifier, shape, LocalGlassBackdrop.current, shadow, tint = tint, content = content)
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
    shape: Shape = RoundedCornerShape(22.dp),
    content: @Composable BoxScope.() -> Unit
) {
    glassSurface(modifier, shape, LocalGlassBackdrop.current, false, content = content)
}

@Composable
fun GlassBar(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(30.dp),
    shadow: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    glassSurface(modifier, shape, LocalBarBackdrop.current, shadow, blur = true, blurRadius = 10.dp, content = content)
}

@Composable
fun GlassChip(
    modifier: Modifier = Modifier,
    corner: Dp = 10.dp,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val dark = LocalDark.current
    val primary = MaterialTheme.colorScheme.primary
    val fill = if (selected) primary.copy(alpha = if (dark) 0.34f else 0.22f) else glassTint()
    val stroke = if (selected) primary.copy(alpha = if (dark) 0.55f else 0.45f) else glassStroke()
    Box(
        modifier
            .clip(RoundedCornerShape(corner))
            .background(fill)
            .drawBehind {
                drawRect(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = if (dark) 0.05f else 0.12f), Color.Transparent)
                    )
                )
                drawRoundRect(
                    color = stroke,
                    topLeft = Offset(0f, 0f),
                    size = Size(size.width, size.height),
                    cornerRadius = CornerRadius(corner.toPx(), corner.toPx()),
                    style = Stroke(1.dp.toPx())
                )
            }
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ) else Modifier
            ),
        content = content
    )
}

class GlassOverlayEntry {
    var onDismiss: () -> Unit by mutableStateOf({})
    var title: String? by mutableStateOf(null)
    var subtitle: String? by mutableStateOf(null)
    var buttons: @Composable () -> Unit by mutableStateOf({})
    var content: @Composable () -> Unit by mutableStateOf({})
}

class GlassOverlayState {
    var entry: GlassOverlayEntry? by mutableStateOf(null)
}

val LocalOverlayBackdrop = compositionLocalOf<Backdrop?> { null }
val LocalGlassOverlay = compositionLocalOf { GlassOverlayState() }

@Composable
fun GlassDialog(
    onDismissRequest: () -> Unit,
    title: String? = null,
    subtitle: String? = null,
    buttons: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    val host = LocalGlassOverlay.current
    val entry = remember { GlassOverlayEntry() }
    SideEffect {
        entry.onDismiss = onDismissRequest
        entry.title = title
        entry.subtitle = subtitle
        entry.buttons = buttons
        entry.content = content
    }
    DisposableEffect(host, entry) {
        host.entry = entry
        onDispose { if (host.entry === entry) host.entry = null }
    }
}

@Composable
internal fun GlassOverlayLayer() {
    val host = LocalGlassOverlay.current
    val e = host.entry ?: return
    val dark = LocalDark.current
    androidx.activity.compose.BackHandler { e.onDismiss() }
    val noIndication = remember { MutableInteractionSource() }
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = if (dark) 0.38f else 0.18f))
            .clickable(
                interactionSource = noIndication,
                indication = null,
                onClick = { e.onDismiss() }
            )
            .padding(horizontal = 26.dp, vertical = 36.dp),
        contentAlignment = Alignment.Center
    ) {
        val panelMax = maxHeight
        glassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = noIndication,
                    indication = null,
                    onClick = {}
                ),
            shape = RoundedCornerShape(28.dp),
            backdrop = LocalOverlayBackdrop.current,
            shadow = true,
            blur = true,
            blurRadius = 10.dp,
            tint = if (dark) Color(0xFF16181B).copy(alpha = 0.56f)
            else Color(0xFFFFFFFF).copy(alpha = 0.58f)
        ) {
            Column(
                Modifier
                    .padding(horizontal = 22.dp, vertical = 20.dp)
                    .heightIn(max = panelMax)
            ) {
                if (e.title != null) {
                    Text(
                        e.title.orEmpty(),
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    if (e.subtitle != null) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            e.subtitle.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    e.content()
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    e.buttons()
                }
            }
        }
    }
}

@Composable
fun GlassDialogButton(text: String, onClick: () -> Unit) {
    glassSurface(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        backdrop = null,
        shadow = false,
        tint = MaterialTheme.colorScheme.primary.copy(
            alpha = if (LocalDark.current) 0.34f else 0.18f
        )
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp)
        )
    }
}
