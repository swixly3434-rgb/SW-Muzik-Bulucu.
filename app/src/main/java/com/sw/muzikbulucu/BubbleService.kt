package com.sw.muzikbulucu

import android.Manifest
import android.animation.ValueAnimator
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Outline
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class BubbleService : Service() {

    companion object {
        const val CH = "bubble"
        const val CH_FOUND = "found"
        const val ACTION_STOP = "com.sw.muzikbulucu.STOP"

        fun start(ctx: Context) {
            ContextCompat.startForegroundService(ctx, Intent(ctx, BubbleService::class.java))
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, BubbleService::class.java))
            Store.bubbleOn = false
        }
    }

    private lateinit var wm: WindowManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null
    private var cardJob: Job? = null
    private var pulse: ValueAnimator? = null

    private var host: FrameLayout? = null
    private var inner: FrameLayout? = null
    private var hostLp: WindowManager.LayoutParams? = null
    private var card: View? = null
    private var cardLp: WindowManager.LayoutParams? = null

    private fun px(v: Int) = (v * resources.displayMetrics.density).toInt()
    private val bub get() = px(80)
    private val circ get() = px(64)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH, "Baloncuk", NotificationManager.IMPORTANCE_LOW)
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_FOUND, "Bulunan şarkılar", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val micOk = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (intent?.action == ACTION_STOP || !Settings.canDrawOverlays(this) || !micOk) {
            Store.bubbleOn = false
            stopSelf()
            return START_NOT_STICKY
        }
        startFg()
        if (host == null) addBubble()
        Store.bubbleOn = true
        return START_NOT_STICKY
    }

    private fun startFg() {
        val stopPi = PendingIntent.getService(
            this, 1, Intent(this, BubbleService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val openPi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(this, CH)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle("SW Müzik Bulucu")
            .setContentText(T("Baloncuğa dokun, müziği bulsun", "Tap the bubble to identify music"))
            .setContentIntent(openPi)
            .addAction(0, T("Kapat", "Close"), stopPi)
            .setOngoing(true)
            .build()
        ServiceCompat.startForeground(this, 1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
    }

    // ---------- Baloncuk ----------

    private fun addBubble() {
        val h = FrameLayout(this)
        val c = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFF0B1F12.toInt())
                setStroke(px(3), 0xFF22C55E.toInt())
            }
            setPadding(px(3), px(3), px(3), px(3))
        }
        val img = ImageView(this).apply {
            setImageResource(R.drawable.bubble)
            scaleType = ImageView.ScaleType.CENTER_CROP
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
        }
        c.addView(img, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        h.addView(c, FrameLayout.LayoutParams(circ, circ, Gravity.CENTER))

        val lp = WindowManager.LayoutParams(
            bub, bub,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = px(8)
            y = px(220)
        }

        h.setOnTouchListener(object : View.OnTouchListener {
            var sx = 0; var sy = 0; var tx = 0f; var ty = 0f; var moved = false
            override fun onTouch(v: View, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> {
                        sx = lp.x; sy = lp.y; tx = e.rawX; ty = e.rawY; moved = false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = e.rawX - tx
                        val dy = e.rawY - ty
                        if (abs(dx) > px(6) || abs(dy) > px(6)) moved = true
                        if (moved) {
                            lp.x = sx + dx.toInt()
                            lp.y = sy + dy.toInt()
                            wm.updateViewLayout(h, lp)
                            positionCard()
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!moved) {
                            onBubbleTap()
                        } else {
                            val sw = resources.displayMetrics.widthPixels
                            lp.x = if (lp.x + bub / 2 < sw / 2) 0 else sw - bub
                            wm.updateViewLayout(h, lp)
                            positionCard()
                        }
                    }
                }
                return true
            }
        })

        host = h; inner = c; hostLp = lp
        wm.addView(h, lp)
    }

    private fun startPulse() {
        pulse?.cancel()
        pulse = ValueAnimator.ofFloat(1f, 1.2f).apply {
            duration = 600
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                val f = it.animatedValue as Float
                inner?.scaleX = f
                inner?.scaleY = f
            }
            start()
        }
    }

    private fun stopPulse() {
        pulse?.cancel()
        pulse = null
        inner?.scaleX = 1f
        inner?.scaleY = 1f
    }

    private fun onBubbleTap() {
        if (job?.isActive == true) return
        startPulse()
        showCard(
            T("Dinleniyor…", "Listening…"),
            T("Müziğe yaklaş, 10 saniye", "Hold near the music, 10 seconds"),
            null, null
        )
        job = scope.launch {
            val out = Finder.listen(this@BubbleService, 10, onRecorded = {
                showCard(T("Şarkı aranıyor…", "Searching…"), "", null, null)
            })
            stopPulse()
            when (out) {
                is Outcome.Found -> {
                    val s = out.song
                    Store.add(s)
                    val bmp = Finder.bitmap(s.artwork)
                    showCard(s.title, s.artist, bmp) { openSong(s.id) }
                    notifyFound(s)
                }
                Outcome.NotFound -> showCard(
                    T("Şarkı bulunamadı", "No match found"),
                    T("Müziğe biraz daha yaklaşıp tekrar dene", "Move closer to the music and try again"),
                    null, null
                )
                is Outcome.Error -> showCard(T("Hata", "Error"), out.msg, null, null)
            }
        }
    }

    // ---------- Sonuç penceresi ----------

    private fun rounded(r: Int) = object : ViewOutlineProvider() {
        override fun getOutline(v: View, o: Outline) {
            o.setRoundRect(0, 0, v.width, v.height, r.toFloat())
        }
    }

    private fun showCard(title: String, sub: String, cover: Bitmap?, onClick: (() -> Unit)?) {
        hideCard()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(px(12), px(12), px(12), px(12))
            background = GradientDrawable().apply {
                cornerRadius = px(18).toFloat()
                setColor(0xF2102A1B.toInt())
                setStroke(px(1), 0xFF22C55E.toInt())
            }
        }
        if (cover != null) {
            val iv = ImageView(this).apply {
                setImageBitmap(cover)
                scaleType = ImageView.ScaleType.CENTER_CROP
                clipToOutline = true
                outlineProvider = rounded(px(10))
            }
            root.addView(iv, LinearLayout.LayoutParams(px(56), px(56)).apply { rightMargin = px(12) })
        }
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(TextView(this).apply {
            text = title
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
        })
        if (sub.isNotBlank()) {
            col.addView(TextView(this).apply {
                text = sub
                setTextColor(0xFFB7D3C0.toInt())
                textSize = 13f
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
            })
        }
        root.addView(col, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(TextView(this).apply {
            text = "✕"
            setTextColor(0xFFB7D3C0.toInt())
            textSize = 18f
            setPadding(px(12), px(4), px(4), px(4))
            setOnClickListener { hideCard() }
        })
        if (onClick != null) root.setOnClickListener { hideCard(); onClick() }

        val w = min(resources.displayMetrics.widthPixels - px(24), px(340))
        val lp = WindowManager.LayoutParams(
            w, ViewGroup.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START }
        card = root
        cardLp = lp
        positionCard()
        wm.addView(root, lp)

        cardJob?.cancel()
        cardJob = scope.launch {
            delay(20000)
            if (job?.isActive != true) hideCard()
        }
    }

    private fun positionCard() {
        val b = hostLp ?: return
        val c = card ?: return
        val l = cardLp ?: return
        val sw = resources.displayMetrics.widthPixels
        val sh = resources.displayMetrics.heightPixels
        l.x = (sw - l.width) / 2
        val below = b.y + bub + px(4)
        l.y = if (below + px(100) < sh) below else max(px(24), b.y - px(100))
        if (c.isAttachedToWindow) wm.updateViewLayout(c, l)
    }

    private fun hideCard() {
        card?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        card = null
    }

    private fun openSong(id: String) {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra("song_id", id)
        )
    }

    private fun notifyFound(s: Song) {
        if (!Store.notif) return
        val nm = NotificationManagerCompat.from(this)
        if (!nm.areNotificationsEnabled()) return
        val pi = PendingIntent.getActivity(
            this, s.id.hashCode(),
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra("song_id", s.id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(this, CH_FOUND)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(s.title)
            .setContentText(s.artist)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        nm.notify(s.id.hashCode(), n)
    }

    override fun onDestroy() {
        stopPulse()
        hideCard()
        host?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        host = null
        scope.cancel()
        Store.bubbleOn = false
        super.onDestroy()
    }
}
