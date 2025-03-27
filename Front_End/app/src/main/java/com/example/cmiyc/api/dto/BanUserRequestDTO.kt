package com.example.cmiyc.api.dto

/**
 * Data transfer object for requesting a user ban operation.
 *
 * This DTO is used when an administrator wants to ban a user from the system.
 * It contains only the administrator ID who is performing the ban action.
 *
 * @property adminID The unique identifier of the administrator performing the ban action.
 */
data class BanUserRequestDTO (
    val adminID : String
)