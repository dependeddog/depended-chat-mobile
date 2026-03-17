package com.depended.chat.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.depended.chat.domain.model.MessageStatus
import com.depended.chat.ui.theme.BubbleMine
import com.depended.chat.ui.theme.BubbleOther

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel, chatId: String, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(chatId) { viewModel.init(chatId) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(state.companionName.ifBlank { "Chat" }) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
        )
    }, bottomBar = {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = state.input,
                onValueChange = viewModel::onInput,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Сообщение") }
            )
            IconButton(onClick = viewModel::send) { Icon(Icons.AutoMirrored.Filled.Send, null) }
        }
    }) { pad ->
        if (state.messages.isEmpty() && !state.loading) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) { Text("Пока нет сообщений") }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad).padding(horizontal = 10.dp),
            reverseLayout = false,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.messages, key = { it.id }) { msg ->
                val isMine = msg.status == MessageStatus.SENT || msg.senderId == "me"
                Column(Modifier.fillMaxWidth(), horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
                    Surface(color = if (isMine) BubbleMine else BubbleOther, shape = MaterialTheme.shapes.medium) {
                        Text(msg.text, Modifier.padding(10.dp))
                    }
                    if (isMine) {
                        Text(
                            when (msg.status) {
                                MessageStatus.SENT -> "Отправлено"
                                MessageStatus.DELIVERED -> "Доставлено"
                                MessageStatus.READ -> "Просмотрено"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}
