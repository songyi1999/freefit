package com.freefit.app.data

import android.content.Context
import com.freefit.app.t
import org.json.JSONArray
import org.json.JSONObject

/**
 * 本地存储：用户资料（性别/年龄，只问一次）+ 定制训练计划。
 * 规则式生成器根据 性别/年龄/目标 推荐 4~5 个动作并调整强度。
 */
object PlanStore {

    private const val PREFS = "freefit_store"
    private const val KEY_GENDER = "gender"
    private const val KEY_AGE = "age"
    private const val KEY_CUSTOM = "custom_plans"

    const val GENDER_MALE = "male"
    const val GENDER_FEMALE = "female"

    data class CustomPlan(
        val id: String,
        val name: String,
        val items: List<PlanItem>,
        val createdAt: Long,
    )

    data class Goal(val id: String, val zh: String, val en: String)

    val goals = listOf(
        Goal("upper", "上肢力量", "Upper Body"),
        Goal("lower", "下肢与臀腿", "Lower Body"),
        Goal("core", "核心强化", "Core Strength"),
        Goal("cardio", "全身燃脂", "Fat Burn"),
    )

    fun goalLabel(id: String): String = goals.firstOrNull { it.id == id }
        ?.let { t(it.zh, it.en) } ?: id

    // ---------- 用户资料 ----------

    fun gender(ctx: Context): String? = prefs(ctx).getString(KEY_GENDER, null)

    fun age(ctx: Context): Int = prefs(ctx).getInt(KEY_AGE, 0)

    fun hasProfile(ctx: Context): Boolean = gender(ctx) != null && age(ctx) > 0

    fun saveProfile(ctx: Context, gender: String, age: Int) {
        prefs(ctx).edit().putString(KEY_GENDER, gender).putInt(KEY_AGE, age).apply()
    }

    // ---------- 定制计划 ----------

    fun customPlans(ctx: Context): List<CustomPlan> {
        val raw = prefs(ctx).getString(KEY_CUSTOM, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val items = (0 until o.getJSONArray("items").length()).map { j ->
                    val it = o.getJSONArray("items").getJSONObject(j)
                    PlanItem(
                        exerciseId = it.getString("ex"),
                        sets = it.getInt("sets"),
                        durationSec = if (it.has("dur")) it.getInt("dur") else null,
                        reps = if (it.has("reps")) it.getInt("reps") else null,
                        restSec = it.getInt("rest"),
                    )
                }
                CustomPlan(o.getString("id"), o.getString("name"), items, o.getLong("at"))
            }
        }.getOrDefault(emptyList())
    }

    fun addPlan(ctx: Context, plan: CustomPlan) {
        val all = customPlans(ctx).filter { it.id != plan.id } + plan
        prefs(ctx).edit().putString(KEY_CUSTOM, toJson(all)).apply()
    }

    fun deletePlan(ctx: Context, id: String) {
        val all = customPlans(ctx).filter { it.id != id }
        prefs(ctx).edit().putString(KEY_CUSTOM, toJson(all)).apply()
    }

    /** 定制计划转 WorkoutPlan（含中英文描述） */
    fun toWorkoutPlans(ctx: Context): List<WorkoutPlan> = customPlans(ctx).map {
        WorkoutPlan(
            id = it.id,
            nameZh = it.name,
            nameEn = "",
            descZh = "根据你的资料定制",
            descEn = "Customized for you",
            levelZh = "定制",
            levelEn = "Custom",
            items = it.items,
        )
    }

    /** 统一解析：内置计划优先，其次定制计划 */
    fun byIdAny(ctx: Context, id: String): WorkoutPlan? =
        Plans.all.firstOrNull { it.id == id } ?: toWorkoutPlans(ctx).firstOrNull { it.id == id }

    // ---------- 生成器 ----------

    /**
     * 规则式推荐：
     * - 目标决定动作池（热身/主项/辅助搭配，人工编排过的组合）
     * - 年龄分三档调强度：≤35 标准 / 36-55 减量 / ≥56 只保留入门动作并降低组数
     * - 性别用于个性化文案；负荷差异主要由年龄与难度决定
     */
    fun generate(gender: String, age: Int, goalId: String): CustomPlan {
        val senior = age >= 56
        val mid = age in 36..55

        val pool: List<String> = when (goalId) {
            "upper" -> if (senior) listOf("pushup", "plank", "superman", "crunch")
            else listOf("pushup", "plank", "mountainclimber", "superman", "crunch")
            "lower" -> if (senior) listOf("squat", "glutebridge", "superman", "plank")
            else listOf("squat", "lunge", "glutebridge", "jumpingjack", "plank")
            "core" -> if (senior) listOf("plank", "crunch", "superman", "glutebridge")
            else listOf("plank", "crunch", "mountainclimber", "superman", "glutebridge")
            else -> if (senior) listOf("squat", "glutebridge", "plank", "superman")
            else listOf("jumpingjack", "highknees", "squat", "mountainclimber", "pushup")
        }

        val sets = when {
            senior -> 2
            mid -> 3
            else -> 3
        }
        val rest = when {
            senior -> 60
            mid -> 45
            else -> 40
        }

        val items = pool.map { exId ->
            val ex = ExerciseLibrary.byId(exId)
            if (ex.mode == ExerciseMode.TIMED) {
                val dur = when {
                    senior -> 25
                    mid -> 30
                    else -> 40
                }
                PlanItem(exId, sets, durationSec = dur, restSec = rest)
            } else {
                val reps = when {
                    senior -> if (ex.difficulty == 1) 10 else 8
                    mid -> if (ex.difficulty == 1) 12 else 10
                    else -> if (ex.difficulty == 1) 15 else 12
                }
                PlanItem(exId, sets, reps = reps, restSec = rest)
            }
        }

        val label = goalLabel(goalId)
        return CustomPlan(
            id = "c_${System.currentTimeMillis()}",
            name = label,
            items = items,
            createdAt = System.currentTimeMillis(),
        )
    }

    // ---------- 内部 ----------

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun toJson(plans: List<CustomPlan>): String {
        val arr = JSONArray()
        plans.forEach { p ->
            val items = JSONArray()
            p.items.forEach {
                val o = JSONObject().put("ex", it.exerciseId).put("sets", it.sets).put("rest", it.restSec)
                it.durationSec?.let { d -> o.put("dur", d) }
                it.reps?.let { r -> o.put("reps", r) }
                items.put(o)
            }
            arr.put(JSONObject().put("id", p.id).put("name", p.name).put("items", items).put("at", p.createdAt))
        }
        return arr.toString()
    }
}
