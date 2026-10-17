package com.freefit.app

/**
 * 极简双语支持：跟随系统语言，启动时初始化一次。
 * 用法：t("训练计划", "Training Plans")
 */
object L10n {
    @Volatile
    var english: Boolean = false
}

fun t(zh: String, en: String): String = if (L10n.english && en.isNotEmpty()) en else zh
