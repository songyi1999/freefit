package com.freefit.app.fitness.camera

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.freefit.app.data.Exercise
import com.freefit.app.fitness.TtsSpeaker
import com.freefit.app.t
import kotlin.math.acos
import kotlin.math.sqrt

/** MediaPipe Pose 33 点位中我们用到的索引 */
object Lm {
    const val NOSE = 0
    const val SHOULDER_L = 11
    const val SHOULDER_R = 12
    const val ELBOW_L = 13
    const val ELBOW_R = 14
    const val WRIST_L = 15
    const val WRIST_R = 16
    const val HIP_L = 23
    const val HIP_R = 24
    const val KNEE_L = 25
    const val KNEE_R = 26
    const val ANKLE_L = 27
    const val ANKLE_R = 28
}

/** 一个检测到的关节点，坐标为相对画面的 0..1 */
data class Pt(val x: Float, val y: Float, val vis: Float) {
    val visible: Boolean get() = vis > 0.5f
}

/** b 点处两条线段 ab/bc 的夹角（度） */
fun angleDeg(a: Pt, b: Pt, c: Pt): Float {
    val abx = a.x - b.x
    val aby = a.y - b.y
    val cbx = c.x - b.x
    val cby = c.y - b.y
    val magA = sqrt(abx * abx + aby * aby)
    val magC = sqrt(cbx * cbx + cby * cby)
    if (magA < 1e-6f || magC < 1e-6f) return 180f
    val cos = ((abx * cbx + aby * cby) / (magA * magC)).coerceIn(-1f, 1f)
    return Math.toDegrees(acos(cos.toDouble())).toFloat()
}

/** 人体骨架连线（用于叠加绘制） */
val POSE_EDGES = listOf(
    Lm.SHOULDER_L to Lm.SHOULDER_R,
    Lm.SHOULDER_L to Lm.ELBOW_L, Lm.ELBOW_L to Lm.WRIST_L,
    Lm.SHOULDER_R to Lm.ELBOW_R, Lm.ELBOW_R to Lm.WRIST_R,
    Lm.SHOULDER_L to Lm.HIP_L, Lm.SHOULDER_R to Lm.HIP_R,
    Lm.HIP_L to Lm.HIP_R,
    Lm.HIP_L to Lm.KNEE_L, Lm.KNEE_L to Lm.ANKLE_L,
    Lm.HIP_R to Lm.KNEE_R, Lm.KNEE_R to Lm.ANKLE_R,
)

/** 左/右侧关键点索引组 */
private class Side(val shoulder: Int, val elbow: Int, val wrist: Int, val hip: Int, val knee: Int, val ankle: Int)

private val SIDE_L = Side(Lm.SHOULDER_L, Lm.ELBOW_L, Lm.WRIST_L, Lm.HIP_L, Lm.KNEE_L, Lm.ANKLE_L)
private val SIDE_R = Side(Lm.SHOULDER_R, Lm.ELBOW_R, Lm.WRIST_R, Lm.HIP_R, Lm.KNEE_R, Lm.ANKLE_R)

private enum class CountJoint { KNEE, ELBOW, HIP_SHOULDER_KNEE, HIP_SHOULDER_WRIST, HIP_DROP }

private class Spec(
    val joint: CountJoint,
    /** reversed=false：关节角变小算"底部"（如深蹲）；true：角度变大算"顶点"（如臀桥） */
    val reversed: Boolean,
    val contractAngle: Float,
    val openAngle: Float,
    /** 一次动作的极值角度不达标时提示幅度不足 */
    val depthWarn: Float?,
    /** 俯身类动作检查肩-髋-踝是否一条直线 */
    val bodyLineCheck: Boolean,
    /** 双腿交替动作（高抬腿/登山跑）：左右腿独立追踪计步 */
    val perLeg: Boolean = false,
)

