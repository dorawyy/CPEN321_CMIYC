package com.example.cmiyc.api.dto

/**
 * Data transfer object for Firebase Cloud Messaging token operations.
 *
 * This DTO is used to submit or update a user's FCM token for push notification delivery.
 * It contains a single field for the Firebase Cloud Messaging token.
 *
 * @property fcmToken The Firebase Cloud Messaging token used for sending push notifications to the user's device.
 */
data class FCMTokenRequestDTO (
    val fcmToken: String
)