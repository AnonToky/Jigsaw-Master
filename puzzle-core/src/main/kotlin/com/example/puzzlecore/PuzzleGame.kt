package com.example.puzzlecore

import kotlin.random.Random

/**
 * 纯 JVM 测试用的 PuzzleGame 模拟类。
 * 支持固定随机种子打乱，保证测试可复现。
 */
class PuzzleGame(
    val gridSize: Int,
    val rotateMode: Boolean = false
) {
    private val size = gridSize * gridSize
    private val positions = IntArray(size) { it } // 当前每块所在位置
    private val rotations = IntArray(size) { 0 }  // 每块旋转角度（90°为单位）
    private var selected: Int? = null
    private var moveCount = 0

    /** 单击（选中/取消/交换） */
    fun onPieceClicked(pos: Int): Boolean {
        val sel = selected
        return if (sel == null) {
            selected = pos
            true
        } else if (sel == pos) {
            selected = null
            false
        } else {
            val tmp = positions[sel]
            positions[sel] = positions[pos]
            positions[pos] = tmp
            moveCount++
            selected = null
            true
        }
    }

    /** 双击旋转 */
    fun rotatePieceAtPosition(pos: Int): Boolean {
        if (!rotateMode) return false
        rotations[pos] = (rotations[pos] + 90) % 360
        moveCount++
        return true
    }

    /** 是否完成 */
    fun isCompleted(): Boolean {
        return positions.withIndex().all { it.index == it.value } &&
                (!rotateMode || rotations.all { it == 0 })
    }

    fun getMoveCount(): Int = moveCount
    fun getPieces(): List<Int> = positions.toList()

    // ✅ 下面是新增部分 ----------------------------------------------------

    /** 打乱拼图（使用随机数种子，可复现） */
    fun shuffle(seed: Int? = null) {
        val rng = if (seed != null) Random(seed) else Random
        for (i in positions.lastIndex downTo 1) {
            val j = rng.nextInt(i + 1)
            val tmp = positions[i]
            positions[i] = positions[j]
            positions[j] = tmp
        }
        if (rotateMode) {
            val angles = listOf(0, 90, 180, 270)
            for (i in rotations.indices) {
                rotations[i] = angles[rng.nextInt(angles.size)]
            }
        }
        moveCount = 0
        selected = null
    }

    /** 初始化新游戏（可指定随机种子） */
    fun initNewGame(seed: Int = 42) {
        shuffle(seed)
    }

    /** 获取当前拼块位置数组 */
    fun getPositionsByPiece(): IntArray = positions.copyOf()

    /** 获取旋转角度数组 */
    fun getRotationsById(): List<Int> = rotations.toList()
}
