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
    override fun onNewToken(token: String) { PushSignals.refresh() }
    override fun onMessageReceived(message: RemoteMessage) {
        if (AppEnvironment.firebaseProjectId != "good4tr-v2") return
        if (message.data["recipientUid"] != FirebaseAuth.getInstance().currentUser?.uid) return
        PushSignals.refresh()
        val id = message.data["notificationId"] ?: return
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        message.data.forEach { (key, value) -> intent.putExtra(key, value) }
        val pending = PendingIntent.getActivity(this, id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        createNotificationChannels(this)
        val builder = NotificationCompat.Builder(this, if (message.data["kind"] == "announcement") "announcements" else "events")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(message.notification?.title).setContentText(message.notification?.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message.notification?.body))
            .setContentIntent(pending).setAutoCancel(true)
        if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            runCatching { NotificationManagerCompat.from(this).notify(id, 0, builder.build()) }
        }
    }
}
