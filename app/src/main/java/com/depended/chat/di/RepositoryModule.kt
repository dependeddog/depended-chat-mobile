package com.depended.chat.di

import com.depended.chat.data.repository.AuthRepositoryImpl
import com.depended.chat.data.repository.ChatsRepositoryImpl
import com.depended.chat.domain.repository.AuthRepository
import com.depended.chat.domain.repository.ChatsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindChatsRepository(impl: ChatsRepositoryImpl): ChatsRepository
}
