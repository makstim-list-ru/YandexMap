package ru.netology.yandexmap

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraListener
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.CameraUpdateReason
import com.yandex.mapkit.map.CircleMapObject
import com.yandex.mapkit.map.ClusterizedPlacemarkCollection
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.mapkit.map.MapObjectVisitor
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.map.PolygonMapObject
import com.yandex.mapkit.map.PolylineMapObject
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.Runtime.getApplicationContext
import com.yandex.runtime.image.ImageProvider
import ru.netology.yandexmap.databinding.FragmentMainBinding
import ru.netology.yandexmap.dto.Post

class MainFragment : Fragment(), CameraListener {

    private val startLocation = Point(59.9402, 30.315)
    private var zoomValue: Float = 16.5f

    private val postList = mutableListOf<Post>()
    private var postLiked: Post = Post(id = -1)

    private lateinit var mapObjectCollection: MapObjectCollection
    private val placemarkMapObjectList = mutableListOf<PlacemarkMapObject>()
    //    private lateinit var placemarkMapObject: PlacemarkMapObject

    private val zoomBoundary = 16.4f
    private var flagAction: FlagAction = FlagAction.OFF


    private val mapObjectVisitor = object : MapObjectVisitor {
        override fun onPlacemarkVisited(p0: PlacemarkMapObject) {
//            println("onPlacemarkVisited - text - ${p0.text}")
            placemarkMapObjectList.add(p0)
        }

        override fun onPolylineVisited(p0: PolylineMapObject) {
            println("onPolylineVisited")
        }

        override fun onPolygonVisited(p0: PolygonMapObject) {
            println("onPolygonVisited")
        }

        override fun onCircleVisited(p0: CircleMapObject) {
            println("onCircleVisited")
        }

        override fun onCollectionVisitStart(p0: MapObjectCollection): Boolean {
            println("onCollectionVisitStart")
            return true
        }

        override fun onCollectionVisitEnd(p0: MapObjectCollection) {
            println("onCollectionVisitEnd")
        }

        override fun onClusterizedCollectionVisitStart(p0: ClusterizedPlacemarkCollection): Boolean {
            println("onClusterizedCollectionVisitStart")
            return true
        }

        override fun onClusterizedCollectionVisitEnd(p0: ClusterizedPlacemarkCollection) {
            println("onClusterizedCollectionVisitEnd")
        }
    }

    private val mapObjectTapListener = MapObjectTapListener { mapObject, point ->
        Toast.makeText(
            requireContext(),
            "Эрмитаж — музей изобразительных искусств",
            Toast.LENGTH_LONG
        ).show()
        when (flagAction) {
            FlagAction.OFF -> {}
            FlagAction.CREATE -> {}
            FlagAction.EDIT -> {
                val viewExist = view ?: return@MapObjectTapListener true
                val binding = FragmentMainBinding.bind(viewExist)
                binding.pointNameText.setText("NEW mark")
                binding.pointNameLayout.visibility = View.VISIBLE
                binding.pointNameText.setOnEditorActionListener { v, actionId, event ->
                    println("ACTION   $actionId")
                    if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_NEXT ||
                        (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
                    ) {
                        // Perform your action when Enter or a similar action key is pressed
                        val locationText = binding.pointNameText.text.toString()
                        mapObjectCollection.remove(mapObject)
                        setMarkerInLocation(point, locationText)
                        binding.pointNameLayout.visibility = View.GONE
                        true // Indicate that the event was handled
                    } else {
                        false // Indicate that the event was not handled
                    }
                }
            }

            FlagAction.DELETE -> {
                mapObjectCollection.remove(mapObject)
            }
        }
        true
    }

