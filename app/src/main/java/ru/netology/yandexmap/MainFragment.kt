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
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
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
import com.yandex.runtime.image.ImageProvider
import ru.netology.yandexmap.databinding.FragmentMainBinding
import ru.netology.yandexmap.dto.Marker
import ru.netology.yandexmap.viewmodel.FlagAction
import ru.netology.yandexmap.viewmodel.YaMapViewModel

class MainFragment : Fragment(), CameraListener {

    private var markerChose: Marker = Marker(id = -1)

    private lateinit var mapObjectCollection: MapObjectCollection
    private lateinit var searchManager: SearchManager
    private lateinit var searchSession: Session
    private lateinit var iconMarker: Bitmap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var searchResultStreetName = MutableLiveData("")
    private val startLocation = Point(59.9402, 30.315)
    private val zoomValue: Float = 16.5f
    private val zoomBoundary = 16.4f

    val viewModel by activityViewModels<YaMapViewModel>()

    private val searchListener = object : Session.SearchListener {
        override fun onSearchResponse(response: Response) {
            val street =
                response.collection.children.firstOrNull()?.obj?.metadataContainer?.getItem(
                    ToponymObjectMetadata::class.java
                )?.address?.components?.firstOrNull { it.kinds.contains(Address.Component.Kind.STREET) }?.name
                    ?: "No information found"

            searchResultStreetName.value = street
            Toast.makeText(requireContext(), street, Toast.LENGTH_SHORT).show()
        }

        override fun onSearchError(p0: Error) {
            Toast.makeText(
                requireContext(), "onSearchError - ERROR is search YMap", Toast.LENGTH_SHORT
            ).show()
        }
    }

