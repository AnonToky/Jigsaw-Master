package com.example.jigsaw

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.jigsaw.achievements.AchievementsManager
import com.example.jigsaw.database.AppDatabase
import com.example.jigsaw.database.PuzzleEntity
import com.example.jigsaw.repository.PuzzleRepository
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class EditorActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var imagePreview: ImageView
    private lateinit var btnSelectImage: MaterialButton
    private lateinit var inputPuzzleName: TextInputEditText
    private lateinit var seekBarDifficulty: SeekBar
    private lateinit var tvDifficulty: TextView
    private lateinit var btnSave: MaterialButton

    private var croppedBitmap: Bitmap? = null
    private var currentDifficulty = 3

    // 图片选择器
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            handleSelectedImage(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        imagePreview = findViewById(R.id.imagePreview)
        btnSelectImage = findViewById(R.id.btnSelectImage)
        inputPuzzleName = findViewById(R.id.inputPuzzleName)
        seekBarDifficulty = findViewById(R.id.seekBarDifficulty)
        tvDifficulty = findViewById(R.id.tvDifficulty)
        btnSave = findViewById(R.id.btnSave)

        // 设置工具栏
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // 初始化难度显示
        updateDifficultyText(currentDifficulty)

        // 初始状态下保存按钮不可用
        btnSave.isEnabled = false
    }

    private fun setupListeners() {
        btnSelectImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        seekBarDifficulty.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentDifficulty = progress + 3 // 3x3 到 6x6
                updateDifficultyText(currentDifficulty)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnSave.setOnClickListener {
            savePuzzle()
        }
    }

    private fun handleSelectedImage(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)

            // 自动裁剪为正方形
            croppedBitmap = cropToSquare(originalBitmap)

            // 显示裁剪后的图片
            imagePreview.setImageBitmap(croppedBitmap)

            // 启用保存按钮
            btnSave.isEnabled = true

            // 如果没有输入名称，使用默认名称
            if (inputPuzzleName.text.isNullOrEmpty()) {
                inputPuzzleName.setText("自定义拼图 ${System.currentTimeMillis() % 1000}")
            }

        } catch (e: Exception) {
            Toast.makeText(this, "无法加载图片: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cropToSquare(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val size = minOf(width, height)

        val cropLeft = (width - size) / 2
        val cropTop = (height - size) / 2

        return Bitmap.createBitmap(bitmap, cropLeft, cropTop, size, size)
    }

    private fun updateDifficultyText(difficulty: Int) {
        tvDifficulty.text = "默认难度: ${difficulty}x${difficulty}"
    }

    private fun savePuzzle() {
        val puzzleName = inputPuzzleName.text?.toString()?.trim()

        if (puzzleName.isNullOrEmpty()) {
            Toast.makeText(this, "请输入拼图名称", Toast.LENGTH_SHORT).show()
            return
        }

        croppedBitmap?.let { bitmap ->
            val puzzleId = savePuzzleToStorage(bitmap, puzzleName)
            if (puzzleId != null) {
                Toast.makeText(this, "拼图保存成功！", Toast.LENGTH_LONG).show()
                // 返回结果给调用者
                setResult(RESULT_OK, Intent().apply {
                    putExtra("puzzle_id", puzzleId)
                    putExtra("puzzle_name", puzzleName)
                })
                finish()
            }
        } ?: run {
            Toast.makeText(this, "请先选择图片", Toast.LENGTH_SHORT).show()
        }
    }

    private fun savePuzzleToStorage(bitmap: Bitmap, puzzleName: String): String? {
        return try {
            val puzzleId = UUID.randomUUID().toString()
            val fileName = "puzzle_$puzzleId.png"
            val thumbnailFileName = "thumbnail_$puzzleId.png"

            // 创建自定义拼图目录
            val puzzleDir = File(filesDir, "custom_puzzles")
            if (!puzzleDir.exists()) {
                puzzleDir.mkdirs()
            }

            // 保存原始图片文件
            val imageFile = File(puzzleDir, fileName)
            val outputStream = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.close()

            // 生成并保存缩略图
            val thumbnailPath = createThumbnail(bitmap, puzzleDir, thumbnailFileName)

            // 保存拼图信息到数据库
            savePuzzleToDatabase(puzzleId, puzzleName, imageFile.absolutePath, thumbnailPath)


            puzzleId
        } catch (e: Exception) {
            Toast.makeText(this, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
        AchievementsManager.onCustomCreated(this)
    }

    private fun createThumbnail(bitmap: Bitmap, directory: File, fileName: String): String {
        // 创建缩略图 (200x200像素)
        val thumbnailSize = 200
        val thumbnailBitmap = Bitmap.createScaledBitmap(bitmap, thumbnailSize, thumbnailSize, true)

        // 保存缩略图
        val thumbnailFile = File(directory, fileName)
        val outputStream = FileOutputStream(thumbnailFile)
        thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        outputStream.close()

        // 回收不再需要的缩略图位图
        thumbnailBitmap.recycle()

        return thumbnailFile.absolutePath
    }

    private fun savePuzzleToDatabase(puzzleId: String, puzzleName: String, imagePath: String, thumbnailPath: String) {
        // 创建拼图实体
        val puzzle = PuzzleEntity(
            id = puzzleId,
            name = puzzleName,
            imagePath = imagePath,
            thumbnailPath = thumbnailPath, // 添加缩略图路径
            difficulty = currentDifficulty,
            pieceCount = currentDifficulty * currentDifficulty,
            createdTime = System.currentTimeMillis(),
            lastPlayedTime = null,
            playCount = 0,
            bestTime = null,
            isCompleted = false
        )

        // 使用协程在后台线程中保存到数据库
        lifecycleScope.launch {
            val database = AppDatabase.getDatabase(applicationContext)
            val repository = PuzzleRepository(database.puzzleDao())
            repository.insertPuzzle(puzzle)
        }

        AchievementsManager.onCustomCreated(this)
    }

    private fun savePuzzleInfo(puzzleId: String, puzzleName: String, imagePath: String) {
        val sharedPrefs = getSharedPreferences("custom_puzzles", MODE_PRIVATE)
        val editor = sharedPrefs.edit()

        // 保存拼图信息
        editor.putString("puzzle_${puzzleId}_name", puzzleName)
        editor.putString("puzzle_${puzzleId}_path", imagePath)
        editor.putInt("puzzle_${puzzleId}_difficulty", currentDifficulty)
        editor.putLong("puzzle_${puzzleId}_time", System.currentTimeMillis())

        // 更新拼图ID列表
        val puzzleIds = sharedPrefs.getStringSet("puzzle_ids", mutableSetOf()) ?: mutableSetOf()
        puzzleIds.add(puzzleId)
        editor.putStringSet("puzzle_ids", puzzleIds)

        editor.apply()
    }
}