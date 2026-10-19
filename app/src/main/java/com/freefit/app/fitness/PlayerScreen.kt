package com.freefit.app.fitness

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.freefit.app.data.Exercise
import com.freefit.app.data.ExerciseMode
import com.freefit.app.data.PlanItem
import com.freefit.app.data.WorkoutPlan
import com.freefit.app.data.db.AppDatabase
import com.freefit.app.data.db.SessionEntity
import com.freefit.app.t
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale
import kotlin.math.ceil

internal fun formatMmSs(sec: Float): String {
    val t = ceil(sec).toInt().coerceAtLeast(0)
    return String.format(Locale.US, "%d:%02d", t / 60, t % 60)
}

/** 动作库「试练一组」用的临时计划 */
fun adhocPlan(ex: Exercise): WorkoutPlan = WorkoutPlan(
    id = "adhoc-${ex.id}",
    nameZh = "试练 · ${ex.nameZh}",
    nameEn = if (ex.nameEn.isNotEmpty()) "Practice · ${ex.nameEn}" else "",
    descZh = "",
    descEn = "",
    levelZh = "试练",
    levelEn = "Practice",
    items = listOf(
        PlanItem(
            exerciseId = ex.id,
            sets = 1,
            durationSec = if (ex.mode == ExerciseMode.TIMED) ex.defaultDurationSec else null,
            reps = if (ex.mode == ExerciseMode.REPS) ex.defaultReps else null,
            restSec = 0,
        )
    ),
)

@Composable
fun PlayerScreen(plan: WorkoutPlan, onExit: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { AppDatabase.get(context).sessionDao() }
    val tts = remember { TtsSpeaker(context) }
    val cues = remember { SoundCues() }
    DisposableEffect(Unit) {
        onDispose {
            tts.shutdown()
            cues.release()
        }
    }

    // 训练全程保持亮屏
    val view = androidx.compose.ui.platform.LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    val controller = remember(plan.id) {
        PlayerController(plan, tts, cues) { elapsed, done, total ->
            scope.launch {
                dao.insert(
                    SessionEntity(
                        date = LocalDate.now().toString(),
                        startTime = System.currentTimeMillis(),
                        planName = plan.name,
                        durationSec = elapsed,
                        doneSets = done,
                        totalSets = total,
                    )
                )
            }
        }
    }

    LaunchedEffect(controller) {
        var last = 0L
        while (true) {
            val now = withFrameNanos { it }
            if (last != 0L) controller.tick((now - last) / 1_000_000_000f)
            last = now
        }
    }

    var showQuitDialog by remember { mutableStateOf(false) }

    // 跟练中开启 AI 摄像头小窗（右下角悬浮）
    var camOn by remember { mutableStateOf(false) }
    var camGranted by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    val camLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) {
        camGranted = it
        if (it) camOn = true
    }
    if (showQuitDialog) {
        AlertDialog(
            onDismissRequest = { showQuitDialog = false },
            title = { Text(t("结束训练？", "End workout?")) },
            text = { Text(t("已完成的部分会保存到训练记录。", "Completed sets will be saved to history.")) },
            confirmButton = {
                TextButton(onClick = { showQuitDialog = false; controller.finish() }) { Text(t("结束训练", "End")) }
            },
            dismissButton = {
                TextButton(onClick = { showQuitDialog = false }) { Text(t("继续训练", "Keep Going")) }
            },
        )
    }

    val s = controller.state
    val step = controller.steps[s.stepIndex]

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // 顶栏：退出 / 计划名 / 总进度
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = { if (s.phase == PlayerPhase.DONE) onExit() else showQuitDialog = true }) {
                Icon(Icons.Filled.Close, contentDescription = t("退出", "Exit"))
            }
            Column(Modifier.weight(1f)) {
                Text(plan.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    t("${step.ex.name} · 第 ${step.setIndex}/${step.totalSets} 组", "${step.ex.name} · Set ${step.setIndex}/${step.totalSets}"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = {
                if (!camGranted) camLauncher.launch(android.Manifest.permission.CAMERA)
                else camOn = !camOn
            }) {
                Icon(
                    Icons.Filled.Videocam,
                    contentDescription = "AI",
                    tint = if (camOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "${s.stepIndex + 1}/${controller.totalSteps}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        val stepFraction = when (s.phase) {
            PlayerPhase.ACTIVE -> (1f - (s.remain / step.targetSec)).coerceIn(0f, 1f)
            PlayerPhase.REST -> 1f
            PlayerPhase.DONE -> 1f
            PlayerPhase.READY -> 0f
        }
        LinearProgressIndicator(
            progress = { (s.stepIndex + stepFraction) / controller.totalSteps },
            modifier = Modifier.fillMaxWidth(),
        )

        // 主区域
        androidx.compose.foundation.layout.Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (s.phase) {
                PlayerPhase.DONE -> DoneContent(s, controller, onExit)
                PlayerPhase.REST -> RestContent(step, s)
                else -> ActiveContent(step, s)
            }

            // AI 摄像头小窗：跟随当前动作计数与纠错（静音，不干扰跟练语音）
            if (camOn && camGranted && s.phase != PlayerPhase.DONE) {
                com.freefit.app.fitness.camera.MiniPoseCam(
                    ex = step.ex,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .size(132.dp),
                )
            }
        }

        // 底部控制
        if (s.phase != PlayerPhase.DONE) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(onClick = controller::togglePause, modifier = Modifier.weight(1f)) {
                    Icon(
                        if (s.paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (s.paused) t("继续", "Resume") else t("暂停", "Pause"))
                }
                if (s.phase == PlayerPhase.REST) {
                    Button(onClick = controller::skipRest, modifier = Modifier.weight(1f)) {
                        Text(t("跳过休息", "Skip Rest"))
                    }
                } else {
                    OutlinedButton(onClick = controller::skipStep, modifier = Modifier.weight(1f)) {
                        Text(t("跳过本组", "Skip Set"))
                    }
                }
                TextButton(onClick = { showQuitDialog = true }, modifier = Modifier.weight(1f)) {
                    Text(t("结束", "End"))
                }
            }
        }
    }
}

