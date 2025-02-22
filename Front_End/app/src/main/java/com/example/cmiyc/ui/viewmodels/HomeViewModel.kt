package com.example.cmiyc.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapbox.geojson.Point
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class HomeViewModel : ViewModel() {
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