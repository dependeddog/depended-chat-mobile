package com.depended.chat.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.depended.chat.ui.account.AccountScreen
import com.depended.chat.ui.account.AccountViewModel
import com.depended.chat.ui.auth.AuthScreen
import com.depended.chat.ui.auth.AuthViewModel
import com.depended.chat.ui.chat.ChatScreen
import com.depended.chat.ui.chat.ChatViewModel
import com.depended.chat.ui.chats.ChatsScreen
import com.depended.chat.ui.chats.ChatsViewModel
import com.depended.chat.ui.profile.UserProfileScreen
import com.depended.chat.ui.profile.UserProfileViewModel
import com.depended.chat.ui.splash.SplashScreen
import com.depended.chat.ui.splash.SplashViewModel

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Route.Splash.path) {
        composable(Route.Splash.path) {
            val vm = hiltViewModel<SplashViewModel>()
            SplashScreen(vm) { authorized ->
                navController.navigate(if (authorized) Route.Chats.path else Route.Auth.path) {
                    popUpTo(Route.Splash.path) { inclusive = true }
                }
            }
        }

        composable(Route.Auth.path) {
            val vm = hiltViewModel<AuthViewModel>()
            AuthScreen(vm) {
                navController.navigate(Route.Chats.path) {
                    popUpTo(Route.Auth.path) { inclusive = true }
                }
            }
        }

        composable(Route.Chats.path) {
            val vm = hiltViewModel<ChatsViewModel>()

            LaunchedEffect(Unit) {
                navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.getStateFlow("deleted_chat_id", "")
                    ?.collect { deletedChatId ->
                        if (deletedChatId.isNotBlank()) {
                            vm.onChatDeleted(deletedChatId)
                            navController.currentBackStackEntry
                                ?.savedStateHandle
                                ?.set("deleted_chat_id", "")
                        }
                    }
            }

            ChatsScreen(
                vm,
                onOpenChat = { navController.navigate(Route.Chat.create(it)) },
                onOpenAccount = { navController.navigate(Route.Account.path) }
            )
        }

        composable(Route.Account.path) {
            val vm = hiltViewModel<AccountViewModel>()
            AccountScreen(
                vm,
                onBack = { navController.popBackStack() },
                onLoggedOut = {
                    navController.navigate(Route.Auth.path) {
                        popUpTo(Route.Chats.path) { inclusive = true }
                    }
                }
            )
        }

        composable(
            Route.Chat.path,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStack ->
            val chatId = backStack.arguments?.getString("chatId").orEmpty()
            val vm = hiltViewModel<ChatViewModel>()

            ChatScreen(
                viewModel = vm,
                chatId = chatId,
                onBack = { navController.popBackStack() },
                onOpenCompanionProfile = { userId ->
                    navController.navigate(Route.UserProfile.create(userId))
                },
                onChatDeleted = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("deleted_chat_id", chatId)
                    navController.popBackStack()
                }
            )
        }

        composable(
            Route.UserProfile.path,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStack ->
            val userId = backStack.arguments?.getString("userId").orEmpty()
            val vm = hiltViewModel<UserProfileViewModel>()
            UserProfileScreen(
                vm,
                userId = userId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}