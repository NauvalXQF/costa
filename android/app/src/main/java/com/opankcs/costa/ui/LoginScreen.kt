package com.opankcs.costa.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.opankcs.costa.R

@Composable
fun LoginScreen(
    state: CostaUiState,
    onLogin: (String, String, String) -> Unit,
    onRegister: (String, String, String, String, String, String) -> Unit,
) {
    var mode by rememberSaveable { mutableStateOf("login") }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    var inviteCode by rememberSaveable { mutableStateOf("") }
    var baseUrl by rememberSaveable(state.baseUrl) { mutableStateOf(state.baseUrl) }
    var showServer by rememberSaveable { mutableStateOf(false) }

    val haptics = rememberCostaHaptics()

    val isRegister = mode == "register"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Costa",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = if (isRegister) stringResource(R.string.create_account) else stringResource(R.string.app_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(24.dp))

        if (isRegister) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(R.string.email)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.password)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        if (isRegister) {
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = confirmation,
                onValueChange = { confirmation = it },
                label = { Text(stringResource(R.string.repeat_password)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = inviteCode,
                onValueChange = { inviteCode = it },
                label = { Text(stringResource(R.string.invite_code)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(6.dp))

        if (showServer) {
            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = { Text(stringResource(R.string.server_base_url)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            TextButton(onClick = { haptics.tick(); showServer = true }) {
                Text(stringResource(R.string.change_server))
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                haptics.confirm()
                if (isRegister) {
                    onRegister(name, email, password, confirmation, inviteCode, baseUrl)
                } else {
                    onLogin(email, password, baseUrl)
                }
            },
            enabled = email.isNotBlank() &&
                password.isNotBlank() &&
                (!isRegister || (name.isNotBlank() && confirmation.isNotBlank() && inviteCode.isNotBlank())) &&
                !state.loading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Text(if (isRegister) stringResource(R.string.register) else stringResource(R.string.login))
            }
        }

        state.error?.let { message ->
            Spacer(Modifier.height(10.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(Modifier.height(16.dp))

        TextButton(
            onClick = {
                haptics.tick()
                mode = if (isRegister) "login" else "register"
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (isRegister) stringResource(R.string.already_have_account) else stringResource(R.string.no_account))
        }

        Text(
            text = if (isRegister) {
                stringResource(R.string.invite_hint)
            } else {
                stringResource(R.string.data_separate)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
