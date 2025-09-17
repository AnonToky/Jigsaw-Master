package com.example.jigsaw

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.jigsaw.adapter.MyPuzzlesAdapter
import com.example.jigsaw.database.PuzzleEntity
import com.example.jigsaw.viewmodel.MyPuzzlesViewModel
import com.example.jigsaw.viewmodel.MyPuzzlesUiState
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyPuzzlesActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: View
    private lateinit var fabAdd: FloatingActionButton

    private val viewModel: MyPuzzlesViewModel by viewModels()
    private lateinit var adapter: MyPuzzlesAdapter

    private val editorLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // 数据会通过Flow自动更新
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_puzzles)

        initViews()
        setupRecyclerView()
        observeUiState()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        recyclerView = findViewById(R.id.recyclerViewMyPuzzles)
        emptyView = findViewById(R.id.emptyView)
        fabAdd = findViewById(R.id.fabAdd)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        fabAdd.setOnClickListener {
            val intent = Intent(this, EditorActivity::class.java)
            editorLauncher.launch(intent)
        }
    }

    private fun setupRecyclerView() {
        adapter = MyPuzzlesAdapter(
            onItemClick = { puzzle -> showPuzzleOptions(puzzle) },
            onItemLongClick = { puzzle -> showDeleteConfirmation(puzzle) }
        )

        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = adapter
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUi(state)
                }
            }
        }
    }

    private fun updateUi(state: MyPuzzlesUiState) {
        adapter.submitList(state.puzzles)

        emptyView.visibility = if (state.isEmpty) View.VISIBLE else View.GONE
        recyclerView.visibility = if (state.isEmpty) View.GONE else View.VISIBLE

        toolbar.subtitle = if (state.puzzles.isNotEmpty()) {
            "共 ${state.puzzles.size} 个拼图"
        } else null

        state.message?.let {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    private fun showPuzzleOptions(puzzle: PuzzleEntity) {
        val options = arrayOf("开始游戏", "查看详情", "删除")

        AlertDialog.Builder(this)
            .setTitle(puzzle.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> startPuzzleGame(puzzle)
                    1 -> showPuzzleDetails(puzzle)
                    2 -> showDeleteConfirmation(puzzle)
                }
            }
            .show()
    }

    private fun startPuzzleGame(puzzle: PuzzleEntity) {
        viewModel.incrementPlayCount(puzzle)
        val intent = Intent(this, GameActivity::class.java).apply {
            putExtra("puzzle_id", puzzle.id)
            putExtra("puzzle_name", puzzle.name)
            putExtra("puzzle_path", puzzle.imagePath)
            putExtra("is_custom", true)
            putExtra("difficulty", puzzle.difficulty) // 用用户保存的默认难度
        }
        startActivity(intent)
    }

    private fun showPuzzleDetails(puzzle: PuzzleEntity) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val details = buildString {
            appendLine("名称：${puzzle.name}")
            appendLine("创建时间：${dateFormat.format(Date(puzzle.createdTime))}")
            appendLine("难度：${puzzle.difficulty}x${puzzle.difficulty}")
            appendLine("游玩次数：${puzzle.playCount}")
            if (puzzle.bestTime != null) {
                val seconds = puzzle.bestTime / 1000
                appendLine("最佳时间：${seconds / 60}分${seconds % 60}秒")
            }
        }

        AlertDialog.Builder(this)
            .setTitle("拼图详情")
            .setMessage(details)
            .setPositiveButton("确定", null)
            .show()
    }

    private fun showDeleteConfirmation(puzzle: PuzzleEntity) {
        AlertDialog.Builder(this)
            .setTitle("删除拼图")
            .setMessage("确定要删除「${puzzle.name}」吗？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deletePuzzle(puzzle)
            }
            .setNegativeButton("取消", null)
            .show()
    }
}