package com.freefit.app.run

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.freefit.app.data.db.AppDatabase
import com.freefit.app.data.db.SessionEntity
import com.freefit.app.t
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/** 跑步详情：大图轨迹 + 数据 + 分享战绩卡片 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunDetailScreen(id: Long, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { AppDatabase.get(context).sessionDao() }
    var record by remember { mutableStateOf<SessionEntity?>(null) }
    LaunchedEffect(id) { record = dao.byId(id) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("跑步详情", "Run Details")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        val r = record
        if (r == null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize().padding(padding),
            ) { Text(t("加载中…", "Loading…")) }
            return@Scaffold
        }

        val km = r.distanceMeters / 1000f
        val paceSec = if (km > 0.03f) r.durationSec / km else 0f
        val paceText = if (paceSec > 0) {
            String.format(Locale.US, "%d'%02d\"", (paceSec / 60).toInt(), (paceSec % 60).toInt())
        } else "--"

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Card(Modifier.fillMaxWidth()) {
                TrackMapView(
                    points = ShareCard.parseRoute(r.routeJson),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .padding(6.dp),
                )
            }
            Text(
                t("底图 © 高德地图 · 双指缩放查看", "Tiles © Amap · pinch to zoom"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )

            Spacer(Modifier.height(20.dp))
            Text(
                String.format(Locale.US, "%.2f", km),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(t("公里", "km"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(36.dp)) {
                DetailStat(formatDuration(r.durationSec), t("总时长", "Duration"))
                DetailStat(paceText, t("平均配速", "Avg Pace"))
                DetailStat(r.date.substring(5), t("日期", "Date"))
            }

            Spacer(Modifier.height(28.dp))
            Button(
                onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) { ShareCard.share(context, r) }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Share, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(t("分享战绩到朋友圈", "Share to Moments"))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DetailStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatDuration(sec: Int): String =
    String.format(Locale.US, "%d:%02d:%02d", sec / 3600, (sec % 3600) / 60, sec % 60)
