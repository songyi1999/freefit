package com.freefit.app.run

import androidx.compose.foundation.Canvas
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * 轨迹绘制：黄色高亮线 + 多层光晕，白点起点、黄环终点。
 * 无地图底图（离线可用），风格参考主流运动 App 分享卡片。
 */
@androidx.compose.runtime.Composable
fun RouteCanvas(
    points: List<Pair<Double, Double>>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFFFACC15),
) {
    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas

        val lats = points.map { it.first }
        val lngs = points.map { it.second }
        val minLat = lats.min(); val maxLat = lats.max()
        val minLng = lngs.min(); val maxLng = lngs.max()
        val latSpan = (maxLat - minLat).coerceAtLeast(1e-6)
        val lngSpan = (maxLng - minLng).coerceAtLeast(1e-6)

        // 保持经纬度纵横比例，整体居中
        val scale = minOf(size.width / lngSpan.toFloat(), size.height / latSpan.toFloat())
        val latC = ((minLat + maxLat) / 2).toFloat()
        val lngC = ((minLng + maxLng) / 2).toFloat()
        fun map(p: Pair<Double, Double>): Offset = Offset(
            x = size.width / 2 + ((p.second - lngC)).toFloat() * scale,
            y = size.height / 2 - ((p.first - latC)).toFloat() * scale,
        )

        val path = Path()
        points.forEachIndexed { i, p ->
            val o = map(p)
            if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
        }

        val base = size.minDimension
        // 三层描边模拟光晕：宽淡 -> 窄亮
        val passes = listOf(
            base * 0.075f to 0.10f,
            base * 0.045f to 0.28f,
            base * 0.022f to 1.0f,
        )
        for ((width, alpha) in passes) {
            drawPath(
                path,
                color = lineColor.copy(alpha = alpha),
                style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }

        // 起点：白点
        drawCircle(Color.White, radius = base * 0.016f, center = map(points.first()))
        // 终点：黄色环 + 内点
        drawCircle(lineColor, radius = base * 0.024f, center = map(points.last()))
        drawCircle(Color(0xFF0B1220), radius = base * 0.010f, center = map(points.last()))
    }
}
