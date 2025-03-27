package com.example.cmiyc.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import com.example.cmiyc.api.dto.*

/**
 * Main API service interface that combines user and friend management functionality.
 *
 * This interface extends both UserApiService and FriendApiService to provide
 * a unified API for all network operations. It also defines additional endpoints
 * that don't fit into either of the sub-interfaces.
 */
interface ApiService: UserApiService, FriendApiService{
    /**
     * Updates the Firebase Cloud Messaging token for a specific user.
     *
     * This endpoint is used to ensure push notifications can be delivered
     * to the user's current device.
     *
     * @param userId The unique identifier of the user to update.
     * @param fcmToken The request DTO containing the new FCM token.
     * @return A Response object indicating success or failure.
     */
    @PUT("/fcm/{userID}")
    suspend fun setFCMToken(
        @Path("userID") userId: String,
        @Body fcmToken: FCMTokenRequestDTO,
    ): Response<Unit>
}

/**
 * API service interface for user management operations.
 *
 * This interface defines endpoints related to user registration, profile updates,
 * location tracking, administrative functions, and notification management.
 */
interface UserApiService {
    /**
     * Registers a new user in the system.
     *
     * This endpoint creates a new user account with the provided details.
     *
     * @param user The request DTO containing all user registration information.
     * @return A Response containing the user's initial status.
     */
    @POST("user")
    suspend fun registerUser(
        @Body user: UserRegistrationRequestDTO
    ): Response<UserRegistrationResponseDTO>

    /**
     * Updates a user's geographical location.
     *
     * This endpoint is used to track and update the user's current position.
     *
     * @param userId The unique identifier of the user to update.
     * @param location The request DTO containing the new location information.
     * @return A Response object indicating success or failure.
     */
    @PUT("location/{userID}")
    suspend fun updateUserLocation(
        @Path("userID") userId: String,
        @Body location: LocationUpdateRequestDTO
    ): Response<Unit>

    /**
     * Bans a user from the system.
     *
     * This administrative endpoint disables a user's account.
     *
     * @param userId The unique identifier of the user to ban.
     * @param banRequestAdminID The request DTO containing the administrator's ID who is performing the ban.
     * @return A Response object indicating success or failure.
     */
    @POST("user/ban/{userID}")
    suspend fun banUser(
        @Path("userID") userId: String,
        @Body banRequestAdminID: BanUserRequestDTO
    ): Response<Unit>

    /**
     * Retrieves a list of all users in the system.
     *
     * This endpoint provides access to all user records, typically for administrative purposes.
     *
     * @param userId The unique identifier of the user making the request (for authorization).
     * @return A Response containing a list of all user DTOs.
     */
    @GET("user/{userID}")
    suspend fun getAllUsers(
        @Path("userID") userId: String
    ): Response<List<UserDTO>>

    /**
     * Retrieves the activity logs for a specific user.
     *
     * This endpoint provides access to notification and activity history.
     *
     * @param userId The unique identifier of the user whose logs are being requested.
     * @return A Response containing a list of log entries.
     */
    @GET("notifications/{userID}")
    suspend fun getLogs(
        @Path("userID") userId: String
    ): Response<List<LogDTO>>

    /**
     * Broadcasts a message or event to users.
     *
     * This endpoint triggers notifications or events for multiple users.
     *
     * @param userId The unique identifier of the user initiating the broadcast.
     * @param eventName The request DTO containing the event to broadcast.
     * @return A Response object indicating success or failure.
     */
    @POST("send-event/{userID}")
    suspend fun broadcastMessage(
        @Path("userID") userId: String,
        @Body eventName: BroadcastMessageRequestDTO,
    ): Response<Unit>
}

/**
 * API service interface for friend relationship management.
 *
 * This interface defines endpoints related to managing friend connections,
 * including sending and responding to friend requests, viewing friends,
 * and removing existing connections.
 */
interface FriendApiService {
    /**
     * Retrieves a list of the user's friends.
     *
     * This endpoint provides access to all established friend connections.
     *
     * @param userID The unique identifier of the user whose friends are being requested.
     * @return A Response containing a list of friend DTOs.
     */
    @GET("friends/{userID}")
    suspend fun getFriends(
        @Path("userID") userID: String,
    ): Response<List<FriendDTO>>

    /**
     * Removes a friend connection between two users.
     *
     * This endpoint terminates an existing friendship.
     *
     * @param userId The unique identifier of the user initiating the removal.
     * @param friendID The unique identifier of the friend to remove.
     * @return A Response object indicating success or failure.
     */
    @PUT("friends/{userID}/deleteFriend/{friendID}")
    suspend fun removeFriend(
        @Path("userID") userId: String,
        @Path("friendID") friendID: String
    ): Response<Unit>

    /**
     * Retrieves a list of pending friend requests for a user.
     *
     * This endpoint provides access to all incoming friend connection requests.
     *
     * @param userId The unique identifier of the user whose requests are being retrieved.
     * @return A Response containing a list of pending friend request DTOs.
     */
    @GET("friends/{userID}/friendRequests")
    suspend fun getFriendRequests(
        @Path("userID") userId: String,
    ): Response<List<FriendDTO>>

    /**
     * Sends a friend request to another user by email.
     *
     * This endpoint initiates a friend connection request.
     *
     * @param userId The unique identifier of the user sending the request.
     * @param friendEmail The email address of the user to whom the request is being sent.
     * @return A Response object indicating success or failure.
     */
    @POST("friends/{userID}/sendRequest/{friendEmail}")
    suspend fun sendFriendRequest(
        @Path("userID") userId: String,
        @Path("friendEmail") friendEmail: String
    ): Response<Unit>

    /**
     * Accepts a pending friend request.
     *
     * This endpoint confirms a friend connection between two users.
     *
     * @param userId The unique identifier of the user accepting the request.
     * @param friendID The unique identifier of the user who sent the request.
     * @return A Response object indicating success or failure.
     */
    @POST("friends/{userID}/acceptRequest/{friendID}")
    suspend fun acceptFriendRequest(
        @Path("userID") userId: String,
        @Path("friendID") friendID: String
    ): Response<Unit>

    /**
     * Declines a pending friend request.
     *
     * This endpoint rejects a friend connection request.
     *
     * @param userId The unique identifier of the user declining the request.
     * @param friendID The unique identifier of the user who sent the request.
     * @return A Response object indicating success or failure.
     */
    @POST("friends/{userID}/declineRequest/{friendID}")
    suspend fun declineFriendRequest(
        @Path("userID") userId: String,
        @Path("friendID") friendID: String
    ): Response<Unit>
}