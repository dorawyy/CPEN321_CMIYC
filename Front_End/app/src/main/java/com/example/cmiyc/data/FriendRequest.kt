package com.example.cmiyc.data

data class FriendRequest(
    val requestId: String,
    val userId: String,
    val displayName: String,
    val timestamp: Long
)