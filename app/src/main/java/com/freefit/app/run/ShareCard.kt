package com.freefit.app.run

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.freefit.app.data.db.SessionEntity
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.min

/**
 * 跑步战绩分享卡片：纯代码绘制位图（1080x1600），无任何依赖。
 * 生成后通过系统分享面板发出（微信/朋友圈选择"图片"即可）。
 */
object ShareCard {

    private const val YELLOW = 0xFFFACC15.toInt()
    private const val WHITE = 0xFFF8FAFC.toInt()
    private const val GRAY = 0xFF94A3B8.toInt()
    private const val GREEN = 0xFF4ADE80.toInt()

    fun parseRoute(json: String?): List<Pair<Double, Double>> =
        json?.split(';')?.mapNotNull { seg ->
            val p = seg.split(',')
            val lat = p.getOrNull(0)?.toDoubleOrNull()
            val lng = p.getOrNull(1)?.toDoubleOrNull()
            if (lat != null && lng != null) lat to lng else null
        }.orEmpty()

    fun generate(r: SessionEntity): Bitmap {
        val w = 1080
        val h = 1600
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        // 深色渐变背景
        p.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            0xFF0A1020.toInt(), 0xFF152238.toInt(), Shader.TileMode.CLAMP,
        )
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        p.shader = null

        fun text(s: String, size: Float, color: Int, bold: Boolean, cx: Float, baseline: Float) {
            p.textSize = size
            p.color = color
            p.textAlign = Paint.Align.CENTER
            p.typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
            c.drawText(s, cx, baseline, p)
        }

        val km = r.distanceMeters / 1000f

        // 头部
        text("FREEFIT · 户外跑步", 46f, GRAY, false, w / 2f, 150f)

        // 大距离数字
        text(String.format(Locale.US, "%.2f", km), 210f, WHITE, true, w / 2f, 400f)
        text("公里", 52f, GRAY, false, w / 2f, 480f)

        // 时长 / 配速
        val durationSec = r.durationSec
        val paceSec = if (km > 0.03f) durationSec / km else 0f
        val paceText = if (paceSec > 0) {
            String.format(Locale.US, "%d'%02d\"", (paceSec / 60).toInt(), (paceSec % 60).toInt())
        } else "--"
        val durationText = String.format(
            Locale.US, "%d:%02d:%02d", durationSec / 3600, (durationSec % 3600) / 60, durationSec % 60,
        )
        text("用时", 42f, GRAY, false, w * 0.30f, 600f)
        text(durationText, 68f, WHITE, true, w * 0.30f, 680f)
        text("平均配速", 42f, GRAY, false, w * 0.70f, 600f)
        text(paceText, 68f, WHITE, true, w * 0.70f, 680f)

        // 轨迹区域
        val areaTop = 800f
        val areaBottom = 1300f
        val padX = 160f
        val pts = parseRoute(r.routeJson)
        if (pts.size >= 2) {
            val lats = pts.map { it.first }
            val lngs = pts.map { it.second }
            val minLat = lats.min(); val maxLat = lats.max()
            val minLng = lngs.min(); val maxLng = lngs.max()
            val latSpan = (maxLat - minLat).coerceAtLeast(1e-6)
            val lngSpan = (maxLng - minLng).coerceAtLeast(1e-6)
            val areaW = w - padX * 2
            val areaH = areaBottom - areaTop
            val scale = min(areaW / lngSpan.toFloat(), areaH / latSpan.toFloat())
            val latC = ((minLat + maxLat) / 2).toFloat()
            val lngC = ((minLng + maxLng) / 2).toFloat()
            fun x(pt: Pair<Double, Double>) = padX + areaW / 2 + ((pt.second - lngC)).toFloat() * scale
            fun y(pt: Pair<Double, Double>) = areaTop + areaH / 2 - ((pt.first - latC)).toFloat() * scale

            val path = Path()
            pts.forEachIndexed { i, pt ->
                if (i == 0) path.moveTo(x(pt), y(pt)) else path.lineTo(x(pt), y(pt))
            }
            p.style = Paint.Style.STROKE
            p.strokeJoin = Paint.Join.ROUND
            p.strokeCap = Paint.Cap.ROUND
            // 光晕三层
            for ((width, alpha) in listOf(56f to 40, 32f to 90, 14f to 255)) {
                p.strokeWidth = width
                p.color = YELLOW
                p.alpha = alpha
                c.drawPath(path, p)
            }
            p.style = Paint.Style.FILL
            p.alpha = 255
            // 起终点
            p.color = WHITE
            c.drawCircle(x(pts.first()), y(pts.first()), 16f, p)
            p.color = YELLOW
            c.drawCircle(x(pts.last()), y(pts.last()), 22f, p)
            p.color = 0xFF0A1020.toInt()
            c.drawCircle(x(pts.last()), y(pts.last()), 10f, p)
        } else {
            text("本次未记录到轨迹", 48f, GRAY, false, w / 2f, (areaTop + areaBottom) / 2)
        }

        // 底部
        text(r.date, 44f, GRAY, false, w / 2f, 1440f)
        text("自律给我自由", 52f, GREEN, true, w / 2f, 1520f)

        return bmp
    }

    /** 生成并弹出系统分享面板（微信里选"朋友圈"即可） */
    fun share(context: Context, r: SessionEntity): Bitmap {
        val bitmap = generate(r)
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, "freefit-run-${r.id}.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "分享跑步战绩"))
        return bitmap
    }
}
