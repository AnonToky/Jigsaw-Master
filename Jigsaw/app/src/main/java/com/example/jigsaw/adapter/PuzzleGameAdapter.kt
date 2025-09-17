package com.example.jigsaw.adapter

import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.jigsaw.R
import com.example.jigsaw.model.PuzzlePiece

class PuzzleGameAdapter(
    private var pieces: List<PuzzlePiece>,
    private val gridSize: Int,
    private val rotationMode: Boolean = false,
    private val onPieceDoubleTap: ((Int) -> Unit)? = null,
    private val onPieceClick: (Int) -> Unit
) : RecyclerView.Adapter<PuzzleGameAdapter.PuzzleViewHolder>() {


    private var selectedPosition: Int? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PuzzleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_puzzle_piece, parent, false)

        // 若需要正方形，可在 item_puzzle_piece 用 ConstraintLayout 约束，这里不强制改 LayoutParams，避免 parent.width=0 时导致点击区域异常
        // 如必须在此设，可解开下面两行：
        // val width = parent.width / gridSize
        // view.layoutParams = ViewGroup.LayoutParams(width, width)

        return PuzzleViewHolder(view)
    }

    override fun onBindViewHolder(holder: PuzzleViewHolder, position: Int) {
        val piece = pieces[position]
        holder.bind(
            piece = piece,
            isSelected = (piece.currentPosition == selectedPosition),
            rotationMode = rotationMode,
            onPieceClick = onPieceClick,
            onPieceDoubleTap = onPieceDoubleTap
        )
    }

    fun updateSelection(prev: Int?, now: Int?) {
        val old = selectedPosition
        selectedPosition = now
        prev?.let { notifyItemChanged(it) }
        now?.let { if (it != prev) notifyItemChanged(it) }
        if (old != null && old != prev && old != now) notifyItemChanged(old)
    }

    fun notifyPieceRotated(position: Int) {
        notifyItemChanged(position)
    }

    fun applySwap(newPieces: List<PuzzlePiece>, a: Int, b: Int, newSelected: Int?) {
        pieces = newPieces
        selectedPosition = newSelected
        notifyItemChanged(a)
        if (b != a) notifyItemChanged(b)
    }

    override fun getItemCount(): Int = pieces.size

    fun updatePieces(newPieces: List<PuzzlePiece>, selected: Int?) {
        pieces = newPieces
        selectedPosition = selected
        notifyDataSetChanged()
    }

    inner class PuzzleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val container: FrameLayout = itemView.findViewById(R.id.pieceContainer)
        private val imageView: ImageView = itemView.findViewById(R.id.imagePiece)
        private val selectionOverlay: View = itemView.findViewById(R.id.selectionOverlay)
        private val correctIndicator: View = itemView.findViewById(R.id.correctIndicator)

        fun bind(
            piece: PuzzlePiece,
            isSelected: Boolean,
            rotationMode: Boolean,
            onPieceClick: (Int) -> Unit,
            onPieceDoubleTap: ((Int) -> Unit)?
        ) {
            imageView.setImageBitmap(piece.bitmap)
            imageView.rotation = if (rotationMode) piece.rotationDeg.toFloat() else 0f

            selectionOverlay.visibility = if (isSelected) View.VISIBLE else View.GONE
            if (isSelected) {
                itemView.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start()
            } else {
                itemView.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            }

            val isCorrect =
                if (rotationMode) piece.currentPosition == piece.correctPosition && (piece.rotationDeg % 360 == 0)
                else piece.currentPosition == piece.correctPosition
            correctIndicator.visibility = if (isCorrect) View.VISIBLE else View.GONE

            // 避免子视图拦截
            selectionOverlay.isClickable = false
            correctIndicator.isClickable = false

            // 清理旧监听
            itemView.setOnClickListener(null)
            itemView.setOnTouchListener(null)

            if (rotationMode) {
                val detector = GestureDetector(itemView.context, object : GestureDetector.SimpleOnGestureListener() {
                    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                        onPieceClick(piece.currentPosition)
                        return true
                    }

                    override fun onDoubleTap(e: MotionEvent): Boolean {
                        onPieceDoubleTap?.invoke(piece.currentPosition)
                        // 小动画（最终角度以外部刷新为准）
                        imageView.animate().rotationBy(90f).setDuration(120).start()
                        return true
                    }
                })
                itemView.setOnTouchListener { _, ev ->
                    detector.onTouchEvent(ev)
                    false // 关键：不拦截，让单击也顺利触发
                }
            } else {
                itemView.setOnClickListener { onPieceClick(piece.currentPosition) }
            }
        }
    }
}