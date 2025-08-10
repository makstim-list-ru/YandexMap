package ru.netology.yandexmap

import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.Map

interface DefaultInputListener : InputListener{
    override fun onMapTap(p0: Map, p1: Point) {
        // Implemented in MainFragment
    }

    override fun onMapLongTap(p0: Map, p1: Point) {
        TODO("Not yet implemented")
    }
}