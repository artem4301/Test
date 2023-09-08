package com.example.test

import android.app.Dialog
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import org.osmdroid.config.Configuration
import org.osmdroid.library.BuildConfig
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MainActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var locationOverlay: MyLocationNewOverlay
    private val startPoint = GeoPoint(55.755864, 37.617698)
    private val point1 = GeoPoint(55.749201, 37.609071)
    private val point2 = GeoPoint(55.763872, 37.606746)
    private val point3 = GeoPoint(55.753130, 37.633115)
    private val markers = listOf(point1, point2, point3) // Список маркеров
    private var currentMarkerIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeOsmdroid()
        setContentView(R.layout.activity_main)
        mapView = findViewById(R.id.mapView)
        setupMapView()
        setupLocationOverlay()
        setupMarkers()

        val imagePlus = findViewById<ImageView>(R.id.imagePlus)
        val imageMinus = findViewById<ImageView>(R.id.imageMinus)
        val imageGPS = findViewById<ImageView>(R.id.imageGPS)
        val imageNext = findViewById<ImageView>(R.id.imageNext)

        setupZoomControls(imagePlus, imageMinus)
        setupGPSButton(imageGPS)
        setupNextButton(imageNext)
    }

    private fun initializeOsmdroid() {
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
    }

    private fun setupMapView() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setUseDataConnection(true)
        mapView.setBuiltInZoomControls(false)
        mapView.controller.setZoom(19)
        mapView.controller.setCenter(startPoint)
    }

    private fun setupLocationOverlay() {
        val locationPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), mapView)
                    locationOverlay.enableMyLocation()
                    locationOverlay.enableFollowLocation()
                    locationOverlay.setPersonIcon(
                        ContextCompat.getDrawable(this, R.drawable.ic_my_tracker_46dp)!!.toBitmap()
                    )

                    mapView.overlays.add(locationOverlay)
                }
            }

        locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun setupMarkers() {
        setMarker(point1, "Иван", R.drawable.tracker1_75dp)
        setMarker(point2, "Артем", R.drawable.tracker2_75dp)
        setMarker(point3, "Лиза", R.drawable.tracker3_75dp)
    }

    private fun setMarker(geoPoint: GeoPoint, name: String, icon: Int) {
        val marker = Marker(mapView)
        marker.position = geoPoint
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        marker.icon = resources.getDrawable(icon)
        marker.title = name


        marker.setOnMarkerClickListener(object : Marker.OnMarkerClickListener {
            override fun onMarkerClick(marker: Marker?, mapView: MapView?): Boolean {
                if (marker != null) {
                    showPartialScreenPopup(marker)
                }
                return true
            }
        })

        mapView.overlays.add(marker)
        mapView.invalidate()
    }

    private fun setupZoomControls(imagePlus: ImageView, imageMinus: ImageView) {
        imagePlus.setOnClickListener {
            val currentZoomLevel = mapView.zoomLevelDouble
            mapView.controller.zoomTo(currentZoomLevel + 1)
        }
        imageMinus.setOnClickListener {
            val currentZoomLevel = mapView.zoomLevelDouble
            mapView.controller.zoomTo(currentZoomLevel - 1)
        }
    }

    private fun setupGPSButton(imageGPS: ImageView) {
        imageGPS.setOnClickListener {
            locationOverlay.let {
                mapView.controller.animateTo(it.myLocation)
                mapView.controller.setZoom(19)
            }
        }
    }

    private fun setupNextButton(imageNext: ImageView) {
        imageNext.setOnClickListener {
            if (markers.isNotEmpty()) {
                val nextMarkerIndex = (currentMarkerIndex + 1) % markers.size
                val nextMarker = markers[nextMarkerIndex]

                mapView.controller.animateTo(nextMarker)
                mapView.controller.setZoom(19)

                currentMarkerIndex = nextMarkerIndex
            }
        }
    }

    private fun showPartialScreenPopup(marker: Marker) {
        val dialog = Dialog(this, android.R.style.Theme_Light)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.info_window)

        val textViewName = dialog.findViewById<TextView>(R.id.textViewName)
        val imageViewAvatar = dialog.findViewById<ImageView>(R.id.imageViewAvatar)
        val textViewData = dialog.findViewById<TextView>(R.id.textViewData)
        val textViewTime = dialog.findViewById<TextView>(R.id.textViewTime)

        textViewName.text = marker.title
        if (marker.title.equals("Артем")){
            imageViewAvatar.setImageResource(R.drawable.tracker1)
        }
        if (marker.title.equals("Иван")){
            imageViewAvatar.setImageResource(R.drawable.tracker2)
        }
        if (marker.title.equals("Лиза")){
            imageViewAvatar.setImageResource(R.drawable.tracker3)
        }
        val currentDate = getCurrentDateInCustomFormat()
        textViewData.text = currentDate
        val currentTime = getCurrentTimeInCustomFormat()
        textViewTime.text = currentTime

        val window = dialog.window
        val layoutParams = WindowManager.LayoutParams()

        layoutParams.gravity = Gravity.BOTTOM
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
        layoutParams.height = (resources.displayMetrics.heightPixels * 0.22).toInt()
        window?.attributes = layoutParams
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        dialog.setCanceledOnTouchOutside(true)
        dialog.show()
    }

    private fun getCurrentDateInCustomFormat(): String {
        val currentDate = Date()
        val dateFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
        return dateFormat.format(currentDate)
    }

    private fun getCurrentTimeInCustomFormat(): String {
        val currentTime = Date()
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        return timeFormat.format(currentTime)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
}