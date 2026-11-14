package com.oilchange.app

import java.util.Calendar
import java.util.Locale

fun en(s: String): String = buildString {
    for (c in s) append(when (c) { in '۰'..'۹' -> '0' + (c - '۰'); in '٠'..'٩' -> '0' + (c - '٠'); else -> c })
}
fun fa(x: Any): String = en(x.toString()).map { if (it in '0'..'9') '۰' + (it - '0') else it }.joinToString("")
fun money(x: Number): String = fa(String.format(Locale.US, "%,d", x.toLong()))
fun toInt(s: String): Int = en(s).filter { it.isDigit() }.take(9).toIntOrNull() ?: 0

object Jalali {
    val MONTHS = listOf("فروردین","اردیبهشت","خرداد","تیر","مرداد","شهریور","مهر","آبان","آذر","دی","بهمن","اسفند")

    fun g2j(gy: Int, gm: Int, gd: Int): IntArray {
        val gdm = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        val gy2 = if (gm > 2) gy + 1 else gy
        var days = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) + ((gy2 + 399) / 400) + gd + gdm[gm - 1]
        var jy = -1595 + (33 * (days / 12053))
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) { jy += (days - 1) / 365; days = (days - 1) % 365 }
        val jm: Int; val jd: Int
        if (days < 186) { jm = 1 + days / 31; jd = 1 + days % 31 } else { jm = 7 + (days - 186) / 30; jd = 1 + (days - 186) % 30 }
        return intArrayOf(jy, jm, jd)
    }

    fun j2g(jy0: Int, jm: Int, jd: Int): IntArray {
        val jy = jy0 + 1595
        var days = -355668 + (365 * jy) + ((jy / 33) * 8) + (((jy % 33) + 3) / 4) + jd + (if (jm < 7) (jm - 1) * 31 else ((jm - 7) * 30) + 186)
        var gy = 400 * (days / 146097)
        days %= 146097
        if (days > 36524) { days--; gy += 100 * (days / 36524); days %= 36524; if (days >= 365) days++ }
        gy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) { gy += (days - 1) / 365; days = (days - 1) % 365 }
        var gd = days + 1
        val sal = intArrayOf(0, 31, if ((gy % 4 == 0 && gy % 100 != 0) || gy % 400 == 0) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var gm = 0
        while (gm < 13 && gd > sal[gm]) { gd -= sal[gm]; gm++ }
        return intArrayOf(gy, gm, gd)
    }

    private fun parts(ms: Long): IntArray {
        val c = Calendar.getInstance().apply { timeInMillis = ms }
        val j = g2j(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
        return intArrayOf(j[0], j[1], j[2], c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
    }

    fun fmt(ms: Long?, time: Boolean = true): String {
        if (ms == null) return "—"
        val p = parts(ms)
        var s = String.format(Locale.US, "%04d/%02d/%02d", p[0], p[1], p[2])
        if (time) s += String.format(Locale.US, " - %02d:%02d", p[3], p[4])
        return fa(s)
    }

    fun nowInput(): String {
        val p = parts(System.currentTimeMillis())
        return String.format(Locale.US, "%04d/%02d/%02d %02d:%02d", p[0], p[1], p[2], p[3], p[4])
    }

    fun parse(s: String): Long? {
        val m = Regex("^(\\d{4})/(\\d{1,2})/(\\d{1,2})(?:\\s+(\\d{1,2}):(\\d{2}))?$").find(en(s.trim())) ?: return null
        val g = j2g(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt())
        val c = Calendar.getInstance()
        c.clear()
        c.set(g[0], g[1] - 1, g[2], m.groupValues[4].ifEmpty { "0" }.toInt(), m.groupValues[5].ifEmpty { "0" }.toInt(), 0)
        return c.timeInMillis
    }

    fun monthStart(): Long {
        val p = parts(System.currentTimeMillis())
        return parse("${p[0]}/${p[1]}/1") ?: 0L
    }

    fun monthKey(ms: Long): Pair<String, String> {
        val p = parts(ms)
        return String.format(Locale.US, "%04d/%02d", p[0], p[1]) to fa("${MONTHS[p[1] - 1]} ${p[0]}")
    }
}
