package com.freefit.app.screens

import java.time.LocalDate

/** 连续打卡天数：从今天（或昨天）往回数连续有记录的天数 */
fun streakDays(dates: Collection<String>): Int {
    val set = dates.toSet()
    var d = LocalDate.now()
    if (d.toString() !in set) d = d.minusDays(1)
    var count = 0
    while (d.toString() in set) {
        count++
        d = d.minusDays(1)
    }
    return count
}

fun formatDuration(sec: Int): String = when {
    sec >= 3600 -> "${sec / 3600} 小时 ${(sec % 3600) / 60} 分"
    sec >= 60 -> "${sec / 60} 分钟"
    else -> "$sec 秒"
}
