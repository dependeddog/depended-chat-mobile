package com.depended.chat.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.depended.chat.MainActivity
import com.depended.chat.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatPushNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun ensureChannel() {
        val manager = context.getSystemService(NotificationManager::class.java)
        val existing = manager.getNotificationChannel(CHANNEL_CHAT_MESSAGES)
        if (existing != null) return

        val channel = NotificationChannel(
            CHANNEL_CHAT_MESSAGES,
            "Chat messages",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Incoming chat messages"
        }
        manager.createNotificationChannel(channel)
    }

    fun showMessageNotification(chatId: String, type: String, messageId: String?, senderId: String?) {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_CHAT_FROM_PUSH
            putExtra(EXTRA_CHAT_ID, chatId)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            chatId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_CHAT_MESSAGES)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("New message")
            .setContentText("type=$type sender=$senderId message=$messageId")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        NotificationManagerCompat.from(context).notify(chatId.hashCode(), notification)
    }

    companion object {
        const val CHANNEL_CHAT_MESSAGES = "chat_messages"
        const val ACTION_OPEN_CHAT_FROM_PUSH = "com.depended.chat.action.OPEN_CHAT_FROM_PUSH"
        const val EXTRA_CHAT_ID = "extra_chat_id"
    }
}
