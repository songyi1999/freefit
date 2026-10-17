package com.freefit.app.run

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 跑步状态单例：前台服务写入，界面读取。
 * 不依赖 Google Play Services，直接用系统 LocationManager，国内手机通用。
 */
object RunTracker {

    data class RunState(
        val running: Boolean = false,
        val paused: Boolean = false,
        val startedAt: Long = 0,
        /** 不含暂停的累计运动毫秒 */
        val accumulatedMs: Long = 0,
        val lastResumeAt: Long = 0,
        val distanceM: Float = 0f,
        /** (lat, lng) 轨迹点 */
        val points: List<Pair<Double, Double>> = emptyList(),
    ) {
        fun elapsedMs(now: Long = System.currentTimeMillis()): Long =
            accumulatedMs + if (running && !paused) now - lastResumeAt else 0

        fun paceSecPerKm(): Float =
            if (distanceM > 30f) elapsedMs() / 1000f / (distanceM / 1000f) else 0f
    }

    private val _state = MutableStateFlow(RunState())
    val state: StateFlow<RunState> = _state

    fun start() {
        val now = System.currentTimeMillis()
        _state.value = RunState(running = true, paused = false, startedAt = now, lastResumeAt = now)
    }

    fun pause() {
        val s = _state.value
        if (s.running && !s.paused) {
            _state.value = s.copy(paused = true, accumulatedMs = s.elapsedMs(), lastResumeAt = 0)
        }
    }

    fun resume() {
        val s = _state.value
        if (s.running && s.paused) {
            _state.value = s.copy(paused = false, lastResumeAt = System.currentTimeMillis())
        }
    }

    /** 服务收到定位回调时写入；带精度与跳点过滤 */
    fun onLocation(lat: Double, lng: Double, accuracy: Float) {
        val s = _state.value
        if (!s.running || s.paused || accuracy > 30f) return
        val last = s.points.lastOrNull()
        var distance = s.distanceM
        if (last != null) {
            val d = haversineM(last.first, last.second, lat, lng)
            val dt = 2.0 // 采样间隔约 2s
            if (d / dt > 9.0) return // 明显漂移（>9m/s）丢弃
            if (d < 1.0) return // 太近不累计
            distance += d.toFloat()
        }
        _state.value = s.copy(
            distanceM = distance,
            points = (s.points + (lat to lng)).takeLast(20000),
        )
    }

    /** 结束并取回最终状态（随后重置） */
    fun finish(): RunState {
        val s = _state.value
        _state.value = RunState()
        return s
    }

    private fun haversineM(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2) * sin(dLng / 2)
        return 2 * r * atan2(sqrt(a), sqrt(1 - a))
    }
}
