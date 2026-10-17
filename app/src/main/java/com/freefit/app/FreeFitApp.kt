package com.freefit.app

import android.app.Application
import android.content.res.Configuration
import java.util.Locale

class FreeFitApp : Application() {
    override fun onCreate() {
        super.onCreate()
        L10n.english = isEnglish(resources.configuration)
        // osmdroid：自定义 UA 与缓存目录（放应用缓存，无需存储权限）
        val osm = org.osmdroid.config.Configuration.getInstance()
        osm.userAgentValue = packageName
        osm.osmdroidBasePath = java.io.File(cacheDir, "osmdroid")
        osm.osmdroidTileCache = java.io.File(osm.osmdroidBasePath, "tiles")
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
