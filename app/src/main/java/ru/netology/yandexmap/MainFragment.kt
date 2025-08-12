package ru.netology.yandexmap

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.location.Location
import com.yandex.mapkit.location.LocationListener
import com.yandex.mapkit.location.LocationStatus
import com.yandex.mapkit.map.CameraListener
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.CameraUpdateReason
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.mapkit.map.MapWindow
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.search.Address
import com.yandex.mapkit.search.Response
import com.yandex.mapkit.search.SearchFactory
import com.yandex.mapkit.search.SearchManager
import com.yandex.mapkit.search.SearchManagerType
import com.yandex.mapkit.search.SearchOptions
import com.yandex.mapkit.search.Session
import com.yandex.mapkit.search.ToponymObjectMetadata
import com.yandex.runtime.Error
import com.yandex.runtime.Runtime.getApplicationContext
import com.yandex.runtime.image.ImageProvider
import ru.netology.yandexmap.databinding.FragmentItemListBinding
import ru.netology.yandexmap.databinding.FragmentMainBinding
import ru.netology.yandexmap.dto.Marker
import ru.netology.yandexmap.viewmodel.YaMapViewModel
import kotlin.getValue

class MainFragment : Fragment(), CameraListener {

    private val startLocation = Point(59.9402, 30.315)
    private val zoomValue: Float = 16.5f

    //    private val markerList = mutableListOf<Marker>()
    private var markerChose: Marker = Marker(id = -1)

    private lateinit var mapObjectCollection: MapObjectCollection
    private lateinit var searchManager: SearchManager
    private lateinit var searchSession: Session
    private var searchResultStreetName = MutableLiveData("")
    private val placemarkMapObjectList = mutableListOf<PlacemarkMapObject>()

    private val zoomBoundary = 16.4f

    private lateinit var iconMarker: Bitmap //= createBitmapFromVector(R.drawable.baseline_add_location_48)
//    TODO ask question

    private var flagAction: FlagAction = FlagAction.OFF

    val viewModel by activityViewModels<YaMapViewModel>()


    private val mapObjectVisitor = object : DefaultMapObjectVisitor {
        override fun onPlacemarkVisited(p0: PlacemarkMapObject) {
            placemarkMapObjectList.add(p0)
        }
    }

