package com.opankcs.costa

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opankcs.costa.notifications.NotificationScheduler
import com.opankcs.costa.ui.CostaApp
import com.opankcs.costa.ui.CostaSplash
import com.opankcs.costa.ui.CostaViewModel
import com.opankcs.costa.ui.theme.CostaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CostaTheme {
                val context = LocalContext.current
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                ) { }

                var showSplash by rememberSaveable { mutableStateOf(true) }
                val viewModel: CostaViewModel = viewModel()

                LaunchedEffect(Unit) {
                    NotificationScheduler.schedule(context)
                }

                LaunchedEffect(showSplash) {
                    if (!showSplash &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    CostaApp(viewModel)

                    if (showSplash) {
                        CostaSplash(onFinished = { showSplash = false })
                    }
                }
            }
        }
    }
}
