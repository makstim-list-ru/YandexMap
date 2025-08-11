package ru.netology.yandexmap.repository

import com.yandex.mapkit.geometry.Point

interface RepositoryInterface {
    fun addMarker(p0: Point, locationText: String)
    fun editMarker(p0: Point, locationText: String)
    fun removeMarker(p0: Point)
}