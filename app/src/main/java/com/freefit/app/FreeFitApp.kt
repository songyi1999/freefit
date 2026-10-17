package com.freefit.app

import android.app.Application
import android.content.res.Configuration
import java.util.Locale

class FreeFitApp : Application() {
    override fun onCreate() {
        super.onCreate()
        L10n.english = isEnglish(resources.configuration)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        L10n.english = isEnglish(newConfig)
    }

    private fun isEnglish(config: Configuration): Boolean =
        runCatching {
            val locale = if (config.locales.isEmpty) Locale.getDefault() else config.locales[0]
            locale.language.equals("en", ignoreCase = true)
        }.getOrDefault(false)
}
