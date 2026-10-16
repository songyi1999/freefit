package com.freefit.app.fitness

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.freefit.app.data.Exercise
import com.freefit.app.data.Pose
import com.freefit.app.data.SkelPoint
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.PI

/**
 * 计算动作周期内某时刻的姿态：关键帧之间做正弦缓动插值，首尾自动循环。
 */
fun poseAtCycle(ex: Exercise, timeSec: Float): Pose {
    val frames = ex.keyframes.sortedBy { it.t }
    if (frames.size == 1) return frames[0].pose

    val cycleT = ((timeSec % ex.cycleSec) / ex.cycleSec).let { if (it < 0f) it + 1f else it }
    var i = 0
    while (i < frames.size - 1 && cycleT >= frames[i + 1].t) i++

    val from = frames[i]
    val to = if (i == frames.size - 1) frames[0] else frames[i + 1]
    val span = (if (i == frames.size - 1) to.t + 1f else to.t) - from.t
    val u = if (span <= 0f) 0f else (cycleT - from.t) / span
    // 正弦缓入缓出，动作更接近真实发力节奏
    val eased = 0.5f - 0.5f * cos(PI * u.toDouble()).toFloat()
    return from.pose.lerp(to.pose, eased)
}

/**
 * 骨架小人渲染：13 个关节点连线 + 头部圆。
 */
@androidx.compose.runtime.Composable
fun StickFigure(
    pose: Pose,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF4ADE80),
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // 留边距的映射区域
        fun map(p: SkelPoint) = Offset(
            x = (0.06f + p.x * 0.88f) * w,
            y = (0.04f + p.y * 0.92f) * h,
        )

        val stroke = Stroke(width = size.minDimension * 0.030f, cap = StrokeCap.Round)
        val lineColor = color

        fun line(a: SkelPoint, b: SkelPoint) {
            drawLine(lineColor, map(a), map(b), stroke.width, stroke.cap)
        }

        // 躯干与四肢
        line(pose.neck, pose.shoulderL)
        line(pose.neck, pose.shoulderR)
        line(pose.shoulderL, pose.elbowL)
        line(pose.elbowL, pose.wristL)
        line(pose.shoulderR, pose.elbowR)
        line(pose.elbowR, pose.wristR)
        line(pose.neck, pose.hip)
        line(pose.hip, pose.kneeL)
        line(pose.kneeL, pose.ankleL)
        line(pose.hip, pose.kneeR)
        line(pose.kneeR, pose.ankleR)

        // 颈到头留一点空隙，头画圆
        val headC = map(pose.head)
        val neckC = map(pose.neck)
        val radius = (neckC - headC).getDistance() * 0.75f
        drawCircle(lineColor, radius = radius * 0.28f, center = neckC + (headC - neckC) * 0.12f) // 颈点
        drawCircle(color, radius = radius, center = headC + (neckC - headC) * 0.10f)
    }
}

/**
 * 持续动画的骨架小人（自带帧时钟，60fps）。
 */
@androidx.compose.runtime.Composable
fun AnimatedStickFigure(
    ex: Exercise,
    modifier: Modifier = Modifier,
    speed: Float = 1f,
    paused: Boolean = false,
    color: Color = Color(0xFF4ADE80),
) {
    var t by remember(ex.id) { mutableFloatStateOf(0f) }
    LaunchedEffect(ex.id, paused) {
        if (paused) return@LaunchedEffect
        var last = 0L
        while (isActive) {
            val now = androidx.compose.runtime.withFrameNanos { it }
            if (last != 0L) {
                t += (now - last) / 1_000_000_000f * speed
            }
            last = now
        }
    }
    StickFigure(pose = poseAtCycle(ex, t), modifier = modifier, color = color)
}
