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

private enum class CountJoint { KNEE, ELBOW, HIP_SHOULDER_KNEE }

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
)

private val SPECS = mapOf(
    "squat" to Spec(CountJoint.KNEE, false, contractAngle = 110f, openAngle = 150f, depthWarn = 100f, bodyLineCheck = false),
    "dbsquat" to Spec(CountJoint.KNEE, false, contractAngle = 110f, openAngle = 150f, depthWarn = 100f, bodyLineCheck = false),
    "pushup" to Spec(CountJoint.ELBOW, false, contractAngle = 95f, openAngle = 150f, depthWarn = 90f, bodyLineCheck = true),
    "plank" to Spec(CountJoint.ELBOW, false, contractAngle = 95f, openAngle = 150f, depthWarn = null, bodyLineCheck = true),
    "glutebridge" to Spec(CountJoint.HIP_SHOULDER_KNEE, true, contractAngle = 145f, openAngle = 168f, depthWarn = 160f, bodyLineCheck = false),
    "lunge" to Spec(CountJoint.KNEE, false, contractAngle = 105f, openAngle = 155f, depthWarn = 100f, bodyLineCheck = false),
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
    private var lastFeedbackAt = 0L
    private var lastLineWarnAt = 0L
    private var noSupportHintShown = false

    fun onPose(lms: List<Pt>) {
        val now = System.currentTimeMillis()

        val side = pickSide(lms)
        if (side == null) {
            lastPoseGood = false
            hint(t("请调整站位/手机角度，让全身入镜", "Adjust position so your full body is in view"), now, 5000)
            return
        }
        lastPoseGood = true

        val s = spec ?: run {
            if (!noSupportHintShown) {
                noSupportHintShown = true
                feedback = t("该动作暂不支持自动计数，可对照骨架自查姿态", "Auto-count not supported for this move yet")
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

        val angle = when (s.joint) {
            CountJoint.KNEE -> angleDeg(lms[side.hip], lms[side.knee], lms[side.ankle])
            CountJoint.ELBOW -> angleDeg(lms[side.shoulder], lms[side.elbow], lms[side.wrist])
            CountJoint.HIP_SHOULDER_KNEE -> angleDeg(lms[side.shoulder], lms[side.hip], lms[side.knee])
        }

        val contracted = if (s.reversed) angle > s.openAngle else angle < s.contractAngle
        val opened = if (s.reversed) angle < s.contractAngle else angle > s.openAngle

        if (contracted) {
            if (!inRep) {
                inRep = true
                extremeAngle = angle
            } else {
                extremeAngle = if (s.reversed) maxOf(extremeAngle, angle) else minOf(extremeAngle, angle)
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
