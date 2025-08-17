package ru.netology.yandexmap.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.yandex.mapkit.geometry.Point
import ru.netology.yandexmap.repository.RepositoryOnPrefs

class YaMapViewModel : ViewModel() {

    private val repository = RepositoryOnPrefs()

    private val _interfaceState = MutableLiveData<InterfaceState>(InterfaceState())
    val interfaceState: LiveData<InterfaceState> = _interfaceState

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

    fun setRadioButton(flag: FlagAction) {
        _interfaceState.value = _interfaceState.value?.copy(radioButtonState = flag)
            ?: throw RuntimeException("ERROR: YaMapViewModel._interfaceState - value is null")
    }

    data class InterfaceState(
        val radioButtonState: FlagAction = FlagAction.OFF
    )
}

enum class FlagAction { CREATE, EDIT, DELETE, OFF }