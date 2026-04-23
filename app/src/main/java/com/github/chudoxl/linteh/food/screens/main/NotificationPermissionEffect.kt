package com.github.chudoxl.linteh.food.screens.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun NotificationPermissionEffect(shouldRequest: Boolean) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    var alreadyAsked by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { }
    )

    LaunchedEffect(shouldRequest) {
        if (!shouldRequest || alreadyAsked) return@LaunchedEffect

        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            alreadyAsked = true
        }
    }
}
