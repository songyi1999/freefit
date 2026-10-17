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
    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(state.running) {
        while (state.running) {
            tick++
            delay(1000)
        }
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
    val elapsed = state.elapsedMs() / 1000
    val km = state.distanceM / 1000f
    val pace = state.paceSecPerKm()
    val paceText = if (pace > 0) String.format(Locale.US, "%d'%02d\"", (pace / 60).toInt(), (pace % 60).toInt()) else "--"

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Spacer(Modifier.weight(0.4f))
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
            Stat("${state.points.size}", "轨迹点")
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

/** 轨迹缩略图：经纬度归一化后画折线 */
@Composable
fun RouteMiniMap(points: List<Pair<Double, Double>>, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(140.dp)
                .padding(12.dp)
        ) {
            if (points.size < 2) return@Canvas
            val lats = points.map { it.first }
            val lngs = points.map { it.second }
            val minLat = lats.min(); val maxLat = lats.max()
            val minLng = lngs.min(); val maxLng = lngs.max()
            val latSpan = (maxLat - minLat).coerceAtLeast(1e-6)
            val lngSpan = (maxLng - minLng).coerceAtLeast(1e-6)
            // 保持经纬度纵横比例
            val scale = minOf(size.width / lngSpan.toFloat(), size.height / latSpan.toFloat())
            fun map(p: Pair<Double, Double>): Offset {
                val x = size.width / 2 + ((p.second - (minLng + maxLng) / 2)).toFloat() * scale
                val y = size.height / 2 - ((p.first - (minLat + maxLat) / 2)).toFloat() * scale
                return Offset(x, y)
            }
            val path = Path()
            points.forEachIndexed { i, p ->
                if (i == 0) path.moveTo(map(p).x, map(p).y) else path.lineTo(map(p).x, map(p).y)
            }
            drawPath(path, Color(0xFF4ADE80), style = Stroke(width = 4f, cap = StrokeCap.Round))
            drawCircle(Color.White, radius = 5f, center = map(points.first()))
            drawCircle(Color(0xFF38BDF8), radius = 6f, center = map(points.last()))
        }
    }
}
