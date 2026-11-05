package `in`.sambhav.glassvolume.util

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import `in`.sambhav.glassvolume.GlassVolumeService

object ServiceUtils {

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            ?: return false

        val enabledServices: List<AccessibilityServiceInfo> =
            am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val expectedServiceName = "${context.packageName}/${GlassVolumeService::class.java.canonicalName}"

        for (service in enabledServices) {
            val id = service.id
            if (id != null && (id.equals(expectedServiceName, ignoreCase = true) || id.contains(context.packageName))) {
                return true
            }
        }

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

    fun isServiceFullyActive(context: Context): Boolean {
        return GlassVolumeService.isServiceRunning || isAccessibilityServiceEnabled(context)
    }
}
