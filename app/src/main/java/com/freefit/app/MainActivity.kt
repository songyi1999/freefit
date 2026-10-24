package com.freefit.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.freefit.app.data.ExerciseLibrary
import com.freefit.app.data.Plans
import com.freefit.app.fitness.PlayerScreen
import com.freefit.app.fitness.adhocPlan
import com.freefit.app.fitness.camera.PoseCameraScreen
import com.freefit.app.run.RunScreen
import com.freefit.app.screens.ExerciseDetailScreen
import com.freefit.app.screens.HistoryScreen
import com.freefit.app.screens.HomeScreen
import com.freefit.app.screens.LibraryScreen
import com.freefit.app.ui.FreeFitTheme
import com.freefit.app.t

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FreeFitTheme {
                AppRoot()
            }
        }
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

@Composable
fun AppRoot() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? ComponentActivity

    // 首次启动隐私协议（未同意前不进入应用、不申请任何权限）
    var privacyAgreed by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(PrivacyGate.agreed(context))
    }
    if (!privacyAgreed) {
        PrivacyDialog(
            onAgree = {
                PrivacyGate.agree(context)
                privacyAgreed = true
            },
            onExit = { activity?.finish() },
        )
        return
    }

    val nav = rememberNavController()
    val tabs = listOf(
        Tab("home", t("首页", "Home"), Icons.Filled.Home),
        Tab("library", t("动作库", "Library"), Icons.Filled.FitnessCenter),
        Tab("run", t("跑步", "Run"), Icons.Filled.DirectionsRun),
        Tab("history", t("记录", "History"), Icons.Filled.History),
    )
    val tabRoutes = tabs.map { it.route }.toSet()

    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = currentRoute in tabRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            if (currentRoute != tab.route) {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(navController = nav, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") {
                HomeScreen(
                    onOpenPlan = { nav.navigate("plan/$it") },
                    onOpenCamera = { nav.navigate("camera/squat") },
                    onOpenRun = { nav.navigate("run") },
                    onNewPlan = { nav.navigate("newplan") },
                    onEditProfile = { nav.navigate("newplanEdit") },
                )
            }
            composable("plan/{id}") { entry ->
                val ctx = androidx.compose.ui.platform.LocalContext.current
                val id = entry.arguments?.getString("id") ?: "p1"
                val plan = com.freefit.app.data.PlanStore.byIdAny(ctx, id)
                if (plan != null) {
                    com.freefit.app.screens.PlanDetailScreen(
                        plan = plan,
                        isCustom = plan.id.startsWith("c_"),
                        onBack = { nav.popBackStack() },
                        onStart = { nav.navigate("player/plan/${plan.id}") },
                        onOpenExercise = { nav.navigate("exercise/$it") },
                        onDelete = {
                            com.freefit.app.data.PlanStore.deletePlan(ctx, plan.id)
                            nav.popBackStack()
                        },
                    )
                }
            }
            composable("newplan") {
                com.freefit.app.screens.NewPlanScreen(
                    onBack = { nav.popBackStack() },
                    onSaved = { nav.popBackStack() },
                    onStartPlan = {
                        nav.navigate("player/plan/$it") {
                            popUpTo("home")
                        }
                    },
                )
            }
            composable("newplanEdit") {
                com.freefit.app.screens.NewPlanScreen(
                    editProfile = true,
                    onBack = { nav.popBackStack() },
                    onSaved = { nav.popBackStack() },
                    onStartPlan = {
                        nav.navigate("player/plan/$it") {
                            popUpTo("home")
                        }
                    },
                )
            }
            composable("library") {
                LibraryScreen(onOpenExercise = { nav.navigate("exercise/$it") })
            }
            composable("run") {
                RunScreen(onOpenDetail = { nav.navigate("rundetail/$it") })
            }
            composable("history") {
                HistoryScreen(onOpenRunDetail = { nav.navigate("rundetail/$it") })
            }
            composable("rundetail/{id}") { entry ->
                val id = entry.arguments?.getString("id")?.toLongOrNull() ?: 0L
                com.freefit.app.run.RunDetailScreen(id = id, onBack = { nav.popBackStack() })
            }
            composable("exercise/{id}") { entry ->
                val ex = ExerciseLibrary.byId(entry.arguments?.getString("id") ?: "squat")
                ExerciseDetailScreen(
                    ex = ex,
                    onBack = { nav.popBackStack() },
                    onTry = { nav.navigate("player/exercise/${ex.id}") },
                    onCamera = { nav.navigate("camera/${ex.id}") },
                )
            }
            composable("camera/{id}") { entry ->
                val ex = ExerciseLibrary.byId(entry.arguments?.getString("id") ?: "squat")
                PoseCameraScreen(ex = ex, onExit = { nav.popBackStack() })
            }
            composable("player/plan/{id}") { entry ->
                val ctx = androidx.compose.ui.platform.LocalContext.current
                val id = entry.arguments?.getString("id") ?: "p1"
                val plan = com.freefit.app.data.PlanStore.byIdAny(ctx, id) ?: Plans.byId(id)
                PlayerScreen(plan = plan, onExit = { nav.popBackStack() })
            }
            composable("player/exercise/{id}") { entry ->
                val ex = ExerciseLibrary.byId(entry.arguments?.getString("id") ?: "squat")
                PlayerScreen(plan = adhocPlan(ex), onExit = { nav.popBackStack() })
            }
        }
    }
}

@Composable
private fun PrivacyDialog(onAgree: () -> Unit, onExit: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.material3.AlertDialog(
        onDismissRequest = {},
        title = { Text(t("用户协议与隐私政策", "Privacy Policy")) },
        text = {
            androidx.compose.foundation.layout.Column {
                Text(
                    t(
                        "自由炼是一款完全离线、无账号、无服务器的健身应用：训练记录、跑步轨迹、个人资料仅保存在本机；相机画面与定位仅在你主动使用对应功能时于本机实时处理，不上传任何数据。点击「同意」即表示你已阅读并同意",
                        "FreeFit is fully offline with no account and no server: all data stays on your device. Camera and location are processed on-device only when you use those features. Nothing is ever uploaded. Tapping Agree means you accept",
                    ),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(4.dp))
                androidx.compose.material3.TextButton(onClick = {
                    runCatching {
                        context.startActivity(
                            android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(PrivacyGate.POLICY_URL),
                            ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }) {
                    Text(
                        t("查看完整隐私政策 →", "View full privacy policy →"),
                        color = androidx.compose.material3.MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onAgree) {
                Text(t("同意并继续", "Agree & Continue"))
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onExit) {
                Text(t("不同意，退出", "Exit"))
            }
        },
    )
}
