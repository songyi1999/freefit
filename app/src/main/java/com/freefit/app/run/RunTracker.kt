package com.freefit.app.run

import android.location.Location
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

    /** 上一个被接受的定位时间戳（毫秒），用于真实速度过滤 */
    private var lastFixAt = 0L

    /** 暂停恢复后丢弃第一段位移（暂停期间的位置变化不计入距离） */
    private var skipNextSegment = false


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
        lastFixAt = 0L
        skipNextSegment = false
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
            skipNextSegment = true
            _state.value = s.copy(paused = false, lastResumeAt = System.currentTimeMillis())
        }
    }

    /**
     * 服务收到定位回调时写入。过滤规则：
     * - 精度差于 50m 的点丢弃（口袋里/楼旁常见 40-60m，不能全扔）
     * - 速度按【真实时间差】计算（>8m/s 视为漂移），不能用固定间隔，
     *   否则"错过几个点后补来的正常位移"会被误判成漂移，距离永久冻结
     * - 位移 <0.8m 视为原地不动
     */
    fun onLocation(location: Location) {
        val s = _state.value
        if (!s.running || s.paused) return
        if (location.accuracy > 50f) return

        val newPoint = location.latitude to location.longitude
        val last = s.points.lastOrNull()
        if (last == null) {
            lastFixAt = location.time
            _state.value = s.copy(points = listOf(newPoint))
            return
        }

        // 暂停恢复后的第一个点只作新锚点，不累计暂停期间的位移
        if (skipNextSegment) {
            skipNextSegment = false
            lastFixAt = location.time
            _state.value = s.copy(points = s.points + newPoint)
            return
        }

        val dtSec = ((location.time - lastFixAt) / 1000.0).coerceAtLeast(0.5)
        val d = haversineM(last.first, last.second, newPoint.first, newPoint.second)
        if (d / dtSec > 8.0) return
        if (d < 0.8) return

        lastFixAt = location.time
        _state.value = s.copy(
            distanceM = s.distanceM + d.toFloat(),
            points = (s.points + newPoint).takeLast(20000),
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
