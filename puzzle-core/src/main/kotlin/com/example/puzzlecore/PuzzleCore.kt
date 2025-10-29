package com.example.puzzlecore

/**
 * PuzzleCore: 用于测试 PuzzleGame 的 JVM 入口。
 * 不依赖 Android。
 */
object PuzzleCore {

    data class GameConfig(
        val gridSize: Int,
        val rotateMode: Boolean = false
    )

    /** 创建游戏实例（支持 Android 与纯 JVM 两种实现） */
    fun createPuzzleGame(config: GameConfig): Any {
        val game = ReflectionAdapters.createPuzzleGame(
            gridSize = config.gridSize,
            rotateMode = config.rotateMode
        )

        // 使用固定随机种子初始化，保证测试可复现
        try {
            val init = game::class.java.getMethod("initNewGame", Int::class.javaPrimitiveType)
            init.invoke(game, 42)
        } catch (_: NoSuchMethodException) {
            // 若无 initNewGame(seed)，尝试无参版本
            val init = game::class.java.getMethod("initNewGame")
            init.invoke(game)
        }

        return game
    }

    fun isCompleted(game: Any): Boolean {
        val m = game::class.java.getMethod("isCompleted")
        return m.invoke(game) as Boolean
    }

    fun getMoveCount(game: Any): Int {
        val m = game::class.java.getMethod("getMoveCount")
        return m.invoke(game) as Int
    }

    fun clickPiece(game: Any, pos: Int): Boolean {
        val m = game::class.java.getMethod("onPieceClicked", Int::class.javaPrimitiveType)
        return m.invoke(game, pos) as Boolean
    }

    fun rotatePiece(game: Any, pos: Int): Boolean {
        return try {
            val m = game::class.java.getMethod("rotatePieceAtPosition", Int::class.javaPrimitiveType)
            (m.invoke(game, pos) as? Boolean) ?: false
        } catch (_: NoSuchMethodException) {
            false
        }
    }

    fun getPieceCount(game: Any): Int {
        val m = game::class.java.getMethod("getPieces")
        val list = m.invoke(game) as List<*>
        return list.size
    }

    fun getPositions(game: Any): List<Int> {
        return try {
            val m = game::class.java.getMethod("getPositionsByPiece")
            val arr = m.invoke(game) as IntArray
            arr.toList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
