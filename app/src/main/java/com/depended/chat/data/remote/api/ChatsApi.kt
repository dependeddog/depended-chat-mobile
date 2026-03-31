package com.depended.chat.data.remote.api

import com.depended.chat.data.remote.dto.*
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatsApi {
    @POST("chats/direct")
    suspend fun createDirect(@Body body: CreateDirectChatRequestDto): DirectChatResponseDto

    @GET("chats")
    suspend fun getChats(): List<ChatListItemDto>

    @GET("chats/{chatId}")
    suspend fun getChat(@Path("chatId") chatId: String): ChatDetailsDto

    @GET("chats/{chatId}/messages")
    suspend fun getMessages(
        @Path("chatId") chatId: String,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): ChatMessagesResponseDto

    @POST("chats/{chatId}/messages")
    suspend fun sendMessage(
        @Path("chatId") chatId: String,
        @Body body: MessageCreateRequestDto
    ): MessageDto

    @PATCH("chats/{chatId}/messages/{messageId}")
    suspend fun updateMessage(
        @Path("chatId") chatId: String,
        @Path("messageId") messageId: String,
        @Body body: MessageUpdateRequestDto
    ): MessageDto

    @DELETE("chats/{chatId}/messages/{messageId}")
    suspend fun deleteMessage(
        @Path("chatId") chatId: String,
        @Path("messageId") messageId: String
    )

    @DELETE("chats/{chatId}")
    suspend fun deleteChat(@Path("chatId") chatId: String)

    @POST("chats/{chatId}/read")
    suspend fun markRead(@Path("chatId") chatId: String): MarkReadResponseDto
}
