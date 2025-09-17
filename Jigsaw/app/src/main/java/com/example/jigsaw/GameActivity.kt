package com.example.jigsaw

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.jigsaw.adapter.PuzzleGameAdapter
import com.example.jigsaw.game.PuzzleGame
import com.example.jigsaw.utils.DebugUtils
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import androidx.preference.PreferenceManager
import android.media.MediaPlayer
import android.content.Context
import android.os.Build
import android.os.Vibrator
import android.os.VibrationEffect
import androidx.appcompat.app.AppCompatDelegate
import android.content.Intent
import android.view.ViewGroup




class GameActivity : AppCompatActivity() {


    private lateinit var toolbar: MaterialToolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvMoves: TextView
    private lateinit var tvTime: TextView
    private lateinit var tvProgress: TextView
    private lateinit var btnPause: MaterialButton
    private lateinit var btnHint: MaterialButton
    private lateinit var btnRestart: MaterialButton
    private lateinit var btnUndo: MaterialButton

    private lateinit var puzzleGame: PuzzleGame
    private lateinit var adapter: PuzzleGameAdapter

    // 游戏参数
    private var puzzleId: String = ""
    private var puzzleName: String = ""
    private var difficulty: Int = 3
    private var isCustom: Boolean = false
    private var imagePath: String? = null
    private var imageResId: Int = 0
    private var rotateMode: Boolean = false

    // 计时器
    private var startTime: Long = 0
    private val timeHandler = Handler(Looper.getMainLooper())
    private var pausedTime: Long = 0
    private var isPaused = false
    private var usedHintInThisRun = false
    private var manualPauseUsedInThisRun = false

    //debug
    private var tvDebug: TextView? = null

    private val timeRunnable = object : Runnable {
        override fun run() {
            if (!isPaused) {
                updateTime()
                timeHandler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        getIntentData()
        initViews()

        applySettings()

        // 检查是否需要恢复游戏
        if (intent.getBooleanExtra("resume_game", false)) {
            Log.d("GameActivity", "Resuming saved game")
            resumeGame()
        } else {
            Log.d("GameActivity", "Starting new game")
            setupNewGame()
        }
    }

    // 每次从设置页面返回时刷新音乐/显示模式/震动等配置
    override fun onResume() {
        super.onResume()
        delegate.applyDayNight()  // 刷新主题
        applySettings() // 启动音乐
    }

    private fun getIntentData() {
        if (intent.getBooleanExtra("resume_game", false)) return
        puzzleId = intent.getStringExtra("level_id") ?: intent.getStringExtra("puzzle_id") ?: ""
        puzzleName = intent.getStringExtra("level_name") ?: intent.getStringExtra("puzzle_name") ?: "未知拼图"
        difficulty = intent.getIntExtra("difficulty", 3)
        isCustom = intent.getBooleanExtra("is_custom", false) || !intent.getStringExtra("puzzle_path").isNullOrEmpty()
        imagePath = intent.getStringExtra("puzzle_path")
        imageResId = intent.getIntExtra("level_image", 0)
        rotateMode = intent.getBooleanExtra("rotate_mode", false)
    }

    private fun applySettings() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        // 背景音乐
        val bgmEnabled = prefs.getBoolean("bgm_enabled", false)
        if (bgmEnabled) {
            startMusic()
        } else {
            stopMusic()
        }

        // 开发者选项
        val debugMode = prefs.getBoolean("debug_mode", false)
        val cheatMode = prefs.getBoolean("cheat_mode", false)
        if (debugMode) {
            Log.d("GameActivity", "调试模式已启用")
        }

    }
    private fun resumeGame() {
        lifecycleScope.launch {
            val prefs = getSharedPreferences("game_prefs", MODE_PRIVATE)
            if (!prefs.getBoolean("has_saved_game", false)) { setupNewGame(); return@launch }
            puzzleId = prefs.getString("saved_puzzle_id", null) ?: return@launch setupNewGame()
            puzzleName = prefs.getString("puzzle_name", "未知拼图") ?: "未知拼图"
            difficulty = prefs.getInt("difficulty", 3)
            rotateMode = prefs.getBoolean("rotate_mode", false)
            isCustom = prefs.getBoolean("is_custom", false)
            imagePath = prefs.getString("puzzle_path", null)
            imageResId = prefs.getInt("image_res_id", 0)

            supportActionBar?.title = puzzleName
            supportActionBar?.subtitle = if (rotateMode) "${difficulty}×${difficulty} · 旋转" else "${difficulty}×${difficulty}"

            val bitmap = loadBitmap()
            puzzleGame = PuzzleGame(bitmap, difficulty, rotateMode)

            val positionsStr = prefs.getString("piece_positions", null)
            val positions = positionsStr?.split(",")?.mapNotNull { it.toIntOrNull() } ?: emptyList()
            val rotationsStr = prefs.getString("piece_rotations", null)
            val rotations = rotationsStr?.split(",")?.mapNotNull { it.toIntOrNull() }
            val savedMoveCount = prefs.getInt("move_count", 0)
            val basePosStr = prefs.getString("baseline_positions", null)
            if (basePosStr == null) {
                // 兜底：老版本存档没有基线，用当前读取到的 positions 写入一次，避免 best=0
                if (positions.isNotEmpty()) {
                    prefs.edit().putString("baseline_positions", positions.joinToString(",")).apply()
                    if (rotateMode && rotations != null) {
                        prefs.edit().putString("baseline_rotations", rotations.joinToString(",")).apply()
                    }
                }
            }

            if (positions.size == difficulty * difficulty) {
                if (rotateMode && rotations != null && rotations.size == positions.size) {
                    puzzleGame.restoreState(positions, rotations, savedMoveCount)
                } else {
                    puzzleGame.restoreState(positions, savedMoveCount)
                }
            } else {
                // 存档损坏则开新局
                puzzleGame.initNewGame()
            }

            val savedTime = prefs.getLong("elapsed_time", 0)
            pausedTime = savedTime
            startTime = SystemClock.elapsedRealtime() - savedTime
            isPaused = false

            setupRecyclerView()
            updateUI()
            timeHandler.post(timeRunnable)
            updateUndoButtonState()
        }
    }

    private fun isSoundEnabled(): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        return prefs.getBoolean("sound_enabled", true)
    }

