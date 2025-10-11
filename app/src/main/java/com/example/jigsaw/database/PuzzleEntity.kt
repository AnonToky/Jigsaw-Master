package com.example.jigsaw.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_puzzles")
data class PuzzleEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val imagePath: String,
    val thumbnailPath: String? = null,
    val difficulty: Int,
    val pieceCount: Int,
    val createdTime: Long,
    val lastPlayedTime: Long? = null,
    val playCount: Int = 0,
    val bestTime: Long? = null,
    val isCompleted: Boolean = false
)