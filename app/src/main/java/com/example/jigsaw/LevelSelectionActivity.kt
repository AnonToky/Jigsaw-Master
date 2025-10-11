package com.example.jigsaw

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.jigsaw.adapter.CategoryPagerAdapter
import com.example.jigsaw.adapter.LevelAdapter
import com.example.jigsaw.database.Category
import com.example.jigsaw.database.Level
import com.example.jigsaw.repository.LevelRepository
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch

class LevelSelectionActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var chipGroupDifficulty: ChipGroup

    private lateinit var categoryAdapter: CategoryPagerAdapter
    private lateinit var levelRepository: LevelRepository

    private val gameActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Log.d("LevelSelection", "Game completed, refreshing categories")
            // 重新加载关卡数据
            loadCategories()
        }
    }

    override fun onResume() {
        super.onResume()
        loadCategories()
    }

    private var selectedDifficulty = 3 // 默认3*3
    private var currentCategories: List<Category> = emptyList()
    private var tabMediator: TabLayoutMediator? = null
    private var isRotateMode = false            // 是否旋转模式
    private var lastNonRotateDifficulty = 3     // 记住最近一次非旋转难度（供切换回来）

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_level_selection)
        levelRepository = LevelRepository.getInstance(this)
        initViews()
        setupToolbar()
        setupDifficultyChips()
        loadCategories()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)
        chipGroupDifficulty = findViewById(R.id.chipGroupDifficulty)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupDifficultyChips() {
        // 默认选中 3×3
        selectedDifficulty = 3
        lastNonRotateDifficulty = 3
        chipGroupDifficulty.check(R.id.chip3x3)

        chipGroupDifficulty.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.chip3x3 -> { isRotateMode = false; selectedDifficulty = 3; lastNonRotateDifficulty = 3 }
                R.id.chip4x4 -> { isRotateMode = false; selectedDifficulty = 4; lastNonRotateDifficulty = 4 }
                R.id.chip5x5 -> { isRotateMode = false; selectedDifficulty = 5; lastNonRotateDifficulty = 5 }
                R.id.chip6x6 -> { isRotateMode = false; selectedDifficulty = 6; lastNonRotateDifficulty = 6 }
                R.id.chipRotate -> {
                    isRotateMode = true
                    selectedDifficulty = 3 // 旋转模式固定3×3
                }
            }
            loadCategories() // 你已有“保持当前类别”的逻辑，这里沿用
        }
    }



    private fun loadCategories() {
        val keepCategoryId = currentCategories.getOrNull(viewPager.currentItem)?.id
        lifecycleScope.launch {
            val categories = levelRepository.getCategories(selectedDifficulty, isRotateMode)
            currentCategories = categories
            setupViewPager(categories, keepCategoryId)
        }
    }

    private fun setupViewPager(categories: List<Category>, keepCategoryId: String? = null) {
        categoryAdapter = CategoryPagerAdapter(categories) { level -> startGame(level) }
        viewPager.adapter = categoryAdapter
        tabMediator?.detach()
        tabMediator = TabLayoutMediator(tabLayout, viewPager) { tab, pos ->
            tab.text = categories[pos].name
            tab.setIcon(categories[pos].iconRes)
        }.also { it.attach() }
        if (keepCategoryId != null) {
            val idx = categories.indexOfFirst { it.id == keepCategoryId }
            if (idx >= 0) viewPager.setCurrentItem(idx, false)
        }
    }



    private fun startGame(level: Level) {
        val intent = Intent(this, GameActivity::class.java).apply {
            putExtra("difficulty", selectedDifficulty) // 旋转模式下为3
            putExtra("rotate_mode", isRotateMode)      // 关键：告诉 GameActivity 当前是旋转难度
            putExtra("category_id", level.categoryId)
            if (level.isCustom) {
                putExtra("puzzle_id", level.id)
                putExtra("puzzle_name", level.name)
                putExtra("puzzle_path", level.imagePath)
                putExtra("is_custom", true)
            } else {
                putExtra("level_id", level.id)
                putExtra("level_name", level.name)
                putExtra("level_image", level.imageRes)
                putExtra("is_custom", false)
            }
        }
        gameActivityLauncher.launch(intent)
    }
}