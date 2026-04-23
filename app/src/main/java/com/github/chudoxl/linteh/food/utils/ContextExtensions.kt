package com.github.chudoxl.linteh.food.utils

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.net.toUri
import com.github.chudoxl.linteh.food.R

private const val HLYNOV_TOPUP_URL = "https://my.bank-hlynov.ru/payments/pay-service/53888/"

fun Context.copyToClipboard(textToCopy: CharSequence, label: String = getString(R.string.app_name)) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip: ClipData = ClipData.newPlainText(label, textToCopy)
    clipboard.setPrimaryClip(clip)
}

fun Context.launchHlynov(cardNumber: String) {
    if (isAppAvailableAndEnabled("ru.bank_hlynov.xbank")) {
        val launchIntent = packageManager.getLaunchIntentForPackage("ru.bank_hlynov.xbank")
        if (launchIntent != null) {
            startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        }
    }
    openTopUpInBrowser()
}

private fun Context.openTopUpInBrowser() {
    val browserIntent = Intent(Intent.ACTION_VIEW, HLYNOV_TOPUP_URL.toUri())
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        startActivity(browserIntent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(
            this,
            "Не удалось открыть «Хлынов Банк» или страницу пополнения",
            Toast.LENGTH_SHORT
        ).show()
    }
}

private fun Context.isAppAvailableAndEnabled(packageName: String): Boolean {
    return try {
        val appInfo = packageManager.getApplicationInfo(packageName, 0)
        appInfo.enabled
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }
}
