package com.springboard.launcher.systemui

import android.app.Notification
import android.app.PendingIntent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class SpringboardNotification(
    val key: String,
    val packageName: String,
    val title: String,
    val text: String?,
    val bigText: String?,
    val whenPosted: Long,
    val appLabel: String?,
    val canClear: Boolean,
    val contentIntent: PendingIntent?,
)

/**
 * NotificationListenerService backing Springboard's Notification Center. The exposed flow
 * is null until notification access is granted; empty once connected with nothing posted.
 */
class SpringboardNotificationListener : NotificationListenerService() {

    override fun onCreate() {
        super.onCreate()
        companion.instance = this
    }

    override fun onDestroy() {
        companion.instance = null
        super.onDestroy()
    }

    override fun onListenerConnected() {
        val sorted = activeNotifications
            .map { it.toModel() }
            .sortedByDescending { it.whenPosted }
        companion.publish(sorted)
    }

    override fun onListenerDisconnected() {
        companion.publish(null)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val model = sbn?.toModel() ?: return
        val current = companion.notifications.value.orEmpty()
        companion.publish(
            (current.filter { it.key != model.key } + model).sortedByDescending { it.whenPosted },
        )
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn ?: return
        val current = companion.notifications.value.orEmpty()
        companion.publish(current.filter { it.key != sbn.key })
    }

    private fun StatusBarNotification.toModel(): SpringboardNotification {
        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        return SpringboardNotification(
            key = key,
            packageName = packageName,
            title = title ?: packageName,
            text = text,
            bigText = bigText,
            whenPosted = postTime,
            appLabel = runCatching {
                @Suppress("DEPRECATION")
                packageManager.getApplicationLabel(
                    packageManager.getApplicationInfo(packageName, 0),
                ).toString()
            }.getOrNull(),
            canClear = notification.flags and Notification.FLAG_ONGOING_EVENT == 0,
            contentIntent = notification.contentIntent,
        )
    }

    companion object {
        private val _notifications = MutableStateFlow<List<SpringboardNotification>?>(null)
        val notifications: StateFlow<List<SpringboardNotification>?> = _notifications

        @Volatile var instance: SpringboardNotificationListener? = null

        fun publish(list: List<SpringboardNotification>?) {
            _notifications.value = list
        }

        /** Clears one notification through the connected listener. */
        fun dismiss(key: String) {
            if (key.isBlank()) return
            runCatching { instance?.cancelNotification(key) }
        }

        fun dismissAll() {
            val current = _notifications.value.orEmpty()
            current.forEach { dismiss(it.key) }
        }

        /** Whether this listener is connected (i.e. notification access was granted). */
        val isConnected: Boolean get() = _notifications.value != null
    }
}