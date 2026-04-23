package com.github.chudoxl.linteh.food

import android.app.Application
import com.github.chudoxl.linteh.food.data.StudentRepository
import com.github.chudoxl.linteh.food.data.db.AppDatabase
import com.github.chudoxl.linteh.food.data.network.BalanceApi
import com.github.chudoxl.linteh.food.data.network.SchoolBalanceApi
import com.github.chudoxl.linteh.food.worker.BalanceCheckScheduler
import com.github.chudoxl.linteh.food.worker.NotificationHelper

open class App : Application() {
    lateinit var database: AppDatabase
        private set
    lateinit var repository: StudentRepository
        private set
    lateinit var balanceApi: BalanceApi
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        balanceApi = SchoolBalanceApi()
        repository = StudentRepository(database, balanceApi)
        NotificationHelper.createChannel(this)
        BalanceCheckScheduler.scheduleAll(this)
    }
}
