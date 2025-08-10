package ru.netology.yandexmap

import com.yandex.mapkit.map.CircleMapObject
import com.yandex.mapkit.map.ClusterizedPlacemarkCollection
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.MapObjectVisitor
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.map.PolygonMapObject
import com.yandex.mapkit.map.PolylineMapObject

interface DefaultMapObjectVisitor : MapObjectVisitor {
    override fun onPlacemarkVisited(p0: PlacemarkMapObject) {
        // Implemented in MainFragment
    }

    override fun onPolylineVisited(p0: PolylineMapObject) {
        TODO("Not yet implemented")
    }

    override fun onPolygonVisited(p0: PolygonMapObject) {
        TODO("Not yet implemented")
    }

    override fun onCircleVisited(p0: CircleMapObject) {
        TODO("Not yet implemented")
    }

    override fun onCollectionVisitStart(p0: MapObjectCollection): Boolean {
//        TODO("Not yet implemented")
        return false
    }

    override fun onCollectionVisitEnd(p0: MapObjectCollection) {
        TODO("Not yet implemented")
    }

    override fun onClusterizedCollectionVisitStart(p0: ClusterizedPlacemarkCollection): Boolean {
//        TODO("Not yet implemented")
        return false
    }

    override fun onClusterizedCollectionVisitEnd(p0: ClusterizedPlacemarkCollection) {
        TODO("Not yet implemented")
    }
}