    private val searchListener = object : Session.SearchListener {
        override fun onSearchResponse(response: Response) {
            val street = response.collection.children.firstOrNull()?.obj
                ?.metadataContainer
                ?.getItem(ToponymObjectMetadata::class.java)
                ?.address
                ?.components
                ?.firstOrNull { it.kinds.contains(Address.Component.Kind.STREET) }
                ?.name ?: "No information found"

            searchResultStreetName.value = street

            Toast.makeText(requireContext(), street, Toast.LENGTH_SHORT).show()
        }

        override fun onSearchError(p0: Error) {
            Toast.makeText(
                requireContext(),
                "onSearchError - ERROR is search YMap",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private val placemarkMapObjectTapListener = MapObjectTapListener { mapObject, _ ->

        if (mapObject !is PlacemarkMapObject)
            throw IllegalArgumentException("placemarkMapObjectTapListener -> Object is not valid")
        val placemarkMapObject: PlacemarkMapObject = mapObject
        val pointPlaceMark = placemarkMapObject.geometry

//        Toast.makeText(
//            requireContext(),
//            "Эрмитаж — музей изобразительных искусств",
//            Toast.LENGTH_LONG
//        ).show()
        when (flagAction) {
            FlagAction.OFF -> {}
            FlagAction.CREATE -> {}
            FlagAction.EDIT -> {
                val viewExist = view ?: return@MapObjectTapListener true
                val binding = FragmentMainBinding.bind(viewExist)
                binding.pointNameText.setText("NEW mark")
                binding.pointNameLayout.visibility = View.VISIBLE
                binding.okButton.visibility = View.VISIBLE
                binding.pointNameText.setOnEditorActionListener { v, actionId, event ->
                    if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_NEXT ||
                        (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
                    ) {
                        // Perform your action when Enter or a similar action key is pressed
                        val inputFieldTextBody = binding.pointNameText.text.toString()
                        mapObjectCollection.remove(mapObject)
                        setMarkerInLocation(pointPlaceMark, inputFieldTextBody)
                        binding.pointNameLayout.visibility = View.GONE
                        binding.okButton.visibility = View.GONE

                        viewModel.editMarkerViewModel(pointPlaceMark, inputFieldTextBody)

                        true // Indicate that the event was handled
                    } else {
                        false // Indicate that the event was not handled
                    }
                }
                binding.okButton.setOnClickListener {
                    val inputFieldTextBody = binding.pointNameText.text.toString()
                    mapObjectCollection.remove(mapObject)
                    setMarkerInLocation(pointPlaceMark, inputFieldTextBody)
                    binding.pointNameLayout.visibility = View.GONE
                    binding.okButton.visibility = View.GONE

                    viewModel.editMarkerViewModel(pointPlaceMark, inputFieldTextBody)
                }
            }

            FlagAction.DELETE -> {
                mapObjectCollection.remove(mapObject)
                viewModel.removeMarkerViewModel(pointPlaceMark)
//                markerListRemoveMarker(pointPlaceMark)
//                putToFile()
            }
        }
        true
    }

    private val inputListener = object : DefaultInputListener {
        override fun onMapTap(p0: Map, p1: Point) {
            when (flagAction) {
                FlagAction.CREATE -> {
                    val viewExist = view ?: return
                    val binding = FragmentMainBinding.bind(viewExist)

                    searchSession = searchManager.submit(p1, 20, SearchOptions(), searchListener)

                    binding.pointNameLayout.visibility = View.VISIBLE
                    binding.okButton.visibility = View.VISIBLE
                    binding.pointNameText.requestFocus()
                    binding.pointNameText.setOnEditorActionListener { v, actionId, event ->
                        if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_NEXT ||
                            (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
                        ) {
                            // Perform your action when Enter or a similar action key is pressed
                            val inputFieldTextBody = binding.pointNameText.text.toString()
                            setMarkerInLocation(p1, inputFieldTextBody)
                            binding.pointNameLayout.visibility = View.GONE
                            binding.okButton.visibility = View.GONE

                            viewModel.addMarkerViewModel(p1, inputFieldTextBody)

                            true // Indicate that the event was handled
                        } else {
                            false // Indicate that the event was not handled
                        }
                    }
                    binding.okButton.setOnClickListener {
                        val inputFieldTextBody = binding.pointNameText.text.toString()
                        setMarkerInLocation(p1, inputFieldTextBody)
                        binding.pointNameLayout.visibility = View.GONE
                        binding.okButton.visibility = View.GONE

                        viewModel.addMarkerViewModel(p1, inputFieldTextBody)
                    }
                }

                FlagAction.EDIT -> {}
                FlagAction.DELETE -> {}
                FlagAction.OFF -> {
                    searchSession = searchManager.submit(p1, 20, SearchOptions(), searchListener)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //todo - delete println
        println("MainFragment ======================== onCreate")

        arguments?.let {
            val gson = Gson()
            val token = TypeToken.getParameterized(Marker::class.java).type
            val string = it.getString("KEY_LIST_TO_MAIN")
            if (!string.isNullOrBlank()) {
                markerChose = gson.fromJson(string, token)
                println(markerChose)
            }
        }


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //todo - delete println
        println("MainFragment ======================== onCreateView")
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //todo - delete println
        println("MainFragment ======================== onViewCreated")

        val binding = FragmentMainBinding.bind(view)
        val yandexMap = binding.mapview.mapWindow.map

        MapKitFactory.initialize(requireContext())

        subscribeToLifecycle(binding.mapview)

        checkPermissions(binding.mapview.mapWindow)

        viewModel.data.observe(viewLifecycleOwner) { it ->
            mapObjectCollection.clear()
            it.forEach { post ->
                setMarkerInLocation(Point(post.latitude, post.longitude), post.text)
            }
        }

        searchResultStreetName.observe(viewLifecycleOwner, Observer<String> {
            binding.pointNameText.setText(searchResultStreetName.value)
            println(searchResultStreetName.value)
        })

        searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.ONLINE)
        yandexMap.addInputListener(inputListener) // Добавляем слушатель тапов по карте с извлечением информации
        mapObjectCollection =
            binding.mapview.mapWindow.map.mapObjects // Инициализируем коллекцию различных объектов на карте

//        val fileExists = getFromFile()
//        if (!fileExists) setMarkerInStartLocation()
//        markerList.forEach { post ->
//            setMarkerInLocation(Point(post.latitude, post.longitude), post.text)
//        }
        moveToStartLocation()

        if (markerChose.id == -1L) {
            moveToStartLocation()
//            setMarkerInStartLocation()
        } else {
            val point = Point(markerChose.latitude, markerChose.longitude)
            moveToLocation(point)
            markerChose = Marker()
//            setMarkerInLocation(point, "Вот то что искал!")
        }

        yandexMap.addCameraListener(this)
//        yandexMap.addInputListener(inputListener)

        binding.buttonCreate.setOnClickListener {
            flagAction = FlagAction.CREATE
        }
        binding.buttonEdit.setOnClickListener {
            flagAction = FlagAction.EDIT
        }
        binding.buttonDelete.setOnClickListener {
            flagAction = FlagAction.DELETE
        }
        binding.buttonOff.setOnClickListener {
            flagAction = FlagAction.OFF
        }
        binding.buttonList.setOnClickListener {
//            val gson = Gson()
//            val bundle = Bundle()
//            bundle.putSerializable("KEY_MAIN_TO_LIST", gson.toJson(markerList))
//            findNavController().navigate(R.id.action_mainFragment_to_itemFragment, bundle)

            findNavController().navigate(R.id.action_mainFragment_to_itemFragment)
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
        view ?: return

//        mapObjectCollection =
//            binding.mapview.mapWindow.map.mapObjects // Инициализируем коллекцию различных объектов на карте

        if (!this::iconMarker.isInitialized) {
            iconMarker = createBitmapFromVector(R.drawable.baseline_add_location_48)!!
            //todo - delete println
            println("setMarkerInLocation - init - black")
        }
        val placemarkMapObject = mapObjectCollection.addPlacemark().apply {
            geometry = location
            setIcon(ImageProvider.fromBitmap(iconMarker))
            opacity = 0.5f // Устанавливаем прозрачность метке
            setText(locationText)
        } // Добавляем метку со значком
        //todo - delete println
        println("setMarkerInLocation - метка++")

//        postList.add(Post(0, location.latitude, location.longitude, locationText))
//        putToFile()
        placemarkMapObject.addTapListener(placemarkMapObjectTapListener)
    }

    private fun setMarkerInStartLocation() = setMarkerInLocation()


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
            //todo - delete println
            println("onCameraPositionChanged - finished")
            placemarkMapObjectList.clear()
            mapObjectCollection.traverse(mapObjectVisitor)
            when {
                cameraPosition.zoom > zoomBoundary -> {
                    iconMarker = createBitmapFromVector(R.drawable.baseline_add_location_48_red)!!
                    placemarkMapObjectList.forEach {
                        it.setIcon(ImageProvider.fromBitmap(iconMarker))
                    }
                    //todo - delete println
                    println("onCameraPositionChanged - RED")
                }

                cameraPosition.zoom <= zoomBoundary -> {
                    iconMarker = createBitmapFromVector(R.drawable.baseline_add_location_48)!!
                    placemarkMapObjectList.forEach {
                        it.setIcon(ImageProvider.fromBitmap(iconMarker))
                    }
                    //todo - delete println
                    println("onCameraPositionChanged - BLACK")
                }
            }
        }
    }

    private fun checkPermissions(mapWindow: MapWindow) {
        val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) enableUserLocation(mapWindow) else {
                    Toast.makeText(
                        requireContext(),
                        "SORRY, The application will not work properly without PERMISSIONS",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        when { // 1. Проверяем дали/есть/нет права
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                enableUserLocation(mapWindow)

                MapKitFactory.getInstance()
                    .createLocationManager()
                    .requestSingleUpdate(
                        object : LocationListener {
                            override fun onLocationUpdated(location: Location) {
                                println(location)
                            }

                            override fun onLocationStatusUpdated(locationStatus: LocationStatus) {
                                println(locationStatus)
                            }
                        }
                    )
            }
            // 2. Должны показать обоснование необходимости прав
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                // show rationale dialog
                Toast.makeText(
                    requireContext(),
                    "PLEASE, Grant PERMISSIONS to make the application working properly",
                    Toast.LENGTH_LONG
                ).show()
            }
            // 3. Запрашиваем права
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun enableUserLocation(mapWindow: MapWindow) {
        val userLocation = MapKitFactory.getInstance().createUserLocationLayer(mapWindow)
        userLocation.isVisible = true
        userLocation.isHeadingModeActive = true
    }

    enum class FlagAction { CREATE, EDIT, DELETE, OFF }
}