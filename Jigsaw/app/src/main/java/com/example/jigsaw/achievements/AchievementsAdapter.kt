// com/example/jigsaw/achievements/AchievementsAdapter.kt
package com.example.jigsaw.achievements

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.jigsaw.R
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AchievementsAdapter :
    ListAdapter<AchievementsAdapter.UiItem, AchievementsAdapter.VH>(Diff()) {

    data class UiItem(
        val achievement: Achievement,
        val unlocked: Boolean,
        val progress: Int,
        val unlockTime: Long
    )

    class Diff : DiffUtil.ItemCallback<UiItem>() {
        override fun areItemsTheSame(oldItem: UiItem, newItem: UiItem) = oldItem.achievement.id == newItem.achievement.id
        override fun areContentsTheSame(oldItem: UiItem, newItem: UiItem) = oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_achievement, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgIcon: ImageView = itemView.findViewById(R.id.imgIcon)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvDesc: TextView = itemView.findViewById(R.id.tvDesc)
        private val progressContainer: View = itemView.findViewById(R.id.progressContainer)
        private val progress: LinearProgressIndicator = itemView.findViewById(R.id.progress)
        private val tvProgressText: TextView = itemView.findViewById(R.id.tvProgressText)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)

        fun bind(ui: UiItem) {
            val ctx = itemView.context

            // 主题相关颜色
            val colorOnSurface = com.google.android.material.color.MaterialColors.getColor(
                itemView, com.google.android.material.R.attr.colorOnSurface
            )
            val colorSecondary = com.google.android.material.color.MaterialColors.getColor(
                itemView, com.google.android.material.R.attr.colorSecondary
            )
            // “禁用/次级”颜色（部分版本没有 colorOutline，可回退用 onSurface 加 alpha）
            val colorOutline = runCatching {
                com.google.android.material.color.MaterialColors.getColor(
                    itemView, com.google.android.material.R.attr.colorOutline
                )
            }.getOrElse { (colorOnSurface and 0x00FFFFFF) or 0x88000000.toInt() }

            val a = ui.achievement
            tvTitle.text = a.title
            tvDesc.text = a.desc

            // 已解锁使用主文色/次要高亮，未解锁使用 outline
            tvTitle.setTextColor(if (ui.unlocked) colorOnSurface else colorOutline)
            tvDesc.setTextColor(if (ui.unlocked) colorOnSurface else colorOutline)
            tvStatus.setTextColor(if (ui.unlocked) colorOnSurface else colorOutline)

            // 图标着色：已解锁用 colorSecondary（或你的 star_color），未解锁用 outline
            imgIcon.imageTintList = android.content.res.ColorStateList.valueOf(
                if (ui.unlocked) colorSecondary else colorOutline
            )
            imgIcon.setImageResource(a.iconRes)

            // 进度显示
            if (!ui.unlocked && (a.type == Achievement.Type.INCREMENTAL || a.type == Achievement.Type.STREAK)) {
                progressContainer.visibility = View.VISIBLE
                progress.max = a.goal
                progress.progress = ui.progress.coerceAtMost(a.goal)
                tvProgressText.text = "${ui.progress}/${a.goal}"
            } else {
                progressContainer.visibility = View.GONE
            }

            // 状态文字
            tvStatus.text = if (ui.unlocked) {
                val ts = ui.unlockTime
                if (ts > 0) {
                    val fmt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                    "已解锁 · ${fmt.format(java.util.Date(ts))}"
                } else "已解锁"
            } else {
                when (a.type) {
                    Achievement.Type.ONE_SHOT -> "未解锁"
                    Achievement.Type.INCREMENTAL, Achievement.Type.STREAK -> "进行中"
                }
            }
        }
    }
}