package com.example.jigsaw

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.example.jigsaw.achievements.AchievementsActivity
import com.example.jigsaw.database.AppDatabase
import com.example.jigsaw.GameModeDialog
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var cardStartGame: MaterialCardView
    private lateinit var cardEditor: MaterialCardView
    private lateinit var cardContinue: MaterialCardView
    private lateinit var btnAchievements: LinearLayout
    private lateinit var btnMyPuzzles: LinearLayout
    private lateinit var btnSettings: LinearLayout

    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupFullScreen()
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        database = AppDatabase.getDatabase(this)

        initViews()
        setupClickListeners()

        // 隐藏主界面的继续游戏按钮
        cardContinue.visibility = View.GONE
    }

    private fun setupFullScreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController?.let {
            it.hide(WindowInsetsCompat.Type.statusBars())
            it.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun initViews() {
        cardStartGame = findViewById(R.id.cardStartGame)
        cardEditor = findViewById(R.id.cardEditor)
        cardContinue = findViewById(R.id.cardContinue)
        btnAchievements = findViewById(R.id.btnAchievements)
        btnMyPuzzles = findViewById(R.id.btnMyPuzzles)
        btnSettings = findViewById(R.id.btnSettings)
    }

    private fun setupClickListeners() {
        // 开始游戏 - 直接显示选择对话框
        cardStartGame.setOnClickListener {
            showGameModeDialog()
        }

        // 拼图编辑器
        cardEditor.setOnClickListener {
            val intent = Intent(this, EditorActivity::class.java)
            startActivity(intent)
        }

        // 成就
        btnAchievements.setOnClickListener {
            val intent = Intent(this, AchievementsActivity::class.java)
            startActivity(intent)
        }

        // 我的拼图
        btnMyPuzzles.setOnClickListener {
            val intent = Intent(this, MyPuzzlesActivity::class.java)
            startActivity(intent)
        }

        // 设置
        btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

    }

    private fun showGameModeDialog() {
        lifecycleScope.launch {
            val savedGame = getSavedGameInfo()

            val dialog = GameModeDialog(
                context = this@MainActivity,
                hasSavedGame = savedGame != null,
                savedGameInfo = savedGame,
                onModeSelected = { mode ->
                    when (mode) {
                        GameModeDialog.GameMode.NEW_GAME -> {
                            startLevelSelection()
                        }
                        GameModeDialog.GameMode.CONTINUE_GAME -> {
                            continueGame()
                        }
                    }
                }
            )
            dialog.show()
        }
    }

    private suspend fun getSavedGameInfo(): GameModeDialog.SavedGameInfo? {
        val sharedPrefs = getSharedPreferences("game_prefs", MODE_PRIVATE)
        val hasSavedGame = sharedPrefs.getBoolean("has_saved_game", false)

        return if (hasSavedGame) {
            GameModeDialog.SavedGameInfo(
                puzzleName = sharedPrefs.getString("puzzle_name", "未知拼图") ?: "未知拼图",
                difficulty = "${sharedPrefs.getInt("difficulty", 4)}×${sharedPrefs.getInt("difficulty", 4)}",
                progress = sharedPrefs.getInt("game_progress", 0),
                timeSpent = formatTime(sharedPrefs.getLong("elapsed_time", 0))
            )
        } else {
            null
        }
    }

    private fun startLevelSelection() {
        val intent = Intent(this, LevelSelectionActivity::class.java)
        startActivity(intent)
    }

    private fun continueGame() {
        val sharedPrefs = getSharedPreferences("game_prefs", MODE_PRIVATE)
        val puzzleId = sharedPrefs.getString("saved_puzzle_id", "") ?: ""
        val puzzleName = sharedPrefs.getString("puzzle_name", "未知拼图") ?: "未知拼图"
        val difficulty = sharedPrefs.getInt("difficulty", 4)

        val intent = Intent(this, GameActivity::class.java).apply {
            putExtra("puzzle_id", puzzleId)
            putExtra("puzzle_name", puzzleName)
            putExtra("difficulty", difficulty)
            putExtra("resume_game", true)
        }
        startActivity(intent)
    }

    private fun formatTime(milliseconds: Long): String {
        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}