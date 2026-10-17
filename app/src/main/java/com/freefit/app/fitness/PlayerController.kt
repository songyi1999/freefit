package com.freefit.app.fitness

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.freefit.app.data.Exercise
import com.freefit.app.data.ExerciseLibrary
import com.freefit.app.data.ExerciseMode
import com.freefit.app.data.PlanItem
import com.freefit.app.data.WorkoutPlan
import kotlin.math.ceil
import kotlin.math.roundToInt

enum class PlayerPhase { READY, ACTIVE, REST, DONE }

/**
 * 跟练状态机：准备(3s) → 训练 → 组间休息 → 下一组 …… → 完成。
 * 由界面帧时钟驱动 tick()。
 */
class PlayerController(
    plan: WorkoutPlan,
    private val tts: TtsSpeaker?,
    private val cues: SoundCues? = null,
    private val onFinish: (elapsedSec: Int, doneSets: Int, totalSets: Int) -> Unit,
) {
    data class Step(
        val ex: Exercise,
        val setIndex: Int,
        val totalSets: Int,
        val targetSec: Float,
        val restSec: Int,
        val isLast: Boolean,
        val next: Exercise?,
    ) {
        val totalReps: Int get() = (targetSec / ex.cycleSec).roundToInt()
    }

    data class State(
        val phase: PlayerPhase = PlayerPhase.READY,
        val stepIndex: Int = 0,
        val remain: Float = 3f,
        val paused: Boolean = false,
        val elapsed: Float = 0f,
        val doneSets: Int = 0,
        val currentRep: Int = 0,
        val saved: Boolean = false,
    )

    val steps: List<Step> = buildSteps(plan)
    val totalSteps: Int get() = steps.size

    var state by mutableStateOf(State())
        private set

    private var lastSpokenSec = Int.MIN_VALUE
    private var savedOnce = false

    /** 帧驱动；dt 秒 */
    fun tick(dt: Float) {
        val s = state
        if (s.paused || s.phase == PlayerPhase.DONE) return
        val step = steps[s.stepIndex]

        state = when (s.phase) {
            PlayerPhase.READY -> {
                val remain = s.remain - dt
                speakCountdown(remain)
                if (remain <= 0f) {
                    tts?.speak("开始")
                    cues?.setStart()
                    s.copy(phase = PlayerPhase.ACTIVE, remain = step.targetSec, elapsed = s.elapsed + dt)
                } else {
                    s.copy(remain = remain, elapsed = s.elapsed + dt)
                }
            }

            PlayerPhase.ACTIVE -> {
                val remain = s.remain - dt
                val progress = step.targetSec - remain
                val rep = if (step.ex.mode == ExerciseMode.REPS) {
                    ((progress / step.ex.cycleSec).toInt() + 1).coerceAtLeast(1)
                } else 0
                if (remain <= 0f) {
                    val doneSets = s.doneSets + 1
                    when {
                        step.isLast -> {
                            tts?.speak("训练完成，辛苦了")
                            cues?.finish()
                            s.copy(phase = PlayerPhase.DONE, remain = 0f, doneSets = doneSets,
                                currentRep = step.totalReps, elapsed = s.elapsed + dt)
                                .also { save(it.elapsed, doneSets) }
                        }
                        step.restSec <= 0 -> {
                            tts?.speak("直接开始下一组")
                            advance(s, doneSets, s.elapsed + dt)
                        }
                        else -> {
                            tts?.speak("休息")
                            cues?.setDone()
                            State(phase = PlayerPhase.REST, stepIndex = s.stepIndex,
                                remain = step.restSec.toFloat(), elapsed = s.elapsed + dt, doneSets = doneSets)
                        }
                    }
                } else {
                    s.copy(remain = remain, currentRep = rep, elapsed = s.elapsed + dt)
                }
            }

            PlayerPhase.REST -> {
                val remain = s.remain - dt
                speakCountdown(remain)
                if (remain <= 0f) {
                    cues?.restOver()
                    advance(s, s.doneSets, s.elapsed + dt)
                } else {
                    s.copy(remain = remain, elapsed = s.elapsed + dt)
                }
            }

            PlayerPhase.DONE -> s
        }
    }

    fun togglePause() {
        val s = state
        if (s.phase != PlayerPhase.DONE) state = s.copy(paused = !s.paused)
    }

    /** 跳过当前组（不计入完成组数） */
    fun skipStep() {
        val s = state
        if (s.phase == PlayerPhase.DONE) return
        tts?.speak("跳过")
        if (steps[s.stepIndex].isLast) {
            state = s.copy(phase = PlayerPhase.DONE, remain = 0f)
            save(s.elapsed, s.doneSets)
        } else {
            state = advanceState(s)
        }
    }

    /** 跳过休息，直接进入下一组 */
    fun skipRest() {
        val s = state
        if (s.phase == PlayerPhase.REST) {
            cues?.restOver()
            state = advanceState(s)
        }
    }

    /** 主动结束训练 */
    fun finish() {
        val s = state
        if (s.phase == PlayerPhase.DONE) return
        val worthSaving = s.elapsed >= 30f || s.doneSets > 0
        if (worthSaving) {
            tts?.speak("训练结束")
            save(s.elapsed, s.doneSets)
        }
        state = s.copy(phase = PlayerPhase.DONE, remain = 0f, paused = false)
    }

    private fun advance(s: State, doneSets: Int, elapsed: Float): State =
        advanceState(s).copy(doneSets = doneSets, elapsed = elapsed)

    private fun advanceState(s: State): State {
        lastSpokenSec = Int.MIN_VALUE
        return State(
            phase = PlayerPhase.READY,
            stepIndex = (s.stepIndex + 1).coerceAtMost(steps.size - 1),
            elapsed = s.elapsed,
            doneSets = s.doneSets,
        )
    }

    private fun speakCountdown(remain: Float) {
        if (tts == null) return
        val sec = ceil(remain).toInt()
        if (sec in 1..3 && sec != lastSpokenSec) {
            lastSpokenSec = sec
            tts.speak("$sec")
        }
    }

    private fun save(elapsed: Float, doneSets: Int) {
        if (savedOnce) return
        savedOnce = true
        state = state.copy(saved = true)
        onFinish(elapsed.roundToInt(), doneSets, steps.size)
    }

    private fun buildSteps(plan: WorkoutPlan): List<Step> {
        data class Flat(val item: PlanItem, val ex: Exercise, val setIndex: Int)

        val flat = plan.items.flatMap { item ->
            val ex = ExerciseLibrary.byId(item.exerciseId)
            (1..item.sets).map { setIndex -> Flat(item, ex, setIndex) }
        }
        return flat.mapIndexed { idx, f ->
            Step(
                ex = f.ex,
                setIndex = f.setIndex,
                totalSets = f.item.sets,
                targetSec = (f.item.durationSec
                    ?: ((f.item.reps ?: f.ex.defaultReps) * f.ex.cycleSec)).toFloat(),
                restSec = if (idx == flat.size - 1) 0 else f.item.restSec,
                isLast = idx == flat.size - 1,
                next = flat.getOrNull(idx + 1)?.ex,
            )
        }
    }
}
