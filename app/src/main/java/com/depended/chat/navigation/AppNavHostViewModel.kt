package com.depended.chat.navigation

import androidx.lifecycle.ViewModel
import com.depended.chat.notifications.NotificationNavigationState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AppNavHostViewModel @Inject constructor(
    val navigationState: NotificationNavigationState
) : ViewModel()
