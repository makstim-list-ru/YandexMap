package ru.netology.yandexmap.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yandex.mapkit.geometry.Point
import com.yandex.runtime.Runtime.getApplicationContext
import ru.netology.yandexmap.dto.Marker

class RepositoryOnPrefs : RepositoryInterface {

    private var markerList = emptyList<Marker>()

    private val _data = MutableLiveData(markerList)
    val data: LiveData<List<Marker>> = _data

    init {
        getFromFile()
    }

    private fun putToFile() {
        val appContext = getApplicationContext()
        val gson = Gson()
        val preferences = appContext.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        preferences.edit().apply {
            putString("PREF_ITEM", gson.toJson(markerList))
            commit()
        }
    }

    private fun getFromFile(): Boolean {
        val appContext = getApplicationContext()
        val gson = Gson()
        val preferences = appContext.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        val string = preferences.getString("PREF_ITEM", "")
        if (string.isNullOrBlank()) return false
        val token = TypeToken.getParameterized(List::class.java, Marker::class.java).type
        markerList = gson.fromJson(string, token)
        _data.value = markerList
        return true
    }

    private fun markerListAddMarker(p0: Point, locationText: String) {
        val id = markerHashCode(p0)
        markerList = markerList + Marker(id, p0.latitude, p0.longitude, locationText)
        _data.value = markerList
    }

    private fun markerListRemoveMarker(p0: Point) {
        val id = markerHashCode(p0)
        markerList = markerList.filter { it -> it.id != id }
        _data.value = markerList
    }

    private fun markerHashCode(p0: Point): Long {
        return (p0.latitude.toString() + p0.longitude.toString()).hashCode().toLong()
    }

    override fun addMarker(p0: Point, locationText: String) {
        markerListAddMarker(p0, locationText)
        putToFile()
    }

    override fun editMarker(p0: Point, locationText: String) {
        markerListRemoveMarker(p0)
        markerListAddMarker(p0, locationText)
        putToFile()
    }

    override fun removeMarker(p0: Point) {
        markerListRemoveMarker(p0)
        putToFile()
    }


}