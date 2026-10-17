package com.freefit.app.data

import com.freefit.app.L10n
import com.freefit.app.t

/** 归一化骨骼点，x/y 取值 0..1，坐标系：x 向右、y 向下 */
data class SkelPoint(val x: Float, val y: Float) {
    fun lerp(o: SkelPoint, t: Float) = SkelPoint(x + (o.x - x) * t, y + (o.y - y) * t)
}

fun P(x: Float, y: Float) = SkelPoint(x, y)

/**
 * 一个姿态快照：13 个关节点。
 * L/R 仅用于侧视图中的微小前后错位或正面图中的左右肢体。
 */
data class Pose(
    val head: SkelPoint,
    val neck: SkelPoint,
    val shoulderL: SkelPoint,
    val shoulderR: SkelPoint,
    val elbowL: SkelPoint,
    val elbowR: SkelPoint,
    val wristL: SkelPoint,
    val wristR: SkelPoint,
    val hip: SkelPoint,
    val kneeL: SkelPoint,
    val kneeR: SkelPoint,
    val ankleL: SkelPoint,
    val ankleR: SkelPoint,
) {
    fun lerp(o: Pose, t: Float) = Pose(
        head = head.lerp(o.head, t),
        neck = neck.lerp(o.neck, t),
        shoulderL = shoulderL.lerp(o.shoulderL, t),
        shoulderR = shoulderR.lerp(o.shoulderR, t),
        elbowL = elbowL.lerp(o.elbowL, t),
        elbowR = elbowR.lerp(o.elbowR, t),
        wristL = wristL.lerp(o.wristL, t),
        wristR = wristR.lerp(o.wristR, t),
        hip = hip.lerp(o.hip, t),
        kneeL = kneeL.lerp(o.kneeL, t),
        kneeR = kneeR.lerp(o.kneeR, t),
        ankleL = ankleL.lerp(o.ankleL, t),
        ankleR = ankleR.lerp(o.ankleR, t),
    )
}

/** 关键帧：t 为动作周期内的时刻（0..1），pose 为该时刻姿态 */
data class Keyframe(val t: Float, val pose: Pose)

enum class ExerciseMode { TIMED, REPS }

data class Exercise(
    val id: String,
    val nameZh: String,
    val nameEn: String = "",
    val categoryZh: String,
    val categoryEn: String = "",
    /** 1 入门 / 2 进阶 / 3 挑战 */
    val difficulty: Int,
    val musclesZh: List<String>,
    val musclesEn: List<String> = emptyList(),
    /** 动作要领 */
    val cuesZh: List<String>,
    val cuesEn: List<String> = emptyList(),
    val mode: ExerciseMode,
    /** 计时模式的默认时长（秒） */
    val defaultDurationSec: Int,
    /** 计次模式的默认次数 */
    val defaultReps: Int,
    /** 完整一个动作周期的秒数（驱动动画与计次节奏） */
    val cycleSec: Float,
    /** 所需器械：bodyweight 徒手 / dumbbell 哑铃 */
    val equipment: String = "bodyweight",
    /** 有以下伤病时应避免该动作（injury id） */
    val avoid: List<String> = emptyList(),
    val keyframes: List<Keyframe>,
) {
    val name: String get() = t(nameZh, nameEn)
    val category: String get() = t(categoryZh, categoryEn)
    val muscles: List<String> get() = if (L10n.english && musclesEn.isNotEmpty()) musclesEn else musclesZh
    val cues: List<String> get() = if (L10n.english && cuesEn.isNotEmpty()) cuesEn else cuesZh
}

data class PlanItem(
    val exerciseId: String,
    val sets: Int,
    val durationSec: Int? = null,
    val reps: Int? = null,
    val restSec: Int,
)

data class WorkoutPlan(
    val id: String,
    val nameZh: String,
    val nameEn: String = "",
    val descZh: String,
    val descEn: String = "",
    val levelZh: String,
    val levelEn: String = "",
    val items: List<PlanItem>,
) {
    val name: String get() = t(nameZh, nameEn)
    val desc: String get() = t(descZh, descEn)
    val level: String get() = t(levelZh, levelEn)
}
