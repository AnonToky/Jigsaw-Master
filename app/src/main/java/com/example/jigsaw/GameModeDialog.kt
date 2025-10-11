package com.example.jigsaw

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.example.jigsaw.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class GameModeDialog(
    context: Context,
    private val hasSavedGame: Boolean,
    private val savedGameInfo: SavedGameInfo? = null,
    private val onModeSelected: (GameMode) -> Unit
) : Dialog(context) {

    enum class GameMode {
        NEW_GAME,
        CONTINUE_GAME
    }

    data class SavedGameInfo(
        val puzzleName: String,
        val difficulty: String,
        val progress: Int,
        val timeSpent: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 设置全屏效果
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_game_mode)

        // 设置窗口属性
        window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

            // 设置全屏标志
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            }

            // 设置点击外部可关闭
            setCanceledOnTouchOutside(true)
        }

        setupViews()
    }

    private fun setupViews() {
        val cardNewGame = findViewById<CardView>(R.id.cardNewGame)
        val cardContinue = findViewById<CardView>(R.id.cardContinue)
        val tvContinueTitle = findViewById<TextView>(R.id.tvContinueTitle)
        val tvContinueInfo = findViewById<TextView>(R.id.tvContinueInfo)
        val tvContinueProgress = findViewById<TextView>(R.id.tvContinueProgress)

        // 设置继续游戏卡片
        if (hasSavedGame && savedGameInfo != null) {
            cardContinue.visibility = View.VISIBLE
            tvContinueTitle.text = savedGameInfo.puzzleName
            tvContinueInfo.text = "${savedGameInfo.difficulty} • ${savedGameInfo.timeSpent}"
            tvContinueProgress.text = "完成度: ${savedGameInfo.progress}%"

            cardContinue.setOnClickListener {
                onModeSelected(GameMode.CONTINUE_GAME)
                dismiss()
            }
        } else {
            cardContinue.visibility = View.GONE
        }

        // 新游戏按钮
        cardNewGame.setOnClickListener {
            onModeSelected(GameMode.NEW_GAME)
            dismiss()
        }

        // 点击外部关闭
        findViewById<View>(R.id.rootLayout).setOnClickListener {
            dismiss()
        }
    }
}