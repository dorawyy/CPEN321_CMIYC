package com.example.cmiyc.repository

import com.example.cmiyc.api.ApiClient
import com.example.cmiyc.data.Friend
import com.example.cmiyc.data.FriendRequest
import com.example.cmiyc.repositories.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FriendsRepository(
    private val userRepository: UserRepository
) {
    private val api = ApiClient.apiService

    suspend fun getFriendRequests(): List<FriendRequest> {
        return api.getFriendRequests()
    }

    suspend fun acceptFriendRequest(requestId: String) {
        api.respondToFriendRequest(requestId, "accept")
    }

    suspend fun denyFriendRequest(requestId: String) {
        api.respondToFriendRequest(requestId, "deny")
    }

    suspend fun getFriends(): List<Friend> {
        val userId = userRepository.getCurrentUserId()
        val response = api.getFriends(userId)
        if (response.isSuccessful) {
            return response.body() ?: emptyList()
        } else {
            throw Exception("Failed to fetch friends: ${response.code()}")
        }
    }

    suspend fun searchUsers(query: String): List<Friend> {
        val userId = userRepository.getCurrentUserId()
        val response = api.searchUsers(query, userId)
        if (response.isSuccessful) {
            return response.body() ?: emptyList()
        } else {
            throw Exception("Search failed: ${response.code()}")
        }
    }

    suspend fun sendFriendRequest(targetUserId: String) {
        val userId = userRepository.getCurrentUserId()
        val response = api.sendFriendRequest(userId, targetUserId)
        if (!response.isSuccessful) {
            throw Exception("Failed to send friend request: ${response.code()}")
        }
    }

    suspend fun removeFriend(targetUserId: String) {
        val userId = userRepository.getCurrentUserId()
        val response = api.removeFriend(userId, targetUserId)
        if (!response.isSuccessful) {
            throw Exception("Failed to remove friend: ${response.code()}")
        }
    }
}