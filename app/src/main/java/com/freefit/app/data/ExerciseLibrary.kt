package com.freefit.app.data

import com.freefit.app.data.ExerciseMode.TIMED
import com.freefit.app.data.ExerciseMode.REPS

/**
 * 内置动作库。每个动作 = 关键帧姿态数据（AI 生成，可手工调整）。
 * 坐标系：x 向右、y 向下，取值 0..1；侧视图人物面向右。
 */

/** 侧视站立模板（面向右） */
private fun poseSide(
    head: SkelPoint = P(0.50f, 0.10f),
    neck: SkelPoint = P(0.50f, 0.185f),
    shoulderL: SkelPoint = P(0.487f, 0.21f),
    shoulderR: SkelPoint = P(0.513f, 0.21f),
    elbowL: SkelPoint = P(0.505f, 0.34f),
    elbowR: SkelPoint = P(0.515f, 0.34f),
    wristL: SkelPoint = P(0.510f, 0.46f),
    wristR: SkelPoint = P(0.520f, 0.46f),
    hip: SkelPoint = P(0.50f, 0.50f),
    kneeL: SkelPoint = P(0.507f, 0.68f),
    kneeR: SkelPoint = P(0.513f, 0.68f),
    ankleL: SkelPoint = P(0.500f, 0.87f),
    ankleR: SkelPoint = P(0.510f, 0.87f),
) = Pose(head, neck, shoulderL, shoulderR, elbowL, elbowR, wristL, wristR, hip, kneeL, kneeR, ankleL, ankleR)

/** 正面站立模板 */
private fun poseFront(
    head: SkelPoint = P(0.50f, 0.10f),
    neck: SkelPoint = P(0.50f, 0.185f),
    shoulderL: SkelPoint = P(0.475f, 0.21f),
    shoulderR: SkelPoint = P(0.525f, 0.21f),
    elbowL: SkelPoint = P(0.455f, 0.33f),
    elbowR: SkelPoint = P(0.545f, 0.33f),
    wristL: SkelPoint = P(0.445f, 0.45f),
    wristR: SkelPoint = P(0.555f, 0.45f),
    hip: SkelPoint = P(0.50f, 0.50f),
    kneeL: SkelPoint = P(0.490f, 0.68f),
    kneeR: SkelPoint = P(0.510f, 0.68f),
    ankleL: SkelPoint = P(0.485f, 0.87f),
    ankleR: SkelPoint = P(0.515f, 0.87f),
) = Pose(head, neck, shoulderL, shoulderR, elbowL, elbowR, wristL, wristR, hip, kneeL, kneeR, ankleL, ankleR)

object ExerciseLibrary {

