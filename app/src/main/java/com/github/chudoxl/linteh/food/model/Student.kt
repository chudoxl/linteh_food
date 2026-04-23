package com.github.chudoxl.linteh.food.model

import kotlinx.datetime.LocalTime
import java.math.BigDecimal

data class Student(
    val name: String,       //имя (для отображения в списке на шлавном экране)
    val cardNumber: String, //номер счёта питания (обычно номер транспортной карты)
    val password: String,   //пароль для доступа к сервису Информация о питании https://school28-kirov.ru/informaciya-o-pitanii
    //баланс
    val balanceState: BalanceState,  //статус загрузки балана
    val balance: BigDecimal,        //значение баланса на карте
    //параметры уведомлений
    val notificationEnabled: Boolean,       //включены ли уведомления о низком балансе
    val notificationTime: LocalTime,        //время для показа уведомления
    val notificationThreshold: BigDecimal   //уведомление будет показано, если значение баланса ниже этого значения
) {
    companion object {
        val Empty = Student(
            name = "",
            cardNumber = "",
            password = "",
            balanceState = BalanceState.Unknown,
            balance = BigDecimal.ZERO,
            notificationEnabled = true,
            notificationTime = LocalTime(hour = 7, minute = 0),
            notificationThreshold = BigDecimal(200)
        )
    }
}
