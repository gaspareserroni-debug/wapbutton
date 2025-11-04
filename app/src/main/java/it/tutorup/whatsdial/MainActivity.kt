package it.tutorup.whatsdial

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1) Permesso overlay
        if (!Settings.canDrawOverlays(this)) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        }

        // 2) Abilita il servizio di Accessibilità
        if (!isServiceEnabled(this, DialerOverlayService::class.java)) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        finish()
    }

    private fun isServiceEnabled(context: Context, service: Class<*>): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val cn = ComponentName(context, service)
        return am.enabledAccessibilityServiceList.any {
            it.resolveInfo.serviceInfo.packageName == cn.packageName &&
            it.resolveInfo.serviceInfo.name == cn.className
        }
    }
}
