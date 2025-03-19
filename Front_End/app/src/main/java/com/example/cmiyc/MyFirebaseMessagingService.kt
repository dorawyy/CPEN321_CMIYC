package com.example.cmiyc

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.messaging.messaging

/**
 * Firebase Cloud Messaging (FCM) Service for handling incoming push notifications.
 *
 * This service extends [FirebaseMessagingService] to provide custom handling of
 * remote messages received from Firebase Cloud Messaging.
 *
 * Key Responsibilities:
 * - Intercept and log incoming Firebase Cloud Messages
 * - Create and display local notifications for received messages
 * - Customize notification appearance and behavior
 *
 * @property TAG Constant used for logging Firebase messaging events
 *
 * @see FirebaseMessagingService
 * @see RemoteMessage
 */
class MyFirebaseMessagingService : FirebaseMessagingService() {
    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }

    /**
     * Called when a remote message is received.
     *
     * This method is triggered when a Firebase Cloud Message is received while the app is running.
     * It logs the notification body and triggers the local notification display.
     *
     * @param remoteMessage The received remote message containing notification payload
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            sendNotification(it.title ?: "New Message", it.body ?: "New message")
        }
    }

    /**
     * Generates and displays a local notification based on the received message.
     *
     * Features of the notification:
     * - Sets a small icon using the app's launcher icon
     * - Uses the message title and body for notification content
     * - Plays default notification sound
     * - Automatically cancels the notification when tapped
     * - Creates a pending intent to navigate to MainActivity when notification is tapped
     *
     * @param messageTitle Title of the notification
     * @param messageBody Body text of the notification
     */
    private fun sendNotification(messageTitle: String, messageBody: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.putExtra("NAVIGATE_TO_LOG", true) // Add this flag
        val requestCode = 0
        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE,
        )

        val channelId = "fcm_default_channel"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(messageTitle)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Default Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Default notification channel"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationId = 0
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

}