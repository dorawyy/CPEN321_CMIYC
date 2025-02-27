package com.example.cmiyc.repository

import com.example.cmiyc.api.ApiClient
import com.example.cmiyc.data.Friend
import com.example.cmiyc.data.FriendRequest
import com.example.cmiyc.repositories.UserRepository
import com.example.cmiyc.api.dto.*
import com.mapbox.geojson.Point

class FriendsRepository(
    private val userRepository: UserRepository
) {
    private val api = ApiClient.apiService

    private fun FriendDTO.toFriend(): Friend = Friend(
        userId = userID,
        name = displayName,
        email = email,
        photoURL = photoURL,
        location = currentLocation.let { Point.fromLngLat(it.longitude, it.latitude) },
        lastUpdated = currentLocation.timestamp,
    )

    private fun FriendDTO.toFriendRequest(): FriendRequest = FriendRequest(
        userId = userID,
        displayName = displayName,
        timestamp = currentLocation.timestamp,
    )

    suspend fun getFriendRequests(): List<FriendRequest> {
        val response = api.getFriendRequests(userRepository.getCurrentUserId())
        if (response.isSuccessful) {
            return response.body()?.map { it.toFriendRequest() } ?: emptyList()
        } else {
            throw Exception("Failed to fetch friend requests: ${response.code()}")
        }
    }

    suspend fun acceptFriendRequest(friendId: String) {
        val response = api.acceptFriendRequest(
            userId = userRepository.getCurrentUserId(),
            friendID = friendId
        )
        if (!response.isSuccessful) {
            throw Exception("Failed to accept friend request: ${response.code()}")
        }
    }

    suspend fun denyFriendRequest(friendId: String) {
        val response = api.declineFriendRequest(
            userId = userRepository.getCurrentUserId(),
            friendID = friendId
        )
        if (!response.isSuccessful) {
            throw Exception("Failed to decline friend request: ${response.code()}")
        }
    }

    suspend fun getFriends(): List<Friend> {
        val response = api.getFriends(userRepository.getCurrentUserId())
        if (response.isSuccessful) {
            return response.body()?.map { it.toFriend() } ?: emptyList()
        } else {
            throw Exception("Failed to fetch friends: ${response.code()}")
        }
    }

    suspend fun sendFriendRequest(friendEmail: String) {
        val response = api.sendFriendRequest(
            userId = userRepository.getCurrentUserId(),
            friendEmail = friendEmail
        )
        if (!response.isSuccessful) {
            throw Exception("Failed to send friend request: ${response.code()}")
        }
    }

    suspend fun removeFriend(friendId: String) {
        val response = api.removeFriend(
            userId = userRepository.getCurrentUserId(),
            friendID = friendId
        )
        if (!response.isSuccessful) {
            throw Exception("Failed to remove friend: ${response.code()}")
        }
    }
}