    private val placemarkMapObjectTapListener = MapObjectTapListener { mapObject, _ ->

        if (mapObject !is PlacemarkMapObject) throw IllegalArgumentException("placemarkMapObjectTapListener -> Object is not valid")
        val placemarkMapObject: PlacemarkMapObject = mapObject
        val pointPlaceMark = placemarkMapObject.geometry

        when (viewModel.interfaceState.value?.radioButtonState) {
            FlagAction.OFF -> {}
            FlagAction.CREATE -> {}
            FlagAction.EDIT -> {
                val viewExist = view ?: return@MapObjectTapListener true
                val binding = FragmentMainBinding.bind(viewExist)
                binding.pointNameText.setText("NEW mark")
                binding.pointNameLayout.visibility = View.VISIBLE
                binding.okButton.visibility = View.VISIBLE
                binding.pointNameText.setOnEditorActionListener { v, actionId, event ->
                    if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEND ||
                        actionId == EditorInfo.IME_ACTION_NEXT ||
                        (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
                    ) {
                        // Perform your action when Enter or a similar action key is pressed
                        val inputFieldTextBody = binding.pointNameText.text.toString()
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
                    binding.pointNameLayout.visibility = View.GONE
                    binding.okButton.visibility = View.GONE

                    viewModel.editMarkerViewModel(pointPlaceMark, inputFieldTextBody)
                }
            }

            FlagAction.DELETE -> {
                mapObjectCollection.remove(mapObject)
                viewModel.removeMarkerViewModel(pointPlaceMark)
            }

            null -> throw RuntimeException("ERROR: viewModel.interfaceState - value is null")
        }
        true
    }

    private val inputListener = object : DefaultInputListener {
        override fun onMapTap(p0: Map, p1: Point) {
            when (viewModel.interfaceState.value?.radioButtonState) {
                FlagAction.CREATE -> {
                    val viewExist = view ?: return
                    val binding = FragmentMainBinding.bind(viewExist)

                    searchSession = searchManager.submit(p1, 20, SearchOptions(), searchListener)

                    binding.pointNameLayout.visibility = View.VISIBLE
                    binding.okButton.visibility = View.VISIBLE
                    binding.pointNameText.requestFocus()
                    binding.pointNameText.setOnEditorActionListener { v, actionId, event ->
                        if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_NEXT || (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                            // Perform your action when Enter or a similar action key is pressed
                            val inputFieldTextBody = binding.pointNameText.text.toString()
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

                null -> throw RuntimeException("ERROR: viewModel.interfaceState - value is null")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        println("MainFragment ======================== onCreate")

        arguments?.let {
            val gson = Gson()
            val token = TypeToken.getParameterized(Marker::class.java).type
            val string = it.getString("KEY_LIST_TO_MAIN")
            if (!string.isNullOrBlank()) {
                markerChose = gson.fromJson(string, token)
            }
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        println("MainFragment ======================== onCreateView")
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        println("MainFragment ======================== onViewCreated")

        val binding = FragmentMainBinding.bind(view)
        val yandexMap = binding.mapview.mapWindow.map
        iconMarker = createBitmapFromVector(R.drawable.baseline_add_location_48)!!

        MapKitFactory.initialize(requireContext())

        subscribeToLifecycle(binding.mapview)

        checkPermissions(binding.mapview.mapWindow)

        viewModel.data.observe(viewLifecycleOwner) { it ->
            mapObjectCollection.clear()
            it.forEach { post ->
                setMarkerInLocation(Point(post.latitude, post.longitude), post.text, iconMarker)
            }
        }
        viewModel.interfaceState.observe(viewLifecycleOwner) {
            binding.radioGroup.clearCheck()
            when (viewModel.interfaceState.value?.radioButtonState) {
                FlagAction.CREATE -> binding.radioGroup.check(R.id.button_create)
                FlagAction.EDIT -> binding.radioGroup.check(R.id.button_edit)
                FlagAction.DELETE -> binding.radioGroup.check(R.id.button_delete)
                FlagAction.OFF -> binding.radioGroup.check(R.id.button_off)
                null -> throw RuntimeException("ERROR: viewModel.interfaceState - value is null")
            }
        }

        searchResultStreetName.observe(viewLifecycleOwner, Observer<String> {
            binding.pointNameText.setText(searchResultStreetName.value)
        })

        searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.ONLINE)
        yandexMap.addInputListener(inputListener) // Добавляем слушатель тапов по карте с извлечением информации
        mapObjectCollection =
            binding.mapview.mapWindow.map.mapObjects // Инициализируем коллекцию различных объектов на карте

        moveToStartLocation()

        if (markerChose.id == -1L) {
            moveToStartLocation()

        } else {
            val point = Point(markerChose.latitude, markerChose.longitude)
            moveToLocation(point)
            markerChose = Marker()

        }

        yandexMap.addCameraListener(this)

        binding.buttonCreate.setOnClickListener {
            viewModel.setRadioButton(FlagAction.CREATE)
        }
        binding.buttonEdit.setOnClickListener {
            viewModel.setRadioButton(FlagAction.EDIT)
        }
        binding.buttonDelete.setOnClickListener {
            viewModel.setRadioButton(FlagAction.DELETE)
        }
        binding.buttonOff.setOnClickListener {
            viewModel.setRadioButton(FlagAction.OFF)
        }
        binding.buttonList.setOnClickListener {
//            val gson = Gson()
//            val bundle = Bundle()
//            bundle.putSerializable("KEY_MAIN_TO_LIST", gson.toJson(markerList))
//            findNavController().navigate(R.id.action_mainFragment_to_itemFragment, bundle)

            findNavController().navigate(R.id.action_mainFragment_to_itemFragment)
        }
        binding.myGeoposition.setOnClickListener {
            try {
                getLastKnownLocation()
            } catch (e: SecurityException) {
                throw SecurityException("SecurityException $e - no necessary permissions for Geo")
            }
        }
    }

    private fun subscribeToLifecycle(mapView: MapView) {
        viewLifecycleOwner.lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(
                source: LifecycleOwner, event: Lifecycle.Event
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
        })
    }

    private fun moveToLocation(location: Point = startLocation, zoom: Float = zoomValue) {
        val viewExist = view ?: return
        val binding = FragmentMainBinding.bind(viewExist)
        binding.mapview.mapWindow.map.move(
            CameraPosition(location, zoom, 0.0f, 0.0f), Animation(Animation.Type.SMOOTH, 5f), null
        )
    }

    private fun moveToStartLocation() = moveToLocation()

    private fun setMarkerInLocation(
        location: Point = startLocation,
        locationText: String = "Обязательно к посещению!",
        iconMarkerParam: Bitmap? = null
    ) {
        view ?: return

        var iconMarker = createBitmapFromVector(R.drawable.baseline_add_location_48)!!
        if (iconMarkerParam != null) iconMarker = iconMarkerParam

        val placemarkMapObject =
            mapObjectCollection.addPlacemark().apply { // Добавляем метку со значком
                geometry = location
                setIcon(ImageProvider.fromBitmap(iconMarker))
                opacity = 0.5f // Устанавливаем прозрачность метке
                setText(locationText)
            }
        placemarkMapObject.addTapListener(placemarkMapObjectTapListener)
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
            when {
                cameraPosition.zoom > zoomBoundary -> {
                    iconMarker = createBitmapFromVector(R.drawable.baseline_add_location_48_red)!!
                }

                cameraPosition.zoom <= zoomBoundary -> {
                    iconMarker = createBitmapFromVector(R.drawable.baseline_add_location_48_blue)!!
                }
            }
            mapObjectCollection.clear()
            viewModel.data.value?.forEach { post ->
                setMarkerInLocation(Point(post.latitude, post.longitude), post.text, iconMarker)
            } ?: throw RuntimeException("ERROR: viewModel.data - value is null")
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
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                enableUserLocation(mapWindow)

                MapKitFactory.getInstance().createLocationManager()
                    .requestSingleUpdate(object : LocationListener {
                        override fun onLocationUpdated(location: Location) {
                            println(location)
                        }

                        override fun onLocationStatusUpdated(locationStatus: LocationStatus) {
                            println(locationStatus)
                        }
                    })
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

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun getLastKnownLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    val point = Point(location.latitude, location.longitude)
                    val viewExist = view ?: return@addOnSuccessListener
                    val binding = FragmentMainBinding.bind(viewExist)

                    binding.mapview.mapWindow.map.move(
                        CameraPosition(point, 1f, 0.0f, 0.0f),
                        Animation(Animation.Type.LINEAR, 15f), null
                    )
                }
            }
    }
}