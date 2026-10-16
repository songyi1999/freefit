package com.freefit.app.fitness

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * 系统自带 TTS 做语音倒计时与提示；设备不支持时静默降级（不影响训练）。
 */
class TtsSpeaker(context: Context) {
    private var tts: TextToSpeech? = null
    private var ready = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS && tts?.let {
                it.language == null || it.setLanguage(Locale.SIMPLIFIED_CHINESE) >= 0
            } == true
        }
    }

    fun speak(text: String) {
        if (!ready) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "freefit_$text")
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
