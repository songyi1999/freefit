package com.freefit.app.run

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.freefit.app.data.db.AppDatabase
import com.freefit.app.data.db.SessionEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

@Composable
fun RunScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { AppDatabase.get(context).sessionDao() }
    val state by RunTracker.state.collectAsState()

    // 秒级刷新计时显示
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state.running) {
        while (state.running) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }

    // 跑步进行中保持亮屏
    val view = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.DisposableEffect(state.running) {
        view.keepScreenOn = state.running
        onDispose { view.keepScreenOn = false }
    }

    var locationGranted by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var notificationGranted by remember { mutableStateOf(true) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        locationGranted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true
        notificationGranted = grants[Manifest.permission.POST_NOTIFICATIONS] != false
    }

    fun requestPermissions() {
        val perms = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(perms.toTypedArray())
    }

    if (!state.running) {
        // 待机态
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
        ) {
            Icon(
                Icons.Filled.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.height(72.dp).fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Text("户外跑步", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "GPS 记录轨迹、距离与配速，数据只保存在本机",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = {
                    if (locationGranted) RunService.start(context) else requestPermissions()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("开始跑步") }
        }
        return
    }

    // 进行中 / 暂停 / 结束确认
    val elapsed = state.elapsedMs(now) / 1000
    val km = state.distanceM / 1000f
    val pace = state.paceSecPerKm()
    val paceText = if (pace > 0) String.format(Locale.US, "%d'%02d\"", (pace / 60).toInt(), (pace % 60).toInt()) else "--"

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Spacer(Modifier.weight(0.3f))
        Text(
            formatElapsed(elapsed),
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
        )
        Text("用时", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
            Stat(String.format(Locale.US, "%.2f", km), "公里")
            Stat(paceText, "配速/公里")
        }
        Spacer(Modifier.height(16.dp))
        if (state.points.size > 1) {
            Card(Modifier.fillMaxWidth()) {
                RouteCanvas(
                    points = state.points,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .padding(10.dp),
                )
            }
        }
        Spacer(Modifier.weight(1f))
        if (state.paused) {
            Button(
                onClick = { RunService.resume(context) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("继续")
            }
        } else {
            FilledTonalButton(
                onClick = { RunService.pause(context) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Pause, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("暂停")
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = {
                RunService.stop(context)
                scope.launch {
                    delay(300) // 等服务收尾
                    val final = RunTracker.finish()
                    if (final.points.size > 1 && final.distanceM > 20f) {
                        dao.insert(
                            SessionEntity(
                                date = LocalDate.now().toString(),
                                startTime = final.startedAt,
                                planName = "户外跑步",
                                durationSec = (final.elapsedMs() / 1000).toInt(),
                                doneSets = 0,
                                totalSets = 0,
                                distanceMeters = final.distanceM.toInt(),
                                routeJson = final.points.joinToString(";") { "${"%.5f".format(it.first)},${"%.5f".format(it.second)}" },
                            )
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Stop, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("结束并保存")
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatElapsed(sec: Long): String =
    String.format(Locale.US, "%d:%02d:%02d", sec / 3600, (sec % 3600) / 60, sec % 60)

