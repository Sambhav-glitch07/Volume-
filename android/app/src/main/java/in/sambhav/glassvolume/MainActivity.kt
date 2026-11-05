package in.sambhav.glassvolume

import android.content.Intent
import android.graphics.Color
import android.media.AudioManager
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import in.sambhav.glassvolume.ui.GlassVolumeView
import in.sambhav.glassvolume.util.ServiceUtils

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatusBadge: TextView
    private lateinit var tvStatusDescription: TextView
    private lateinit var btnEnableService: Button
    private lateinit var btnTestOverlay: Button
    private lateinit var btnTclDiagnostics: Button
    private lateinit var livePreviewSlider: GlassVolumeView
    private lateinit var tvLivePreviewHint: TextView

    private var previewVolume = 52

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        updateServiceStatus()
    }

    private fun initViews() {
        tvStatusBadge = findViewById(R.id.tvStatusBadge)
        tvStatusDescription = findViewById(R.id.tvStatusDescription)
        btnEnableService = findViewById(R.id.btnEnableService)
        btnTestOverlay = findViewById(R.id.btnTestOverlay)
        btnTclDiagnostics = findViewById(R.id.btnTclDiagnostics)
        livePreviewSlider = findViewById(R.id.livePreviewSlider)
        tvLivePreviewHint = findViewById(R.id.tvLivePreviewHint)

        // Initial preview matches the visual reference exactly
        livePreviewSlider.setVolume(previewVolume, 100, muted = false, animate = false)

        // Make buttons default focused for TV remote D-Pad
        btnEnableService.requestFocus()
    }

    private fun setupListeners() {
        btnEnableService.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                Toast.makeText(
                    this,
                    "Navigate to Downloaded Services > Sambhav Glass Volume to activate",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Could not open Accessibility Settings directly", Toast.LENGTH_SHORT).show()
            }
        }

        btnTestOverlay.setOnClickListener {
            if (GlassVolumeService.isServiceRunning) {
                GlassVolumeService.triggerTestOverlay(previewVolume)
                Toast.makeText(this, "Showing TV Overlay for 2 seconds", Toast.LENGTH_SHORT).show()
            } else {
                // If service is not enabled yet, pulse the in-app preview and remind user
                livePreviewSlider.setVolume((previewVolume + 10).coerceAtMost(100), 100, muted = false, animate = true)
                Toast.makeText(
                    this,
                    "Service not enabled yet. Enable accessibility service to display floating overlay on all TV apps.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        btnTclDiagnostics.setOnClickListener {
            showTclDiagnosticsDialog()
        }
    }

    private fun updateServiceStatus() {
        val isEnabledInSettings = ServiceUtils.isAccessibilityServiceEnabled(this)
        val isRunning = GlassVolumeService.isServiceRunning

        if (isRunning || isEnabledInSettings) {
            tvStatusBadge.text = getString(R.string.status_active)
            tvStatusBadge.setTextColor(Color.parseColor("#10B981"))
            tvStatusBadge.setBackgroundColor(Color.parseColor("#142E25"))

            tvStatusDescription.text = "Service is active in background. Remote volume key presses will show the glass HUD and suppress stock TV popups."
            btnEnableService.text = "SETTINGS CONFIGURED (CLICK TO MANAGE)"
            btnTestOverlay.visibility = View.VISIBLE
        } else {
            tvStatusBadge.text = getString(R.string.status_disabled)
            tvStatusBadge.setTextColor(Color.parseColor("#F59E0B"))
            tvStatusBadge.setBackgroundColor(Color.parseColor("#2E2314"))

            tvStatusDescription.text = "Accessibility Service is currently OFF. Click below to enable Sambhav Glass Volume so it can intercept remote volume keys."
            btnEnableService.text = getString(R.string.btn_enable_service)
        }
    }

    private fun showTclDiagnosticsDialog() {
        AlertDialog.Builder(this, R.style.Theme_AppCompat_Dialog_Alert)
            .setTitle(R.string.tcl_diag_title)
            .setMessage(
                "TCL Android TV Specific Details:\n\n" +
                "1. KEY FILTERING: Sambhav Glass Volume uses Android's official AccessibilityService " +
                "with FLAG_REQUEST_FILTER_KEY_EVENTS. When you press Vol+ or Vol-, the app intercepts the key, " +
                "updates media volume directly via AudioManager without FLAG_SHOW_UI, and renders the translucent glass overlay.\n\n" +
                "2. BLUETOOTH / CEC REMOTES: On certain TCL TV software updates, Bluetooth remotes may bypass standard " +
                "key dispatch directly to the audio amplifier. Our built-in Volume ContentObserver detects external adjustments " +
                "and displays the glass HUD automatically.\n\n" +
                "3. REMOVAL: To restore default TV behavior at any time, toggle off Sambhav Glass Volume in " +
                "Settings > Device Preferences > Accessibility."
            )
            .setPositiveButton("OK") { d, _ -> d.dismiss() }
            .show()
    }

    /**
     * Allows user to test the volume slider inside the app using TV Remote D-Pad Left/Right
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                previewVolume = (previewVolume - 5).coerceAtLeast(0)
                livePreviewSlider.setVolume(previewVolume, 100, muted = false, animate = true)
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                previewVolume = (previewVolume + 5).coerceAtMost(100)
                livePreviewSlider.setVolume(previewVolume, 100, muted = false, animate = true)
                return true
            }
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (GlassVolumeService.isServiceRunning) {
                    // Let the service handle it
                    return super.onKeyDown(keyCode, event)
                } else {
                    previewVolume = (previewVolume + 5).coerceAtMost(100)
                    livePreviewSlider.setVolume(previewVolume, 100, muted = false, animate = true)
                    return true
                }
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (GlassVolumeService.isServiceRunning) {
                    return super.onKeyDown(keyCode, event)
                } else {
                    previewVolume = (previewVolume - 5).coerceAtLeast(0)
                    livePreviewSlider.setVolume(previewVolume, 100, muted = false, animate = true)
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}
