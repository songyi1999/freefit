package com.freefit.app.fitness.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlin.math.max

/**
 * 逐帧分析：YUV 转位图 → 旋转至竖直 → 降采样 → MediaPipe 流式检测。
 * 限频约 12fps，lite 模型 CPU 即可实时。全屏纠错页与跟练小窗共用。
 */
internal class PoseAnalyzer(
    private val landmarker: PoseLandmarker,
    private val onFrameSize: (Int, Int) -> Unit,
) : ImageAnalysis.Analyzer {

    private var lastRunMs = 0L

    override fun analyze(image: ImageProxy) {
        try {
            val now = System.currentTimeMillis()
            if (now - lastRunMs < 80) {
                image.close()
                return
            }
            lastRunMs = now
            val rotation = image.imageInfo.rotationDegrees
            var bmp = image.toBitmap()
            image.close()

            if (rotation != 0) bmp = rotate(bmp, rotation)
            bmp = downscale(bmp, 320)
            onFrameSize(bmp.width, bmp.height)
            val mpImage = BitmapImageBuilder(bmp).build()
            landmarker.detectAsync(mpImage, System.nanoTime() / 1_000_000L)
        } catch (_: Throwable) {
            runCatching { image.close() }
        }
    }

    private fun rotate(b: Bitmap, degrees: Int): Bitmap {
        val m = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(b, 0, 0, b.width, b.height, m, true)
    }

    private fun downscale(b: Bitmap, maxSide: Int): Bitmap {
        val m = max(b.width, b.height)
        if (m <= maxSide) return b
        val ratio = maxSide.toFloat() / m
        return Bitmap.createScaledBitmap(b, (b.width * ratio).toInt().coerceAtLeast(1), (b.height * ratio).toInt().coerceAtLeast(1), true)
    }
}

/** 创建流式姿态检测器（lite 模型随 APK 分发），结果回调在主线程 */
internal fun createLandmarker(
    context: Context,
    onResult: (List<Pt>) -> Unit,
): PoseLandmarker {
    val base = BaseOptions.builder()
        .setModelAssetPath("pose_landmarker_lite.task")
        .setDelegate(Delegate.CPU)
        .build()
    val options = PoseLandmarker.PoseLandmarkerOptions.builder()
        .setBaseOptions(base)
        .setRunningMode(RunningMode.LIVE_STREAM)
        .setNumPoses(1)
        .setResultListener { result: PoseLandmarkerResult, _ ->
            val lms = result.landmarks().firstOrNull().orEmpty()
            onResult(lms.map { Pt(it.x(), it.y(), it.visibility().orElse(0.5f)) })
        }
        .setErrorListener { /* 检测失败静默，下一帧重试 */ }
        .build()
    return PoseLandmarker.createFromOptions(context, options)
}
