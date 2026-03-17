package com.depended.chat.ui.chats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(viewModel: ChatsViewModel, onOpenChat: (String) -> Unit, onLoggedOut: () -> Unit) {
    val state by viewModel.state.collectAsState()
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Chats") },
            actions = { TextButton(onClick = { viewModel.logout(onLoggedOut) }) { Text("Выйти") } }
        )
    }) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            LazyColumn {
                items(state.items, key = { it.id }) { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onOpenChat(item.id) }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(44.dp).clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) { Text(item.companion.username.take(1).uppercase()) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.companion.username, style = MaterialTheme.typography.titleMedium)
                            Text(item.lastMessage?.text ?: "Пустой чат", maxLines = 1, style = MaterialTheme.typography.bodyMedium)
                        }
                        if (item.unreadCount > 0) Badge { Text(item.unreadCount.toString()) }
                    }
                    HorizontalDivider()
                }
            }
            if (state.loading) CircularProgressIndicator(Modifier.align(Alignment.Center))
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp)) }
        }
    }
}
