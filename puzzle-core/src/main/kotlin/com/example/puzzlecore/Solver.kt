package com.example.puzzlecore

import com.example.puzzlecore.PuzzleCore

object Solver {

    /**
     * 执行 AI/测试用例的动作
     * 例如 [{"swap":[0,1]}, {"swap":[3,4]}]
     */
    fun executeActions(game: Any, actions: List<Map<String, List<Int>>>) {
        for (action in actions) {
            val swap = action["swap"]
            if (swap != null && swap.size == 2) {
                val a = swap[0]
                val b = swap[1]
                PuzzleCore.clickPiece(game, a)
                PuzzleCore.clickPiece(game, b)
            }

            val rotate = action["rotate"]
            if (rotate != null && rotate.size == 1) {
                val pos = rotate[0]
                PuzzleCore.rotatePiece(game, pos)
            }
        }
    }
}

