package com.depended.chat.ui.account

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.depended.chat.ui.components.UserAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(viewModel: AccountViewModel, onBack: () -> Unit, onLoggedOut: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val bytes = context.readBytes(uri)
            val mime = context.contentResolver.getType(uri) ?: "image/*"
            if (bytes != null) {
                viewModel.uploadAvatar(bytes, mime)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Аккаунт") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val profile = state.profile
            if (state.loading && profile == null) {
                CircularProgressIndicator()
            }

            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.updateSuccess?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

            UserAvatar(
                username = profile?.username.orEmpty(),
                avatarUrl = profile?.avatarUrl,
                avatarBase64 = profile?.avatarBase64,
                size = 84.dp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = !state.uploadingAvatar,
                    onClick = {
                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                ) { Text(if (profile?.avatarUrl != null || profile?.avatarBase64 != null) "Заменить фото" else "Загрузить фото") }

                if (profile?.avatarUrl != null || profile?.avatarBase64 != null) {
                    Button(enabled = !state.uploadingAvatar, onClick = viewModel::deleteAvatar) {
                        Text("Удалить")
                    }
                }
            }

            Text("Username", style = MaterialTheme.typography.labelMedium)
            Text(profile?.username ?: "—", style = MaterialTheme.typography.titleLarge)
            Text("Last seen", style = MaterialTheme.typography.labelMedium)
            Text(profile?.lastSeen ?: "—")

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Описание", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = state.bioInput,
                        onValueChange = viewModel::onBioChanged,
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        placeholder = { Text("Расскажите о себе") }
                    )
                    Button(enabled = !state.savingBio, onClick = viewModel::saveBio) {
                        Text(if (state.savingBio) "Сохраняем..." else "Сохранить bio")
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Button(onClick = { viewModel.logout(onLoggedOut) }) {
                Text("Выйти")
            }
        }
    }
}

private fun Context.readBytes(uri: Uri): ByteArray? = runCatching {
    contentResolver.openInputStream(uri)?.use { it.readBytes() }
}.getOrNull()
