package com.freefit.app.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.freefit.app.data.Plans
import com.freefit.app.data.db.AppDatabase
import com.freefit.app.update.CheckResult
import com.freefit.app.update.UpdateInfo
import com.freefit.app.update.UpdateState
import com.freefit.app.update.Updater
import com.freefit.app.t
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun HomeScreen(
    onOpenPlan: (String) -> Unit,
    onOpenCamera: () -> Unit,
    onOpenRun: () -> Unit,
    onNewPlan: () -> Unit,
    onEditProfile: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { AppDatabase.get(context).sessionDao() }
    val records by dao.observeAll().collectAsState(initial = emptyList())

    // 计划列表 = 内置 + 定制
    val allPlans = remember { Plans.all + com.freefit.app.data.PlanStore.toWorkoutPlans(context) }

    // 应用内自更新（OTA）：启动时静默检查 GitHub 最新 Release
    var updateState by remember { mutableStateOf<UpdateState>(UpdateState.Idle) }
    val currentVersion = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
    }
    LaunchedEffect(Unit) {
        val r = Updater.check(currentVersion)
        if (r is CheckResult.Found) updateState = UpdateState.Available(r.info)
    }
    var checkDialog by remember { mutableStateOf<String?>(null) }
    val startDownload: (UpdateInfo) -> Unit = { info ->
        updateState = UpdateState.Downloading(0)
        scope.launch {
            val file = Updater.download(context, info) { p -> updateState = UpdateState.Downloading(p) }
            updateState = if (file != null) UpdateState.Ready(file, info) else UpdateState.Failed(info)
        }
    }

    // 手动检查更新：入口在页面底部版本栏
    var manualChecking by remember { mutableStateOf(false) }
    var upToDate by remember { mutableStateOf(false) }
    val manualCheck: () -> Unit = {
        if (!manualChecking) {
            manualChecking = true
            upToDate = false
            scope.launch {
                when (val r = Updater.check(currentVersion)) {
                    is CheckResult.Found -> startDownload(r.info) // 直接开下，横幅立即显示进度
                    CheckResult.UpToDate -> {
                        upToDate = true
                        checkDialog = t(
                            "✓ 已是最新版本 v$currentVersion",
                            "✓ You're on the latest version v$currentVersion",
                        )
                    }
                    CheckResult.Failed -> {
                        checkDialog = t(
                            "检查失败：无法访问 GitHub。\n请切换网络（如换 WiFi）后重试，或点「用浏览器下载」手动更新。",
                            "Check failed: cannot reach GitHub.\nTry another network (e.g. WiFi) or download manually via browser.",
                        )
                    }
                }
                manualChecking = false
            }
        }
    }

    checkDialog?.let { msg ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { checkDialog = null },
            title = { Text(t("检查更新", "Check for Updates")) },
            text = { Text(msg) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { checkDialog = null }) { Text(t("知道了", "OK")) }
            },
        )
    }

    val streak = remember(records) { streakDays(records.map { it.date }) }
    val totalMin = remember(records) { records.sumOf { it.durationSec } / 60 }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            UpdateBanner(
                state = updateState,
                onDownload = startDownload,
                onInstall = { file, _ ->
                    if (Updater.canInstall(context)) Updater.install(context, file)
                    else Updater.requestInstallPermission(context)
                },
                onRetry = startDownload,
                onBrowser = { info -> Updater.openInBrowser(context, info) },
            )
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("FreeFit", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(
                        t("自律给我自由", "Self-discipline sets me free"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Card {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Icon(
                            Icons.Filled.LocalFireDepartment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(t("连续 $streak 天", "$streak-day streak"), style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
            if (totalMin > 0) {
                Text(
                    t("累计训练 $totalMin 分钟", "$totalMin min trained in total"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        item {
            // 快捷入口：AI 纠错（核心卖点）与跑步
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(onClick = onOpenCamera, modifier = Modifier.weight(1f)) {
                    Column(Modifier.padding(14.dp)) {
                        Icon(
                            Icons.Filled.Visibility,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(t("AI 实时纠错", "AI Form Check"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            t("摄像头识别姿态，自动计数", "Camera pose detection, auto counting"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Card(onClick = onOpenRun, modifier = Modifier.weight(1f)) {
                    Column(Modifier.padding(14.dp)) {
                        Icon(
                            Icons.Filled.DirectionsRun,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(t("户外跑步", "Outdoor Run"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            t("GPS 记录距离与配速", "GPS distance & pace"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Text(
                    t("训练计划", "Training Plans"),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                androidx.compose.material3.TextButton(onClick = onNewPlan) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.height(18.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(t("新建计划", "New Plan"))
                }
            }
        }
        items(allPlans) { plan ->
            Card(onClick = { onOpenPlan(plan.id) }, modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(plan.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            plan.desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            t("${plan.level} · ${plan.items.size} 个动作 · ${Plans.totalSets(plan)} 组 · 约 ${Plans.estimateSec(plan) / 60} 分钟", "${plan.level} · ${plan.items.size} moves · ${Plans.totalSets(plan)} sets · ~${Plans.estimateSec(plan) / 60} min"),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    FilledIconButton(onClick = { onOpenPlan(plan.id) }) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "开始")
                    }
                }
            }
        }

        if (records.isNotEmpty()) {
            item {
                Text(
                    t("最近记录", "Recent Activity"),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(records.take(5)) { r ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(r.planName, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                t("${r.date} · ${r.doneSets}/${r.totalSets} 组 · ${formatDuration(r.durationSec)}", "${r.date} · ${r.doneSets}/${r.totalSets} sets · ${formatDuration(r.durationSec)}"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            if (r.date == LocalDate.now().toString()) t("今天", "Today") else r.date.substring(5),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        // 个人资料（性别/年龄/器械/伤病，用于定制计划）+ 修改入口
        item {
            val g = com.freefit.app.data.PlanStore.gender(context)
            val a = com.freefit.app.data.PlanStore.age(context)
            val eq = com.freefit.app.data.PlanStore.equipment(context)
            val inj = com.freefit.app.data.PlanStore.injuries(context)
            if (g != null && a > 0) {
                val genderText = if (g == com.freefit.app.data.PlanStore.GENDER_MALE) "男" else "女"
                val genderTextEn = if (g == com.freefit.app.data.PlanStore.GENDER_MALE) "Male" else "Female"
                val eqText = if (eq == com.freefit.app.data.PlanStore.EQ_DUMBBELL) "哑铃" else "徒手"
                val eqTextEn = if (eq == com.freefit.app.data.PlanStore.EQ_DUMBBELL) "Dumbbell" else "Bodyweight"
                val injText = if (inj.isNotEmpty()) " · ${inj.size}${t("处伤病", " injuries")}" else ""
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    Text(
                        t(
                            "资料：$genderText · $a 岁 · $eqText$injText",
                            "Profile: $genderTextEn · $a · $eqTextEn$injText",
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    androidx.compose.material3.TextButton(onClick = onEditProfile) {
                        Text(t("修改", "Edit"))
                    }
                }
            }
        }

        // 版本栏：当前版本 + 手动检查更新
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                Text(
                    "FreeFit v$currentVersion",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                when {
                    manualChecking -> Text(
                        t("正在检查更新…", "Checking for updates…"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    upToDate -> Text(
                        t("✓ 已是最新版本", "✓ Up to date"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    else -> androidx.compose.material3.TextButton(onClick = manualCheck) {
                        Text(t("检查更新", "Check for Updates"))
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateBanner(
    state: UpdateState,
    onDownload: (UpdateInfo) -> Unit,
    onInstall: (java.io.File, UpdateInfo) -> Unit,
    onRetry: (UpdateInfo) -> Unit,
    onBrowser: (UpdateInfo) -> Unit,
) {
    when (state) {
        is UpdateState.Idle -> Unit
        is UpdateState.Available -> Card(onClick = { onDownload(state.info) }, modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp),
            ) {
                Icon(
                    Icons.Filled.Download,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(t("发现新版本 v${state.info.version}", "New version v${state.info.version}"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        t("点击下载更新（约 ${state.info.sizeBytes / 1048576}MB）", "Tap to download (~${state.info.sizeBytes / 1048576}MB)"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        is UpdateState.Downloading -> Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(t("正在下载新版本… ${state.progress}%", "Downloading… ${state.progress}%"), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { state.progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        is UpdateState.Ready -> Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(t("v${state.info.version} 下载完成", "v${state.info.version} downloaded"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    t("如弹出权限申请，请允许本应用安装未知应用后重试", "If prompted, allow installing unknown apps, then tap Install again"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = { onInstall(state.file, state.info) }, modifier = Modifier.fillMaxWidth()) {
                    Text(t("立即安装", "Install Now"))
                }
            }
        }

        is UpdateState.Failed -> Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(t("下载失败", "Download failed"), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onRetry(state.info) }) { Text(t("重试", "Retry")) }
                    TextButton(onClick = { onBrowser(state.info) }) { Text(t("用浏览器下载", "Open in Browser")) }
                }
            }
        }
    }
}
