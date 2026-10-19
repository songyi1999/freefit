package com.freefit.app.fitness.camera

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.freefit.app.data.Exercise
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max

/**
 * 跟练小窗：右下角悬浮的 AI 姿态识别窗（相机画面+骨架+次数+提示）。
 * 静音模式（不播报，避免与跟练倒计时语音冲突），全部反馈走视觉。
 */
@Composable
fun MiniPoseCam(
    ex: Exercise,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var landmarks by remember { mutableStateOf<List<Pt>>(emptyList()) }
    var frameW by remember { mutableIntStateOf(1) }
    var frameH by remember { mutableIntStateOf(1) }

    // 小窗模式不播报，纯视觉反馈
    val coach = remember(ex.id) { FormCoach(ex, tts = null) }

    val landmarker = remember {
        createLandmarker(context) { pts ->
            landmarks = pts
            if (pts.isNotEmpty()) coach.onPose(pts)
        }
    }
    DisposableEffect(Unit) { onDispose { landmarker.close() } }

    val analyzerExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) { onDispose { analyzerExecutor.shutdown() } }

    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f), RoundedCornerShape(14.dp)),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val future = ProcessCameraProvider.getInstance(ctx)
                future.addListener({
                    try {
                        val provider = future.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                        analysis.setAnalyzer(
                            analyzerExecutor,
                            PoseAnalyzer(landmarker) { w, h ->
                                frameW = w
                                frameH = h
                            },
                        )
                        provider.unbindAll()
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_FRONT_CAMERA,
                            preview,
                            analysis,
                        )
                    } catch (_: Exception) {
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
        )

        // 骨架叠加（前摄镜像，FILL_CENTER 映射）
        Canvas(Modifier.fillMaxSize()) {
            val pts = landmarks
            if (pts.size <= Lm.ANKLE_R) return@Canvas
            val vw = size.width
            val vh = size.height
            val bw = frameW.toFloat()
            val bh = frameH.toFloat()
            val scale = max(vw / bw, vh / bh)
            val dx = (vw - bw * scale) / 2f
            val dy = (vh - bh * scale) / 2f

            fun map(p: Pt): Offset {
                var x = dx + p.x * bw * scale
                x = vw - x // 前摄镜像
                return Offset(x, dy + p.y * bh * scale)
            }

            val lineW = size.minDimension * 0.035f
            for ((a, b) in POSE_EDGES) {
                val pa = pts[a]
                val pb = pts[b]
                if (pa.visible && pb.visible) {
                    drawLine(Color(0xFF4ADE80), map(pa), map(pb), strokeWidth = lineW, cap = StrokeCap.Round)
                }
            }
            for (idx in listOf(Lm.NOSE, Lm.SHOULDER_L, Lm.SHOULDER_R, Lm.HIP_L, Lm.HIP_R, Lm.KNEE_L, Lm.KNEE_R, Lm.ANKLE_L, Lm.ANKLE_R)) {
                val p = pts[idx]
                if (p.visible) drawCircle(Color(0xFF38BDF8), radius = lineW, center = map(p))
            }
        }

        // 次数徽标（右上角圆）
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(30.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "${coach.repCount}",
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        // 底部纠错提示条
        coach.feedback?.let { fb ->
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0x99000000)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    fb,
                    color = Color.White,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}
