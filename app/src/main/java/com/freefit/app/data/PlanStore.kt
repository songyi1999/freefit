package com.freefit.app.data

import android.content.Context
import com.freefit.app.t
import org.json.JSONArray
import org.json.JSONObject

/**
 * 本地存储：用户资料（性别/年龄/器械/伤病，只问一次）+ 定制训练计划。
 * 规则式生成器根据资料与目标推荐 4~5 个动作并调整强度。
 */
object PlanStore {

    private const val PREFS = "freefit_store"
    private const val KEY_GENDER = "gender"
    private const val KEY_AGE = "age"
    private const val KEY_EQUIPMENT = "equipment"
    private const val KEY_INJURIES = "injuries"
    private const val KEY_CUSTOM = "custom_plans"

    const val GENDER_MALE = "male"
    const val GENDER_FEMALE = "female"
    const val EQ_BODYWEIGHT = "bodyweight"
    const val EQ_DUMBBELL = "dumbbell"

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

    data class Injury(val id: String, val zh: String, val en: String)

    val injuries = listOf(
        Injury("foot", "脚踝/脚部", "Foot / Ankle"),
        Injury("knee", "膝盖", "Knee"),
        Injury("back", "腰部", "Lower Back"),
        Injury("shoulder", "肩部", "Shoulder"),
        Injury("wrist", "手腕", "Wrist"),
    )

    fun goalLabel(id: String): String = goals.firstOrNull { it.id == id }
        ?.let { t(it.zh, it.en) } ?: id

    fun injuryLabel(id: String): String = injuries.firstOrNull { it.id == id }
        ?.let { t(it.zh, it.en) } ?: id

    // ---------- 用户资料 ----------

    fun gender(ctx: Context): String? = prefs(ctx).getString(KEY_GENDER, null)

    fun age(ctx: Context): Int = prefs(ctx).getInt(KEY_AGE, 0)

    fun equipment(ctx: Context): String? = prefs(ctx).getString(KEY_EQUIPMENT, null)

    fun injuries(ctx: Context): Set<String> = prefs(ctx).getStringSet(KEY_INJURIES, emptySet()) ?: emptySet()

    /** 资料完整 = 四项都已设置（含"无伤病"这种空集，通过 KEY_INJURIES 是否存在判断） */
    fun hasProfile(ctx: Context): Boolean =
        gender(ctx) != null && age(ctx) > 0 && equipment(ctx) != null && prefs(ctx).contains(KEY_INJURIES)

    fun saveProfile(ctx: Context, gender: String, age: Int, equipment: String, injuries: Set<String>) {
        prefs(ctx).edit()
            .putString(KEY_GENDER, gender)
            .putInt(KEY_AGE, age)
            .putString(KEY_EQUIPMENT, equipment)
            .putStringSet(KEY_INJURIES, injuries)
            .apply()
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

    /** 当前资料下可用的动作（器械匹配 + 伤病规避） */
    fun allowedExercises(equipment: String, injuries: Set<String>): List<Exercise> =
        ExerciseLibrary.all.filter { ex ->
            (ex.equipment == EQ_BODYWEIGHT || equipment == EQ_DUMBBELL) &&
                ex.avoid.none { it in injuries }
        }

    /** 替换/添加动作时的候选池 */
    fun candidates(equipment: String, injuries: Set<String>, exclude: Set<String>): List<Exercise> =
        allowedExercises(equipment, injuries).filter { it.id !in exclude && it.id != "rest" }

    /** 按年龄档给动作定量 */
    fun buildItem(exId: String, age: Int, setsOverride: Int? = null): PlanItem {
        val ex = ExerciseLibrary.byId(exId)
        val senior = age >= 56
        val mid = age in 36..55
        val sets = setsOverride ?: when {
            senior -> 2
            else -> 3
        }
        val rest = when {
            senior -> 60
            mid -> 45
            else -> 40
        }
        return if (ex.mode == ExerciseMode.TIMED) {
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

    /**
     * 规则式推荐：目标定动作池（哑铃可用时用哑铃版组合），年龄调强度，
     * 伤病过滤后不足 4 个时从可用池补位。
     */
    fun generate(gender: String, age: Int, goalId: String, equipment: String, injuries: Set<String>): CustomPlan {
        val senior = age >= 56
        val hasDb = equipment == EQ_DUMBBELL

        val pool: List<String> = when (goalId) {
            "upper" -> when {
                hasDb && !senior -> listOf("dbpress", "dbrow", "dbshoulderpress", "dbcurl", "plank")
                hasDb -> listOf("dbcurl", "dbrow", "plank", "superman", "crunch")
                senior -> listOf("pushup", "plank", "superman", "crunch")
                else -> listOf("pushup", "plank", "mountainclimber", "superman", "crunch")
            }
            "lower" -> when {
                hasDb && !senior -> listOf("dbsquat", "dbrdl", "glutebridge", "squat", "plank")
                hasDb -> listOf("dbsquat", "glutebridge", "superman", "plank")
                senior -> listOf("squat", "glutebridge", "superman", "plank")
                else -> listOf("squat", "lunge", "glutebridge", "jumpingjack", "plank")
            }
            "core" -> if (senior) listOf("plank", "crunch", "superman", "glutebridge")
            else listOf("plank", "crunch", "mountainclimber", "superman", "glutebridge")
            else -> when {
                hasDb && !senior -> listOf("jumpingjack", "dbsquat", "highknees", "mountainclimber", "dbpress")
                senior -> listOf("squat", "glutebridge", "plank", "superman")
                else -> listOf("jumpingjack", "highknees", "squat", "mountainclimber", "pushup")
            }
        }

        val allowed = allowedExercises(equipment, injuries).map { it.id }.toSet()
        val targetCount = if (senior) 4 else 5
        val selected = pool.filter { it in allowed }.toMutableList()

        // 伤病过滤后不足，从可用池补位（优先目标分类，再全局）
        if (selected.size < targetCount) {
            val goalCategory = when (goalId) {
                "upper" -> "上肢"
                "lower" -> "下肢"
                "core" -> "核心"
                else -> "燃脂"
            }
            val backfill = allowedExercises(equipment, injuries)
                .sortedByDescending { it.categoryZh == goalCategory }
                .map { it.id }
                .filter { it !in selected }
            selected += backfill.take(targetCount - selected.size)
        }

        val items = selected.take(5).map { buildItem(it, age) }
        return CustomPlan(
            id = "c_${System.currentTimeMillis()}",
            name = goalLabel(goalId),
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