@Composable
private fun ActiveContent(step: PlayerController.Step, s: PlayerController.State) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text(step.ex.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            if (s.phase == PlayerPhase.READY) t("准备开始", "Get Ready") else if (step.ex.mode == ExerciseMode.REPS) t("跟着节奏完成次数", "Match the tempo") else t("坚持住，跟着小人动起来", "Keep going, follow the figure"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AnimatedStickFigure(
            ex = step.ex,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            paused = s.paused,
        )
        val big = when {
            s.phase == PlayerPhase.READY -> ceil(s.remain).toInt().toString()
            step.ex.mode == ExerciseMode.REPS -> t("${s.currentRep.coerceAtMost(step.totalReps)} / ${step.totalReps} 次", "${s.currentRep.coerceAtMost(step.totalReps)}/${step.totalReps} reps")
            else -> formatMmSs(s.remain)
        }
        Text(big, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
        if (s.paused) {
            Text(t("已暂停", "Paused"), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp))
        } else {
            Text(
                step.ex.cues.firstOrNull() ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, start = 24.dp, end = 24.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun RestContent(step: PlayerController.Step, s: PlayerController.State) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text(t("休息一下", "Take a Rest"), style = MaterialTheme.typography.headlineSmall)
        Text(
            formatMmSs(s.remain),
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
        )
        step.next?.let { next ->
            Text(
                t("下一组：${next.name}", "Next: ${next.name}"),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
            )
        }
        Text(
            t("小口补水，调整呼吸", "Sip water, steady your breath"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // 休息小人：叉腰喘气的轻微起伏
        AnimatedStickFigure(
            ex = com.freefit.app.data.ExerciseLibrary.restFigure,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DoneContent(s: PlayerController.State, controller: PlayerController, onExit: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Text("🎉", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(8.dp))
        Text(t("训练完成", "Workout Complete"), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(formatMmSs(s.elapsed), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                Text(t("总时长", "Total Time"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${s.doneSets}/${controller.totalSteps}", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                Text(t("完成组数", "Sets Done"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (s.saved) {
            Text(
                t("记录已保存", "Saved to History"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
        Spacer(Modifier.height(32.dp))
        Button(onClick = onExit) { Text(t("返回", "Done")) }
    }
}
