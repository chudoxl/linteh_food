package com.github.chudoxl.linteh.food.utils

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * prod.md §3.4, §8 — действия карточки: копирование номера карты и запуск «Хлынов Банк».
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ContextExtensionsTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    /** §3.4 — copyToClipboard кладёт текст в primaryClip системного буфера. */
    @Test
    fun copyToClipboard_puts_text_into_primary_clip() {
        context.copyToClipboard(textToCopy = "001 002 003", label = "card")

        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = cm.primaryClip
        assertThat(clip).isNotNull()
        assertThat(clip!!.getItemAt(0).text.toString()).isEqualTo("001 002 003")
    }

    /** §8 — если «Хлынов Банк» не установлен, launchHlynov открывает страницу пополнения в браузере. */
    @Test
    fun launchHlynov_opens_browser_when_app_not_installed() {
        context.launchHlynov(cardNumber = "001")

        val startedIntent = shadowOf(context as Application).nextStartedActivity
        assertThat(startedIntent).isNotNull()
        assertThat(startedIntent.action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(startedIntent.data)
            .isEqualTo(Uri.parse("https://my.bank-hlynov.ru/payments/pay-service/53888/"))
        assertThat(startedIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK).isNotEqualTo(0)
    }
}
