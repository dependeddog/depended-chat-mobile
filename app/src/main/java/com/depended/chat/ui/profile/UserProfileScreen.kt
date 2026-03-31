package com.depended.chat.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.depended.chat.ui.components.UserAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(viewModel: UserProfileViewModel, userId: String, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(userId) {
        viewModel.load(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Профиль") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when {
                state.loading && state.profile == null -> CircularProgressIndicator()
                state.error != null && state.profile == null -> {
                    Text(state.error!!)
                    Button(onClick = { viewModel.load(userId) }) { Text("Повторить") }
                }
                state.profile != null -> {
                    val profile = state.profile!!
                    UserAvatar(
                        username = profile.username,
                        avatarUrl = profile.avatarUrl,
                        userId = profile.id,
                        hasAvatar = profile.hasAvatar,
                        size = 92.dp
                    )
                    Text("Username: ${profile.username}")
                    Text("Bio: ${profile.bio?.takeIf { it.isNotBlank() } ?: "Описание не указано"}")
                    Text("Last seen: ${profile.lastSeenAt ?: "—"}")
                }
            }
        }
    }
}
