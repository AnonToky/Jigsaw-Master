package com.example.jigsaw.adapter

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.jigsaw.R
import com.example.jigsaw.database.Level
import com.google.android.material.card.MaterialCardView

class LevelAdapter(
    private val levels: List<Level>,
    private val onLevelClick: (Level) -> Unit
) : RecyclerView.Adapter<LevelAdapter.LevelViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LevelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_level, parent, false)
        return LevelViewHolder(view)
    }

    override fun onBindViewHolder(holder: LevelViewHolder, position: Int) {
        holder.bind(levels[position])
    }

    override fun getItemCount(): Int = levels.size

    inner class LevelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardLevel)
        private val imageView: ImageView = itemView.findViewById(R.id.imageLevelPreview)
        private val lockOverlay: View = itemView.findViewById(R.id.lockOverlay)
        private val lockIcon: ImageView = itemView.findViewById(R.id.lockIcon)
        private val tvLevelName: TextView = itemView.findViewById(R.id.tvLevelName)
        private val tvLevelNumber: TextView = itemView.findViewById(R.id.tvLevelNumber)
        private val starsContainer: ViewGroup = itemView.findViewById(R.id.starsContainer)
        private val star1: ImageView = itemView.findViewById(R.id.star1)
        private val star2: ImageView = itemView.findViewById(R.id.star2)
        private val star3: ImageView = itemView.findViewById(R.id.star3)

        fun bind(level: Level) {
            if (level.isCustom && !level.imagePath.isNullOrEmpty()) {
                val options = BitmapFactory.Options().apply { inSampleSize = 2 }
                val bm = BitmapFactory.decodeFile(level.imagePath, options)
                if (bm != null) imageView.setImageBitmap(bm)
                else imageView.setImageResource(R.drawable.ic_image_placeholder)
            }else {
                imageView.setImageResource(level.thumbnailRes)
            }

            // 设置名称和编号
            tvLevelName.text = level.name
            tvLevelNumber.text = "关卡 ${adapterPosition + 1}"

            // 处理锁定状态
            if (level.isLocked) {
                lockOverlay.visibility = View.VISIBLE
                lockIcon.visibility = View.VISIBLE
                starsContainer.visibility = View.GONE
                cardView.isClickable = false
                cardView.alpha = 0.7f
            } else {
                lockOverlay.visibility = View.GONE
                lockIcon.visibility = View.GONE
                starsContainer.visibility = View.VISIBLE
                cardView.isClickable = true
                cardView.alpha = 1.0f

                // 设置星级
                updateStars(level.stars)

                // 设置点击事件
                cardView.setOnClickListener {
                    onLevelClick(level)
                }
            }
        }

        private fun updateStars(stars: Int) {
            val starViews = listOf(star1, star2, star3)
            starViews.forEachIndexed { index, imageView ->
                if (index < stars) {
                    imageView.setImageResource(R.drawable.ic_star_filled)
                    imageView.setColorFilter(ContextCompat.getColor(itemView.context, R.color.star_color))
                } else {
                    imageView.setImageResource(R.drawable.ic_star_outline)
                    imageView.setColorFilter(ContextCompat.getColor(itemView.context, R.color.star_empty_color))
                }
            }
        }
    }
}