package com.example.jigsaw.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PuzzleDao {

    @Query("SELECT * FROM custom_puzzles ORDER BY createdTime DESC")
    fun getAllPuzzles(): Flow<List<PuzzleEntity>>

    @Query("SELECT * FROM custom_puzzles WHERE id = :puzzleId")
    suspend fun getPuzzleById(puzzleId: String): PuzzleEntity?

    @Insert
    suspend fun insertPuzzle(puzzle: PuzzleEntity)

    @Update
    suspend fun updatePuzzle(puzzle: PuzzleEntity)

    @Delete
    suspend fun deletePuzzle(puzzle: PuzzleEntity)

    @Query("UPDATE custom_puzzles SET playCount = playCount + 1, lastPlayedTime = :timestamp WHERE id = :puzzleId")
    suspend fun incrementPlayCount(puzzleId: String, timestamp: Long)

    @Query("UPDATE custom_puzzles SET bestTime = :time, isCompleted = 1 WHERE id = :puzzleId AND (bestTime IS NULL OR bestTime > :time)")
    suspend fun updateBestTime(puzzleId: String, time: Long)

    @Query("SELECT COUNT(*) FROM custom_puzzles")
    suspend fun getPuzzleCount(): Int
}