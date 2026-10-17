package com.freefit.app.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.freefit.app.data.ExerciseLibrary
import com.freefit.app.data.ExerciseMode
import com.freefit.app.data.Plans
import com.freefit.app.data.WorkoutPlan
import com.freefit.app.t

/** 计划总览：动作清单 + 数据预估，点「开始训练」才进入跟练 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanDetailScreen(
    plan: WorkoutPlan,
    isCustom: Boolean,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onOpenExercise: (String) -> Unit,
    onDelete: () -> Unit,
) {
    var showDelete by remember { mutableStateOf(false) }
    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text(t("删除这个定制计划？", "Delete this custom plan?")) },
            text = { Text(plan.name) },
            confirmButton = {
                TextButton(onClick = { showDelete = false; onDelete() }) { Text(t("删除", "Delete")) }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text(t("取消", "Cancel")) }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(plan.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = t("返回", "Back"))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(
                plan.desc,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                t(
                    "${plan.level} · ${plan.items.size} 个动作 · ${Plans.totalSets(plan)} 组 · 约 ${Plans.estimateSec(plan) / 60} 分钟",
                    "${plan.level} · ${plan.items.size} moves · ${Plans.totalSets(plan)} sets · ~${Plans.estimateSec(plan) / 60} min",
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(Modifier.height(16.dp))
            Text(t("动作清单", "Exercise List"), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            plan.items.forEachIndexed { index, item ->
                val ex = ExerciseLibrary.byId(item.exerciseId)
                val load = if (item.durationSec != null) {
                    t("${item.sets} 组 × ${item.durationSec} 秒", "${item.sets} × ${item.durationSec}s")
                } else {
                    t("${item.sets} 组 × ${item.reps ?: ex.defaultReps} 次", "${item.sets} × ${item.reps ?: ex.defaultReps} reps")
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onOpenExercise(ex.id) },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Text(
                            "${index + 1}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(28.dp),
                        )
                        Column(Modifier.weight(1f)) {
                            Text(ex.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(
                                "$load · ${t("休息 ${item.restSec} 秒", "rest ${item.restSec}s")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(t("开始训练", "Start Workout"))
            }
            if (isCustom) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { showDelete = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(t("删除定制计划", "Delete Custom Plan"))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