    val all: List<Exercise> = listOf(
        Exercise(
            id = "squat", nameZh = "深蹲", nameEn = "Squat",
            categoryZh = "下肢", categoryEn = "Lower Body", difficulty = 1,
            musclesZh = listOf("股四头肌", "臀大肌", "腘绳肌"),
            musclesEn = listOf("Quads", "Glutes", "Hamstrings"),
            cuesZh = listOf("双脚与肩同宽，脚尖微微外展", "下蹲时臀部向后坐，膝盖对准脚尖方向", "蹲至大腿约与地面平行即可", "起身时收紧核心，背部全程挺直"),
            cuesEn = listOf("Feet shoulder-width apart, toes slightly out", "Sit hips back, knees tracking over toes", "Squat until thighs are near parallel", "Brace your core, keep back straight"),
            mode = TIMED, defaultDurationSec = 45, defaultReps = 15, cycleSec = 3f,
            keyframes = listOf(
                Keyframe(0.0f, poseSide( // 站立，双臂前平举保持平衡
                    elbowL = P(0.60f, 0.21f), elbowR = P(0.61f, 0.21f),
                    wristL = P(0.70f, 0.215f), wristR = P(0.71f, 0.215f),
                )),
                Keyframe(0.5f, poseSide( // 蹲到底：髋后坐下沉，躯干前倾
                    head = P(0.555f, 0.245f), neck = P(0.545f, 0.32f),
                    shoulderL = P(0.535f, 0.345f), shoulderR = P(0.545f, 0.345f),
                    elbowL = P(0.63f, 0.345f), elbowR = P(0.64f, 0.345f),
                    wristL = P(0.72f, 0.35f), wristR = P(0.73f, 0.35f),
                    hip = P(0.44f, 0.63f),
                    kneeL = P(0.525f, 0.66f), kneeR = P(0.535f, 0.66f),
                    ankleL = P(0.50f, 0.87f), ankleR = P(0.51f, 0.87f),
                )),
            ),
        ),
        Exercise(
            id = "pushup", nameZh = "俯卧撑", nameEn = "Push-up",
            categoryZh = "上肢", categoryEn = "Upper Body", difficulty = 2,
            musclesZh = listOf("胸大肌", "肱三头肌", "三角肌前束"),
            musclesEn = listOf("Chest", "Triceps", "Front Delts"),
            cuesZh = listOf("双手略宽于肩，手指向前", "从头到脚呈一条直线，核心收紧", "下落时肘部约 45° 夹角，胸口贴近地面", "撑起时呼气，不要塌腰撅臀"),
            cuesEn = listOf("Hands slightly wider than shoulders", "Keep a straight line head to heels", "Lower until chest is near the floor", "Exhale as you press up, no sagging hips"),
            mode = REPS, defaultDurationSec = 40, defaultReps = 12, cycleSec = 2.5f,
            keyframes = listOf(
                Keyframe(0.0f, poseSide( // 撑起
                    head = P(0.66f, 0.52f), neck = P(0.62f, 0.545f),
                    shoulderL = P(0.60f, 0.565f), shoulderR = P(0.61f, 0.565f),
                    elbowL = P(0.575f, 0.665f), elbowR = P(0.585f, 0.665f),
                    wristL = P(0.565f, 0.835f), wristR = P(0.575f, 0.835f),
                    hip = P(0.47f, 0.605f),
                    kneeL = P(0.36f, 0.665f), kneeR = P(0.37f, 0.665f),
                    ankleL = P(0.24f, 0.745f), ankleR = P(0.25f, 0.745f),
                )),
                Keyframe(0.5f, poseSide( // 下落
                    head = P(0.66f, 0.62f), neck = P(0.62f, 0.645f),
                    shoulderL = P(0.60f, 0.665f), shoulderR = P(0.61f, 0.665f),
                    elbowL = P(0.585f, 0.75f), elbowR = P(0.595f, 0.75f),
                    wristL = P(0.565f, 0.835f), wristR = P(0.575f, 0.835f),
                    hip = P(0.47f, 0.705f),
                    kneeL = P(0.36f, 0.755f), kneeR = P(0.37f, 0.755f),
                    ankleL = P(0.24f, 0.815f), ankleR = P(0.25f, 0.815f),
                )),
            ),
        ),
        Exercise(
            id = "plank", nameZh = "平板支撑", nameEn = "Plank",
            categoryZh = "核心", categoryEn = "Core", difficulty = 1,
            musclesZh = listOf("腹直肌", "腹横肌", "竖脊肌"),
            musclesEn = listOf("Abs", "Deep Core", "Lower Back"),
            cuesZh = listOf("肘部在肩正下方，前臂撑地", "从头到脚踝呈一条直线", "收紧腹部与臀部，不塌腰不撅臀", "自然呼吸，不要憋气"),
            cuesEn = listOf("Elbows under shoulders, forearms down", "Straight line from head to ankles", "Squeeze abs and glutes, no sagging", "Breathe naturally, don't hold breath"),
            mode = TIMED, defaultDurationSec = 40, defaultReps = 1, cycleSec = 4f,
            keyframes = listOf(
                Keyframe(0.0f, poseSide( // 前臂支撑
                    head = P(0.65f, 0.575f), neck = P(0.61f, 0.60f),
                    shoulderL = P(0.59f, 0.615f), shoulderR = P(0.60f, 0.615f),
                    elbowL = P(0.565f, 0.715f), elbowR = P(0.575f, 0.715f),
                    wristL = P(0.56f, 0.83f), wristR = P(0.57f, 0.83f),
                    hip = P(0.46f, 0.645f),
                    kneeL = P(0.35f, 0.69f), kneeR = P(0.36f, 0.69f),
                    ankleL = P(0.23f, 0.755f), ankleR = P(0.24f, 0.755f),
                )),
                Keyframe(0.5f, poseSide( // 呼吸微动
                    head = P(0.652f, 0.583f), neck = P(0.612f, 0.608f),
                    shoulderL = P(0.592f, 0.622f), shoulderR = P(0.602f, 0.622f),
                    elbowL = P(0.565f, 0.715f), elbowR = P(0.575f, 0.715f),
                    wristL = P(0.56f, 0.83f), wristR = P(0.57f, 0.83f),
                    hip = P(0.46f, 0.657f),
                    kneeL = P(0.35f, 0.695f), kneeR = P(0.36f, 0.695f),
                    ankleL = P(0.23f, 0.755f), ankleR = P(0.24f, 0.755f),
                )),
            ),
        ),
        Exercise(
            id = "crunch", nameZh = "卷腹", nameEn = "Crunch",
            categoryZh = "核心", categoryEn = "Core", difficulty = 1,
            musclesZh = listOf("腹直肌"),
            musclesEn = listOf("Abs"),
            cuesZh = listOf("仰卧屈膝，双脚踩实地面", "双手轻扶耳侧，不要抱头发力", "呼气时卷起肩胛骨离开地面", "下落时缓慢控制，不要完全放松"),
            cuesEn = listOf("Lie back with knees bent, feet flat", "Hands lightly by ears, don't pull your neck", "Exhale and lift shoulder blades off floor", "Lower slowly, keep tension"),
            mode = REPS, defaultDurationSec = 40, defaultReps = 16, cycleSec = 2.5f,
            keyframes = listOf(
                Keyframe(0.0f, poseSide( // 仰卧，手扶耳侧
                    head = P(0.08f, 0.66f), neck = P(0.13f, 0.69f),
                    shoulderL = P(0.17f, 0.72f), shoulderR = P(0.18f, 0.72f),
                    elbowL = P(0.13f, 0.62f), elbowR = P(0.14f, 0.62f),
                    wristL = P(0.09f, 0.67f), wristR = P(0.10f, 0.67f),
                    hip = P(0.33f, 0.80f),
                    kneeL = P(0.44f, 0.66f), kneeR = P(0.45f, 0.66f),
                    ankleL = P(0.55f, 0.79f), ankleR = P(0.56f, 0.79f),
                )),
                Keyframe(0.5f, poseSide( // 卷起：肩胛离地
                    head = P(0.09f, 0.56f), neck = P(0.14f, 0.60f),
                    shoulderL = P(0.18f, 0.63f), shoulderR = P(0.19f, 0.63f),
                    elbowL = P(0.14f, 0.53f), elbowR = P(0.15f, 0.53f),
                    wristL = P(0.10f, 0.58f), wristR = P(0.11f, 0.58f),
                    hip = P(0.33f, 0.80f),
                    kneeL = P(0.44f, 0.66f), kneeR = P(0.45f, 0.66f),
                    ankleL = P(0.55f, 0.79f), ankleR = P(0.56f, 0.79f),
                )),
            ),
        ),
        Exercise(
            id = "jumpingjack", nameZh = "开合跳", nameEn = "Jumping Jacks",
            categoryZh = "燃脂", categoryEn = "Cardio", difficulty = 1,
            musclesZh = listOf("全身", "肩部", "小腿"),
            musclesEn = listOf("Full Body", "Shoulders", "Calves"),
            cuesZh = listOf("起跳时双脚向外分开落地", "同时双臂经体侧上举至头顶击掌", "再跳回并拢，双臂放回体侧", "保持节奏，前脚掌缓冲落地"),
            cuesEn = listOf("Jump feet apart as you land", "Raise arms overhead, clap at top", "Jump back in, arms down", "Stay rhythmic, land on the balls of feet"),
            mode = TIMED, defaultDurationSec = 30, defaultReps = 30, cycleSec = 1.2f,
            keyframes = listOf(
                Keyframe(0.0f, poseFront()), // 并拢站立
                Keyframe(0.5f, poseFront( // 打开：手臂上举、双腿分开
                    head = P(0.50f, 0.085f), neck = P(0.50f, 0.17f),
                    elbowL = P(0.41f, 0.185f), elbowR = P(0.59f, 0.185f),
                    wristL = P(0.375f, 0.085f), wristR = P(0.625f, 0.085f),
                    kneeL = P(0.435f, 0.685f), kneeR = P(0.565f, 0.685f),
                    ankleL = P(0.395f, 0.885f), ankleR = P(0.605f, 0.885f),
                )),
            ),
        ),
        Exercise(
            id = "highknees", nameZh = "高抬腿", nameEn = "High Knees",
            categoryZh = "燃脂", categoryEn = "Cardio", difficulty = 2,
            musclesZh = listOf("髂腰肌", "股四头肌", "小腿"),
            musclesEn = listOf("Hip Flexors", "Quads", "Calves"),
            cuesZh = listOf("膝盖抬至髋部高度", "躯干略前倾，核心收紧", "双臂自然前后摆动配合节奏", "前脚掌着地，频率由慢到快"),
            cuesEn = listOf("Drive knees to hip height", "Slight forward lean, core tight", "Swing arms with the rhythm", "Stay on the balls of feet"),
            mode = TIMED, defaultDurationSec = 30, defaultReps = 40, cycleSec = 1.0f,
            keyframes = listOf(
                Keyframe(0.0f, poseFront()),
                Keyframe(0.25f, poseFront( // 右膝抬起，左臂前摆
                    kneeR = P(0.525f, 0.50f), ankleR = P(0.505f, 0.64f),
                    kneeL = P(0.492f, 0.68f), ankleL = P(0.487f, 0.87f),
                    elbowL = P(0.44f, 0.27f), elbowR = P(0.56f, 0.37f),
                    wristL = P(0.42f, 0.17f), wristR = P(0.575f, 0.47f),
                )),
                Keyframe(0.75f, poseFront( // 左膝抬起，右臂前摆
                    kneeL = P(0.475f, 0.50f), ankleL = P(0.495f, 0.64f),
                    kneeR = P(0.508f, 0.68f), ankleR = P(0.513f, 0.87f),
                    elbowL = P(0.44f, 0.37f), elbowR = P(0.56f, 0.27f),
                    wristL = P(0.425f, 0.47f), wristR = P(0.58f, 0.17f),
                )),
            ),
        ),
        Exercise(
            id = "mountainclimber", nameZh = "登山跑", nameEn = "Mountain Climbers",
            categoryZh = "核心", categoryEn = "Core", difficulty = 2,
            musclesZh = listOf("腹直肌", "肩部", "髋屈肌"),
            musclesEn = listOf("Abs", "Shoulders", "Hip Flexors"),
            cuesZh = listOf("俯撑姿势，双手位于肩正下方", "腹部收紧，臀部不要抬起过高", "双腿交替快速向胸口提膝", "保持肩部稳定，节奏均匀"),
            cuesEn = listOf("Push-up position, hands under shoulders", "Brace core, don't pike hips too high", "Drive knees to chest alternately", "Keep shoulders steady and even"),
            mode = TIMED, defaultDurationSec = 30, defaultReps = 30, cycleSec = 1.2f,
            keyframes = listOf(
                Keyframe(0.0f, poseSide( // 俯撑
                    head = P(0.66f, 0.52f), neck = P(0.62f, 0.545f),
                    shoulderL = P(0.60f, 0.565f), shoulderR = P(0.61f, 0.565f),
                    elbowL = P(0.575f, 0.665f), elbowR = P(0.585f, 0.665f),
                    wristL = P(0.565f, 0.835f), wristR = P(0.575f, 0.835f),
                    hip = P(0.47f, 0.605f),
                    kneeL = P(0.36f, 0.665f), kneeR = P(0.37f, 0.665f),
                    ankleL = P(0.24f, 0.745f), ankleR = P(0.25f, 0.745f),
                )),
                Keyframe(0.5f, poseSide( // 单腿提膝向胸口
                    head = P(0.66f, 0.52f), neck = P(0.62f, 0.545f),
                    shoulderL = P(0.60f, 0.565f), shoulderR = P(0.61f, 0.565f),
                    elbowL = P(0.575f, 0.665f), elbowR = P(0.585f, 0.665f),
                    wristL = P(0.565f, 0.835f), wristR = P(0.575f, 0.835f),
                    hip = P(0.47f, 0.605f),
                    kneeL = P(0.47f, 0.64f), kneeR = P(0.37f, 0.665f),
                    ankleL = P(0.40f, 0.72f), ankleR = P(0.25f, 0.745f),
                )),
            ),
        ),
        Exercise(
            id = "glutebridge", nameZh = "臀桥", nameEn = "Glute Bridge",
            categoryZh = "下肢", categoryEn = "Lower Body", difficulty = 1,
            musclesZh = listOf("臀大肌", "腘绳肌", "核心"),
            musclesEn = listOf("Glutes", "Hamstrings", "Core"),
            cuesZh = listOf("仰卧屈膝，双脚踩地与髋同宽", "脚跟发力，将臀部抬至肩髋膝呈一条直线", "顶点收紧臀部停顿一秒", "下落时缓慢控制，不要砸向地面"),
            cuesEn = listOf("Lie back, feet hip-width apart", "Drive through heels to a straight line", "Squeeze glutes at the top", "Lower slowly, don't drop"),
            mode = REPS, defaultDurationSec = 45, defaultReps = 15, cycleSec = 3f,
            keyframes = listOf(
                Keyframe(0.0f, poseSide( // 仰卧，臀部着地
                    head = P(0.07f, 0.70f), neck = P(0.12f, 0.725f),
                    shoulderL = P(0.16f, 0.74f), shoulderR = P(0.17f, 0.74f),
                    elbowL = P(0.20f, 0.80f), elbowR = P(0.21f, 0.80f),
                    wristL = P(0.26f, 0.81f), wristR = P(0.27f, 0.81f),
                    hip = P(0.36f, 0.80f),
                    kneeL = P(0.47f, 0.64f), kneeR = P(0.48f, 0.64f),
                    ankleL = P(0.585f, 0.78f), ankleR = P(0.595f, 0.78f),
                )),
                Keyframe(0.5f, poseSide( // 髋顶起
                    head = P(0.07f, 0.72f), neck = P(0.12f, 0.745f),
                    shoulderL = P(0.16f, 0.74f), shoulderR = P(0.17f, 0.74f),
                    elbowL = P(0.20f, 0.80f), elbowR = P(0.21f, 0.80f),
                    wristL = P(0.26f, 0.81f), wristR = P(0.27f, 0.81f),
                    hip = P(0.36f, 0.68f),
                    kneeL = P(0.475f, 0.62f), kneeR = P(0.485f, 0.62f),
                    ankleL = P(0.585f, 0.78f), ankleR = P(0.595f, 0.78f),
                )),
            ),
        ),
        Exercise(
            id = "lunge", nameZh = "弓步蹲", nameEn = "Lunge",
            categoryZh = "下肢", categoryEn = "Lower Body", difficulty = 2,
            musclesZh = listOf("股四头肌", "臀大肌", "平衡"),
            musclesEn = listOf("Quads", "Glutes", "Balance"),
            cuesZh = listOf("向前迈一大步，后脚跟抬起", "垂直下蹲至前腿膝盖约 90°", "前侧膝盖对准脚尖，不超过脚尖过多", "前腿发力站起，双手叉腰保持平衡"),
            cuesEn = listOf("Step forward into a long stance", "Drop until front knee is ~90°", "Front knee tracks over toes", "Drive through front leg to stand"),
            mode = REPS, defaultDurationSec = 45, defaultReps = 12, cycleSec = 3f,
            keyframes = listOf(
                Keyframe(0.0f, poseSide( // 站立叉腰
                    elbowL = P(0.56f, 0.33f), elbowR = P(0.57f, 0.33f),
                    wristL = P(0.52f, 0.48f), wristR = P(0.53f, 0.48f),
                )),
                Keyframe(0.5f, poseSide( // 下蹲：前弓后跪
                    head = P(0.51f, 0.14f), neck = P(0.51f, 0.22f),
                    shoulderL = P(0.50f, 0.24f), shoulderR = P(0.51f, 0.24f),
                    elbowL = P(0.57f, 0.37f), elbowR = P(0.58f, 0.37f),
                    wristL = P(0.53f, 0.50f), wristR = P(0.54f, 0.50f),
                    hip = P(0.50f, 0.62f),
                    kneeL = P(0.55f, 0.66f), kneeR = P(0.45f, 0.71f),
                    ankleL = P(0.55f, 0.87f), ankleR = P(0.38f, 0.85f),
                )),
            ),
        ),
        Exercise(
            id = "superman", nameZh = "超人式", nameEn = "Superman",
            categoryZh = "核心", categoryEn = "Core", difficulty = 1,
            musclesZh = listOf("竖脊肌", "臀大肌", "下背"),
            musclesEn = listOf("Erector Spinae", "Glutes", "Lower Back"),
            cuesZh = listOf("俯卧，双臂向前伸直", "呼气时同时抬起手臂、胸口和双腿", "目视地面，颈部保持中立", "顶端停顿一秒后缓慢下落"),
            cuesEn = listOf("Lie prone, arms extended forward", "Exhale, lift arms, chest and legs", "Gaze down, neck neutral", "Pause a second, lower slowly"),
            mode = TIMED, defaultDurationSec = 30, defaultReps = 15, cycleSec = 3f,
            keyframes = listOf(
                Keyframe(0.0f, poseSide( // 俯卧，手臂前伸贴地
                    head = P(0.10f, 0.72f), neck = P(0.15f, 0.735f),
                    shoulderL = P(0.18f, 0.74f), shoulderR = P(0.19f, 0.74f),
                    elbowL = P(0.24f, 0.80f), elbowR = P(0.25f, 0.80f),
                    wristL = P(0.30f, 0.82f), wristR = P(0.31f, 0.82f),
                    hip = P(0.38f, 0.76f),
                    kneeL = P(0.50f, 0.78f), kneeR = P(0.51f, 0.78f),
                    ankleL = P(0.60f, 0.80f), ankleR = P(0.61f, 0.80f),
                )),
                Keyframe(0.5f, poseSide( // 手脚同时抬起
                    head = P(0.10f, 0.66f), neck = P(0.15f, 0.68f),
                    shoulderL = P(0.18f, 0.68f), shoulderR = P(0.19f, 0.68f),
                    elbowL = P(0.25f, 0.72f), elbowR = P(0.26f, 0.72f),
                    wristL = P(0.32f, 0.74f), wristR = P(0.33f, 0.74f),
                    hip = P(0.38f, 0.74f),
                    kneeL = P(0.50f, 0.70f), kneeR = P(0.51f, 0.70f),
                    ankleL = P(0.60f, 0.64f), ankleR = P(0.61f, 0.64f),
                )),
            ),
        ),
    )

