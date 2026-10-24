package com.freefit.app

import android.content.Context

/** 首次启动隐私协议同意状态（上架合规要求：未同意前不申请任何权限） */
object PrivacyGate {
    const val POLICY_URL = "https://songyi1999.github.io/freefit/"

    private const val PREFS = "app_privacy"
    private const val KEY_AGREED = "agreed"

    fun agreed(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_AGREED, false)

    fun agree(ctx: Context) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_AGREED, true).apply()
    }
}
