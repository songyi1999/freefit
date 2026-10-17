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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.freefit.app.data.Exercise
import com.freefit.app.data.ExerciseLibrary
import com.freefit.app.data.ExerciseMode
import com.freefit.app.fitness.AnimatedStickFigure

private val difficultyNames = mapOf(1 to "入门", 2 to "进阶", 3 to "挑战")

@Composable
fun LibraryScreen(onOpenExercise: (String) -> Unit) {
    val grouped = remember { ExerciseLibrary.all.groupBy { it.category } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        grouped.forEach { (category, list) ->
            item(key = "header-$category") {
                Text(
                    category,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
            }
            items(list, key = { it.id }) { ex ->
                Card(onClick = { onOpenExercise(ex.id) }, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(ex.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(
                                "${difficultyNames[ex.difficulty] ?: ""} · ${
                                    if (ex.mode == ExerciseMode.REPS) "计次 ${ex.defaultReps} 次" else "计时 ${ex.defaultDurationSec} 秒"
                                }",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            ex.muscles.firstOrNull() ?: "",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    ex: Exercise,
    onBack: () -> Unit,
    onTry: () -> Unit,
    onCamera: () -> Unit = {},
) {
    var speed by remember { mutableFloatStateOf(1f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ex.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
            AnimatedStickFigure(
                ex = ex,
                speed = speed,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp),
            ) {
                listOf(0.5f to "0.5x", 1f to "正常", 1.5f to "1.5x").forEach { (v, label) ->
                    if (speed == v) {
                        AssistChip(onClick = { speed = v }, label = { Text(label) })
                    } else {
                        TextButton(onClick = { speed = v }) { Text(label) }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text(ex.category) })
                AssistChip(onClick = {}, label = { Text(difficultyNames[ex.difficulty] ?: "") })
            }

            Spacer(Modifier.height(12.dp))
            Text("目标肌群", style = MaterialTheme.typography.titleSmall)
            Text(
                ex.muscles.joinToString("、"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(12.dp))
            Text("动作要领", style = MaterialTheme.typography.titleSmall)
            ex.cues.forEachIndexed { i, cue ->
                Text(
                    "${i + 1}. $cue",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Spacer(Modifier.height(24.dp))
            Button(onClick = onTry, modifier = Modifier.fillMaxWidth()) {
                Text("试练一组")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onCamera, modifier = Modifier.fillMaxWidth()) {
                Text("AI 摄像头纠错")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
