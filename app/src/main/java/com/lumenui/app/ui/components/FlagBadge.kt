package com.lumenui.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import dev.carlsen.flagkit.FlagKit

/**
 * Vector country flag from kmp-flagkit.
 *
 * Emoji flags were the obvious choice, but ROMs strip individual flags from the emoji font
 * (the test device renders Taiwan as nothing at all), so flags are drawn from vectors.
 */
fun flagVector(region: String): ImageVector? {
    if (region.length != 2) return null
    return runCatching { FlagKit.getFlag(countryCode = region) }.getOrNull()
}

@Composable
fun FlagBadge(vector: ImageVector, modifier: Modifier = Modifier) {
    Image(
        imageVector = vector,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(width = 21.dp, height = 14.dp)
            .clip(RoundedCornerShape(2.dp))
    )
}
