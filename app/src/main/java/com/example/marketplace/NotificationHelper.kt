package com.example.marketplace

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Manages low stock notifications for sellers
 */
class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "low_stock_channel"
        const val CHANNEL_NAME = "Low Stock Alerts"
        const val CHANNEL_DESCRIPTION = "Notifications for low product stock"
        const val NOTIFICATION_ID_BASE = 1000
        const val PREFS_NAME = "notification_prefs"
        const val KEY_LAST_NOTIFICATION_TIME = "last_notification_time_"
        const val NOTIFICATION_INTERVAL = 60
    }

    private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
            }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Prevents notification spam by checking if we've notified about this product recently
     */
    private fun shouldShowNotification(productId: Long): Boolean {
        val key = KEY_LAST_NOTIFICATION_TIME + productId
        val lastNotificationTime = sharedPreferences.getLong(key, 0)
        val currentTime = System.currentTimeMillis()

        return (currentTime - lastNotificationTime) > NOTIFICATION_INTERVAL
    }

    private fun markNotificationShown(productId: Long) {
        sharedPreferences.edit()
            .putLong(KEY_LAST_NOTIFICATION_TIME + productId, System.currentTimeMillis())
            .apply()
    }

    /**
     * Shows a notification when product stock is low (≤10 items)
     */
    fun showLowStockNotification(productName: String, quantity: Int, productId: Long) {
        if (!shouldShowNotification(productId)) {
            return
        }

        val intent = Intent(context, SellerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE
            } else {
                0
            }
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Low Stock Alert!")
            .setContentText("$productName has only $quantity pieces left")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$productName has only $quantity pieces left. Please restock soon."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(NOTIFICATION_ID_BASE + productId.toInt(), builder.build())
            }
            markNotificationShown(productId)
        } catch (e: Exception) {
            // Silently handle notification failures
        }
    }

    /**
     * Scans seller's products and notifies about low stock items
     * Only shows notifications once per login session to avoid spam
     */
    fun checkAndNotifyLowStock(products: List<Product>) {
        val sessionKey = "session_notifications_shown"
        val sessionNotificationsShown = sharedPreferences.getBoolean(sessionKey, false)

        if (sessionNotificationsShown) {
            return
        }

        var notificationShown = false

        for (product in products) {
            if (product.quantity <= 10 && product.quantity >= 0) {
                if (shouldShowNotification(product.id)) {
                    showLowStockNotification(product.name, product.quantity, product.id)
                    notificationShown = true
                }
            }
        }

        if (notificationShown) {
            sharedPreferences.edit()
                .putBoolean(sessionKey, true)
                .apply()
        }
    }

    /**
     * Resets notification flags when user logs out
     */
    fun clearSessionFlag() {
        sharedPreferences.edit()
            .putBoolean("session_notifications_shown", false)
            .apply()
    }
}