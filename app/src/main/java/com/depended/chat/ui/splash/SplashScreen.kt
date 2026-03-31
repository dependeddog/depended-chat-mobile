package com.depended.chat.ui.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun SplashScreen(viewModel: SplashViewModel, onResolved: (Boolean) -> Unit) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.bootstrap() }
    if (state is SplashState.Result) {
        onResolved((state as SplashState.Result).authorized)
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
