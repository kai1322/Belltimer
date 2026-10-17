package com.belltimer

import java.util.Locale

object Fmt {
    fun long(min: Int): String = when {
        min < 60 -> "$min นาที"
        min % 60 == 0 -> "${min / 60} ชม."
        else -> "${min / 60} ชม. ${min % 60} นาที"
    }

    fun short(min: Int): String = if (min < 60) "${min}น." else "${min / 60}ชม."

    fun countdown(totalSec: Long): String {
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        else String.format(Locale.US, "%02d:%02d", m, s)
    }
}
