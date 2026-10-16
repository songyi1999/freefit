package com.freefit.app.data

object Plans {

    val all: List<WorkoutPlan> = listOf(
        WorkoutPlan(
            id = "p1", name = "新手全身启动",
            desc = "隔天练一次，动作简单，帮你建立训练习惯",
            level = "入门",
            items = listOf(
                PlanItem("squat", sets = 3, reps = 12, restSec = 45),
                PlanItem("pushup", sets = 3, reps = 8, restSec = 45),
                PlanItem("plank", sets = 3, durationSec = 30, restSec = 45),
                PlanItem("jumpingjack", sets = 2, durationSec = 20, restSec = 30),
            ),
        ),
        WorkoutPlan(
            id = "p2", name = "核心强化",
            desc = "针对性腰腹训练，改善核心力量与体态",
            level = "进阶",
            items = listOf(
                PlanItem("plank", sets = 3, durationSec = 45, restSec = 30),
                PlanItem("crunch", sets = 3, reps = 16, restSec = 30),
                PlanItem("mountainclimber", sets = 3, durationSec = 30, restSec = 30),
                PlanItem("superman", sets = 2, durationSec = 30, restSec = 30),
            ),
        ),
        WorkoutPlan(
            id = "p3", name = "燃脂 HIIT",
            desc = "高强度间歇循环，短时间全身燃脂",
            level = "挑战",
            items = listOf(
                PlanItem("jumpingjack", sets = 4, durationSec = 30, restSec = 20),
                PlanItem("squat", sets = 4, durationSec = 45, restSec = 20),
                PlanItem("highknees", sets = 4, durationSec = 30, restSec = 20),
                PlanItem("mountainclimber", sets = 4, durationSec = 30, restSec = 20),
                PlanItem("pushup", sets = 3, reps = 12, restSec = 20),
            ),
        ),
    )

    fun byId(id: String): WorkoutPlan = all.firstOrNull { it.id == id }
        ?: error("未知计划 id: $id")

    /** 预估一次训练总时长（含组间休息），单位秒 */
    fun estimateSec(plan: WorkoutPlan): Int = plan.items.sumOf { item ->
        val ex = ExerciseLibrary.byId(item.exerciseId)
        val perSet = item.durationSec ?: ((item.reps ?: ex.defaultReps) * ex.cycleSec).toInt()
        item.sets * (perSet + item.restSec)
    }

    /** 计划总组数 */
    fun totalSets(plan: WorkoutPlan): Int = plan.items.sumOf { it.sets }
}
