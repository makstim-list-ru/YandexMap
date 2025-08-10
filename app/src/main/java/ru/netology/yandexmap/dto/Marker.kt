package ru.netology.yandexmap.dto

import kotlinx.serialization.Serializable

@Serializable
data class Marker(
    val id: Long = 0L,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val text: String = "My mark",
)