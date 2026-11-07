package com.opankcs.costa.ui

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.opankcs.costa.R
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun CostaApp(viewModel: CostaViewModel, startAtHome: Boolean = false) {
    val state by viewModel.state.collectAsState()
    var tab by rememberSaveable { mutableStateOf(0) }

    LaunchedEffect(startAtHome) {
        if (startAtHome) {
            tab = 0
        }
    }
    val haptics = rememberCostaHaptics()
    val tagline = stringResource(R.string.app_tagline)

    LaunchedEffect(state.error) {
        if (state.error != null) {
            haptics.reject()
        }
    }

    state.availableUpdate?.let { update ->
        AlertDialog(
            onDismissRequest = viewModel::dismissUpdate,
            title = { Text(stringResource(R.string.update_available)) },
            text = {
                val versionText = stringResource(R.string.update_version_available, update.versionName)
                val notes = if (Locale.getDefault().language == "en" && update.notesEn.isNotBlank()) update.notesEn else update.notes
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        buildString {
                            append(versionText)
                            if (notes.isNotBlank()) {
                                append("\n\n")
                                append(notes)
                            }
                        },
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.downloadAndInstallUpdate() },
                    enabled = !state.updateDownloading,
                ) {
                    Text(if (state.updateDownloading) stringResource(R.string.downloading) else stringResource(R.string.download_install))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissUpdate) {
                    Text(stringResource(R.string.later))
                }
            },
        )
    }

    LaunchedEffect(state.notice) {
        if (state.notice != null) {
            delay(3000)
            viewModel.clearNotice()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        if (!state.isLoggedIn) {
            LoginScreen(
                state = state,
                onLogin = { email, password, baseUrl ->
                    viewModel.login(email, password, baseUrl)
                },
                onRegister = { name, email, password, confirmation, inviteCode, baseUrl ->
                    viewModel.register(name, email, password, confirmation, inviteCode, baseUrl)
                },
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .imePadding(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "Costa",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = state.userName.ifBlank { tagline },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    if (state.loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        TextButton(onClick = { haptics.tick(); viewModel.refresh() }) {
                            Text(stringResource(R.string.refresh))
                        }
                    }
                }

                state.error?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
                    )
                }

                state.notice?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = tab,
                        transitionSpec = {
                            val direction = if (targetState > initialState) 1 else -1
                            (slideInHorizontally(
                                animationSpec = tween(240, easing = FastOutSlowInEasing),
                            ) { width -> direction * width / 12 } + fadeIn(tween(220))) togetherWith
                                (slideOutHorizontally(
                                    animationSpec = tween(180, easing = FastOutSlowInEasing),
                                ) { width -> -direction * width / 18 } + fadeOut(tween(160)))
                        },
                        label = "tabTransition",
                    ) { currentTab ->
                        when (currentTab) {
                            0 -> HomeScreen(state, viewModel)
                            1 -> HistoryScreen(state, viewModel)
                            else -> SettingsScreen(state, viewModel)
                        }
                    }
                }

                NavigationBar {
                    NavigationBarItem(
                        selected = tab == 0,
                        onClick = { haptics.tick(); tab = 0 },
                        icon = { Text("🏠") },
                        label = { Text(stringResource(R.string.nav_home)) },
                    )
                    NavigationBarItem(
                        selected = tab == 1,
                        onClick = { haptics.tick(); tab = 1 },
                        icon = { Text("📊") },
                        label = { Text(stringResource(R.string.nav_history)) },
                    )
                    NavigationBarItem(
                        selected = tab == 2,
                        onClick = { haptics.tick(); tab = 2 },
                        icon = { Text("⚙️") },
                        label = { Text(stringResource(R.string.nav_settings)) },
                    )
                }
            }
        }
    }
}
@Composable
private fun AnimatedNavIcon(icon: String, selected: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.12f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "navIconScale",
    )

    Text(
        text = icon,
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
    )
}