private val SPECS = mapOf(
    // 下肢：膝角（站立~175，蹲底~80-100）
    "squat" to Spec(CountJoint.HIP_DROP, false, contractAngle = 0.40f, openAngle = 0.70f, depthWarn = 0.50f, bodyLineCheck = false),
    "dbsquat" to Spec(CountJoint.HIP_DROP, false, contractAngle = 0.40f, openAngle = 0.70f, depthWarn = 0.50f, bodyLineCheck = false),
    "lunge" to Spec(CountJoint.KNEE, false, contractAngle = 105f, openAngle = 155f, depthWarn = 100f, bodyLineCheck = false),
    "highknees" to Spec(CountJoint.KNEE, false, contractAngle = 115f, openAngle = 155f, depthWarn = null, bodyLineCheck = false, perLeg = true),
    "mountainclimber" to Spec(CountJoint.KNEE, false, contractAngle = 135f, openAngle = 160f, depthWarn = null, bodyLineCheck = true, perLeg = true),
    // 上肢：肘角（伸直~170，弯~40-90）
    "pushup" to Spec(CountJoint.ELBOW, false, contractAngle = 95f, openAngle = 150f, depthWarn = 90f, bodyLineCheck = true),
    "plank" to Spec(CountJoint.ELBOW, false, contractAngle = 95f, openAngle = 150f, depthWarn = null, bodyLineCheck = true),
    "dbrow" to Spec(CountJoint.ELBOW, false, contractAngle = 105f, openAngle = 160f, depthWarn = null, bodyLineCheck = false),
    "dbpress" to Spec(CountJoint.ELBOW, false, contractAngle = 90f, openAngle = 160f, depthWarn = null, bodyLineCheck = false),
    "dbshoulderpress" to Spec(CountJoint.ELBOW, false, contractAngle = 100f, openAngle = 160f, depthWarn = null, bodyLineCheck = false),
    "dbcurl" to Spec(CountJoint.ELBOW, false, contractAngle = 70f, openAngle = 150f, depthWarn = null, bodyLineCheck = false),
    // 髋角（肩-髋-膝）：臀桥/卷腹/硬拉
    "glutebridge" to Spec(CountJoint.HIP_SHOULDER_KNEE, true, contractAngle = 140f, openAngle = 162f, depthWarn = 155f, bodyLineCheck = false),
    "crunch" to Spec(CountJoint.HIP_SHOULDER_KNEE, false, contractAngle = 68f, openAngle = 85f, depthWarn = 72f, bodyLineCheck = false),
    "dbrdl" to Spec(CountJoint.HIP_SHOULDER_KNEE, false, contractAngle = 135f, openAngle = 160f, depthWarn = null, bodyLineCheck = false),
    // 开合跳：肩-髋-腕夹角（垂手~15-40，举顶~150）
    "jumpingjack" to Spec(CountJoint.HIP_SHOULDER_WRIST, true, contractAngle = 50f, openAngle = 135f, depthWarn = null, bodyLineCheck = false),
)

/**
 * 根据实时姿态做计数与动作质量提示。
 * 只依赖 2D 关键点，规则刻意保持简单可靠。
 */
class FormCoach(private val ex: Exercise, private val tts: TtsSpeaker?) {

    var repCount by mutableIntStateOf(0)
        private set
    var feedback by mutableStateOf<String?>(null)
        private set
    var lastPoseGood by mutableStateOf(false)
        private set

    private val spec = SPECS[ex.id]
    private var inRep = false
    private var extremeAngle = Float.MIN_VALUE
    private var legLInRep = false
    private var legRInRep = false
    private var lastFeedbackAt = 0L
    private var lastLineWarnAt = 0L
    private var noSupportHintShown = false

    fun onPose(lms: List<Pt>) {
        val now = System.currentTimeMillis()
        val s = spec

        // 深蹲族（髋部下落比例）：不选侧、不看肩——手持哑铃时哑铃恰好遮挡单侧髋腿，
        // 旧的"选一侧+含肩可见度门槛"会被卡死
        if (s != null && s.joint == CountJoint.HIP_DROP) {
            val ratio = hipDropRatio(lms)
            if (ratio == null) {
                lastPoseGood = false
                hint(t("请调整站位/手机角度，让全身入镜", "Adjust position so your full body is in view"), now, 5000)
                return
            }
            lastPoseGood = true
            trackAngle(ratio, s, now)
            return
        }

        val side = pickSide(lms)
        if (side == null) {
            lastPoseGood = false
            hint(t("请调整站位/手机角度，让全身入镜", "Adjust position so your full body is in view"), now, 5000)
            return
        }
        lastPoseGood = true

        if (s == null) {
            if (!noSupportHintShown) {
                noSupportHintShown = true
                feedback = t("保持类动作：对照骨架保持标准姿态即可", "Hold move: match the figure and keep good form")
            }
            return
        }

        if (s.bodyLineCheck) {
            val line = angleDeg(lms[side.shoulder], lms[side.hip], lms[side.ankle])
            if (line < 150f && now - lastLineWarnAt > 6000) {
                lastLineWarnAt = now
                speakFeedback(t("保持头、髋、脚踝一条直线", "Keep head, hips and ankles in a line"), now)
            }
        }

        if (s.perLeg) {
            onPerLeg(lms, s, now)
            return
        }

        val angle = when (s.joint) {
            CountJoint.KNEE -> angleDeg(lms[side.hip], lms[side.knee], lms[side.ankle])
            CountJoint.ELBOW -> angleDeg(lms[side.shoulder], lms[side.elbow], lms[side.wrist])
            CountJoint.HIP_SHOULDER_KNEE -> angleDeg(lms[side.shoulder], lms[side.hip], lms[side.knee])
            CountJoint.HIP_DROP -> 0f // 已在 onPose 前置分支处理，不会走到这里
            CountJoint.HIP_SHOULDER_WRIST -> {
                // 肩关节处 髋-肩-腕 夹角；开合跳双臂对称，取双臂平均更抗遮挡抖动
                val angles = listOf(SIDE_L, SIDE_R).map { sd ->
                    angleDeg(lms[sd.hip], lms[sd.shoulder], lms[sd.wrist])
                }
                angles.average().toFloat()
            }
        }

        trackAngle(angle, s, now)
    }

