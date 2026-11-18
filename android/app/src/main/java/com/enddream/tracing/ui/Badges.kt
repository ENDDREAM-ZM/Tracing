package com.enddream.tracing.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val FirstColor = Color(0xFF2FBF5C)
val LastColor = Color(0xFFE8453C)
val FavRed = Color(0xFFE8453C)

@Composable
fun TimeBadge(first: Boolean, value: String, fontSize: Int = 10) {
    val bg = if (first) FirstColor else LastColor
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .background(bg, RoundedCornerShape(4.dp))
                .padding(horizontal = 3.dp, vertical = 1.dp)
        ) {
            Text(
                if (first) "首" else "末",
                style = MaterialTheme.typography.labelSmall,
                fontSize = (fontSize - 1).sp,
                color = Color.White
            )
        }
        Spacer(Modifier.width(3.dp))
        Text(
            value,
            style = MaterialTheme.typography.labelSmall,
            fontSize = fontSize.sp,
            color = TextMuted
        )
    }
}

@Composable
fun FavButton(
    fav: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showLabel: Boolean = false
) {
    if (showLabel) {
        GlassChip(modifier = modifier, corner = 12.dp, onClick = onClick) {
            Row(
                Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "收藏",
                    modifier = Modifier.size(15.dp),
                    tint = if (fav) FavRed else TextMuted.copy(alpha = 0.65f)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "收藏",
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 11.sp,
                    color = if (fav) FavRed else TextSecondary
                )
            }
        }
    } else {
        Row(
            modifier = modifier
                .background(
                    if (fav) FavRed.copy(alpha = 0.12f) else Color.Transparent,
                    RoundedCornerShape(12.dp)
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = "收藏",
                modifier = Modifier.size(17.dp),
                tint = if (fav) FavRed else TextMuted.copy(alpha = 0.65f)
            )
        }
    }
}
