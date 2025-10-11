package com.example.jigsaw.model

import android.graphics.Bitmap

data class PuzzlePiece(
    val id: Int,
    val correctPosition: Int,
    var currentPosition: Int,
    val bitmap: Bitmap,
    var rotationDeg: Int = 0,
    val row: Int,
    val col: Int
) {
    fun isInCorrectPosition(): Boolean {
        return currentPosition == correctPosition
    }
}