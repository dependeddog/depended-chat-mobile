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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.depended.chat.ui.components.UserAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(viewModel: AccountViewModel, onBack: () -> Unit, onLoggedOut: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var bioDraft by remember(state.profile?.bio) { mutableStateOf(state.profile?.bio.orEmpty()) }
    var editBioDialogOpen by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val bytes = context.readCompressedWebp(
                uri = uri,
                maxSidePx = 512,
                quality = 80
            )

            if (bytes != null) {
                viewModel.uploadAvatar(
                    bytes = bytes,
                    mimeType = "image/webp"
                )
            }
        }
    }

    if (editBioDialogOpen) {
        AlertDialog(
            onDismissRequest = { editBioDialogOpen = false },
            title = { Text("Изменить описание") },
            text = {
                OutlinedTextField(
                    value = bioDraft,
                    onValueChange = {
                        bioDraft = it
                        viewModel.onBioChanged(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    placeholder = { Text("Расскажите о себе") }
                )
            },
            confirmButton = {
                Button(
                    enabled = !state.savingBio,
                    onClick = {
                        viewModel.saveBio()
                        editBioDialogOpen = false
                    }
                ) { Text(if (state.savingBio) "Сохраняем..." else "Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { editBioDialogOpen = false }) { Text("Отмена") }
            }
        )
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
                userId = profile?.id,
                hasAvatar = profile?.hasAvatar == true,
                avatarVersion = profile?.avatarVersion,
                size = 84.dp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = !state.uploadingAvatar,
                    onClick = {
                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                ) { Text(if (profile?.hasAvatar == true) "Заменить фото" else "Загрузить фото") }

                if (profile?.hasAvatar == true) {
                    Button(enabled = !state.uploadingAvatar, onClick = viewModel::deleteAvatar) {
                        Text("Удалить")
                    }
                }
            }

            Text("Username", style = MaterialTheme.typography.labelMedium)
            Text(profile?.username ?: "—", style = MaterialTheme.typography.titleLarge)

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Описание", style = MaterialTheme.typography.titleMedium)
                    Text(profile?.bio?.ifBlank { "Описание не указано" } ?: "Описание не указано")
                    Button(onClick = {
                        bioDraft = profile?.bio.orEmpty()
                        viewModel.onBioChanged(bioDraft)
                        editBioDialogOpen = true
                    }) {
                        Text("Изменить описание")
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


private fun Context.resolveImageMimeType(uri: Uri): String {
    val direct = contentResolver.getType(uri)
    if (!direct.isNullOrBlank() && direct != "image/*") return direct

    val path = uri.toString().lowercase()
    return when {
        path.endsWith(".png") -> "image/png"
        path.endsWith(".webp") -> "image/webp"
        else -> "image/jpeg"
    }
}
