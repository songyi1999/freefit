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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.freefit.app.data.db.AppDatabase
import com.freefit.app.data.db.SessionEntity
import com.freefit.app.t
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen(onOpenRunDetail: (Long) -> Unit = {}) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).sessionDao() }
    val records by dao.observeAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var deleteTarget by remember { mutableStateOf<SessionEntity?>(null) }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(t("删除这条记录？", "Delete this record?")) },
            text = { Text("${target.date} · ${target.planName}") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { dao.delete(target.id) }
                    deleteTarget = null
                }) { Text(t("删除", "Delete")) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(t("取消", "Cancel")) }
            },
        )
    }

    val totalTimes = records.size
    val totalSec = records.sumOf { it.durationSec }
    val streak = streakDays(records.map { it.date })

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(t("训练统计", "Your Stats"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        t("累计 $totalTimes 次 · ${formatDuration(totalSec)} · 连续 $streak 天", "$totalTimes workouts · ${formatDuration(totalSec)} · $streak-day streak"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        if (records.isEmpty()) {
            item {
                Text(
                    t("还没有训练记录，去首页开始第一次训练吧 💪", "No workouts yet — start your first one from Home 💪"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 32.dp),
                )
            }
        }

        items(records, key = { it.id }) { r ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = r.distanceMeters > 0) { onOpenRunDetail(r.id) },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(r.planName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(
                            if (r.distanceMeters > 0) {
                                val paceSec = if (r.distanceMeters > 0) r.durationSec / (r.distanceMeters / 1000f) else 0f
                                t("${r.date} · ${"%.2f".format(r.distanceMeters / 1000f)} 公里 · 配速 ${paceSec.toInt() / 60}'${"%02d".format(paceSec.toInt() % 60)}\" · ${formatDuration(r.durationSec)}", "${r.date} · ${"%.2f".format(r.distanceMeters / 1000f)} km · ${paceSec.toInt() / 60}'${"%02d".format(paceSec.toInt() % 60)}\"/km · ${formatDuration(r.durationSec)}")
                            } else {
                                t("${r.date} · 完成 ${r.doneSets}/${r.totalSets} 组 · ${formatDuration(r.durationSec)}", "${r.date} · ${r.doneSets}/${r.totalSets} sets · ${formatDuration(r.durationSec)}")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { deleteTarget = r }) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
