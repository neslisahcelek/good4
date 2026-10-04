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
import com.good4.core.util.AppEnvironment
import com.good4.core.util.FirebaseBackend

class Good4MessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        PushSignals.refresh()
        AndroidCampusPush.initialize(this)
        AndroidCampusPush.refresh()
    }
    override fun onMessageReceived(message: RemoteMessage) {
        if (message.data["recipientUid"] != FirebaseAuth.getInstance().currentUser?.uid) return
        PushSignals.refresh()
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        message.data.forEach { (key, value) -> intent.putExtra(key, value) }
        createNotificationChannels(this)
        val manager = NotificationManagerCompat.from(this)
        if (!manager.areNotificationsEnabled()) return
        val notificationId = message.data["notificationId"]
        val builder = if (notificationId != null && AppEnvironment.firebaseProjectId == "good4tr-v2") {
            val pending = PendingIntent.getActivity(this, notificationId.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            NotificationCompat.Builder(this, if (message.data["kind"] == "announcement") "announcements" else "events")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(message.notification?.title).setContentText(message.notification?.body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message.notification?.body))
                .setContentIntent(pending).setAutoCancel(true)
        } else {
            val destination = campusPushDestination(
                message.data["recipientUid"], message.data["type"], message.data["conversationId"] ?: message.data["listingId"]
            ) ?: return
            val id = (destination.type + destination.targetId).hashCode()
            val pending = PendingIntent.getActivity(this, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            NotificationCompat.Builder(this, CAMPUS_NOTIFICATION_CHANNEL)
                .setSmallIcon(R.drawable.ic_campus_notification)
                .setContentTitle(message.notification?.title ?: "Kampüs Dolabı")
                .setContentText(message.notification?.body ?: "Yeni bir bildirimin var.")
                .setContentIntent(pending).setAutoCancel(true)
        }
        val id = notificationId?.hashCode() ?: (message.data["type"] + message.data["conversationId"] + message.data["listingId"]).hashCode()
        runCatching { manager.notify(id, 0, builder.build()) }
    }
}
