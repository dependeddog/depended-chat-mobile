package com.depended.chat.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun AuthScreen(viewModel: AuthViewModel, onSuccess: () -> Unit) {
    val state by viewModel.state.collectAsState()
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .align(Alignment.Center),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(if (state.isLoginMode) "Вход" else "Регистрация", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(value = state.username, onValueChange = viewModel::onUsername, label = { Text("Username") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::onPassword,
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            if (!state.isLoginMode) {
                OutlinedTextField(
                    value = state.confirmPassword,
                    onValueChange = viewModel::onConfirmPassword,
                    label = { Text("Confirm password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = { viewModel.submit(onSuccess) }, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) {
                if (state.loading) CircularProgressIndicator(modifier = Modifier.size(18.dp))
                else Text(if (state.isLoginMode) "Войти" else "Создать аккаунт")
            }
            TextButton(onClick = viewModel::toggleMode, modifier = Modifier.align(Alignment.End)) {
                Text(if (state.isLoginMode) "Нет аккаунта? Зарегистрироваться" else "Уже есть аккаунт? Войти")
            }
        }
    }
}
