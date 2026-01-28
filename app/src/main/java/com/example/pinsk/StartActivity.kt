package com.example.pinsk

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.DashPathEffect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.viewpager2.widget.ViewPager2
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import kotlin.concurrent.thread

class StartActivity : AppCompatActivity() {

    private lateinit var previewMap: MapView
    private lateinit var locationOverlay: MyLocationNewOverlay
    private lateinit var viewPager: ViewPager2
    private val sliderHandler = Handler(Looper.getMainLooper())

    private var currentMainLine: Polyline? = null
    private var currentDashLine: Polyline? = null
    private val visitedPoints = mutableSetOf<String>()

    // Твои точки маршрутов
    private val historyPoints = listOf(
        GeoPoint(52.127968, 26.069469), GeoPoint(52.127496, 26.088354),
        GeoPoint(52.115631, 26.098187), GeoPoint(52.11385, 26.10238),
        GeoPoint(52.11098, 26.10433), GeoPoint(52.11298, 26.10828),
        GeoPoint(52.11404, 26.10802), GeoPoint(52.11469, 26.11315),
        GeoPoint(52.120089, 26.115355)
    )

    private val warPoints = listOf(
        GeoPoint(52.12643, 26.10173), GeoPoint(52.1168, 26.1095),
        GeoPoint(52.122346, 26.112711), GeoPoint(52.11934, 26.12084),
        GeoPoint(52.119598, 26.121725), GeoPoint(52.12129, 26.12137),
        GeoPoint(52.120189, 26.124274)
    )

    private val sliderRunnable = object : Runnable {
        override fun run() {
            if (::viewPager.isInitialized && viewPager.adapter != null) {
                val itemCount = viewPager.adapter!!.itemCount
                if (itemCount > 0) {
                    viewPager.currentItem = (viewPager.currentItem + 1) % itemCount
                    sliderHandler.postDelayed(this, 4000)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().userAgentValue = packageName
        Configuration.getInstance().load(this, getSharedPreferences("osm_prefs", MODE_PRIVATE))

        setContentView(R.layout.activity_start)

        setupSlider()
        setupMap()

        // Кнопки с категориями
        setupButton(findViewById(R.id.startHistory), "HISTORICAL", historyPoints)
        setupButton(findViewById(R.id.startWar), "WAR", warPoints)

        findViewById<Button>(R.id.btnVirtualTour).setOnClickListener {
            startActivity(Intent(this, PanoramaActivity::class.java))
        }
        findViewById<Button>(R.id.btnAbout).setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        checkPermissions()
    }

    private fun setupSlider() {
        viewPager = findViewById(R.id.viewPagerSlider)
        val images = listOf(
            R.drawable.costel12, R.drawable.foto1, R.drawable.costel13,
            R.drawable.foto2, R.drawable.foto3, R.drawable.foto4,
            R.drawable.foto5, R.drawable.foto6, R.drawable.foto7,
            R.drawable.foto8, R.drawable.foto9, R.drawable.foto10,
            R.drawable.foto11, R.drawable.foto12, R.drawable.foto13,
            R.drawable.foto14, R.drawable.foto15, R.drawable.v_kollege
        )
        viewPager.adapter = SliderAdapter(images)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                sliderHandler.removeCallbacks(sliderRunnable)
                sliderHandler.postDelayed(sliderRunnable, 4000)
            }
        })
    }

    private fun setupMap() {
        previewMap = findViewById(R.id.previewMap)
        previewMap.setTileSource(TileSourceFactory.MAPNIK)
        previewMap.setMultiTouchControls(true)
        previewMap.controller.setZoom(14.0)
        previewMap.controller.setCenter(GeoPoint(52.115, 26.107))

        locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), previewMap)
        locationOverlay.enableMyLocation()

        // Логика слежения как в MainActivity
        locationOverlay.runOnFirstFix {
            locationOverlay.myLocationProvider.startLocationProvider { location, _ ->
                val currentPos = GeoPoint(location.latitude, location.longitude)
                checkProximity(currentPos)
            }
        }
        previewMap.overlays.add(locationOverlay)
    }

    private fun checkProximity(currentPos: GeoPoint) {
        val allPoints = historyPoints + warPoints
        allPoints.forEach { point ->
            val dist = currentPos.distanceToAsDouble(point)
            if (dist < 30.0 && !visitedPoints.contains(point.toString())) {
                runOnUiThread {
                    visitedPoints.add(point.toString())
                    Toast.makeText(this, "Вы рядом с достопримечательностью!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupButton(button: Button, category: String, points: List<GeoPoint>) {
        val detector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                drawSmartPreview(points, category)
                return true
            }
            override fun onDoubleTap(e: MotionEvent): Boolean {
                val intent = Intent(this@StartActivity, MainActivity::class.java)
                intent.putExtra("SELECTED_CATEGORY", category)
                startActivity(intent)
                return true
            }
        })
        button.setOnTouchListener { v, event ->
            detector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP) v.performClick()
            true
        }
    }

    private fun drawSmartPreview(destinations: List<GeoPoint>, category: String) {
        thread {
            try {
                val roadManager = OSRMRoadManager(this, packageName)
                roadManager.setMean(OSRMRoadManager.MEAN_BY_FOOT)

                val userLoc = locationOverlay.myLocation
                val routeColor = if (category == "HISTORICAL") Color.BLUE else Color.RED

                // 1. Пунктир до пользователя
                var dashOverlay: Polyline? = null
                if (userLoc != null && destinations.isNotEmpty()) {
                    val roadToFirst = roadManager.getRoad(arrayListOf(userLoc, destinations[0]))
                    dashOverlay = RoadManager.buildRoadOverlay(roadToFirst)
                    dashOverlay.outlinePaint.color = Color.GRAY
                    dashOverlay.outlinePaint.strokeWidth = 10f
                    dashOverlay.outlinePaint.pathEffect = DashPathEffect(floatArrayOf(20f, 20f), 0f)
                }

                // 2. Основной маршрут
                val mainRoad = roadManager.getRoad(ArrayList(destinations))
                val mainOverlay = RoadManager.buildRoadOverlay(mainRoad)
                mainOverlay.outlinePaint.color = routeColor
                mainOverlay.outlinePaint.strokeWidth = 14f

                runOnUiThread {
                    currentMainLine?.let { previewMap.overlays.remove(it) }
                    currentDashLine?.let { previewMap.overlays.remove(it) }

                    currentDashLine = dashOverlay
                    dashOverlay?.let { previewMap.overlays.add(it) }

                    currentMainLine = mainOverlay
                    previewMap.overlays.add(mainOverlay)

                    previewMap.invalidate()
                    if (destinations.isNotEmpty()) previewMap.controller.animateTo(destinations[0])
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun checkPermissions() {
        val perms = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (ActivityCompat.checkSelfPermission(this, perms[0]) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, perms, 1)
        }
    }

    override fun onResume() {
        super.onResume()
        previewMap.onResume()
        sliderHandler.postDelayed(sliderRunnable, 4000)
    }

    override fun onPause() {
        super.onPause()
        previewMap.onPause()
        sliderHandler.removeCallbacks(sliderRunnable)
    }
}