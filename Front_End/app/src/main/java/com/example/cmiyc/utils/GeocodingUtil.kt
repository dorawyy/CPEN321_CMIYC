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

    /**
     * Gets the address for the given latitude and longitude.
     *
     * This suspending function performs reverse geocoding to convert coordinates
     * into a human-readable address. It handles the operation on the IO dispatcher
     * to avoid blocking the main thread.
     *
     * @param context The Android context required for geocoding operations.
     * @param latitude The latitude coordinate.
     * @param longitude The longitude coordinate.
     * @return A formatted address string or coordinates string if geocoding fails.
     */
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

    /**
     * Formats address components into a readable string.
     *
     * This method combines various address components (street, city, state, country)
     * into a comma-separated string. It filters out null or empty components.
     *
     * @param street The street or thoroughfare component of the address.
     * @param city The city or locality component of the address.
     * @param state The state or administrative area component of the address.
     * @param country The country component of the address.
     * @return A formatted address string or "Unknown location" if no components are available.
     */
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

    /**
     * Formats coordinates into a readable string.
     *
     * This method is used as a fallback when geocoding fails, providing
     * a formatted representation of the original coordinates.
     *
     * @param latitude The latitude coordinate.
     * @param longitude The longitude coordinate.
     * @return A formatted string representation of the coordinates.
     */
    private fun formatCoordinates(latitude: Double, longitude: Double): String {
        return "(${latitude.format(6)}, ${longitude.format(6)})"
    }

    /**
     * Extension function to format a Double with specified number of decimal places.
     *
     * @param digits The number of decimal places to include.
     * @return A String representation of the Double with the specified precision.
     */
    private fun Double.format(digits: Int) = "%.${digits}f".format(this)
}