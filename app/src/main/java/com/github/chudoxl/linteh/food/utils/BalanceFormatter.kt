package com.github.chudoxl.linteh.food.utils

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

private val RUBLES_FORMAT: ThreadLocal<DecimalFormat> = ThreadLocal.withInitial {
    DecimalFormat(
        "#,##0.00",
        DecimalFormatSymbols().apply {
            groupingSeparator = ' '
            decimalSeparator = ','
        },
    )
}

fun BigDecimal.formatAsRubles(): String = "${RUBLES_FORMAT.get()!!.format(this)} ₽"

private val RUBLES_INTEGER_FORMAT: ThreadLocal<DecimalFormat> = ThreadLocal.withInitial {
    DecimalFormat(
        "#,##0",
        DecimalFormatSymbols().apply {
            groupingSeparator = ' '
        },
    )
}

fun BigDecimal.formatAsRublesInteger(): String = "${RUBLES_INTEGER_FORMAT.get()!!.format(this)} ₽"
