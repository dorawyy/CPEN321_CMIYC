package com.example.cmiyc.utils

import android.content.Context
import android.location.Geocoder
import coil.network.HttpException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

object GeocodingUtil {

    /**
     * Gets the address for the given latitude and longitude.
     * Returns a formatted address string or coordinates if geocoding fails.
     */
    suspend fun getAddressFromLocation(
        context: Context,
        latitude: Double,
        longitude: Double
    ): String = withContext(Dispatchers.IO) {
        fetchAddress(context, latitude, longitude)
    }


    private suspend fun fetchAddress(context: Context, latitude: Double, longitude: Double): String {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())

            // For API level 33 and above
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                var addressResult = "Unknown location"

                geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                    if (addresses.isNotEmpty()) {
                        val address = addresses[0]
                        addressResult = formatAddress(address.thoroughfare,
                            address.locality,
                            address.adminArea,
                            address.countryName)
                    }
                }
                return addressResult
            }
            // For API level below 33
            else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                if (addresses != null && addresses.isNotEmpty()) {
                    val address = addresses[0]
                    return formatAddress(address.thoroughfare,
                        address.locality,
                        address.adminArea,
                        address.countryName)
                }
            }

            // Fallback to coordinates if geocoding fails
            return formatCoordinates(latitude, longitude)
        } catch (e: IOException) {
            return formatCoordinates(latitude, longitude)
        } catch (e: HttpException) {
            return formatCoordinates(latitude, longitude)
        }
    }


    private fun formatAddress(
        street: String?,
        city: String?,
        state: String?,
        country: String?
    ): String {
        val parts = mutableListOf<String>()

        if (!street.isNullOrBlank()) parts.add(street)
        if (!city.isNullOrBlank()) parts.add(city)
        if (!state.isNullOrBlank()) parts.add(state)
        if (!country.isNullOrBlank()) parts.add(country)

        return if (parts.isNotEmpty()) parts.joinToString(", ") else "Unknown location"
    }


    private fun formatCoordinates(latitude: Double, longitude: Double): String {
        return "(${latitude.format(6)}, ${longitude.format(6)})"
    }


    private fun Double.format(digits: Int) = "%.${digits}f".format(this)
}