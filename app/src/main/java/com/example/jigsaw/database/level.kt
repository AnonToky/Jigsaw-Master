package com.example.jigsaw.database

data class Level(
    val id: String,
    val name: String,
    val categoryId: String,
    val imageRes: Int,
    val thumbnailRes: Int,
    val imagePath: String? = null,   // 新增：自定义关卡的图片文件路径
    val isCustom: Boolean = false,   // 新增：是否为自定义关卡
    val difficulty: Int = 4,
    val isLocked: Boolean = false,
    val stars: Int = 0,  // 0-3星评价
    val bestTime: Long? = null,
    val playCount: Int = 0
)

data class Category(
    val id: String,
    val name: String,
    val iconRes: Int,
    val levels: List<Level>
)