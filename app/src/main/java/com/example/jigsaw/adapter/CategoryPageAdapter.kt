package com.example.jigsaw.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.jigsaw.R
import com.example.jigsaw.database.Category
import com.example.jigsaw.database.Level

class CategoryPagerAdapter(
    private val categories: List<Category>,
    private val onLevelClick: (Level) -> Unit
) : RecyclerView.Adapter<CategoryPagerAdapter.CategoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.page_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount(): Int = categories.size

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val recyclerView: RecyclerView = itemView.findViewById(R.id.recyclerViewLevels)

        fun bind(category: Category) {
            recyclerView.layoutManager = GridLayoutManager(itemView.context, 2)
            recyclerView.adapter = LevelAdapter(category.levels, onLevelClick)
        }
    }
}