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
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.freefit.app.data.Plans
import com.freefit.app.data.db.AppDatabase
import java.time.LocalDate

@Composable
fun HomeScreen(
    onStartPlan: (String) -> Unit,
    onOpenCamera: () -> Unit,
    onOpenRun: () -> Unit,
) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).sessionDao() }
    val records by dao.observeAll().collectAsState(initial = emptyList())

    val streak = remember(records) { streakDays(records.map { it.date }) }
    val totalMin = remember(records) { records.sumOf { it.durationSec } / 60 }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("FreeFit", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "自律给你自由",
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
                        Text("连续 $streak 天", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
            if (totalMin > 0) {
                Text(
                    "累计训练 $totalMin 分钟",
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
                        Text("AI 实时纠错", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "摄像头识别姿态，自动计数",
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
                        Text("户外跑步", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "GPS 记录距离与配速",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            Text(
                "训练计划",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        items(Plans.all) { plan ->
            Card(onClick = { onStartPlan(plan.id) }, modifier = Modifier.fillMaxWidth()) {
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
                            "${plan.level} · ${plan.items.size} 个动作 · ${Plans.totalSets(plan)} 组 · 约 ${Plans.estimateSec(plan) / 60} 分钟",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    FilledIconButton(onClick = { onStartPlan(plan.id) }) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "开始")
                    }
                }
            }
        }

        if (records.isNotEmpty()) {
            item {
                Text(
                    "最近记录",
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
                                "${r.date} · ${r.doneSets}/${r.totalSets} 组 · ${formatDuration(r.durationSec)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            if (r.date == LocalDate.now().toString()) "今天" else r.date.substring(5),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}
