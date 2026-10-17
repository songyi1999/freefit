package com.freefit.app.fitness

import android.media.AudioManager
import android.media.ToneGenerator

/**
 * 过场提示音：用系统 ToneGenerator 合成，零音频资源、零成本。
 * 部分设备在勿扰模式下可能静音，属预期行为。
 */
class SoundCues {
    private val tone: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, 85)
    } catch (_: Exception) {
        null
    }

    /** 开始运动：短促一声 */
    fun setStart() = tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 180)

    /** 一组结束：双音 */
    fun setDone() = tone?.startTone(ToneGenerator.TONE_PROP_BEEP2, 250)

    /** 休息结束即将开始：确认音 */
    fun restOver() = tone?.startTone(ToneGenerator.TONE_PROP_ACK, 320)

    /** 全部完成：长确认音 */
    fun finish() = tone?.startTone(ToneGenerator.TONE_PROP_ACK, 650)

    fun release() = tone?.release()
}
