package com.example.jigsaw.achievements

import android.content.Context
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.min

object AchievementsManager {
    private const val PREFS = "achievements_prefs"
    private const val TAG = "Achievements"

    // streak 成就定义（id -> 目标天数）
    private val STREAKS = listOf("streak_3" to 3, "streak_7" to 7)

    data class CompletionEvent(
        val levelId: String,
        val categoryId: String,
        val difficulty: Int,
        val stars: Int,
        val timeSeconds: Int,
        val moves: Int,
        val usedHint: Boolean,
        val manualPauseUsed: Boolean,
        val isCustom: Boolean,
        val rotateMode: Boolean // 新增：是否旋转模式
    )

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isUnlocked(ctx: Context, id: String): Boolean =
        prefs(ctx).getBoolean("${id}_unlocked", false)

    fun getProgress(ctx: Context, id: String): Int {
        val p = prefs(ctx)
        val stored = p.getInt("${id}_progress", -1)
        if (stored >= 0) return stored
        // 兜底：对 STREAK 成就，若未单独存进度，则用全局 streak 推导
        val def = AchievementsCatalog.ALL.find { it.id == id } ?: return 0
        return if (def.type == Achievement.Type.STREAK) {
            val s = p.getInt("streak", 0)
            min(s, def.goal)
        } else 0
    }

    fun getUnlockTime(ctx: Context, id: String): Long =
        prefs(ctx).getLong("${id}_time", 0L)

    private fun unlock(ctx: Context, id: String) {
        if (isUnlocked(ctx, id)) return
        prefs(ctx).edit()
            .putBoolean("${id}_unlocked", true)
            .putLong("${id}_time", System.currentTimeMillis())
            .apply()
        Log.d(TAG, "Unlocked: $id")
    }

    private fun addProgress(ctx: Context, id: String, delta: Int, goal: Int): Boolean {
        val p = (getProgress(ctx, id) + delta).coerceAtMost(goal)
        prefs(ctx).edit().putInt("${id}_progress", p).apply()
        return p >= goal
    }

    private fun incCounter(ctx: Context, key: String): Int {
        val v = prefs(ctx).getInt(key, 0) + 1
        prefs(ctx).edit().putInt(key, v).apply()
        return v
    }

    // 连续天数（按本地日历“日期字符串”）
    private fun todayString(): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return fmt.format(cal.time)
    }
    private fun yesterdayString(): String {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return fmt.format(cal.time)
    }

    // 每次通关时调用：更新 streak 值，并同步到两个成就的 progress
    private fun updateStreak(ctx: Context): Int {
        val p = prefs(ctx)
        val today = todayString()
        val yesterday = yesterdayString()
        val last = p.getString("last_day", null)
        var streak = p.getInt("streak", 0)
        streak = when {
            last == null -> 1
            last == today -> streak                 // 同一天多次通关不叠加
            last == yesterday -> streak + 1
            else -> 1
        }
        val e = p.edit().putString("last_day", today).putInt("streak", streak)
        // 同步到 streak_3 / streak_7 的 progress
        STREAKS.forEach { (id, goal) ->
            e.putInt("${id}_progress", min(streak, goal))
        }
        e.apply()
        return streak
    }

    // 在成就页 onResume 时可调用，确保显示最新 streak 进度
    fun syncStreakProgress(ctx: Context) {
        val s = prefs(ctx).getInt("streak", 0)
        val e = prefs(ctx).edit()
        STREAKS.forEach { (id, goal) ->
            e.putInt("${id}_progress", min(s, goal))
        }
        e.apply()
    }

    private fun speedLimitSeconds(d: Int): Int = when (d) {
        3 -> 60; 4 -> 120; 5 -> 240; else -> 420
    }

    fun checkOnPuzzleCompleted(ctx: Context, e: CompletionEvent): List<Achievement> {
        val unlocked = mutableListOf<Achievement>()

        fun getDef(id: String): Achievement? = AchievementsCatalog.ALL.find { it.id == id }
        fun tryUnlock(id: String) {
            val a = getDef(id) ?: return
            if (!isUnlocked(ctx, id)) { unlock(ctx, id); unlocked += a }
        }
        fun tryProgress(id: String, goal: Int) {
            val a = getDef(id) ?: return
            if (!isUnlocked(ctx, id) && addProgress(ctx, id, 1, goal)) {
                unlock(ctx, id); unlocked += a
            }
        }

        // 1) 首次完成
        tryUnlock("first_clear")

        // 2) 三星
        if (e.stars >= 3) tryUnlock("three_star_once")

        // 3) 速度
        if (e.difficulty == 3 && e.timeSeconds <= speedLimitSeconds(3)) tryUnlock("speed_3")
        if (e.difficulty == 4 && e.timeSeconds <= speedLimitSeconds(4)) tryUnlock("speed_4")

        // 4) 省步（理想步数）
        val idealMoves = e.difficulty * e.difficulty * 3
        if (e.moves <= idealMoves) tryUnlock("smart_moves")

        // 5) 不用提示
        if (!e.usedHint) tryUnlock("no_hint")

        // 6) 不手动暂停
        if (!e.manualPauseUsed) tryUnlock("no_pause")

        // 7) 6×6 首胜
        if (e.difficulty == 6) tryUnlock("win_6")

        // 8) 累计完成
        incCounter(ctx, "total_completed")
        tryProgress("clear_10", 10)
        tryProgress("clear_50", 50)

        // 9) 自定义类
        if (e.isCustom) {
            incCounter(ctx, "custom_completed")
            tryProgress("custom_clear_3", 3)
        }

        // 10) 连续天数（每天通关，进度+1；达标解锁）
        val streak = updateStreak(ctx)
        if (streak >= 3) tryUnlock("streak_3")
        if (streak >= 7) tryUnlock("streak_7")

        // 11) 旋转模式累计通关
        if (e.rotateMode) {
            incCounter(ctx, "rot_mode_completed")
            tryProgress("rot_clear_3", 3)
            tryProgress("rot_clear_10", 10)
        }

        return unlocked
    }

    fun onCustomCreated(ctx: Context) {
        if (!isUnlocked(ctx, "custom_created")) unlock(ctx, "custom_created")
    }
}