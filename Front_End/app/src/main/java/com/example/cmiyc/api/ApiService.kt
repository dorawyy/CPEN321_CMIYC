package com.example.cmiyc.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import com.example.cmiyc.api.dto.*

interface ApiService {

    // User API
    @POST("user")
    suspend fun registerUser(
        @Body user: UserRegistrationRequestDTO
    ): Response<Unit>

    @PUT("location/{userID}")
    suspend fun updateUserLocation(
        @Path("userID") userId: String,
        @Body location: LocationUpdateRequestDTO
    ): Response<Unit>


    // Friends API
    @GET("friends/{userID}")
    suspend fun getFriends(
        @Path("userID") userID: String,
    ): Response<List<FriendDTO>>

    @PUT("friends/{userID}/declineRequest/{friendID}")
    suspend fun removeFriend(
        @Path("userID") userId: String,
        @Path("friendID") friendID: String
    ): Response<Unit>

    @GET("/friends/{userID}/friendRequests")
    suspend fun getFriendRequests(
        @Path("userID") userId: String,
    ): Response<List<FriendDTO>>

    @POST("friends/{userID}/sendRequest/{friendEmail}")
    suspend fun sendFriendRequest(
        @Path("userID") userId: String,
        @Path("friendEmail") friendEmail: String
    ): Response<Unit>

    @POST("friends/{userID}/acceptRequest/{friendID}")
    suspend fun acceptFriendRequest(
        @Path("userID") userId: String,
        @Path("friendID") friendID: String
    ): Response<Unit>

    @POST("friends/{userID}/declineRequest/{friendID}")
    suspend fun declineFriendRequest(
        @Path("userID") userId: String,
        @Path("friendID") friendID: String
    ): Response<Unit>


    // Notifications API
    @GET("notifications/{userID}")
    suspend fun getLogs(
        @Path("userID") userId: String
    ): Response<List<LogDTO>>

    @POST("send-event/{userID}")
    suspend fun broadcastMessage(
        @Path("userID") userId: String,
        @Body eventName: String,
    ): Response<Unit>

}