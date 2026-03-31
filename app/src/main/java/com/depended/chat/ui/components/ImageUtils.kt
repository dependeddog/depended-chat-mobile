package com.depended.chat.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import java.io.ByteArrayOutputStream
import kotlin.math.max

fun Context.readCompressedWebp(
    uri: Uri,
    maxSidePx: Int = 512,
    quality: Int = 80
): ByteArray? {
    return runCatching {
        val originalBytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return null

        val bitmap = BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.size)
            ?: return null

        val resized = resizeBitmapPreservingRatio(bitmap, maxSidePx)

        val output = ByteArrayOutputStream()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            resized.compress(Bitmap.CompressFormat.WEBP_LOSSY, quality, output)
        } else {
            @Suppress("DEPRECATION")
            resized.compress(Bitmap.CompressFormat.WEBP, quality, output)
        }

        if (resized !== bitmap) {
            resized.recycle()
        }
        bitmap.recycle()

        output.toByteArray()
    }.getOrNull()
}

private fun resizeBitmapPreservingRatio(
    bitmap: Bitmap,
    maxSidePx: Int
): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    val largestSide = max(width, height)

    if (largestSide <= maxSidePx) {
        return bitmap
    }

    val scale = maxSidePx.toFloat() / largestSide.toFloat()
    val newWidth = (width * scale).toInt().coerceAtLeast(1)
    val newHeight = (height * scale).toInt().coerceAtLeast(1)

    return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
}