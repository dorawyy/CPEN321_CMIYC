package com.example.cmiyc.data

data class AdminUserItem (
    val userId: String,
    val name: String,
    val email: String,
    val photoURL: String?,
    val isBanned: Boolean
)