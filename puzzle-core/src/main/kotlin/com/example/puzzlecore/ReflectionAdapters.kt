package com.example.puzzlecore

/**
 * ReflectionAdapters: 用于隔离 PuzzleGame 类和测试框架
 * 保持 puzzle-core 纯 JVM，不依赖 Android。
 */
object ReflectionAdapters {

    // 这里指向 puzzle-core 中的纯 JVM 版 PuzzleGame
    private const val PUZZLE_GAME_CLASS = "com.example.puzzlecore.PuzzleGame"

    /**
     * 通过反射创建 PuzzleGame 实例
     * 签名：(Int gridSize, Boolean rotateMode)
     */
    fun createPuzzleGame(gridSize: Int, rotateMode: Boolean): Any {
        val clazz = Class.forName(PUZZLE_GAME_CLASS)
        val ctor = clazz.getConstructor(
            Int::class.javaPrimitiveType,
            Boolean::class.javaPrimitiveType
        )
        return ctor.newInstance(gridSize, rotateMode)
    }

    /**
     * 调用 isCompleted()
     */
    fun callIsCompleted(game: Any): Boolean {
        val method = game::class.java.getMethod("isCompleted")
        return method.invoke(game) as Boolean
    }

    /**
     * 调用 onPieceClicked(position)
     */
    fun callOnPieceClicked(game: Any, pos: Int): Boolean {
        val method = game::class.java.getMethod("onPieceClicked", Int::class.javaPrimitiveType)
        return method.invoke(game, pos) as Boolean
    }

    /**
     * 调用 rotatePieceAtPosition(position)
     */
    fun callRotatePiece(game: Any, pos: Int): Boolean {
        val method = game::class.java.getMethod("rotatePieceAtPosition", Int::class.javaPrimitiveType)
        return method.invoke(game, pos) as Boolean
    }

    /**
     * 调用 getMoveCount()
     */
    fun callGetMoveCount(game: Any): Int {
        val method = game::class.java.getMethod("getMoveCount")
        return method.invoke(game) as Int
    }
}
