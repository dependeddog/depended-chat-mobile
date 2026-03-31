package com.depended.chat.ui.chat

import com.depended.chat.domain.model.ChatDetails
import com.depended.chat.domain.model.ChatItem
import com.depended.chat.domain.model.ChatUser
import com.depended.chat.domain.model.Message
import com.depended.chat.domain.model.MessageStatus
import com.depended.chat.domain.repository.ChatListEvent
import com.depended.chat.domain.repository.ChatsRepository
import com.depended.chat.domain.repository.MessageEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `updateMessage replaces message in ui state`() = runTest(dispatcher) {
        val repo = FakeChatsRepository()
        val vm = ChatViewModel(repo)
        vm.init("chat-1")
        dispatcher.scheduler.advanceUntilIdle()

        vm.updateMessage("m1", "hello", "hello updated")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("hello updated", vm.state.value.messages.first().text)
        assertTrue(vm.state.value.messages.first().isEdited)
    }

    @Test
    fun `deleteMessage removes item from list`() = runTest(dispatcher) {
        val repo = FakeChatsRepository()
        val vm = ChatViewModel(repo)
        vm.init("chat-1")
        dispatcher.scheduler.advanceUntilIdle()

        vm.deleteMessage("m1")
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.state.value.messages.isEmpty())
    }

    @Test
    fun `message updated websocket event updates ui`() = runTest(dispatcher) {
        val repo = FakeChatsRepository()
        val vm = ChatViewModel(repo)
        vm.init("chat-1")
        dispatcher.scheduler.advanceUntilIdle()

        repo.chatEventsFlow.emit(
            MessageEvent.Updated(
                repo.message.copy(text = "ws update", isEdited = true)
            )
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("ws update", vm.state.value.messages.first().text)
        assertTrue(vm.state.value.messages.first().isEdited)
    }

    private class FakeChatsRepository : ChatsRepository {
        val message = Message(
            id = "m1",
            chatId = "chat-1",
            senderId = "me",
            text = "hello",
            createdAt = "2026-01-01T00:00:00Z",
            isMine = true,
            status = MessageStatus.SENT,
            isEdited = false,
            editedAt = null
        )
        val chatEventsFlow = MutableSharedFlow<MessageEvent>()
        private var localMessages = listOf(message)

        override suspend fun getChats(): List<ChatItem> = emptyList()
        override suspend fun createDirectChat(username: String): String = ""
        override suspend fun getChatDetails(chatId: String): ChatDetails =
            ChatDetails(chatId, ChatUser("u2", "Companion"), 0)

        override suspend fun getMessages(chatId: String): List<Message> = localMessages

        override suspend fun sendMessage(chatId: String, text: String): Message = message.copy(text = text)

        override suspend fun updateMessage(chatId: String, messageId: String, text: String): Message {
            val updated = message.copy(id = messageId, text = text, isEdited = true, editedAt = "2026-01-01T00:01:00Z")
            localMessages = listOf(updated)
            return updated
        }

        override suspend fun deleteMessage(chatId: String, messageId: String) {
            localMessages = localMessages.filterNot { it.id == messageId }
        }

        override suspend fun deleteChat(chatId: String) = Unit
        override suspend fun markRead(chatId: String) = Unit
        override fun globalEvents(currentUserId: String): Flow<ChatListEvent> = flowOf()
        override fun chatEvents(chatId: String, currentUserId: String): Flow<MessageEvent> = chatEventsFlow
        override suspend fun connectGlobal() = Unit
        override suspend fun connectChat(chatId: String) = Unit
        override suspend fun disconnectChat(chatId: String) = Unit
        override suspend fun disconnectAllSockets() = Unit
        override suspend fun getCurrentUserId(): String = "me"
    }
}
