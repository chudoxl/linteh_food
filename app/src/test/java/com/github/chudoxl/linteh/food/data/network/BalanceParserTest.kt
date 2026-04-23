package com.github.chudoxl.linteh.food.data.network

import com.github.chudoxl.linteh.food.data.network.BalanceParserTest.Companion.RESPONSE_JSON_TEMPLATE
import com.google.common.truth.Truth.assertThat
import org.json.JSONException
import org.junit.Assert.assertThrows
import org.junit.Test
import java.math.BigDecimal

/**
 * Тесты парсинга ответа API школы (prod.md §9).
 * В качестве HTML-контента для параметризованных сценариев используется тот же JSON,
 * что приходит с сервера (см. [RESPONSE_JSON_TEMPLATE]), с плейсхолдерами `${'$'}expense`
 * и `${'$'}income` в последней строке `Остаток на …`.
 */
class BalanceParserTest {

    /** §9 — из JSON-ответа извлекаются ФИО и актуальный баланс. */
    @Test
    fun parseResponse_extractsNameAndBalance() {
        val json = jsonOf(expense = "-22\u00a0666,57", income = "22\u00a0751,59")

        val result = SchoolBalanceApi.parseResponse(json)

        assertThat(result.studentName).isEqualTo("ЧуСаЕв")
        assertThat(result.balance).isEqualTo(BigDecimal("85.02"))
    }

    /** §9 — ответ с success=false бросает IllegalStateException с пометкой «Сервер вернул…». */
    @Test
    fun success_false_throws_with_spec_message() {
        val json = """{"success":false,"content":"","resource":2456}"""
        val ex = assertThrows(IllegalStateException::class.java) {
            SchoolBalanceApi.parseResponse(json)
        }
        assertThat(ex.message).contains("Сервер вернул")
    }

    /** §9 — невалидный JSON на входе бросает JSONException. */
    @Test
    fun invalid_json_throws() {
        assertThrows(JSONException::class.java) {
            SchoolBalanceApi.parseResponse("not-a-json")
        }
    }

    /** §9 — если в content нет данных о балансе, бросается ошибка с пояснением. */
    @Test
    fun empty_content_throws_balance_not_found() {
        val json = """{"success":true,"content":""}"""
        val ex = assertThrows(IllegalStateException::class.java) {
            SchoolBalanceApi.parseResponse(json)
        }
        assertThat(ex.message).contains("баланс")
    }

    /** §5 — положительный остаток округляется по HALF_UP до двух знаков. */
    @Test
    fun balance_uses_half_up_rounding_for_last_two_decimals() {
        // income + expense: 0.005 + 0 -> 0.01 по HALF_UP (prod.md §5)
        val json = jsonOf(expense = "0,005", income = "0")

        val result = SchoolBalanceApi.parseResponse(json)

        assertThat(result.balance).isEqualTo(BigDecimal("0.01"))
    }

    /** §5 — отрицательный остаток тоже округляется по HALF_UP. */
    @Test
    fun balance_uses_half_up_rounding_for_negative() {
        // -0.005 по HALF_UP округляется к -0.01
        val json = jsonOf(expense = "-0,005", income = "0")

        val result = SchoolBalanceApi.parseResponse(json)

        assertThat(result.balance).isEqualTo(BigDecimal("-0.01"))
    }

    /** §5 — итоговый баланс может быть отрицательным и не обнуляется парсером. */
    @Test
    fun balance_may_be_negative() {
        val json = jsonOf(expense = "-100,05", income = "0")

        val result = SchoolBalanceApi.parseResponse(json)

        assertThat(result.balance).isEqualTo(BigDecimal("-100.05"))
    }

    /** §9 / §13 — разделители «\u00a0» и пробелы в числах корректно парсятся. */
    @Test
    fun nbsp_and_space_separators_are_parsed() {
        // Проверяем обработку \u00a0 и пробелов в числах (prod.md §9, §13)
        val json = jsonOf(expense = "-22\u00a0365,68", income = "22\u00a0751,59")

        val result = SchoolBalanceApi.parseResponse(json)

        assertThat(result.balance).isEqualTo(BigDecimal("385.91"))
    }

    private fun jsonOf(expense: String, income: String): String =
        RESPONSE_JSON_TEMPLATE
            .replace("\$expense", expense)
            .replace("\$income", income)

