package com.github.chudoxl.linteh.food.screens.edit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
internal fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        modifier = Modifier.testTag("help_dialog"),
        onDismissRequest = onDismiss,
        title = { Text("Справка по полям") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                HelpItem(
                    "Номер карты",
                    "Обычно номер транспортной карты или браслета.",
                )
                HelpItem(
                    "Пароль",
                    "Пароль от веб-сервиса school28-kirov.ru/informaciya-o-pitanii.\nЕго можно узнать у классного руководителя или IT-специалиста школы.",
                )
                HelpItem(
                    "Имя",
                    "Отображается в карточке ученика и в уведомлениях. Если оставить пустым — будет использован номер карты.",
                )
                HelpItem(
                    "Проверить",
                    "Проверяет введённые номер карты и пароль.\nСохранение данных ученика возможно только после успешной проверки.",
                )
                HelpItem(
                    "Напоминать",
                    "Включает ежедневную автоматическую проверку баланса в указанное ниже время.",
                )
                HelpItem(
                    "eсли баланс меньше, ₽",
                    "Если во время автоматической проверки баланс окажется ниже этого значения, приложение отобразит предупреждающее уведомление.",
                )
                HelpItem(
                    "каждый день в",
                    "Время автоматической проверки.",
                )
            }
        },
        confirmButton = {
            TextButton(
                modifier = Modifier.testTag("help_dialog_ok"),
                onClick = onDismiss,
            ) { Text("ОК") }
        },
    )
}

@Composable
private fun HelpItem(title: String, text: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(text = title, style = MaterialTheme.typography.labelLarge)
        Text(text = text, style = MaterialTheme.typography.bodySmall)
    }
}