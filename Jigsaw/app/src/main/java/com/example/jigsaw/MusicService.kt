package com.example.jigsaw

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class MusicService : Service() {

    companion object {
        private const val CHANNEL_ID = "bgm_channel"
        private const val CHANNEL_NAME = "背景音乐"
        private const val NOTIF_ID = 1001

        private const val ACTION_STOP = "com.example.jigsaw.action.STOP_MUSIC"
    }

    private var mediaPlayer: MediaPlayer? = null
    private var isForeground = false

    // 可选的背景音乐列表（确保这些资源存在）
    private val bgmList = listOf(
        R.raw.bgm1,
        R.raw.bgm2,
        R.raw.bgm3
    )

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 处理通知上的“停止”动作
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        // 8.0+ 必须尽快进入前台
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isForeground) {
            startForeground(NOTIF_ID, buildNotification())
            isForeground = true
        }

        // 初始化或继续播放
        if (mediaPlayer == null) {
            // 随机选择一首
            val resId = bgmList.random()
            mediaPlayer = MediaPlayer.create(this, resId).apply {
                // 标注为媒体音乐
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                }
                isLooping = true
                setOnErrorListener { mp, _, _ ->
                    // 简单恢复策略：重建播放器
                    mp.reset()
                    mp.release()
                    mediaPlayer = null
                    // 再次启动，选择一首重新播放
                    onStartCommand(null, 0, startId)
                    true
                }
            }
        }

        mediaPlayer?.let { if (!it.isPlaying) it.start() }

        // 前台服务建议返回 START_STICKY，系统回收后会尝试重启
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        if (isForeground) {
            // 移除前台及通知
            stopForeground(true)
            isForeground = false
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 通知点击进入 App（可改成进入你想要的页面）
        val launchIntent = Intent(this, MainActivity::class.java)
        val contentPI = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 停止动作
        val stopIntent = Intent(this, MusicService::class.java).apply { action = ACTION_STOP }
        val stopPI = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note) // 确保有这个图标资源；可换成你的图标
            .setContentTitle(getString(R.string.app_name))
            .setContentText("背景音乐播放中")
            .setContentIntent(contentPI)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(R.drawable.ic_stop, "停止", stopPI)
            .setPriority(NotificationCompat.PRIORITY_LOW) // 低打扰
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW // 低优先级，不打扰
            )
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }
}