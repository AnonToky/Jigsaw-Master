package com.example.jigsaw.achievements

import com.example.jigsaw.R

data class Achievement(
    val id: String,
    val title: String,
    val desc: String,
    val iconRes: Int,
    val type: Type,
    val goal: Int = 1
) {
    enum class Type { ONE_SHOT, INCREMENTAL, STREAK }
}

object AchievementsCatalog {
    val ALL = listOf(
        Achievement("first_clear", "新手上路", "首次完成任意拼图", R.drawable.ic_ach_first, Achievement.Type.ONE_SHOT),
        Achievement("three_star_once", "完美通关", "任意关卡获得3星", R.drawable.ic_ach_star, Achievement.Type.ONE_SHOT),
        Achievement("speed_3", "迅捷之手", "3×3 在 60 秒内完成", R.drawable.ic_ach_speed, Achievement.Type.ONE_SHOT),
        Achievement("speed_4", "快刀手", "4×4 在 120 秒内完成", R.drawable.ic_ach_speed, Achievement.Type.ONE_SHOT),
        Achievement("smart_moves", "省步达人", "步数不超过理想步数", R.drawable.ic_ach_moves, Achievement.Type.ONE_SHOT),
        Achievement("no_hint", "零提示", "不使用提示完成一关", R.drawable.ic_ach_nohint, Achievement.Type.ONE_SHOT),
        Achievement("no_pause", "专注大师", "不手动暂停完成一关", R.drawable.ic_ach_focus, Achievement.Type.ONE_SHOT),
        Achievement("win_6", "硬核首胜", "首次完成 6×6", R.drawable.ic_ach_hard, Achievement.Type.ONE_SHOT),
        Achievement("clear_10", "热身运动", "累计完成 10 关", R.drawable.ic_ach_counter, Achievement.Type.INCREMENTAL, 10),
        Achievement("clear_50", "长跑选手", "累计完成 50 关", R.drawable.ic_ach_counter, Achievement.Type.INCREMENTAL, 50),
        Achievement("custom_created", "创作者", "创建第一个自定义拼图", R.drawable.ic_ach_create, Achievement.Type.ONE_SHOT),
        Achievement("custom_clear_3", "自定义爱好者", "完成 3 个自定义拼图", R.drawable.ic_ach_create, Achievement.Type.INCREMENTAL, 3),
        Achievement("streak_3", "连胜3天", "连续 3 天都有通关", R.drawable.ic_ach_streak, Achievement.Type.STREAK, 3),
        Achievement("streak_7", "连胜7天", "连续 7 天都有通关", R.drawable.ic_ach_streak, Achievement.Type.STREAK, 7),
        Achievement("rot_clear_3", "旋转入门", "旋转模式通关 3 次", R.drawable.ic_ach_rotate, Achievement.Type.INCREMENTAL, 3),
        Achievement("rot_clear_10", "旋转大师", "旋转模式通关 10 次", R.drawable.ic_ach_rotate, Achievement.Type.INCREMENTAL, 10),
    )
}