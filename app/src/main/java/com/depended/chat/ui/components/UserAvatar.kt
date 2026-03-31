package com.depended.chat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun UserAvatar(
    username: String,
    avatarUrl: String?,
    userId: String?,
    hasAvatar: Boolean,
    avatarVersion: Long? = null,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val resolvedUrl = AvatarUrlResolver.resolve(
        avatarUrl = avatarUrl,
        userId = userId,
        hasAvatar = hasAvatar,
        avatarVersion = avatarVersion
    )
    val context = LocalContext.current

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(username.take(1).uppercase(), style = MaterialTheme.typography.titleLarge)

        if (resolvedUrl != null) {
            key(resolvedUrl) {
                val request = ImageRequest.Builder(context)
                    .data(resolvedUrl)
                    .memoryCacheKey(resolvedUrl)
                    .diskCacheKey(resolvedUrl)
                    .crossfade(true)
                    .build()

                AsyncImage(
                    model = request,
                    contentDescription = username,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                    error = null
                )
            }
        }
    }
}
