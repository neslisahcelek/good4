package com.good4.notification

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.good4.MainActivity
import com.good4.R

class CampusMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        // Query permission again instead of registering a device which opted out.
        AndroidCampusPush.initialize(this)
        AndroidCampusPush.refresh()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        if (data["recipientUid"] != FirebaseAuth.getInstance().currentUser?.uid) return
        val target = campusPushDestination(data["recipientUid"], data["type"], data["conversationId"] ?: data["listingId"] ?: data["activityId"]) ?: return
        val manager = NotificationManagerCompat.from(this)
        if (!manager.areNotificationsEnabled()) return
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            data.forEach { (key, value) -> putExtra(key, value) }
        }
        val id = (target.type + target.targetId).hashCode()
        val pendingIntent = PendingIntent.getActivity(this, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, CAMPUS_NOTIFICATION_CHANNEL)
            .setSmallIcon(R.drawable.ic_campus_notification)
            .setContentTitle(message.notification?.title ?: "Kampüs Dolabı")
            .setContentText(message.notification?.body ?: "Yeni bir bildirimin var.")
            .setContentIntent(pendingIntent).setAutoCancel(true).build()
        try { manager.notify(id, notification) } catch (_: SecurityException) { }
    }
}
