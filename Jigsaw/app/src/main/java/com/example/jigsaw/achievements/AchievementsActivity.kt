// com/example/jigsaw/achievements/AchievementsActivity.kt
package com.example.jigsaw.achievements

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jigsaw.R
import com.google.android.material.appbar.MaterialToolbar
import androidx.recyclerview.widget.RecyclerView

class AchievementsActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AchievementsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_achievements)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = AchievementsAdapter()
        recyclerView.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        // 刷新数据
        val items = AchievementsCatalog.ALL.map { a ->
            val unlocked = AchievementsManager.isUnlocked(this, a.id)
            val progress = AchievementsManager.getProgress(this, a.id)
            val ts = AchievementsManager.getUnlockTime(this, a.id)
            AchievementsAdapter.UiItem(a, unlocked, progress, ts)
        }.sortedWith(compareBy<AchievementsAdapter.UiItem> { !it.unlocked }.thenBy { it.achievement.id })
        adapter.submitList(items)
    }
}