package com.depended.chat.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage

@Composable
fun UserAvatar(
    username: String,
    avatarUrl: String?,
    avatarBase64: String?,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val decoded = remember(avatarBase64) { avatarBase64?.decodeBase64Avatar() }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        when {
            decoded != null -> {
                androidx.compose.foundation.Image(
                    bitmap = decoded.asImageBitmap(),
                    contentDescription = username,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop
                )
            }

            !avatarUrl.isNullOrBlank() -> {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = username,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop
                )
            }

            else -> {
                Text(username.take(1).uppercase(), style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

private fun String.decodeBase64Avatar() = runCatching {
    val payload = substringAfter("base64,", this)
    val bytes = Base64.decode(payload, Base64.DEFAULT)
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}.getOrNull()