    private val byIdMap: Map<String, Exercise> = all.associateBy { it.id }

    /** 组间休息小人：叉腰调整呼吸的轻微起伏动画，不列入动作库 */
    val restFigure = Exercise(
        id = "rest", nameZh = "休息", nameEn = "Rest",
        categoryZh = "休息", categoryEn = "Rest", difficulty = 1,
        musclesZh = emptyList(), musclesEn = emptyList(),
        cuesZh = emptyList(), cuesEn = emptyList(),
        mode = TIMED, defaultDurationSec = 0, defaultReps = 0, cycleSec = 3.6f,
        keyframes = listOf(
            Keyframe(0.0f, poseSide( // 叉腰站立，呼气
                elbowL = P(0.455f, 0.325f), elbowR = P(0.565f, 0.325f),
                wristL = P(0.485f, 0.46f), wristR = P(0.515f, 0.46f),
                hip = P(0.50f, 0.505f),
            )),
            Keyframe(0.5f, poseSide( // 吸气，胸口微微抬起
                head = P(0.502f, 0.094f), neck = P(0.502f, 0.179f),
                shoulderL = P(0.487f, 0.204f), shoulderR = P(0.513f, 0.204f),
                elbowL = P(0.448f, 0.318f), elbowR = P(0.572f, 0.318f),
                wristL = P(0.485f, 0.455f), wristR = P(0.515f, 0.455f),
                hip = P(0.50f, 0.508f),
            )),
        ),
    )

    fun byId(id: String): Exercise =
        byIdMap[id] ?: error("未知动作 id: $id")
}