    companion object {
        // Эталонный JSON-ответ сервера: content содержит unicode-escaped HTML.
        // В последней строке «Остаток на …» значения заменены на плейсхолдеры
        // ${'$'}expense / ${'$'}income, подставляемые через [jsonOf].
        private const val RESPONSE_JSON_TEMPLATE = """{"timestamp":1776153645133,"success":true,"content":"<form class=\"form js__form js__form--success\" action=\"\" data-grecaptcha-action=\"actionmealsform\">\n\t\t\n\t\t\t<input type=\"hidden\" name=\"ya_target\" value=\"mealsform\">\n\t\t\t<input type=\"hidden\" name=\"send_mealsform\" value=\"1\">\n\t\t\t<input type=\"text\" name=\"country\" value=\"\">\n\t\t\t\n\t\t\t\t\t\t\t<script>\n\t\t\t\t\t$.fancybox.close();\n\t\t\t\t\t$.fancybox.open({ \n\t\t\t\t\t\tsrc  : '#thanks_mealsform',\n\t\t\t\t\t\ttype : 'inline',\n\t\t\t\t\t\topts : { }\n\t\t\t\t\t});\n\t\t\t\t<\/script>\n\t\t\t\t<div style=\"display:none;\">\n\t\t\t\t\t<div class=\"popup popup--big wysiwyg tinymce\" id='thanks_mealsform'><HTML><HEAD><META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text\/html; CHARSET=utf-8\"><TITLE><\/TITLE><BASEFONT FACE=\"Arial\" SIZE=8><\/HEAD><BODY><TABLE BORDERCOLOR=#ffffff BORDER CELLSPACING=0><TR><TD WIDTH = 105>&nbsp;<\/TD><TD WIDTH = 231>&nbsp;<\/TD><TD WIDTH = 63>&nbsp;<\/TD><TD WIDTH = 63>&nbsp;<\/TD><TD WIDTH = 63>&nbsp;<\/TD><TD WIDTH = 63>&nbsp;<\/TD><TD WIDTH = 63>&nbsp;<\/TD><\/TR><TR><TD ALIGN=LEFT COLSPAN=2><FONT FACE=\"A\" SIZE=5><B>\u0421\u043e\u0441\u0442\u043e\u044f\u043d\u0438\u0435 \u043b\u0438\u0446\u0435\u0432\u043e\u0433\u043e \u0441\u0447\u0435\u0442\u0430<\/B><\/FONT><\/TD><TD ALIGN=LEFT COLSPAN=5><FONT FACE=\"A\" SIZE=5><B>\u0427\u0443\u0421\u0430\u0415\u0432<\/B><\/FONT><\/TD><\/TR><TR><TD ALIGN=LEFT WIDTH = 105>&nbsp;<\/TD><TD ALIGN=LEFT COLSPAN=3>&nbsp;<\/TD><TD WIDTH = 63>&nbsp;<\/TD><TD WIDTH = 63>&nbsp;<\/TD><TD WIDTH = 63>&nbsp;<\/TD><\/TR><TR><TD ALIGN=CENTER VALIGN=CENTER BORDERCOLOR=\"#000000\" WIDTH = 105 ROWSPAN=2><FONT FACE=\"A\"><B>\u0414\u0430\u0442\u0430, \u0432\u0440\u0435\u043c\u044f<\/B><\/FONT><\/TD><TD ALIGN=CENTER VALIGN=CENTER BORDERCOLOR=\"#000000\" WIDTH = 231 ROWSPAN=2><FONT FACE=\"A\"><B>\u0414\u043e\u043a\u0443\u043c\u0435\u043d\u0442<\/B><\/FONT><\/TD><TD ALIGN=CENTER COLSPAN=2><FONT FACE=\"A\"><B>\u041e\u0441\u043d\u043e\u0432\u043d\u043e\u0435<\/B><\/FONT><\/TD><TD ALIGN=CENTER COLSPAN=2><FONT FACE=\"A\"><B>\u0414\u043e\u043f\u043e\u043b\u043d\u0438\u0442\u0435\u043b\u044c\u043d\u043e\u0435<\/B><\/FONT><\/TD><TD ALIGN=CENTER VALIGN=TOP BORDERCOLOR=\"#000000\" WIDTH = 63><FONT FACE=\"A\"><B>\u0412\u0421\u0415\u0413\u041e<\/B><\/FONT><\/TD><\/TR><TR><TD ALIGN=CENTER VALIGN=TOP BORDERCOLOR=\"#000000\" WIDTH = 63><FONT FACE=\"A\"><B>\u041f\u0440\u0438\u0445\u043e\u0434<\/B><\/FONT><\/TD><TD ALIGN=CENTER VALIGN=TOP BORDERCOLOR=\"#000000\" WIDTH = 63><FONT FACE=\"A\"><B>\u0420\u0430\u0441\u0445\u043e\u0434<\/B><\/FONT><\/TD><TD ALIGN=CENTER VALIGN=TOP BORDERCOLOR=\"#000000\" WIDTH = 63><FONT FACE=\"A\"><B>\u041f\u0440\u0438\u0445\u043e\u0434<\/B><\/FONT><\/TD><TD ALIGN=CENTER VALIGN=TOP BORDERCOLOR=\"#000000\" WIDTH = 63><FONT FACE=\"A\"><B>\u0420\u0430\u0441\u0445\u043e\u0434<\/B><\/FONT><\/TD><TD ALIGN=CENTER VALIGN=TOP BORDERCOLOR=\"#000000\" WIDTH = 63><FONT FACE=\"A\"><B>\u041e\u0441\u0442\u0430\u0442\u043e\u043a<\/B><\/FONT><\/TD><\/TR><TR><TD ALIGN=LEFT VALIGN=TOP BORDERCOLOR=\"#000000\" COLSPAN=2>\u041e\u0441\u0442\u0430\u0442\u043e\u043a \u043d\u0430 13.04.2026 0:00:00<\/TD><TD ALIGN=CENTER VALIGN=TOP BORDERCOLOR=\"#000000\" COLSPAN=2>-22\u00a0365,68<\/TD><TD ALIGN=CENTER VALIGN=TOP BORDERCOLOR=\"#000000\" COLSPAN=2>22\u00a0751,59<\/TD><TD ALIGN=CENTER VALIGN=TOP BORDERCOLOR=\"#000000\" WIDTH = 63>385,91<\/TD><\/TR><TR><TD ALIGN=LEFT VALIGN=TOP BORDERCOLOR=\"#000000\" WIDTH = 105>13.04.26 00:00<\/TD><TD ALIGN=LEFT VALIGN=TOP BORDERCOLOR=\"#000000\" WIDTH = 231>\u0420\u0430\u0437\u0434\u0430\u0442\u043e\u0447\u043d\u044b\u0439 \u043b\u0438\u0441\u0442 \u2116000002364  13.04.26 00:00, 6 \u0431, \u041c\u0435\u043d\u044e 1 \u0432\u0430\u0440\u0438\u0430\u043d\u0442 (5-11 \u043a\u043b\u0430\u0441\u0441), 161,14<\/TD><TD ALIGN=CENTER VALIGN=TOP BORDERCOLOR=\"#000000\" WIDTH = 63>-<\/TD><TD ALIGN=CENTER VALIGN=TOP BORDERCOLOR=\"#000000\" WIDTH = 63>161,14<\/TD><TD ALIGN=CENTER VALIGN=TOP BORDERCOLOR=\"#000000\" WIDTH = 63>-<\/TD><TD ALIGN=CENTER VALIGN=TOP BORDERCOLOR=\"#000000\" WIDTH = 63>-<\/TD><TD ALIGN=CENTER VALIGN=TOP BORDERCOLOR=\"#000000\" WIDTH = 63>&nbsp;<\/TD><\/TR><TR><TD WIDTH = 105>&nbsp;<\/TD><TD ALIGN=LEFT WIDTH = 231>\u041c\u0435\u043d\u044e 1 \u0432\u0430\u0440\u0438\u0430\u043d\u0442 (5-11 \u043a\u043b\u0430\u0441\u0441)<\/TD><TD ALIGN=CENTER VALIGN=TOP BORDERCOLOR=\"#000000\" COLSPAN=4>161,14<\/TD><TD ALIGN=CENTER VALIGN=TOP WIDTH = 63>&nbsp;<\/TD><\/TR><TR><TD ALIGN=LEFT VALIGN=TOP BORDERCOLOR=\"#000000\" WIDTH = 105>14.04.26 00:00<\/TD><TD ALIGN=LEFT VALIGN=TOP BORDERCOLOR=\"#000000\" WIDTH = 231>\u0420\u0430\u0437\u0434\u0430\u0442\u043e\u0447\u043d\u044b\u0439 \u043b\u0438\u0441\u0442 \u2116000002410  14.04.26 00:00, 6 \u0431, \u041c\u0435\u043d\u044e 1 \u0432\u0430\u0440\u0438\u0430\u043d\u0442 (5-11 \u043a\u043b\u0430\u0441\u0441), 139,75<\/TD><TD ALIGN=CENTER VALIGN=TOP BORDERCOLOR=\"#000000\" WIDTH = 63>-<\/TD><TD ALIGN=CENTER VALIGN=TOP BORDERCOLOR=\"#000000\" WIDTH = 63>139,75<\/TD><TD ALIGN=CENTER VALIGN=TOP BORDERCOLOR=\"#000000\" WIDTH = 63>-<\/TD><TD ALIGN=CENTER VALIGN=TOP BORDERCOLOR=\"#000000\" WIDTH = 63>-<\/TD><TD ALIGN=CENTER VALIGN=TOP BORDERCOLOR=\"#000000\" WIDTH = 63>&nbsp;<\/TD><\/TR><TR><TD WIDTH = 105>&nbsp;<\/TD><TD ALIGN=LEFT WIDTH = 231>\u041c\u0435\u043d\u044e 1 \u0432\u0430\u0440\u0438\u0430\u043d\u0442 (5-11 \u043a\u043b\u0430\u0441\u0441)<\/TD><TD ALIGN=CENTER VALIGN=TOP BORDERCOLOR=\"#000000\" COLSPAN=4>139,75<\/TD><TD ALIGN=CENTER VALIGN=TOP WIDTH = 63>&nbsp;<\/TD><\/TR><TR><TD ALIGN=LEFT VALIGN=TOP BORDERCOLOR=\"#000000\" COLSPAN=2>\u041e\u0441\u0442\u0430\u0442\u043e\u043a \u043d\u0430 14.04.2026 0:00:00<\/TD><TD ALIGN=CENTER VALIGN=TOP BORDERCOLOR=\"#000000\" COLSPAN=2>${'$'}expense<\/TD><TD ALIGN=CENTER VALIGN=TOP BORDERCOLOR=\"#000000\" COLSPAN=2>${'$'}income<\/TD><TD ALIGN=CENTER VALIGN=TOP BORDERCOLOR=\"#000000\" WIDTH = 63>224,77<\/TD><\/TR><\/TABLE><\/BODY><\/HTML><\/div>\n\t\t\t\t<\/div>\n\t\t\t\t\n\t\t\t\t\n\t\t\t\t\t\t\n\t\t\t\n\t\t\t\n\t\t\t<div class=\"form__fields\">\n\t\t\t\t<div class=\"form__field\">\n\t\t\t\t\t<fieldset class=\"form__fieldset\">\n\t\t\t\t\t\t<input required name=\"login\" type=\"text\" class=\"input\"\n\t\t\t\t\t\t\tplaceholder=\"\u0412\u0432\u0435\u0434\u0438\u0442\u0435 \u043d\u043e\u043c\u0435\u0440 \u0432\u0430\u0448\u0435\u0439 \u043a\u0430\u0440\u0442\u044b*\"\n\t\t\t\t\t\t\tvalue=\"001002003\"\n\t\t\t\t\t\t>\n\t\t\t\t\t<\/fieldset>\n\t\t\t\t\t<fieldset class=\"form__fieldset\">\n\t\t\t\t\t\t<input name=\"pwd\" type=\"text\" class=\"input\"\n\t\t\t\t\t\t\tplaceholder=\"\u0438 \u043f\u0430\u0440\u043e\u043b\u044c*\"\n\t\t\t\t\t\t\tvalue=\"6PASSWORD9\"\n\t\t\t\t\t\t>\n\t\t\t\t\t<\/fieldset>\n\t\t\t\t<\/div>\n\t\t\t<\/div>\n\t\t\t\n\t\t\t<div class=\"form__bottom\">\n\t\t\t\t\t\t\t<button class=\"button form__button button--hasicon   \">\n\t\t\t<svg xmlns=\"http:\/\/www.w3.org\/2000\/svg\" viewBox=\"0 0 512 512\"><path d=\"M476 3.2L12.5 270.6c-18.1 10.4-15.8 35.6 2.2 43.2L121 358.4l287.3-253.2c5.5-4.9 13.3 2.6 8.6 8.3L176 407v80.5c0 23.6 28.5 32.9 42.5 15.8L282 426l124.6 52.2c14.2 6 30.4-2.9 33-18.2l72-432C515 7.8 493.3-6.8 476 3.2z\"\/><\/svg>\t\t\t<span>\u0417\u0430\u043f\u0440\u043e\u0441\u0438\u0442\u044c \u0438\u043d\u0444\u043e\u0440\u043c\u0430\u0446\u0438\u044e<\/span>\n\t\t<\/button>\n\t\t\t\t<\/div>\n\t\t\t\n\t\t\t\n\t\t\t\t\t\t\t\t\t\t\t\n\t\t<\/form>","resource":2456}"""
    }
}
