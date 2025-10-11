package com.example.jigsaw.game

import android.graphics.Bitmap
import com.example.jigsaw.model.PuzzlePiece
import kotlin.random.Random

class PuzzleGame(
    private val originalBitmap: Bitmap,
    private val gridSize: Int,
    private val rotateMode: Boolean = false
) {
    private val pieces = mutableListOf<PuzzlePiece>()
    private var selectedPiecePosition: Int? = null
    private var moveCount = 0

    // 撤销栈
    private val moveStack: ArrayDeque<Move> = ArrayDeque()

    sealed class Move {
        data class Swap(val pos1: Int, val pos2: Int) : Move() // 使用格子位置（0..n-1）
        data class Rotate(val pos: Int) : Move()
    }

    init {
        createPuzzlePieces()
    }

    private fun createPuzzlePieces() {
        val pieceWidth = originalBitmap.width / gridSize
        val pieceHeight = originalBitmap.height / gridSize
        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                val pos = row * gridSize + col
                val bmp = Bitmap.createBitmap(
                    originalBitmap,
                    col * pieceWidth,
                    row * pieceHeight,
                    pieceWidth,
                    pieceHeight
                )
                pieces.add(
                    PuzzlePiece(
                        id = pos,
                        correctPosition = pos,
                        currentPosition = pos,
                        bitmap = bmp,
                        row = row,
                        col = col,
                        rotationDeg = 0
                    )
                )
            }
        }
    }

    private fun shufflePieces() {
        val positions = (0 until gridSize * gridSize).toMutableList()
        for (i in positions.lastIndex downTo 1) {
            val j = Random.nextInt(i + 1)
            val t = positions[i]
            positions[i] = positions[j]
            positions[j] = t
        }
        pieces.forEachIndexed { index, piece -> piece.currentPosition = positions[index] }

        if (rotateMode) {
            val angles = intArrayOf(0, 90, 180, 270)
            pieces.forEach { it.rotationDeg = angles.random() }
        } else {
            pieces.forEach { it.rotationDeg = 0 }
        }

        // 避免开局即完成
        if (isCompleted()) {
            if (rotateMode) {
                pieces[0].rotationDeg = (pieces[0].rotationDeg + 90) % 360
            } else if (pieces.size >= 2) {
                val p0 = pieces[0]
                val p1 = pieces[1]
                val t = p0.currentPosition
                p0.currentPosition = p1.currentPosition
                p1.currentPosition = t
            }
        }
    }

    private fun isPieceCorrect(p: PuzzlePiece): Boolean {
        val posOK = p.currentPosition == p.correctPosition
        return if (!rotateMode) posOK else posOK && (p.rotationDeg % 360 == 0)
    }

    // 单击：选中/取消/交换
    fun onPieceClicked(position: Int): Boolean {
        val clickedPiece = pieces.find { it.currentPosition == position } ?: return false
        return when (val sel = selectedPiecePosition) {
            null -> {
                // 第一次点击：选中
                selectedPiecePosition = position
                true
            }
            position -> {
                // 点击同一格：取消选中
                selectedPiecePosition = null
                false
            }
            else -> {
                // 交换：先入栈再交换
                moveStack.addLast(Move.Swap(sel, position))
                swapPieces(sel, position)
                selectedPiecePosition = null
                moveCount++
                true
            }
        }
    }

    // 双击：旋转
    fun rotatePieceAtPosition(position: Int): Boolean {
        if (!rotateMode) return false
        val piece = pieces.find { it.currentPosition == position } ?: return false
        moveStack.addLast(Move.Rotate(position))
        piece.rotationDeg = (piece.rotationDeg + 90) % 360
        moveCount++
        return true
    }

    private fun swapPieces(pos1: Int, pos2: Int) {
        val a = pieces.find { it.currentPosition == pos1 }
        val b = pieces.find { it.currentPosition == pos2 }
        if (a != null && b != null) {
            val t = a.currentPosition
            a.currentPosition = b.currentPosition
            b.currentPosition = t
        }
    }

    fun canUndo(): Boolean = moveStack.isNotEmpty()

    fun undoLastMove(): Move? {
        val last = moveStack.removeLastOrNull() ?: return null
        when (last) {
            is Move.Swap -> {
                // 再交换一次即可还原
                swapPieces(last.pos1, last.pos2)
                if (moveCount > 0) moveCount--
                selectedPiecePosition = null
                return last
            }
            is Move.Rotate -> {
                val piece = pieces.find { it.currentPosition == last.pos } ?: return null
                piece.rotationDeg = (piece.rotationDeg + 270) % 360 // 逆 90°
                if (moveCount > 0) moveCount--
                selectedPiecePosition = null
                return last
            }
        }
    }

    fun isCompleted(): Boolean = pieces.all { isPieceCorrect(it) }

    fun getPieces(): List<PuzzlePiece> = pieces.sortedBy { it.currentPosition }

    fun getSelectedPosition(): Int? = selectedPiecePosition

    fun getMoveCount(): Int = moveCount

    fun getCompletionPercentage(): Float {
        val correct = pieces.count { isPieceCorrect(it) }
        return correct.toFloat() / pieces.size * 100f
    }

    fun clearSelection() {
        selectedPiecePosition = null
    }

    fun restoreState(positions: List<Int>, moves: Int) {
        if (positions.size == pieces.size) {
            positions.forEachIndexed { i, pos -> pieces[i].currentPosition = pos }
            if (!rotateMode) pieces.forEach { it.rotationDeg = 0 }
            moveCount = moves
            selectedPiecePosition = null
            // 还原局面时清空历史，避免跨局撤销
            moveStack.clear()
        }
    }

    fun restoreState(positions: List<Int>, rotations: List<Int>?, moves: Int) {
        if (positions.size == pieces.size) {
            positions.forEachIndexed { i, pos -> pieces[i].currentPosition = pos }
            if (rotateMode && rotations != null && rotations.size == pieces.size) {
                rotations.forEachIndexed { i, deg ->
                    pieces[i].rotationDeg = ((deg % 360) + 360) % 360
                }
            } else {
                pieces.forEach { it.rotationDeg = 0 }
            }
            moveCount = moves
            selectedPiecePosition = null
            moveStack.clear()
        }
    }

    fun getRotationsById(): List<Int> = pieces.sortedBy { it.id }.map { it.rotationDeg }

    fun getPositionsByPiece(): IntArray {
        val arr = IntArray(gridSize * gridSize)
        // 直接用内部 pieces（id 即原始索引）
        for (p in pieces) arr[p.id] = p.currentPosition
        return arr
    }

    // 任意两块互换的最少交换次数：Σ(环长-1)
    fun getOptimalSwapCount(): Int {
        val pos = getPositionsByPiece()
        val n = pos.size
        val visited = BooleanArray(n)
        var swaps = 0
        for (i in 0 until n) {
            if (!visited[i]) {
                var j = i
                var len = 0
                while (!visited[j]) {
                    visited[j] = true
                    j = pos[j]
                    len++
                }
                if (len > 0) swaps += (len - 1)
            }
        }
        return swaps
    }

    // 旋转模式下使所有拼块回正所需的最少旋转步数（每次 +90°）
    fun getOptimalRotationCount(): Int {
        if (!rotateMode) return 0
        var total = 0
        for (p in pieces) {
            val deg = ((p.rotationDeg % 360) + 360) % 360
            total += ((360 - deg) % 360) / 90
        }
        return total
    }

    fun getTheoreticalBestMoves(): Int {
        val best = getOptimalSwapCount() + getOptimalRotationCount()
        return if (best <= 0) 1 else best
    }

    fun initNewGame() {
        shufflePieces()
        moveCount = 0
        selectedPiecePosition = null
        moveStack.clear()
    }

    fun forceComplete() {
        pieces.forEachIndexed { index, piece ->
            piece.currentPosition = index
            if (rotateMode) piece.rotationDeg = 0
        }
        moveCount = 0
        selectedPiecePosition = null
        moveStack.clear()
    }
}