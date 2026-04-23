package com.github.chudoxl.linteh.food.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith

/**
 * prod.md §8 — запуск «Хлынов Банк»:
 * - если установлен и активен — приложение запускается;
 * - если не установлен/отключён — открывается страница пополнения в браузере;
 * - если на устройстве нет ни банка, ни браузера — показывается Toast.
 *
 * В инструментальной среде установка «Хлынов Банк» не гарантирована. Тест проверяет,
 * что вызов на реальном устройстве не падает: Intent валиден и резолвится либо в банк,
 * либо в браузер, либо в Toast-фоллбек.
 */
@RunWith(AndroidJUnit4::class)
class LaunchHlynovInstrumentedTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    /** §8 — вызов launchHlynov на реальном устройстве без Хлынов Банка не падает. */
    @Test
    fun launchHlynov_does_not_throw_when_app_missing() {
        // Toast.makeText и startActivity требуют Looper главного потока — запускаем на UI thread.
        // Детали ветвления (браузер vs. Toast) покрыты unit-тестом ContextExtensionsTest (Robolectric).
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            context.launchHlynov(cardNumber = "001002003")
        }
    }
}
