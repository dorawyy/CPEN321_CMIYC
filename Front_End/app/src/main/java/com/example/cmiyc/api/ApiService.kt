package com.example.cmiyc.api

import com.example.cmiyc.data.Friend
import com.example.cmiyc.data.FriendRequest
import com.example.cmiyc.data.User
import com.mapbox.geojson.Point
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import kotlin.random.Random

data class LocationUpdateRequest(
    val longitude: Double,
    val latitude: Double,
    val timestamp: Long = System.currentTimeMillis()
)

interface ApiService {

    @GET("users/{userId}/friends")
    suspend fun getFriends(
        @Path("userId") userId: String
    ): Response<List<Friend>>

    @GET("users/search")
    suspend fun searchUsers(
        @Query("query") query: String,
        @Query("currentUserId") currentUserId: String
    ): Response<List<Friend>>

    @POST("users/{userId}/friends/{targetUserId}")
    suspend fun sendFriendRequest(
        @Path("userId") userId: String,
        @Path("targetUserId") targetUserId: String
    ): Response<Unit>

    @DELETE("users/{userId}/friends/{targetUserId}")
    suspend fun removeFriend(
        @Path("userId") userId: String,
        @Path("targetUserId") targetUserId: String
    ): Response<Unit>

    @POST("users/{userId}/location")
    suspend fun updateUserLocation(
        @Path("userId") userId: String,
        @Body location: LocationUpdateRequest
    ): Response<Unit>

    @GET("friend-requests")
    suspend fun getFriendRequests(): List<FriendRequest>

    @POST("friend-requests/{requestId}/respond")
    suspend fun respondToFriendRequest(
        @Path("requestId") requestId: String,
        @Query("action") action: String
    )
}

interface _MockApiService {
    suspend fun getFriends(userId: String): Response<List<Friend>>
    suspend fun searchUsers(query: String, currentUserId: String): Response<List<Friend>>
    suspend fun sendFriendRequest(userId: String, targetUserId: String): Response<Unit>
    suspend fun removeFriend(userId: String, targetUserId: String): Response<Unit>
    suspend fun updateUserLocation(userId: String, location: LocationUpdateRequest): Response<Unit>
    abstract fun getFriendRequests(): List<FriendRequest>
    abstract fun respondToFriendRequest(requestId: String, s: String): Response<Unit>
}

private fun randomNearbyPoint(base: Point, offset: Double = 0.001): Point {
    val randomLon = base.longitude() + (Random.nextDouble() * 2 - 1) * offset
    val randomLat = base.latitude() + (Random.nextDouble() * 2 - 1) * offset
    return Point.fromLngLat(randomLon, randomLat)
}

class MockApiService : _MockApiService {
    override suspend fun getFriends(userId: String): Response<List<Friend>> {
        val mockFriends = listOf(
            Friend(
                userId = "friend1",
                name = "Alice Smith",
                email = "alice@example.com",
                status = "Online",
                location = randomNearbyPoint(Point.fromLngLat(-123.25041000865335, 49.26524685838906))
            ),
            Friend(
                userId = "friend2",
                name = "Bob Johnson",
                email = "bob@example.com",
                status = "Busy",
                location = randomNearbyPoint(Point.fromLngLat(-123.251, 49.265))
            ),
            Friend(
                userId = "friend3",
                name = "Carol White",
                email = "carol@example.com",
                status = "Away",
                location = randomNearbyPoint(Point.fromLngLat(-123.250, 49.2655))
            )
        )
        return Response.success(mockFriends)
    }

    override suspend fun searchUsers(query: String, currentUserId: String): Response<List<Friend>> {
        val mockUsers = listOf(
            Friend(
                userId = "user1",
                email = "david@example.com",
                name = "David Brown",
                status = "online",
                location = null,
            ),
            Friend(
                userId = "user2",
                email = "emma@example.com",
                name = "Emma Davis",
                status = "online",
                location = null,
            )
        ).filter {
            it.name.contains(query, ignoreCase = true) ||
                    it.email.contains(query, ignoreCase = true)
        }
        return Response.success(mockUsers)
    }

    override suspend fun sendFriendRequest(userId: String, targetUserId: String): Response<Unit> {
        // Simulate success
        println("Sending friend request from $userId to $targetUserId")
        return Response.success(Unit)
    }

    override suspend fun removeFriend(userId: String, targetUserId: String): Response<Unit> {
        // Simulate success
        return Response.success(Unit)
    }

    override suspend fun updateUserLocation(userId: String, location: LocationUpdateRequest): Response<Unit> {
        // Simulate success
        println("Updating location on Server for user $userId: $location")
        return Response.success(Unit)
    }

    override fun getFriendRequests(): List<FriendRequest> {
        return listOf(
            FriendRequest(
                requestId = "request1",
                userId = "user1",
                displayName = "Alice Smith 2",
                timestamp = System.currentTimeMillis()
            ),
            FriendRequest(
                requestId = "request2",
                userId = "user2",
                displayName = "Bob Johnson 2",
                timestamp = System.currentTimeMillis()
            )
        )
    }

    override fun respondToFriendRequest(requestId: String, s: String): Response<Unit> {
        return Response.success(Unit);
    }
}