package com.freefit.app.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.freefit.app.data.ExerciseLibrary
import com.freefit.app.data.PlanStore
import com.freefit.app.t

/**
 * 新建计划向导（参考 Keep AI 问卷式）：
 * 性别 → 年龄 → 目标 → 生成 4~5 个动作的定制计划。
 * 性别/年龄只问一次（本地记住），已有资料时直接进目标选择。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewPlanScreen(
    editProfile: Boolean = false,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onStartPlan: (String) -> Unit,
) {
    val context = LocalContext.current
    val hasProfile = remember { PlanStore.hasProfile(context) }
    var gender by remember { mutableStateOf(PlanStore.gender(context) ?: PlanStore.GENDER_MALE) }
    var age by remember { mutableIntStateOf(PlanStore.age(context).let { if (it > 0) it else 25 }) }
    // editProfile=true 用于首页「修改资料」，强制从第一步开始
    var step by remember { mutableIntStateOf(if (editProfile || !hasProfile) 0 else 2) }
    var plan by remember { mutableStateOf<PlanStore.CustomPlan?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("新建计划", "New Plan")) },
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
                .padding(20.dp),
        ) {
            LinearProgressIndicator(
                progress = { (step + 1) / 4f },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(20.dp))

            when (step) {
                0 -> {
                    StepTitle(t("你的性别是？", "What's your gender?"))
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GenderCard(
                            selected = gender == PlanStore.GENDER_MALE,
                            icon = Icons.Filled.Male,
                            label = t("男", "Male"),
                            modifier = Modifier.weight(1f),
                        ) { gender = PlanStore.GENDER_MALE }
                        GenderCard(
                            selected = gender == PlanStore.GENDER_FEMALE,
                            icon = Icons.Filled.Female,
                            label = t("女", "Female"),
                            modifier = Modifier.weight(1f),
                        ) { gender = PlanStore.GENDER_FEMALE }
                    }
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { step = 1 },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(t("下一步", "Next")) }
                }

                1 -> {
                    StepTitle(t("你的年龄是？", "What's your age?"))
                    Spacer(Modifier.height(24.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        IconButton(onClick = { if (age > 12) age-- }) {
                            Icon(Icons.Filled.Remove, contentDescription = "-")
                        }
                        Text(
                            "$age",
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 28.dp),
                        )
                        IconButton(onClick = { if (age < 90) age++ }) {
                            Icon(Icons.Filled.Add, contentDescription = "+")
                        }
                    }
                    Text(
                        t("岁（影响训练强度推荐）", "years old (affects intensity)"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = {
                            PlanStore.saveProfile(context, gender, age)
                            step = 2
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(t("下一步", "Next")) }
                }

                2 -> {
                    StepTitle(t("你希望增强什么？", "What's your goal?"))
                    Spacer(Modifier.height(16.dp))
                    val goalIcons = mapOf(
                        "upper" to Icons.Filled.FitnessCenter,
                        "lower" to Icons.Filled.Accessibility,
                        "core" to Icons.Filled.SelfImprovement,
                        "cardio" to Icons.Filled.LocalFireDepartment,
                    )
                    PlanStore.goals.chunked(2).forEach { rowGoals ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            rowGoals.forEach { g ->
                                GoalCard(
                                    icon = goalIcons[g.id] ?: Icons.Filled.FitnessCenter,
                                    label = t(g.zh, g.en),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    PlanStore.saveProfile(context, gender, age)
                                    plan = PlanStore.generate(gender, age, g.id)
                                    step = 3
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }

                3 -> {
                    val p = plan ?: return@Column
                    StepTitle(t("为你生成定制计划", "Your custom plan is ready"))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        t(
                            "根据资料（${if (gender == PlanStore.GENDER_MALE) "男" else "女"} · $age 岁）与目标定制",
                            "Based on your profile ($age) and goal",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(p.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            p.items.forEachIndexed { i, item ->
                                val ex = ExerciseLibrary.byId(item.exerciseId)
                                val load = if (item.durationSec != null) {
                                    t("${item.sets} 组 × ${item.durationSec} 秒", "${item.sets} × ${item.durationSec}s")
                                } else {
                                    t("${item.sets} 组 × ${item.reps} 次", "${item.sets} × ${item.reps} reps")
                                }
                                Text(
                                    "${i + 1}. ${ex.name} · $load",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(vertical = 2.dp),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = {
                            PlanStore.addPlan(context, p)
                            onStartPlan(p.id)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(t("保存并开始训练", "Save & Start")) }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            PlanStore.addPlan(context, p)
                            onSaved()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(t("保存到首页", "Save to Home")) }
                }
            }
        }
    }
}

@Composable
private fun StepTitle(text: String) {
    Text(text, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
}

@Composable
private fun GenderCard(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(onClick = onClick, modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.height(40.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.titleMedium)
            if (selected) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "✓",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}

@Composable
private fun GoalCard(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(onClick = onClick, modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.height(36.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                t("生成计划", "Generate"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
