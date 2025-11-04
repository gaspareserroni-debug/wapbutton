package it.tutorup.whatsdial

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.os.Build
import android.view.*
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.Toast
import android.content.Intent
import android.net.Uri
import android.view.Gravity

class DialerOverlayService : AccessibilityService() {

    private lateinit var wm: WindowManager
    private var overlay: View? = null
    private var currentNumber: String? = null

    private val allowedPackages = setOf(
        "com.google.android.dialer",
        "com.samsung.android.dialer",
        "com.android.dialer"
    )

    private val lp by lazy {
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            x = 32
            y = 0
        }
    }

    override fun onServiceConnected() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        showOverlay()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (!allowedPackages.contains(pkg)) {
            hideOverlay()
            return
        }
        showOverlay()

        // prova a leggere un numero dalla UI
        rootInActiveWindow?.let { root ->
            findNumber(root)?.let { currentNumber = it }
        }
    }

    override fun onInterrupt() {}

    private fun showOverlay() {
        if (overlay != null) return
        overlay = LayoutInflater.from(this).inflate(R.layout.overlay_button, null)
        overlay!!.findViewById<Button>(R.id.waButton).setOnClickListener {
            val n = currentNumber
            if (n.isNullOrBlank()) {
                Toast.makeText(this, "Numero non rilevato", Toast.LENGTH_SHORT).show()
            } else {
                openWhatsApp(n)
            }
        }
        wm.addView(overlay, lp)
    }

    private fun hideOverlay() {
        overlay?.let { wm.removeView(it) }
        overlay = null
    }

    private fun findNumber(node: AccessibilityNodeInfo): String? {
        val t = node.text?.toString()?.trim().orEmpty()
        // match numeri tipo +39 391 148 7720 o 081 1809 8878
        val m = Regex("""(\+?\d[\d\s\-\(\)]{5,})""").find(t)
        if (m != null) return normalize(m.value)

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                findNumber(child)?.let { return it }
            }
        }
        return null
    }

    // Default Italia: se manca il +, aggiungo +39
    private fun normalize(raw: String): String {
        var n = raw.replace("""[\s\-\(\)]""".toRegex(), "")
        if (!n.startsWith("+")) n = "+39$n"
        return n
    }

    private fun openWhatsApp(number: String) {
        val noPlus = number.replace("+", "")
        val waUri = Uri.parse("https://wa.me/$noPlus")
        try {
            Intent(Intent.ACTION_VIEW, waUri).apply {
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(this)
            }
        } catch (_: Exception) {
            // fallback al browser se WhatsApp non c'è
            startActivity(Intent(Intent.ACTION_VIEW, waUri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
