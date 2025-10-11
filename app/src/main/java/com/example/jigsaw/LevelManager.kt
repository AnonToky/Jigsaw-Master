package com.example.jigsaw
import android.content.Context

object LevelManager {
    private const val PREFS_NAME = "level_progress"
    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun baseKey(levelId: String, difficulty: Int, rotate: Boolean) =
        "${levelId}_d$difficulty${if (rotate) "_rot" else ""}"

    fun isLevelUnlocked(context: Context, levelId: String, difficulty: Int, rotate: Boolean): Boolean {
        // 第一关默认解锁
        if (levelId.endsWith("_1")) return true
        val n = levelId.substringAfterLast("_").toIntOrNull() ?: return false
        if (n <= 1) return false
        val prefix = levelId.substringBeforeLast("_")
        val previousLevelId = "${prefix}_${n - 1}"
        val key = "${baseKey(previousLevelId, difficulty, rotate)}_completed"
        return prefs(context).getBoolean(key, false)
    }

    fun saveLevelProgress(
        context: Context,
        levelId: String,
        difficulty: Int,
        rotate: Boolean,
        stars: Int,
        time: Int,
        moves: Int
    ) {
        val p = prefs(context)
        val e = p.edit()
        val key = baseKey(levelId, difficulty, rotate)
        e.putBoolean("${key}_completed", true)

        val curStars = p.getInt("${key}_stars", 0)
        if (stars > curStars) e.putInt("${key}_stars", stars)

        val curBestTime = p.getInt("${key}_best_time", Int.MAX_VALUE)
        if (time < curBestTime) e.putInt("${key}_best_time", time)

        val curBestMoves = p.getInt("${key}_best_moves", Int.MAX_VALUE)
        if (moves < curBestMoves) e.putInt("${key}_best_moves", moves)

        e.apply()
    }

    fun getLevelStars(context: Context, levelId: String, difficulty: Int, rotate: Boolean): Int {
        val key = "${baseKey(levelId, difficulty, rotate)}_stars"
        return prefs(context).getInt(key, 0)
    }

    fun isLevelCompleted(context: Context, levelId: String, difficulty: Int, rotate: Boolean): Boolean {
        val key = "${baseKey(levelId, difficulty, rotate)}_completed"
        return prefs(context).getBoolean(key, false)
    }

    // 兼容旧调用（默认非旋转）
    @Deprecated("Use isLevelUnlocked(context, levelId, difficulty, rotate)")
    fun isLevelUnlocked(context: Context, levelId: String, categoryId: String): Boolean =
        isLevelUnlocked(context, levelId, 4, false)

    @Deprecated("Use getLevelStars(context, levelId, difficulty, rotate)")
    fun getLevelStars(context: Context, levelId: String): Int =
        getLevelStars(context, levelId, 4, false)
}