package com.example.jigsaw.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.jigsaw.database.AppDatabase
import com.example.jigsaw.database.PuzzleEntity
import com.example.jigsaw.repository.PuzzleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class MyPuzzlesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PuzzleRepository
    private val _uiState = MutableStateFlow(MyPuzzlesUiState())
    val uiState: StateFlow<MyPuzzlesUiState> = _uiState.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = PuzzleRepository(database.puzzleDao())
        loadPuzzles()
    }

    private fun loadPuzzles() {
        viewModelScope.launch {
            repository.getAllPuzzles().collect { puzzles ->
                _uiState.value = _uiState.value.copy(
                    puzzles = puzzles,
                    isLoading = false,
                    isEmpty = puzzles.isEmpty()
                )
            }
        }
    }

    fun deletePuzzle(puzzle: PuzzleEntity) {
        viewModelScope.launch {
            try {
                // 删除图片文件
                File(puzzle.imagePath).delete()
                puzzle.thumbnailPath?.let { File(it).delete() }

                // 从数据库删除
                repository.deletePuzzle(puzzle)

                _uiState.value = _uiState.value.copy(
                    message = "拼图已删除"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    message = "删除失败: ${e.message}"
                )
            }
        }
    }

    fun incrementPlayCount(puzzle: PuzzleEntity) {
        viewModelScope.launch {
            try {
                val updatedPuzzle = puzzle.copy(playCount = puzzle.playCount + 1)
                repository.updatePuzzle(updatedPuzzle)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    message = "更新游玩次数失败: ${e.message}"
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}

data class MyPuzzlesUiState(
    val puzzles: List<PuzzleEntity> = emptyList(),
    val isLoading: Boolean = true,
    val isEmpty: Boolean = false,
    val message: String? = null
)