package ru.netology.yandexmap.viewmodel

import androidx.lifecycle.ViewModel
import com.yandex.mapkit.geometry.Point
import ru.netology.yandexmap.repository.RepositoryOnPrefs

class YaMapViewModel : ViewModel() {

    private val repository = RepositoryOnPrefs()

    val data = repository.data

    fun addMarkerViewModel(p0: Point, locationText: String) {
        repository.addMarker(p0, locationText)
    }

    fun editMarkerViewModel(p0: Point, locationText: String) {
        repository.editMarker(p0, locationText)
    }

    fun removeMarkerViewModel(p0: Point) {
        repository.removeMarker(p0)
    }


}