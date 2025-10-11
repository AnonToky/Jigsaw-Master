package com.example.jigsaw.repository

import android.content.Context
import com.example.jigsaw.LevelManager
import com.example.jigsaw.R
import com.example.jigsaw.database.AppDatabase
import com.example.jigsaw.database.Category
import com.example.jigsaw.database.Level
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class LevelRepository private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: LevelRepository? = null

        fun getInstance(context: Context): LevelRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LevelRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // 主入口：按当前难度和“是否旋转模式”返回所有分类
    suspend fun getCategories(difficulty: Int, rotateMode: Boolean): List<Category> =
        withContext(Dispatchers.IO) {
            val nature = getNatureLevels(difficulty, rotateMode)
            val animals = getAnimalLevels(difficulty, rotateMode)
            val architecture = getArchitectureLevels(difficulty, rotateMode)
            val cartoon = getCartoonLevels(difficulty, rotateMode)
            val custom = getCustomLevels(difficulty, rotateMode) // suspend

            listOf(
                Category(
                    id = "nature",
                    name = "自然风光",
                    iconRes = R.drawable.ic_nature,
                    levels = nature
                ),
                Category(
                    id = "animals",
                    name = "动物世界",
                    iconRes = R.drawable.ic_animals,
                    levels = animals
                ),
                Category(
                    id = "architecture",
                    name = "建筑艺术",
                    iconRes = R.drawable.ic_architecture,
                    levels = architecture
                ),
                Category(
                    id = "cartoon",
                    name = "卡通动漫",
                    iconRes = R.drawable.ic_cartoon,
                    levels = cartoon
                ),
                Category(
                    id = "custom",
                    name = "我的拼图",
                    iconRes = R.drawable.ic_my_puzzles,
                    levels = custom
                )
            )
        }

    // 兼容旧调用（不带旋转模式，默认非旋转）
    suspend fun getCategories(difficulty: Int): List<Category> =
        getCategories(difficulty, rotateMode = false)

    private fun getNatureLevels(difficulty: Int, rotateMode: Boolean): List<Level> = listOf(
        Level(
            id = "nature_1",
            name = "山水如画",
            categoryId = "nature",
            imageRes = R.drawable.nature_1,
            thumbnailRes = R.drawable.nature_1,
            isLocked = false, // 第一关总是解锁
            stars = LevelManager.getLevelStars(context, "nature_1", difficulty, rotateMode)
        ),
        Level(
            id = "nature_2",
            name = "森林小径",
            categoryId = "nature",
            imageRes = R.drawable.nature_2,
            thumbnailRes = R.drawable.nature_2,
            isLocked = !LevelManager.isLevelUnlocked(context, "nature_2", difficulty, rotateMode),
            stars = LevelManager.getLevelStars(context, "nature_2", difficulty, rotateMode)
        ),
        Level(
            id = "nature_3",
            name = "海滩之子",
            categoryId = "nature",
            imageRes = R.drawable.nature_3,
            thumbnailRes = R.drawable.nature_3,
            isLocked = !LevelManager.isLevelUnlocked(context, "nature_3", difficulty, rotateMode),
            stars = LevelManager.getLevelStars(context, "nature_3", difficulty, rotateMode)
        ),
        Level(
            id = "nature_4",
            name = "理塘之巅",
            categoryId = "nature",
            imageRes = R.drawable.nature_4,
            thumbnailRes = R.drawable.nature_4,
            isLocked = !LevelManager.isLevelUnlocked(context, "nature_4", difficulty, rotateMode),
            stars = LevelManager.getLevelStars(context, "nature_4", difficulty, rotateMode)
        ),
        Level(
            id = "nature_5",
            name = "瀑布奇观",
            categoryId = "nature",
            imageRes = R.drawable.nature_5,
            thumbnailRes = R.drawable.nature_5,
            isLocked = !LevelManager.isLevelUnlocked(context, "nature_5", difficulty, rotateMode),
            stars = LevelManager.getLevelStars(context, "nature_5", difficulty, rotateMode)
        ),
        Level(
            id = "nature_6",
            name = "（并非）西奈沙漠",
            categoryId = "nature",
            imageRes = R.drawable.nature_6,
            thumbnailRes = R.drawable.nature_6,
            isLocked = !LevelManager.isLevelUnlocked(context, "nature_6", difficulty, rotateMode),
            stars = LevelManager.getLevelStars(context, "nature_6", difficulty, rotateMode)
        )
    )

    private fun getAnimalLevels(difficulty: Int, rotateMode: Boolean): List<Level> = listOf(
        Level(
            id = "animal_1",
            name = "哈基米",
            categoryId = "animals",
            imageRes = R.drawable.animal_1,
            thumbnailRes = R.drawable.animal_1,
            isLocked = false,
            stars = LevelManager.getLevelStars(context, "animal_1", difficulty, rotateMode)
        ),
        Level(
            id = "animal_2",
            name = "哈吉汪",
            categoryId = "animals",
            imageRes = R.drawable.animal_2,
            thumbnailRes = R.drawable.animal_2,
            isLocked = !LevelManager.isLevelUnlocked(context, "animal_2", difficulty, rotateMode),
            stars = LevelManager.getLevelStars(context, "animal_2", difficulty, rotateMode)
        ),
        Level(
            id = "animal_3",
            name = "草原大喵",
            categoryId = "animals",
            imageRes = R.drawable.animal_3,
            thumbnailRes = R.drawable.animal_3,
            isLocked = !LevelManager.isLevelUnlocked(context, "animal_3", difficulty, rotateMode),
            stars = LevelManager.getLevelStars(context, "animal_3", difficulty, rotateMode)
        ),
        Level(
            id = "animal_4",
            name = "王不见王",
            categoryId = "animals",
            imageRes = R.drawable.animal_4,
            thumbnailRes = R.drawable.animal_4,
            isLocked = !LevelManager.isLevelUnlocked(context, "animal_4", difficulty, rotateMode),
            stars = LevelManager.getLevelStars(context, "animal_4", difficulty, rotateMode)
        ),
        Level(
            id = "animal_5",
            name = "深海巨鲨",
            categoryId = "animals",
            imageRes = R.drawable.animal_5,
            thumbnailRes = R.drawable.animal_5,
            isLocked = !LevelManager.isLevelUnlocked(context, "animal_5", difficulty, rotateMode),
            stars = LevelManager.getLevelStars(context, "animal_5", difficulty, rotateMode)
        ),
        Level(
            id = "animal_6",
            name = "神鹰黑手",
            categoryId = "animals",
            imageRes = R.drawable.animal_6,
            thumbnailRes = R.drawable.animal_6,
            isLocked = !LevelManager.isLevelUnlocked(context, "animal_6", difficulty, rotateMode),
            stars = LevelManager.getLevelStars(context, "animal_6", difficulty, rotateMode)
        )
    )

    // 目前无内容，保持签名一致以便将来扩展
    private fun getArchitectureLevels(difficulty: Int, rotateMode: Boolean): List<Level> = listOf(
        Level(
            id = "arch_1",
            name = "不眼熟的泳池",
            categoryId = "arch",
            imageRes = R.drawable.arch_1,
            thumbnailRes = R.drawable.arch_1,
            isLocked = false,
            stars = LevelManager.getLevelStars(context, "arch_1", difficulty, rotateMode)
        ),
        Level(
            id = "arch_2",
            name = "绿色都市",
            categoryId = "arch",
            imageRes = R.drawable.arch_2,
            thumbnailRes = R.drawable.arch_2,
            isLocked = !LevelManager.isLevelUnlocked(context, "arch_2", difficulty, rotateMode),
            stars = LevelManager.getLevelStars(context, "arch_2", difficulty, rotateMode)
        ),
        Level(
            id = "arch_3",
            name = "古今交融",
            categoryId = "arch",
            imageRes = R.drawable.arch_3,
            thumbnailRes = R.drawable.arch_3,
            isLocked = !LevelManager.isLevelUnlocked(context, "arch_3", difficulty, rotateMode),
            stars = LevelManager.getLevelStars(context, "arch_3", difficulty, rotateMode)
        ),
        Level(
            id = "arch_4",
            name = "拱形屋檐",
            categoryId = "arch",
            imageRes = R.drawable.arch_4,
            thumbnailRes = R.drawable.arch_4,
            isLocked = !LevelManager.isLevelUnlocked(context, "arch_4", difficulty, rotateMode),
            stars = LevelManager.getLevelStars(context, "arch_4", difficulty, rotateMode)
        ),
        Level(
            id = "arch_5",
            name = "天幕穹顶",
            categoryId = "arch",
            imageRes = R.drawable.arch_5,
            thumbnailRes = R.drawable.arch_5,
            isLocked = !LevelManager.isLevelUnlocked(context, "arch_5", difficulty, rotateMode),
            stars = LevelManager.getLevelStars(context, "arch_5", difficulty, rotateMode)
        ),
        Level(
            id = "arch_6",
            name = "（伪）望京soho",
            categoryId = "arch",
            imageRes = R.drawable.arch_6,
            thumbnailRes = R.drawable.arch_6,
            isLocked = !LevelManager.isLevelUnlocked(context, "arch_6", difficulty, rotateMode),
            stars = LevelManager.getLevelStars(context, "arch_6", difficulty, rotateMode)
        )
    )

    private fun getCartoonLevels(difficulty: Int, rotateMode: Boolean): List<Level> = listOf(
        Level(
            id = "anime_1",
            name = "🍆",
            categoryId = "anime",
            imageRes = R.drawable.anime_1,
            thumbnailRes = R.drawable.anime_1,
            isLocked = false,
            stars = LevelManager.getLevelStars(context, "anime_1", difficulty, rotateMode)
        ),
        Level(
            id = "anime_2",
            name = "bilibili~",
            categoryId = "anime",
            imageRes = R.drawable.anime_2,
            thumbnailRes = R.drawable.anime_2,
            isLocked = !LevelManager.isLevelUnlocked(context, "anime_2", difficulty, rotateMode),
            stars = LevelManager.getLevelStars(context, "anime_2", difficulty, rotateMode)
        ),
        Level(
            id = "anime_3",
            name = "丽",
            categoryId = "anime",
            imageRes = R.drawable.anime_3,
            thumbnailRes = R.drawable.anime_3,
            isLocked = !LevelManager.isLevelUnlocked(context, "anime_3", difficulty, rotateMode),
            stars = LevelManager.getLevelStars(context, "anime_3", difficulty, rotateMode)
        ),
        Level(
            id = "anime_4",
            name = "two-b",
            categoryId = "anime",
            imageRes = R.drawable.anime_4,
            thumbnailRes = R.drawable.anime_4,
            isLocked = !LevelManager.isLevelUnlocked(context, "anime_4", difficulty, rotateMode),
            stars = LevelManager.getLevelStars(context, "anime_4", difficulty, rotateMode)
        ),
        Level(
            id = "anime_5",
            name = "🥒",
            categoryId = "anime",
            imageRes = R.drawable.anime_5,
            thumbnailRes = R.drawable.anime_5,
            isLocked = !LevelManager.isLevelUnlocked(context, "anime_5", difficulty, rotateMode),
            stars = LevelManager.getLevelStars(context, "anime_5", difficulty, rotateMode)
        ),
        Level(
            id = "anime_6",
            name = "广告海报",
            categoryId = "anime",
            imageRes = R.drawable.anime_6,
            thumbnailRes = R.drawable.anime_6,
            isLocked = !LevelManager.isLevelUnlocked(context, "anime_6", difficulty, rotateMode),
            stars = LevelManager.getLevelStars(context, "anime_6", difficulty, rotateMode)
        )
    )

    // 从数据库读取“我的拼图”，旋转与普通难度各自有星级
    private suspend fun getCustomLevels(difficulty: Int, rotateMode: Boolean): List<Level> {
        val db = AppDatabase.getDatabase(context)
        val repository = PuzzleRepository(db.puzzleDao())
        val puzzles = repository.getAllPuzzles().first() // suspend: 拿到当前快照
        return puzzles.map { p ->
            Level(
                id = p.id,
                name = p.name,
                categoryId = "custom",
                imageRes = 0,
                thumbnailRes = 0,
                imagePath = p.thumbnailPath ?: p.imagePath,
                isCustom = true,
                isLocked = false, // 自定义关卡不走串行解锁
                stars = LevelManager.getLevelStars(context, p.id, difficulty, rotateMode)
            )
        }
    }
}

