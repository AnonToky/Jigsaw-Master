package com.example.jigsaw.repository

import com.example.jigsaw.database.PuzzleDao
import com.example.jigsaw.database.PuzzleEntity
import kotlinx.coroutines.flow.Flow

class PuzzleRepository(
    private val puzzleDao: PuzzleDao
) {

    fun getAllPuzzles(): Flow<List<PuzzleEntity>> = puzzleDao.getAllPuzzles()

    suspend fun getPuzzleById(puzzleId: String): PuzzleEntity? = puzzleDao.getPuzzleById(puzzleId)

    suspend fun insertPuzzle(puzzle: PuzzleEntity) = puzzleDao.insertPuzzle(puzzle)

    suspend fun updatePuzzle(puzzle: PuzzleEntity) = puzzleDao.updatePuzzle(puzzle)

    suspend fun deletePuzzle(puzzle: PuzzleEntity) = puzzleDao.deletePuzzle(puzzle)

    suspend fun incrementPlayCount(puzzleId: String) {
        puzzleDao.incrementPlayCount(puzzleId, System.currentTimeMillis())
    }

    suspend fun updateBestTime(puzzleId: String, time: Long) {
        puzzleDao.updateBestTime(puzzleId, time)
    }

    suspend fun getPuzzleCount(): Int = puzzleDao.getPuzzleCount()
}