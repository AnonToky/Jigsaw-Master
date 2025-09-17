package com.example.jigsaw.adapter

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.jigsaw.R
import com.example.jigsaw.adapter.MyPuzzlesAdapter.PuzzleViewHolder.PuzzleDiffCallback
import com.example.jigsaw.database.PuzzleEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyPuzzlesAdapter(
    private val onItemClick: (PuzzleEntity) -> Unit,
    private val onItemLongClick: (PuzzleEntity) -> Unit
) : ListAdapter<PuzzleEntity, MyPuzzlesAdapter.PuzzleViewHolder>(PuzzleDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PuzzleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_puzzle, parent, false)
        return PuzzleViewHolder(view)
    }

    override fun onBindViewHolder(holder: PuzzleViewHolder, position: Int) {
        val puzzle = getItem(position)
        holder.bind(puzzle, onItemClick, onItemLongClick)
    }

    class PuzzleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.imageViewPuzzle)
        private val textViewName: TextView = itemView.findViewById(R.id.textViewPuzzleName)
        private val textViewInfo: TextView = itemView.findViewById(R.id.textViewPuzzleInfo)

        fun bind(
            puzzle: PuzzleEntity,
            onItemClick: (PuzzleEntity) -> Unit,
            onItemLongClick: (PuzzleEntity) -> Unit
        ) {
            textViewName.text = puzzle.name

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateStr = dateFormat.format(Date(puzzle.createdTime))
            textViewInfo.text = "${puzzle.difficulty}×${puzzle.difficulty} · $dateStr"

            // 优化图片加载逻辑
            loadPuzzleImage(puzzle)

            itemView.setOnClickListener { onItemClick(puzzle) }
            itemView.setOnLongClickListener {
                onItemLongClick(puzzle)
                true
            }
        }

        private fun loadPuzzleImage(puzzle: PuzzleEntity) {
            // 首先尝试加载缩略图
            if (!puzzle.thumbnailPath.isNullOrEmpty()) {
                val thumbnailFile = File(puzzle.thumbnailPath)
                if (thumbnailFile.exists()) {
                    imageView.setImageURI(android.net.Uri.fromFile(thumbnailFile))
                    return
                }
            }

            // 如果缩略图不存在，尝试加载原始图片
            val imageFile = File(puzzle.imagePath)
            if (imageFile.exists()) {
                try {
                    // 加载原始图片并缩放以避免内存问题
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = 4  // 缩小为原来的1/4
                    }
                    val bitmap = BitmapFactory.decodeFile(puzzle.imagePath, options)
                    imageView.setImageBitmap(bitmap)
                    return
                } catch (e: Exception) {
                    // 图片加载失败，使用默认图片
                }
            }

            // 如果都失败了，使用默认图片
            imageView.setImageResource(R.drawable.ic_image_placeholder)
        }

        class PuzzleDiffCallback : DiffUtil.ItemCallback<PuzzleEntity>() {
            override fun areItemsTheSame(oldItem: PuzzleEntity, newItem: PuzzleEntity): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: PuzzleEntity, newItem: PuzzleEntity): Boolean {
                return oldItem == newItem
            }
        }
    }
}