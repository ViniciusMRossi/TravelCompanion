package com.travelcompanion.app.design

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext

/**
 * Loads packaged trip photography straight from the APK assets.
 *
 * Trip images are local and small in number, so no image-loading library is
 * pulled in. A missing binary returns null and the caller falls back to the
 * approved striped placeholder — content production is not a runtime error.
 */
@Composable
fun rememberPackagedImage(assetPath: String?): ImageBitmap? {
    val context = LocalContext.current
    return remember(assetPath) {
        if (assetPath == null) {
            null
        } else {
            runCatching {
                context.assets.open(assetPath).use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
            }.getOrNull()
        }
    }
}
