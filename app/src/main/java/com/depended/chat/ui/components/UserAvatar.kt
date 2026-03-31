package com.depended.chat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter

@Composable
fun UserAvatar(
    username: String,
    avatarUrl: String?,
    userId: String?,
    hasAvatar: Boolean,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val resolvedUrl = AvatarUrlResolver.resolve(avatarUrl = avatarUrl, userId = userId, hasAvatar = hasAvatar)
    val painter = rememberAsyncImagePainter(model = resolvedUrl)
    val state by painter.state
    val showImage = resolvedUrl != null && state !is AsyncImagePainter.State.Error

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (showImage) {
            AsyncImage(
                model = resolvedUrl,
                contentDescription = username,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(username.take(1).uppercase(), style = MaterialTheme.typography.titleLarge)
        }
    }
}
