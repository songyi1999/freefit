package com.freefit.app.fitness.camera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.freefit.app.data.Exercise
import com.freefit.app.fitness.TtsSpeaker
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max

/**
 * 摄像头 AI 纠错：实时骨架叠加 + 次数统计 + 语音/文字纠错提示。
 */
@Composable
fun PoseCameraScreen(ex: Exercise, onExit: () -> Unit) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
    }
    LaunchedEffect(Unit) { if (!hasPermission) launcher.launch(Manifest.permission.CAMERA) }

    if (!hasPermission) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
        ) {
            Text("需要相机权限才能进行姿态识别", textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) { Text("授予权限") }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onExit) { Text("返回") }
        }
        return
    }
    CameraContent(ex, onExit)
}

@Composable
private fun CameraContent(ex: Exercise, onExit: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var facing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_FRONT) }
    var landmarks by remember { mutableStateOf<List<Pt>>(emptyList()) }
    var frameW by remember { mutableIntStateOf(1) }
    var frameH by remember { mutableIntStateOf(1) }

    val tts = remember { TtsSpeaker(context) }
    DisposableEffect(Unit) { onDispose { tts.shutdown() } }

    val coach = remember(ex.id) { FormCoach(ex, tts) }

    val landmarker = remember(ex.id) {
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
                val pts = lms.map { Pt(it.x(), it.y(), it.visibility().orElse(0.5f)) }
                landmarks = pts
                if (pts.isNotEmpty()) coach.onPose(pts)
            }
            .setErrorListener { /* 检测失败静默，下一帧重试 */ }
            .build()
        PoseLandmarker.createFromOptions(context, options)
    }
    DisposableEffect(ex.id) { onDispose { landmarker.close() } }

    val analyzerExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) { onDispose { analyzerExecutor.shutdown() } }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onExit) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
            Column(Modifier.weight(1f)) {
                Text(ex.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "AI 实时纠错 · 手机放在正侧方",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = {
                facing = if (facing == CameraSelector.LENS_FACING_FRONT) CameraSelector.LENS_FACING_BACK
                else CameraSelector.LENS_FACING_FRONT
            }) {
                Icon(Icons.Filled.Cameraswitch, contentDescription = "切换摄像头")
            }
        }

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            key(facing) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
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
                                    CameraSelector.Builder().requireLensFacing(facing).build(),
                                    preview,
                                    analysis,
                                )
                            } catch (_: Exception) {
                                // 相机被占用等异常，保持黑屏可退出
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                )
            }

            // 骨架叠加层：FILL_CENTER 映射 + 前摄镜像
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
                val mirror = facing == CameraSelector.LENS_FACING_FRONT

                fun map(p: Pt): Offset {
                    var x = dx + p.x * bw * scale
                    if (mirror) x = vw - x
                    return Offset(x, dy + p.y * bh * scale)
                }

                val lineW = size.minDimension * 0.012f
                for ((a, b) in POSE_EDGES) {
                    val pa = pts[a]
                    val pb = pts[b]
                    if (pa.visible && pb.visible) {
                        drawLine(
                            Color(0xFF4ADE80),
                            map(pa),
                            map(pb),
                            strokeWidth = lineW,
                            cap = StrokeCap.Round,
                        )
                    }
                }
                // 关节点
                for (idx in listOf(Lm.NOSE, Lm.SHOULDER_L, Lm.SHOULDER_R, Lm.HIP_L, Lm.HIP_R, Lm.KNEE_L, Lm.KNEE_R, Lm.ANKLE_L, Lm.ANKLE_R)) {
                    val p = pts[idx]
                    if (p.visible) drawCircle(Color(0xFF38BDF8), radius = lineW * 0.9f, center = map(p))
                }
            }

            // 纠错提示条
            coach.feedback?.let { fb ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(Color(0xB3000000))
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(fb, color = Color.White, style = MaterialTheme.typography.titleSmall)
                }
            }
        }

        // 底部计数与要领
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                "${coach.repCount}",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text("完成次数（自动统计）", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!coach.lastPoseGood) {
                Text(
                    "未检测到完整人体",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Text(
                ex.cues.firstOrNull() ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/**
 * 逐帧分析：YUV 转位图 → 旋转至竖直 → 降采样 → MediaPipe 流式检测。
 * 限频约 12fps，lite 模型 CPU 即可实时。
 */
private class PoseAnalyzer(
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
