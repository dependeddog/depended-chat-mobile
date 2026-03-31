package com.depended.chat.ui.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.depended.chat.domain.model.Message
import com.depended.chat.domain.model.MessageStatus
import com.depended.chat.ui.theme.BubbleMine
import com.depended.chat.ui.theme.BubbleOther

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel, chatId: String, onBack: () -> Unit, onChatDeleted: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    var initialScrollDone by remember(chatId) { mutableStateOf(false) }
    var selectedMessage by remember { mutableStateOf<Message?>(null) }
    var messageToEdit by remember { mutableStateOf<Message?>(null) }
    var messageToDelete by remember { mutableStateOf<Message?>(null) }
    var chatMenuExpanded by remember { mutableStateOf(false) }
    var showDeleteChatConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(chatId) {
        viewModel.init(chatId)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ChatUiEvent.Error -> snackbarHostState.showSnackbar(event.message)
                ChatUiEvent.ChatDeleted -> onChatDeleted()
            }
        }
    }

    LaunchedEffect(state.loading, state.messages.size) {
        if (!state.loading && state.messages.isNotEmpty() && !initialScrollDone) {
            listState.scrollToItem(state.messages.lastIndex)
            initialScrollDone = true
        }
    }

    LaunchedEffect(state.messages.size) {
        if (initialScrollDone && state.messages.isNotEmpty()) {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
            val nearBottom = lastVisible == null || lastVisible >= state.messages.lastIndex - 2

            if (nearBottom) {
                listState.animateScrollToItem(state.messages.lastIndex)
            }
        }
    }

    if (messageToEdit != null) {
        EditMessageDialog(
            initialText = messageToEdit!!.text,
            loading = state.editingInProgress,
            onDismiss = { if (!state.editingInProgress) messageToEdit = null },
            onSave = { newText ->
                viewModel.updateMessage(
                    messageId = messageToEdit!!.id,
                    originalText = messageToEdit!!.text,
                    newText = newText
                )
                if (newText.trim() != messageToEdit!!.text && newText.trim().isNotBlank()) {
                    messageToEdit = null
                }
            }
        )
    }

    if (messageToDelete != null) {
        AlertDialog(
            onDismissRequest = { if (!state.deleteMessageInProgress) messageToDelete = null },
            title = { Text("Удалить сообщение?") },
            text = { Text("Это действие нельзя отменить.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMessage(messageToDelete!!.id)
                        messageToDelete = null
                    },
                    enabled = !state.deleteMessageInProgress
                ) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(
                    onClick = { messageToDelete = null },
                    enabled = !state.deleteMessageInProgress
                ) { Text("Отмена") }
            }
        )
    }

    if (showDeleteChatConfirm) {
        AlertDialog(
            onDismissRequest = { if (!state.deleteChatInProgress) showDeleteChatConfirm = false },
            title = { Text("Удалить чат?") },
            text = { Text("После удаления чат будет недоступен.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteChat()
                        showDeleteChatConfirm = false
                    },
                    enabled = !state.deleteChatInProgress
                ) {
                    if (state.deleteChatInProgress) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.padding(end = 8.dp))
                    }
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteChatConfirm = false }, enabled = !state.deleteChatInProgress) {
                    Text("Отмена")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(state.companionName.ifBlank { "Chat" }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { chatMenuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Действия")
                        }
                        DropdownMenu(expanded = chatMenuExpanded, onDismissRequest = { chatMenuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("Удалить чат") },
                                onClick = {
                                    chatMenuExpanded = false
                                    showDeleteChatConfirm = true
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .imePadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = state.input,
                    onValueChange = viewModel::onInput,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Сообщение") },
                    shape = RectangleShape,
                    singleLine = true
                )
                IconButton(onClick = viewModel::send) {
                    Icon(Icons.AutoMirrored.Filled.Send, null)
                }
            }
        }
    ) { pad ->
        if (state.error != null && state.messages.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(pad),
                contentAlignment = Alignment.Center
            ) {
                Text(state.error!!)
            }
            return@Scaffold
        }

        if (state.messages.isEmpty() && !state.loading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(pad),
                contentAlignment = Alignment.Center
            ) {
                Text("Пока нет сообщений")
            }
            return@Scaffold
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.messages, key = { it.id }) { msg ->
                MessageBubble(msg, onLongPress = {
                    if (msg.isMine) selectedMessage = msg
                })
            }
        }
    }

    if (selectedMessage != null) {
        AlertDialog(
            onDismissRequest = { selectedMessage = null },
            title = { Text("Действия с сообщением") },
            text = { Text("Выберите действие") },
            confirmButton = {
                TextButton(onClick = {
                    messageToEdit = selectedMessage
                    selectedMessage = null
                }) { Text("Изменить") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        messageToDelete = selectedMessage
                        selectedMessage = null
                    }) { Text("Удалить") }
                    TextButton(onClick = { selectedMessage = null }) { Text("Отмена") }
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(msg: Message, onLongPress: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (msg.isMine) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (msg.isMine) BubbleMine else BubbleOther,
            shape = MaterialTheme.shapes.large,
            tonalElevation = 1.dp,
            modifier = Modifier.combinedClickable(onClick = {}, onLongClick = onLongPress)
        ) {
            Text(msg.text, Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
        }
        Row(modifier = Modifier.padding(top = 2.dp, end = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (msg.isMine) {
                Text(
                    text = if (msg.status == MessageStatus.READ) "Прочитано" else "Отправлено",
                    style = MaterialTheme.typography.labelSmall
                )
            }
            if (msg.isEdited) {
                Text(
                    text = "изменено",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun EditMessageDialog(
    initialText: String,
    loading: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var value by remember(initialText) { mutableStateOf(initialText) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Изменить сообщение") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(value) }, enabled = !loading) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !loading) {
                Text("Отмена")
            }
        }
    )
}