    private val inputListener = object : InputListener {
        override fun onMapTap(p0: Map, point: Point) {
            if (flagAction == FlagAction.CREATE) {
                var locationText: String = "Memorable point"
                val viewExist = view ?: return
                val binding = FragmentMainBinding.bind(viewExist)
                binding.pointNameLayout.visibility = View.VISIBLE
                binding.pointNameText.requestFocus()
                binding.pointNameText.setOnEditorActionListener { v, actionId, event ->
//                    println("actionId=$actionId \t event=$event \t event.keyCode=" + event.keyCode + "event.action=" + event.action)
                    if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_NEXT ||
                        (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
                    ) {
                        // Perform your action when Enter or a similar action key is pressed
                        locationText = binding.pointNameText.text.toString()

                        setMarkerInLocation(point, locationText)
                        postList.add(Post(0, point.latitude, point.longitude, locationText))
                        putToFile()

                        binding.pointNameLayout.visibility = View.GONE
                        true // Indicate that the event was handled
                    } else {
                        false // Indicate that the event was not handled
                    }
                }
            }
        }

        override fun onMapLongTap(
            p0: Map,
            p1: Point
        ) {
            TODO("Not yet implemented")
        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        println("MainFragment == onCreate")
        arguments?.let {
            val gson = Gson()
            val token = TypeToken.getParameterized(Post::class.java).type
            val string = it.getString("KEY_LIST_TO_MAIN")
            if (!string.isNullOrBlank()) {
                postLiked = gson.fromJson(string, token)
                println(postLiked)
            }
        }

        MapKitFactory.initialize(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentMainBinding.bind(view)

        val mapView = binding.mapview
        val mapWindow = binding.mapview.mapWindow
        val yandexMap = binding.mapview.mapWindow.map

        subscribeToLifecycle(mapView)

        mapObjectCollection =
            binding.mapview.mapWindow.map.mapObjects // Инициализируем коллекцию различных объектов на карте

        val fileExists = getFromFile()
        if (!fileExists) setMarkerInStartLocation()
        postList.forEach { post ->
            setMarkerInLocation(Point(post.latitude, post.longitude), post.text)
        }
        moveToStartLocation()

        if (postLiked.id == -1L) {
            moveToStartLocation()
//            setMarkerInStartLocation()
        } else {
            val point = Point(postLiked.latitude, postLiked.longitude)
            moveToLocation(point)
            postLiked = Post()
//            setMarkerInLocation(point, "Вот то что искал!")
        }

        yandexMap.addCameraListener(this)
        yandexMap.addInputListener(inputListener)

        binding.buttonCreate.setOnClickListener {
            flagAction = FlagAction.CREATE
        }
        binding.buttonEdit.setOnClickListener {
            flagAction = FlagAction.EDIT
        }
        binding.buttonDelete.setOnClickListener {
            flagAction = FlagAction.DELETE
        }
        binding.buttonList.setOnClickListener {
            val gson = Gson()
            val bundle = Bundle()
            bundle.putSerializable("KEY_MAIN_TO_LIST", gson.toJson(postList))

            findNavController().navigate(R.id.action_mainFragment_to_itemFragment, bundle)
        }

    }

    private fun subscribeToLifecycle(mapView: MapView) {
        viewLifecycleOwner.lifecycle.addObserver(
            object : LifecycleEventObserver {
                override fun onStateChanged(
                    source: LifecycleOwner,
                    event: Lifecycle.Event
                ) {
                    when (event) {
                        Lifecycle.Event.ON_START -> {
                            MapKitFactory.getInstance().onStart()
                            mapView.onStart()
                        }

                        Lifecycle.Event.ON_STOP -> {
                            mapView.onStop()
                            MapKitFactory.getInstance().onStop()
                        }

                        Lifecycle.Event.ON_DESTROY -> source.lifecycle.removeObserver(this)

                        else -> Unit
                    }
                }
            }
        )
    }

    private fun moveToLocation(location: Point = startLocation, zoom: Float = zoomValue) {
        val viewExist = view ?: return
        val binding = FragmentMainBinding.bind(viewExist)
        binding.mapview.mapWindow.map.move(
            CameraPosition(location, zoom, 0.0f, 0.0f), Animation(Animation.Type.SMOOTH, 5f),
            null
        )
    }

    private fun moveToStartLocation() = moveToLocation()

    private fun setMarkerInLocation(
        location: Point = startLocation,
        locationText: String = "Обязательно к посещению!"
    ) {
        val viewExist = view ?: return
        val binding = FragmentMainBinding.bind(viewExist)
        val marker =
            createBitmapFromVector(R.drawable.baseline_add_location_48) // Добавляем ссылку на картинку
//        mapObjectCollection =
//            binding.mapview.mapWindow.map.mapObjects // Инициализируем коллекцию различных объектов на карте


        val placemarkMapObject = mapObjectCollection.addPlacemark().apply {
            geometry = location
            setIcon(ImageProvider.fromBitmap(marker))
            opacity = 0.5f // Устанавливаем прозрачность метке
            setText(locationText)
        } // Добавляем метку со значком

//        postList.add(Post(0, location.latitude, location.longitude, locationText))
//        putToFile()
        placemarkMapObject.addTapListener(mapObjectTapListener)
    }

    private fun setMarkerInStartLocation() = setMarkerInLocation()

    private fun putToFile() {
        val appContext = getApplicationContext()
        val gson = Gson()
        val preferences = appContext.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        preferences.edit().apply() {
            putString("PREF_ITEM", gson.toJson(postList))
            commit()
        }
    }

    private fun getFromFile(): Boolean {
        val appContext = getApplicationContext()
        val gson = Gson()
        val preferences = appContext.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        val string = preferences.getString("PREF_ITEM", "")
        if (string.isNullOrBlank()) return false
        val token = TypeToken.getParameterized(List::class.java, Post::class.java).type
        if (!string.isNullOrBlank()) {
            val collectedPostList: List<Post> = gson.fromJson(string, token)
            postList.clear()
            postList.addAll(collectedPostList)
        }
        return true
    }

    private fun createBitmapFromVector(art: Int): Bitmap? {
        val drawable = ContextCompat.getDrawable(requireContext(), art) ?: return null
        val bitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    override fun onCameraPositionChanged(
        map: Map,
        cameraPosition: CameraPosition,
        cameraUpdateReason: CameraUpdateReason,
        finished: Boolean
    ) {
        if (finished) { // Если камера закончила движение mapObjectCollection.clear()
            placemarkMapObjectList.clear()
            mapObjectCollection.traverse(mapObjectVisitor)
            when {
                cameraPosition.zoom > zoomBoundary -> {
                    placemarkMapObjectList.forEach {
                        it.setIcon(
                            ImageProvider.fromBitmap(
                                createBitmapFromVector(R.drawable.baseline_add_location_48_red)
                            )
                        )
                    }

                }

                cameraPosition.zoom <= zoomBoundary -> {
                    placemarkMapObjectList.forEach {
                        it.setIcon(
                            ImageProvider.fromBitmap(
                                createBitmapFromVector(R.drawable.baseline_add_location_48)
                            )
                        )
                    }
                }
            }
        }
    }

    enum class FlagAction { CREATE, EDIT, DELETE, OFF }
}