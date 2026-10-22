package ru.example.voiceagent

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

class PhoneControlService : AccessibilityService() {
    companion object {
        const val ACTION_COMMAND = "ru.example.voiceagent.ACTION_COMMAND"
    }

    private val commandReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val command = intent?.getStringExtra("command") ?: return
            executeCommand(command.lowercase().trim())
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val filter = IntentFilter(ACTION_COMMAND)
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(commandReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        else @Suppress("DEPRECATION") registerReceiver(commandReceiver, filter)
    }

    private fun executeCommand(command: String) {
        when {
            command in listOf("назад", "вернись назад", "вернуться назад") -> performGlobalAction(GLOBAL_ACTION_BACK)
            command in listOf("домой", "на главный экран", "главный экран") -> performGlobalAction(GLOBAL_ACTION_HOME)
            command in listOf("открой настройки", "открыть настройки", "настройки") -> launchPackage("com.android.settings")
            command.contains("открой youtube") || command.contains("открой ютуб") -> launchPackage("com.google.android.youtube")
            command.contains("открой карты") || command.contains("открой google maps") -> launchPackage("com.google.android.apps.maps")
            command.contains("открой камеру") -> launchCamera()
            command.startsWith("нажми ") -> clickText(command.removePrefix("нажми ").trim())
            command.startsWith("найди ") || command.startsWith("нажми текст ") ->
                clickText(command.removePrefix("найди ").removePrefix("нажми текст ").trim())
            command.contains("прокрути вниз") || command == "вниз" -> scroll(true)
            command.contains("прокрути вверх") || command == "вверх" -> scroll(false)
            else -> Toast.makeText(this, "Пока не поддерживается: $command", Toast.LENGTH_LONG).show()
        }
    }

    private fun launchPackage(packageName: String) {
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launch)
        } else {
            Toast.makeText(this, "Приложение не найдено на телефоне", Toast.LENGTH_LONG).show()
        }
    }

    private fun launchCamera() {
        val intent = Intent("android.media.action.IMAGE_CAPTURE").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try { startActivity(intent) } catch (_: Exception) {
            Toast.makeText(this, "Камера недоступна", Toast.LENGTH_LONG).show()
        }
    }

    private fun clickText(target: String) {
        if (target.isBlank()) return
        val root = rootInActiveWindow
        if (root == null) {
            Toast.makeText(this, "Не вижу активный экран", Toast.LENGTH_SHORT).show()
            return
        }
        val nodes = root.findAccessibilityNodeInfosByText(target)
        val clickable = nodes.firstOrNull { it.isClickable } ?: nodes.firstOrNull()
        if (clickable != null) {
            var node: AccessibilityNodeInfo? = clickable
            while (node != null && !node.isClickable) node = node.parent
            if (node?.isClickable == true) node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            else Toast.makeText(this, "Элемент найден, но его нельзя нажать", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Не найден текст: $target", Toast.LENGTH_SHORT).show()
        }
    }

    private fun scroll(down: Boolean) {
        val root = rootInActiveWindow ?: return
        val action = if (down) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        if (!root.performAction(action)) {
            val candidates = root.findAccessibilityNodeInfosByText("")
            candidates.firstOrNull { it.isScrollable }?.performAction(action)
        }
    }

    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {}
    override fun onInterrupt() {}
    override fun onDestroy() {
        try { unregisterReceiver(commandReceiver) } catch (_: Exception) {}
        super.onDestroy()
    }
}
