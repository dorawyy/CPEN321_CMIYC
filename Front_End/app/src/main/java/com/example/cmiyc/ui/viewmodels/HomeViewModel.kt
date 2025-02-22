package com.example.cmiyc.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cmiyc.api.ApiClient
import com.example.cmiyc.api.Post
import com.mapbox.geojson.Point
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST

class HomeViewModel : ViewModel() {
    companion object {
        private const val TAG = "HomeViewModel"
    }

    private val _friendLocations = MutableStateFlow<List<FriendLocation>>(emptyList())
    val friendLocations: StateFlow<List<FriendLocation>> = _friendLocations

    init {
        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        viewModelScope.launch {
            while (true) {
                _friendLocations.value = fetchFriendLocationsFromServer()
                kotlinx.coroutines.delay(5000L)
            }
        }
    }

    private suspend fun fetchFriendLocationsFromServer(): List<FriendLocation> {
        kotlinx.coroutines.delay(2000L)

        val postId = 1 // Replace with the desired post ID
        val call = ApiClient.apiService.getPostById(postId)

        call.enqueue(object : Callback<Post> {
            override fun onResponse(call: Call<Post>, response: Response<Post>) {
                if (response.isSuccessful) {
                    val post = response.body()
                    // Handle the retrieved post data
                    Log.d(TAG, post.toString())
                } else {
                    // Handle error
                    Log.e(TAG, response.toString())
                }
            }

            override fun onFailure(call: Call<Post>, t: Throwable) {
                // Handle failure
                Log.e(TAG, "onFailure")
            }
        })

        val baseLocations = listOf(
            Triple("user1", "Alice", Point.fromLngLat(-123.25041000865335, 49.26524685838906)),
            Triple("user2", "Bob", Point.fromLngLat(-123.251, 49.265)),
            Triple("user3", "Carol", Point.fromLngLat(-123.250, 49.2655))
        )

        return baseLocations.map { (userId, name, basePoint) ->
            FriendLocation(
                userId = userId,
                name = name,
                status = when (userId) {
                    "user1" -> "Online"
                    "user2" -> "Busy"
                    "user3" -> "Away"
                    else -> "Offline"
                },
                point = randomNearbyPoint(basePoint)
            )
        }


    }

    private fun randomNearbyPoint(base: Point, offset: Double = 0.001): Point {
        val randomLon = base.longitude() + (Random.nextDouble() * 2 - 1) * offset
        val randomLat = base.latitude() + (Random.nextDouble() * 2 - 1) * offset
        return Point.fromLngLat(randomLon, randomLat)
    }
}

data class FriendLocation(
    val userId: String,
    val name: String,
    val status: String,
    val point: Point
)