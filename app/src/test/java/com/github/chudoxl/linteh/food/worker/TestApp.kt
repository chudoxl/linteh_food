package com.github.chudoxl.linteh.food.worker

import com.github.chudoxl.linteh.food.App
import com.github.chudoxl.linteh.food.data.StudentRepository
import com.github.chudoxl.linteh.food.data.network.BalanceApi
import com.github.chudoxl.linteh.food.data.network.BalanceResponse
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Тестовый Application, подставляемый через @Config(application = TestApp::class).
 * Позволяет worker-тестам обращаться к `applicationContext as App` без запуска
 * реальной инициализации (Room, AlarmManager).
 */
class TestApp : App() {
    override fun onCreate() {
        // Не вызываем super.onCreate(), чтобы избежать реальной инициализации Room / scheduleAll.
    }

    /**
     * Устанавливает зависимости из теста. Используется через рефлексию,
     * т. к. у App private set.
     */
    fun inject(repository: StudentRepository, balanceApi: BalanceApi = FakeBalanceApi()) {
        val repoField = App::class.java.getDeclaredField("repository").apply { isAccessible = true }
        val apiField = App::class.java.getDeclaredField("balanceApi").apply { isAccessible = true }
        repoField.set(this, repository)
        apiField.set(this, balanceApi)
    }
}

private class FakeBalanceApi : BalanceApi {
    override suspend fun getBalance(login: String, password: String): BalanceResponse {
        delay(1500)
        if (password.isBlank()) {
            throw IllegalArgumentException("Пароль не может быть пустым")
        }
        val balance = BigDecimal(Math.random() * 2000).setScale(2, RoundingMode.HALF_UP)
        return BalanceResponse(
            studentName = "Ученик $login",
            balance = balance,
        )
    }
}
