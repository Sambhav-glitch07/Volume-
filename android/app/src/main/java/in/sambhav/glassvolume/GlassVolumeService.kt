package `in`.sambhav.glassvolume

import android.accessibilityservice.AccessibilityService
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.graphics.PixelFormat
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import `in`.sambhav.glassvolume.ui.GlassVolumeView

class GlassVolumeService : AccessibilityService() {

    companion object {
        private const val TAG: String = "GlassVolumeService"
        private const val DISMISS_DELAY_MS: Long = 2000L

        @Volatile
        var isServiceRunning: Boolean = false
            private set

        @Volatile
        private var instance: GlassVolumeService? = null

        fun triggerTestOverlay(volumePercent: Int = 52) {
            instance?.showVolumeHud(volumePercent, 100, isMuted = false)
        }
    }

    private lateinit var windowManager: WindowManager
    private lateinit var audioManager: AudioManager
    private var overlayView: View? = null
    private var glassVolumeView: GlassVolumeView? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private var isOverlayAttached: Boolean = false
    private var isVisible: Boolean = false

    private val dismissRunnable: Runnable = Runnable {
        fadeOutOverlay()
    }

    private val volumeObserver = object : ContentObserver(mainHandler) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            syncFromSystemVolume(animate = true)
        }
    }

    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                val streamType = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1)
                if (streamType == AudioManager.STREAM_MUSIC) {
                    syncFromSystemVolume(animate = true)
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "Sambhav Glass Volume Service connected successfully.")
        instance = this
        isServiceRunning = true

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        initOverlay()

        try {
            contentResolver.registerContentObserver(
                Settings.System.CONTENT_URI,
                true,
                volumeObserver
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register Settings.System volume observer", e)
        }

        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        registerReceiver(volumeReceiver, filter)
    }

    private fun initOverlay() {
        if (overlayView != null) return

        val inflater = LayoutInflater.from(this)
        val inflatedView = inflater.inflate(R.layout.layout_volume_overlay, null)
        overlayView = inflatedView
        glassVolumeView = inflatedView.findViewById<GlassVolumeView>(R.id.glassVolumeView)

        val density = resources.displayMetrics.density
        val hudWidth = (560f * density).toInt()
        val hudHeight = (140f * density).toInt()
        val marginTop = (44f * density).toInt()

        val params = WindowManager.LayoutParams(
            hudWidth,
            hudHeight,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = marginTop
            title = "SambhavGlassVolumeOverlay"
        }

        try {
            inflatedView.alpha = 0f
            inflatedView.visibility = View.GONE
            windowManager.addView(inflatedView, params)
            isOverlayAttached = true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding glass overlay view to WindowManager", e)
        }
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        val action = event.action

        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
            keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
            keyCode == KeyEvent.KEYCODE_VOLUME_MUTE
        ) {
            if (action == KeyEvent.ACTION_DOWN) {
                handleVolumeKey(keyCode)
            }
            return true
        }

        return super.onKeyEvent(event)
    }

    private fun handleVolumeKey(keyCode: Int) {
        val direction = when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> AudioManager.ADJUST_RAISE
            KeyEvent.KEYCODE_VOLUME_DOWN -> AudioManager.ADJUST_LOWER
            KeyEvent.KEYCODE_VOLUME_MUTE -> AudioManager.ADJUST_TOGGLE_MUTE
            else -> AudioManager.ADJUST_SAME
        }

        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, 0)

        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val min = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC)
        } else {
            0
        }

        val range = (max - min).coerceAtLeast(1)
        val percentage = (((current - min).toFloat() / range) * 100f).toInt().coerceIn(0, 100)

        val isMuted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioManager.isStreamMute(AudioManager.STREAM_MUSIC)
        } else {
            current == 0
        }

        showVolumeHud(percentage, 100, isMuted)
    }

    private fun syncFromSystemVolume(animate: Boolean) {
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val min = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC)
        } else {
            0
        }
        val range = (max - min).coerceAtLeast(1)
        val percentage = (((current - min).toFloat() / range) * 100f).toInt().coerceIn(0, 100)

        val isMuted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioManager.isStreamMute(AudioManager.STREAM_MUSIC)
        } else {
            current == 0
        }

        if (isVisible) {
            glassVolumeView?.setVolume(percentage, 100, isMuted, animate = animate)
            resetDismissTimer()
        }
    }

    private fun showVolumeHud(volumeValue: Int, maxVolume: Int = 100, isMuted: Boolean = false) {
        mainHandler.post {
            val v = overlayView ?: return@post
            glassVolumeView?.setVolume(volumeValue, maxVolume, isMuted, animate = true)

            resetDismissTimer()

            if (!isVisible) {
                isVisible = true
                v.visibility = View.VISIBLE
                v.animate()
                    .alpha(1f)
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(160L)
                    .setListener(null)
                    .start()
            }
        }
    }

    private fun resetDismissTimer() {
        mainHandler.removeCallbacks(dismissRunnable)
        mainHandler.postDelayed(dismissRunnable, DISMISS_DELAY_MS)
    }

    private fun fadeOutOverlay() {
        val v = overlayView ?: return
        if (!isVisible) return

        v.animate()
            .alpha(0f)
            .scaleX(0.96f)
            .scaleY(0.96f)
            .setDuration(220L)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    v.visibility = View.GONE
                    isVisible = false
                }
            })
            .start()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {
        Log.w(TAG, "Sambhav Glass Volume Service interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        instance = null

        try {
            contentResolver.unregisterContentObserver(volumeObserver)
            unregisterReceiver(volumeReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering observers", e)
        }

        if (isOverlayAttached && overlayView != null) {
            try {
                windowManager.removeView(overlayView)
            } catch (e: Exception) {
                Log.w(TAG, "Error removing overlay view", e)
            }
            overlayView = null
            isOverlayAttached = false
        }
    }
}