    /** 通用计数状态机：value 越过 contractAngle 为收缩段，回到 openAngle 之外记一次 */
    private fun trackAngle(value: Float, s: Spec, now: Long) {
        val contracted = if (s.reversed) value > s.openAngle else value < s.contractAngle
        val opened = if (s.reversed) value < s.contractAngle else value > s.openAngle

        if (contracted) {
            if (!inRep) {
                inRep = true
                extremeAngle = value
            } else {
                extremeAngle = if (s.reversed) maxOf(extremeAngle, value) else minOf(extremeAngle, value)
            }
        } else if (opened && inRep) {
            inRep = false
            val shallow = s.depthWarn?.let { warn ->
                if (s.reversed) extremeAngle < warn else extremeAngle > warn
            } == true
            if (shallow) {
                speakFeedback(if (s.reversed) t("幅度不够，再顶高一点", "Hips higher for a full rep") else t("幅度不够，再蹲低一点", "Go deeper for a full rep"), now)
            } else {
                repCount++
                feedback = t("✅ 第 $repCount 次", "✅ Rep $repCount")
                tts?.speak("${repCount}")
            }
        }
    }

    /** 髋部下落比例：单腿 (膝y-髋y)/(膝y-踝y)，可见腿按可见度加权平均；站立≈1、蹲底≈0，与拍摄方向无关 */
    private fun hipDropRatio(lms: List<Pt>): Float? {
        var acc = 0f
        var wSum = 0f
        listOf(SIDE_L, SIDE_R).forEach { sd ->
            val vis = (lms[sd.hip].vis + lms[sd.knee].vis + lms[sd.ankle].vis) / 3f
            if (vis >= 0.35f) {
                val r = ((lms[sd.knee].y - lms[sd.hip].y) /
                    (lms[sd.knee].y - lms[sd.ankle].y).coerceAtLeast(0.02f)).coerceIn(0f, 1.5f)
                acc += r * vis
                wSum += vis
            }
        }
        return if (wSum > 0f) acc / wSum else null
    }

    /** 双腿交替动作：左右腿独立状态机，任一腿完成屈伸各计一次 */
    private fun onPerLeg(lms: List<Pt>, s: Spec, now: Long) {
        var counted = false
        listOf(SIDE_L to true, SIDE_R to false).forEach { (sd, isLeft) ->
            val vis = (lms[sd.hip].vis + lms[sd.knee].vis + lms[sd.ankle].vis) / 3f
            if (vis < 0.45f) return@forEach
            val a = angleDeg(lms[sd.hip], lms[sd.knee], lms[sd.ankle])
            val inRep = if (isLeft) legLInRep else legRInRep
            when {
                a < s.contractAngle && !inRep ->
                    if (isLeft) legLInRep = true else legRInRep = true
                a > s.openAngle && inRep -> {
                    if (isLeft) legLInRep = false else legRInRep = false
                    repCount++
                    counted = true
                }
            }
        }
        if (counted) {
            feedback = t("✓ 第 $repCount 次", "✓ Rep $repCount")
            if (tts != null && now - lastFeedbackAt > 1500) {
                lastFeedbackAt = now
                tts.speak("$repCount")
            }
        }
    }

    private fun speakFeedback(msg: String, now: Long) {
        feedback = "⚠️ $msg"
        if (now - lastFeedbackAt > 3000) {
            lastFeedbackAt = now
            tts?.speak(msg)
        }
    }

    private fun hint(msg: String, now: Long, interval: Long) {
        if (now - lastFeedbackAt > interval) {
            lastFeedbackAt = now
            feedback = msg
        }
    }

    private fun pickSide(lms: List<Pt>): Side? {
        if (lms.size <= Lm.ANKLE_R) return null
        fun avgVis(side: Side): Float =
            (lms[side.shoulder].vis + lms[side.hip].vis + lms[side.knee].vis + lms[side.ankle].vis) / 4f
        val lv = avgVis(SIDE_L)
        val rv = avgVis(SIDE_R)
        val side = if (lv >= rv) SIDE_L else SIDE_R
        return if (maxOf(lv, rv) > 0.5f) side else null
    }
}