    private fun isVibrationEnabled(): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        return prefs.getBoolean("vibration_enabled", true)
    }

    private fun triggerVibration() {
        if (!isVibrationEnabled()) return

        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }

    private fun saveGameState() {
        if (!::puzzleGame.isInitialized) return
        if (puzzleGame.getCompletionPercentage() >= 100) { clearSavedGame(); return }

        val prefs = getSharedPreferences("game_prefs", MODE_PRIVATE)
        val displayed = puzzleGame.getPieces()
        val positionsByPiece = IntArray(displayed.size)
        val rotationsByPiece = IntArray(displayed.size)

        displayed.forEach { piece ->
            positionsByPiece[piece.id] = piece.currentPosition
            rotationsByPiece[piece.id] = if (rotateMode) piece.rotationDeg else 0
        }

        val elapsedTime = if (isPaused) pausedTime else SystemClock.elapsedRealtime() - startTime

        prefs.edit().apply {
            putBoolean("has_saved_game", true)
            putString("saved_puzzle_id", puzzleId)
            putString("puzzle_name", puzzleName)
            putInt("difficulty", difficulty)
            putBoolean("rotate_mode", rotateMode)
            putBoolean("is_custom", isCustom)
            putString("puzzle_path", imagePath)
            putInt("image_res_id", imageResId)

            putString("piece_positions", positionsByPiece.joinToString(","))
            if (rotateMode) putString("piece_rotations", rotationsByPiece.joinToString(","))

            putInt("move_count", puzzleGame.getMoveCount())
            putLong("elapsed_time", elapsedTime)
            putInt("game_progress", puzzleGame.getCompletionPercentage().toInt())
            apply()
        }
    }

    private fun clearSavedGame() {
        getSharedPreferences("game_prefs", MODE_PRIVATE).edit().apply {
            putBoolean("has_saved_game", false)
            remove("piece_positions")
            remove("piece_rotations")
            remove("move_count")
            remove("elapsed_time")
            apply()
        }
    }

    private fun isDebugMode(): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        return prefs.getBoolean("debug_mode", false)
    }

    private fun isCheatMode(): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        return prefs.getBoolean("cheat_mode", false)
    }



    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        recyclerView = findViewById(R.id.recyclerViewPuzzle)
        tvMoves = findViewById(R.id.tvMoves)
        tvTime = findViewById(R.id.tvTime)
        tvProgress = findViewById(R.id.tvProgress)
        btnPause = findViewById(R.id.btnPause)
        btnHint = findViewById(R.id.btnHint)
        btnRestart = findViewById(R.id.btnRestart)
        btnUndo = findViewById(R.id.btnUndo)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        supportActionBar?.title = puzzleName
        supportActionBar?.subtitle = if (rotateMode) "${difficulty}×${difficulty} · 旋转" else "${difficulty}×${difficulty}"

        setupButtons()

        findViewById<View>(R.id.rootLayout)?.setOnClickListener {
            if (::puzzleGame.isInitialized && puzzleGame.getSelectedPosition() != null) {
                puzzleGame.clearSelection()
                if (::adapter.isInitialized) { // 加这一层保护
                    adapter.updatePieces(puzzleGame.getPieces(), null)
                }
            }
        }

        //debug模式悬浮球
        if (isDebugMode()) {
            tvDebug = TextView(this).apply {
                textSize = 12f
                setTextColor(android.graphics.Color.RED)
                setBackgroundColor(0x55FFFFFF)
                setPadding(8, 4, 8, 4)
            }

            // 加到根布局
            val rootLayout = findViewById<View>(R.id.rootLayout) as ViewGroup
            val params = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            rootLayout.addView(tvDebug, params)
        }

    }

    private fun setupButtons() {
        btnPause.setOnClickListener { togglePause() }
        btnHint.setOnClickListener { showHint() }
        btnRestart.setOnClickListener { showRestartDialog() }
        btnUndo.setOnClickListener { onUndoClicked() }
    }

    private fun onUndoClicked() {
        if (!::puzzleGame.isInitialized) return
        val undone = puzzleGame.undoLastMove()
        if (undone == null) {
            Toast.makeText(this, "没有可撤销的操作", Toast.LENGTH_SHORT).show()
            return
        }
        when (undone) {
            is com.example.jigsaw.game.PuzzleGame.Move.Swap -> {
                // 只刷新交换的两格
                adapter.applySwap(
                    newPieces = puzzleGame.getPieces(),
                    a = undone.pos1,
                    b = undone.pos2,
                    newSelected = null
                )
            }
            is com.example.jigsaw.game.PuzzleGame.Move.Rotate -> {
                adapter.notifyPieceRotated(undone.pos)
            }
        }
        updateUI()
        updateUndoButtonState()
    }

    // 保留该方法但不用于“再来一局”，避免直接完成态
    private fun setupGame() {
        lifecycleScope.launch {
            try {
                val bitmap = loadBitmap()
                puzzleGame = PuzzleGame(bitmap, difficulty, rotateMode)
                recyclerView.layoutManager = GridLayoutManager(this@GameActivity, difficulty)
                recyclerView.setHasFixedSize(true)
                adapter = PuzzleGameAdapter(
                    pieces = puzzleGame.getPieces(),
                    gridSize = difficulty,
                    rotationMode = rotateMode,
                    onPieceDoubleTap = if (rotateMode) ({ pos -> handlePieceDoubleTap(pos) }) else null,
                    onPieceClick = { pos -> handlePieceClick(pos) }
                )
                recyclerView.adapter = adapter
                updateUI()
            } catch (e: Exception) {
                Toast.makeText(this@GameActivity, "加载拼图失败: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun setupNewGame() {
        lifecycleScope.launch {
            try {
                val bitmap = loadBitmap()
                puzzleGame = PuzzleGame(bitmap, difficulty, rotateMode)
                puzzleGame.initNewGame()

                // 先保存本局“开局基线”，避免评分best=0、以及未初始化访问
                saveBaselineForThisRun()

                usedHintInThisRun = false
                manualPauseUsedInThisRun = false
                isPaused = false
                pausedTime = 0L

                setupRecyclerView()
                updateUI()
                startTimer()
                btnPause.text = "暂停"
                btnPause.setIconResource(R.drawable.ic_pause)
                updateUndoButtonState()

                // 如果需要作弊完成，也放在这里（确保 puzzleGame/adapter 就绪后再调用）
                runCheatIfNeeded()
            } catch (e: Exception) {
                Log.e("GameActivity", "Failed to setup game", e)
                Toast.makeText(this@GameActivity, "加载拼图失败: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun runCheatIfNeeded() {
        if (!isCheatMode()) return
        if (!::puzzleGame.isInitialized || !::adapter.isInitialized) return
        puzzleGame.forceComplete()
        adapter.updatePieces(puzzleGame.getPieces(), null)
        updateUndoButtonState()
        onPuzzleCompleted()
    }

    private fun saveBaselineForThisRun() {
        val prefs = getSharedPreferences("game_prefs", MODE_PRIVATE)
        val positions = puzzleGame.getPositionsByPiece() // 按 id -> currentPosition（需要 PuzzleGame 暴露此函数）
        val posStr = positions.joinToString(",")
        val rotStr = if (rotateMode) puzzleGame.getRotationsById().joinToString(",") else null

        prefs.edit().apply {
            putString("baseline_positions", posStr)
            if (rotateMode) putString("baseline_rotations", rotStr)
            apply()
        }
    }

    private suspend fun loadBitmap(): Bitmap {
        val src = when {
            isCustom && !imagePath.isNullOrEmpty() -> BitmapFactory.decodeFile(imagePath)
            imageResId != 0 -> BitmapFactory.decodeResource(resources, imageResId)
            else -> BitmapFactory.decodeResource(resources, R.drawable.nature_1)
        }
        val square = ensureSquareBitmap(src)
        return downsampleTo(square, resources.displayMetrics.widthPixels)
    }

    private fun ensureSquareBitmap(src: Bitmap): Bitmap {
        if (src.width == src.height) return src
        val size = minOf(src.width, src.height)
        val x = (src.width - size) / 2
        val y = (src.height - size) / 2
        return Bitmap.createBitmap(src, x, y, size, size)
    }
    private fun downsampleTo(src: Bitmap, target: Int): Bitmap {
        if (src.width <= target && src.height <= target) return src
        val ratio = target.toFloat() / maxOf(src.width, src.height).toFloat()
        val w = (src.width * ratio).toInt().coerceAtLeast(1)
        val h = (src.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, w, h, true)
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = GridLayoutManager(this, difficulty)
        recyclerView.setHasFixedSize(true)
        adapter = PuzzleGameAdapter(
            pieces = puzzleGame.getPieces(),
            gridSize = difficulty,
            rotationMode = rotateMode,
            onPieceDoubleTap = if (rotateMode) ({ position -> handlePieceDoubleTap(position) }) else null,
            onPieceClick = { position -> handlePieceClick(position) }
        )
        recyclerView.adapter = adapter
        recyclerView.setOnTouchListener { _, _ -> false }
    }

    private fun handlePieceDoubleTap(position: Int) {
        if (isPaused) return
        if (puzzleGame.rotatePieceAtPosition(position)) {
            adapter.notifyPieceRotated(position)
            updateUI()
            if (puzzleGame.isCompleted()) onPuzzleCompleted()
        }
    }

    private fun handlePieceClick(position: Int) {
        if (isPaused) return
        playClickSound()

        val prevSelected = puzzleGame.getSelectedPosition()
        val changed = puzzleGame.onPieceClicked(position)
        if (!changed) return

        val nowSelected = puzzleGame.getSelectedPosition()
        when {
            // 只是选中
            prevSelected == null && nowSelected != null -> {
                adapter.updateSelection(prev = null, now = nowSelected)
            }
            // 点同一格，取消选中
            prevSelected != null && nowSelected == null && prevSelected == position -> {
                adapter.updateSelection(prev = prevSelected, now = null)
            }
            // 点另一格，发生交换（prevSelected 与 position 互换）
            prevSelected != null && nowSelected == null && prevSelected != position -> {
                adapter.applySwap(
                    newPieces = puzzleGame.getPieces(),
                    a = prevSelected,
                    b = position,
                    newSelected = null
                )
            }
            else -> {
                // 兜底
                adapter.applySwap(
                    newPieces = puzzleGame.getPieces(),
                    a = position,
                    b = position,
                    newSelected = nowSelected
                )
            }
        }

        updateUI()
        updateUndoButtonState()


        // Debug
        if (isDebugMode()) {
            Log.d("GameActivity", "步数=${puzzleGame.getMoveCount()}, 完成度=${puzzleGame.getCompletionPercentage()}")
            updateDebugInfo()
        }

        if (isCheatMode()) {
            puzzleGame.forceComplete()
            // 只需刷新所有正确格子的“正确指示”，可以全量或按需刷新，这里简单全量：
            adapter.updatePieces(puzzleGame.getPieces(), null)
            onPuzzleCompleted()
            return
        }

        if (puzzleGame.isCompleted()) {
            onPuzzleCompleted()
        }
    }

    private fun updateUI() {
        tvMoves.text = "步数: ${puzzleGame.getMoveCount()}"
        tvProgress.text = "完成: ${puzzleGame.getCompletionPercentage().toInt()}%"
    }

    private fun startTimer() {
        startTime = SystemClock.elapsedRealtime()
        timeHandler.post(timeRunnable)
    }

    private fun updateTime() {
        val elapsedMillis = SystemClock.elapsedRealtime() - startTime
        val seconds = (elapsedMillis / 1000).toInt()
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        tvTime.text = String.format("时间: %02d:%02d", minutes, remainingSeconds)
    }

    private fun togglePause() {
        val goingToPause = !isPaused
        isPaused = goingToPause
        if (goingToPause) {
            manualPauseUsedInThisRun = true
            pausedTime = SystemClock.elapsedRealtime() - startTime
            btnPause.text = "继续"
            btnPause.setIconResource(R.drawable.ic_play_arrow)
            timeHandler.removeCallbacks(timeRunnable)
        } else {
            startTime = SystemClock.elapsedRealtime() - pausedTime
            btnPause.text = "暂停"
            btnPause.setIconResource(R.drawable.ic_pause)
            timeHandler.post(timeRunnable)
        }
    }

    private fun showHint() {
        usedHintInThisRun = true
        val dialog = AlertDialog.Builder(this)
            .setTitle("原图提示")
            .setMessage("显示3秒后自动关闭")
            .create()

        val imageView = android.widget.ImageView(this)
        lifecycleScope.launch {
            val bitmap = loadBitmap()
            imageView.setImageBitmap(bitmap)
            dialog.setView(imageView)
            dialog.show()
            Handler(Looper.getMainLooper()).postDelayed({ dialog.dismiss() }, 3000)
        }
    }

    private fun showRestartDialog() {
        AlertDialog.Builder(this)
            .setTitle("重新开始")
            .setMessage("确定要重新开始游戏吗？当前进度将丢失。")
            .setPositiveButton("确定") { _, _ -> restartGame() }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun restartGame() {
        clearSavedGame()
        isPaused = false
        pausedTime = 0L
        timeHandler.removeCallbacks(timeRunnable)
        setupNewGame()
        btnPause.text = "暂停"
        btnPause.setIconResource(R.drawable.ic_pause)
    }

    private fun onPuzzleCompleted() {

        if (isDebugMode()) {//debug模式
            Log.d("GameActivity", "拼图完成！用时=${tvTime.text}, 步数=${puzzleGame.getMoveCount()}")
            updateDebugInfo()
        }


        isPaused = true
        triggerVibration()

        timeHandler.removeCallbacks(timeRunnable)

        val elapsedMillis = SystemClock.elapsedRealtime() - startTime
        val seconds = (elapsedMillis / 1000).toInt()
        val stars = calculateStars(puzzleGame.getMoveCount())

        val event = com.example.jigsaw.achievements.AchievementsManager.CompletionEvent(
            levelId = puzzleId,
            categoryId = intent.getStringExtra("category_id") ?: if (isCustom) "custom" else "unknown",
            difficulty = difficulty,
            stars = stars,
            timeSeconds = seconds,
            moves = puzzleGame.getMoveCount(),
            usedHint = usedHintInThisRun,
            manualPauseUsed = manualPauseUsedInThisRun,
            isCustom = isCustom,
            rotateMode = rotateMode
        )

        val newly = try {
            com.example.jigsaw.achievements.AchievementsManager.checkOnPuzzleCompleted(this, event)
        } catch (e: Exception) {
            emptyList()
        }

        Log.d("GameActivity", "Game completed! Level: $puzzleId")

        LevelManager.saveLevelProgress(
            context = this,
            levelId = puzzleId,
            difficulty = difficulty,
            rotate = rotateMode,
            stars = stars,
            time = seconds,
            moves = puzzleGame.getMoveCount()
        )

        DebugUtils.printAllLevelProgress(this)
        clearSavedGame()

        if (newly.isNotEmpty()) {
            showAchievementsDialog(newly) { showCompletionDialog(seconds, stars) }
        } else {
            showCompletionDialog(seconds, stars)
        }
    }

    private fun showAchievementsDialog(
        newly: List<com.example.jigsaw.achievements.Achievement>,
        onClosed: () -> Unit
    ) {
        val msg = newly.joinToString("\n") { "🏆 ${it.title}" }
        AlertDialog.Builder(this)
            .setTitle("成就解锁")
            .setMessage(msg)
            .setCancelable(false)
            .setPositiveButton("好的") { d, _ ->
                d.dismiss()
                onClosed()
            }
            .show()
    }

    private fun showCompletionDialog(seconds: Int, stars: Int) {
        AlertDialog.Builder(this)
            .setTitle("恭喜完成！")
            .setMessage(
                """
        拼图: $puzzleName
        关卡ID: $puzzleId
        难度: ${difficulty}×${difficulty}
        用时: ${seconds / 60}分${seconds % 60}秒
        步数: ${puzzleGame.getMoveCount()}
        评价: ${"⭐".repeat(stars)}
        """.trimIndent()
            )
            .setPositiveButton("返回") { _, _ ->
                setResult(RESULT_OK)
                finish()
            }
            .setNeutralButton("再来一局") { _, _ -> restartGame() }
            .setCancelable(false)
            .show()
    }

    // 计算星数，改为和最优步数强相关
    private fun calculateStars(moves: Int): Int {
        val prefs = getSharedPreferences("game_prefs", MODE_PRIVATE)
        val basePos = prefs.getString("baseline_positions", null)
            ?.split(",")?.mapNotNull { it.toIntOrNull() }?.toIntArray()

        // 基线不存在则退化为简单阈值（不至于给1星）
        if (basePos == null || basePos.isEmpty()) {
            val bestFallback = (difficulty * difficulty - 1).coerceAtLeast(1)
            val t2 = kotlin.math.ceil(bestFallback * 1.5).toInt()
            return when {
                moves <= bestFallback -> 3
                moves <= t2 -> 2
                else -> 1
            }
        }

        val bestSwaps = minSwapsFromPermutation(basePos)

        var bestRotations = 0
        if (rotateMode) {
            val baseRot = prefs.getString("baseline_rotations", null)
                ?.split(",")?.mapNotNull { it.toIntOrNull() }
            if (baseRot != null) {
                bestRotations = baseRot.sumOf { deg -> ((360 - ((deg % 360 + 360) % 360)) % 360) / 90 }
            }
        }

        val best = (bestSwaps + bestRotations).coerceAtLeast(1)
        val t2 = kotlin.math.ceil(best * 1.5).toInt()

        return when {
            moves <= best * 2 -> 3
            moves <= t2 * 2 -> 2
            else -> 1
        }
    }

    private fun updateUndoButtonState() {
        btnUndo.isEnabled = ::puzzleGame.isInitialized && puzzleGame.canUndo()
    }

    // 最少交换次数：Σ(环长 - 1)，perm[i] = 该 id 的当前格位置
    private fun minSwapsFromPermutation(perm: IntArray): Int {
        val n = perm.size
        val visited = BooleanArray(n)
        var swaps = 0
        for (i in 0 until n) {
            if (!visited[i]) {
                var j = i
                var len = 0
                while (!visited[j]) {
                    visited[j] = true
                    j = perm[j]
                    len++
                }
                if (len > 0) swaps += (len - 1)
            }
        }
        return swaps
    }

    override fun onPause() {
        super.onPause()
        stopMusic()    // 离开界面停止音乐
        if (!isPaused) {
            isPaused = true
            pausedTime = SystemClock.elapsedRealtime() - startTime
            timeHandler.removeCallbacks(timeRunnable)
        }
        saveGameState()
    }

    private fun saveGameProgress() {
        // 保存游戏进度
        val prefs = getSharedPreferences("game_prefs", MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("has_saved_game", true)
            putString("saved_puzzle_id", puzzleId)
            putString("puzzle_name", puzzleName)
            putInt("difficulty", difficulty)
            putInt("game_progress", puzzleGame.getCompletionPercentage().toInt())
            putLong("elapsed_time", SystemClock.elapsedRealtime() - startTime)
            // 可以保存更多游戏状态
            apply()
        }
    }

    private fun playClickSound() {
        if (!isSoundEnabled()) return

        val mediaPlayer = MediaPlayer.create(this, R.raw.click_sound)
        mediaPlayer.setOnCompletionListener { mp -> mp.release() }
        mediaPlayer.start()
    }

    private fun startMusic() {
        val intent = Intent(this, MusicService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent) // Android 8.0+ 用 startForegroundService
        } else {
            startService(intent)
        }
    }


    private fun stopMusic() {
        val intent = Intent(this, MusicService::class.java)
        stopService(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        timeHandler.removeCallbacks(timeRunnable)
    }

    private fun updateDebugInfo() {
        if (!isDebugMode() || tvDebug == null) return

        val info = "步数=${puzzleGame.getMoveCount()}, " +
                "完成度=${puzzleGame.getCompletionPercentage().toInt()}%, " +
                "用时=${tvTime.text}"
        tvDebug?.text = info
    }

}