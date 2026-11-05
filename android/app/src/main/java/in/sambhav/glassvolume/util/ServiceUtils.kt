package in.sambhav.glassvolume.util

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import in.sambhav.glassvolume.GlassVolumeService

object ServiceUtils {

    /**
     * Checks if the Sambhav Glass Volume accessibility service is officially enabled
     * in Android System Settings.
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            ?: return false

        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val expectedServiceName = "${context.packageName}/${GlassVolumeService::class.java.canonicalName}"

        for (service in enabledServices) {
            val id = service.id
            if (id.equals(expectedServiceName, ignoreCase = true) || id.contains(context.packageName)) {
                return true
            }
        }

        // Secondary check via Settings.Secure
        try {
            val enabledServicesSetting = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            return enabledServicesSetting.contains(context.packageName)
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * Determines whether the service is both enabled in settings AND actively running in memory.
     */
    fun isServiceFullyActive(context: Context): Boolean {
        return GlassVolumeService.isServiceRunning || isAccessibilityServiceEnabled(context)
    }
}
