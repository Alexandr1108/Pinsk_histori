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

    private var currentMainLine: Polyline? = null
    private var currentDashLine: Polyline? = null

    private lateinit var viewPager: ViewPager2
    private val sliderHandler = Handler(Looper.getMainLooper())


    private val historyPoints = listOf(
        GeoPoint(52.127968, 26.069469), // Собор
        GeoPoint(52.127496, 26.088354), // Кладбище
        GeoPoint(52.115631, 26.098187), // Замок
        GeoPoint(52.11385, 26.10238),   // Варваринский мон.
        GeoPoint(52.11098, 26.10433),   // Коллегиум
        GeoPoint(52.11298, 26.10828),   // Францисканский мон.
        GeoPoint(52.11404, 26.10802),   // Театр
        GeoPoint(52.11469, 26.11315),   // Дворец
        GeoPoint(52.120089, 26.115355)  // Костел Карла
    )

    private val warPoints = listOf(
        GeoPoint(52.12643, 26.10173),   // Партизанам
        GeoPoint(52.1168, 26.1095),     // Знак гетто
        GeoPoint(52.122346, 26.112711), // Холокост
        GeoPoint(52.11934, 26.12084),   // Интерноционалисты
        GeoPoint(52.119598, 26.121725), // Освободители
        GeoPoint(52.12129, 26.12137),   // Орудие
        GeoPoint(52.120189, 26.124274)  // ДОТ
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(this, getSharedPreferences("osm_prefs", MODE_PRIVATE))
        setContentView(R.layout.activity_start)

        setupSlider()

        previewMap = findViewById(R.id.previewMap)
        previewMap.setTileSource(TileSourceFactory.MAPNIK)
        previewMap.setMultiTouchControls(true)
        previewMap.controller.setZoom(13.5)
        previewMap.controller.setCenter(GeoPoint(52.115, 26.107))

        locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), previewMap)
        locationOverlay.enableMyLocation()
        previewMap.overlays.add(locationOverlay)

        setupButton(findViewById(R.id.startHistory), "HISTORICAL", historyPoints, Color.BLUE)
        setupButton(findViewById(R.id.startWar), "WAR", warPoints, Color.RED)
        findViewById<Button>(R.id.btnAbout).setOnClickListener {
            val intent = Intent(this, AboutActivity::class.java)
            startActivity(intent)
        }
        checkPermissions()
    }

    private fun setupSlider() {
        viewPager = findViewById(R.id.viewPagerSlider)
        val images = listOf(
            R.drawable.costel12,
            R.drawable.foto1,
            R.drawable.costel13,
            R.drawable.foto2,
            R.drawable.foto3,
            R.drawable.foto4,
            R.drawable.foto5,
            R.drawable.foto6,
            R.drawable.foto7,
            R.drawable.foto8,
            R.drawable.foto9,
            R.drawable.foto10,
            R.drawable.foto11,
            R.drawable.foto12,
            R.drawable.foto13,
            R.drawable.foto14,
            R.drawable.foto15,
            R.drawable.v_kollege
        )
        viewPager.adapter = SliderAdapter(images)

        viewPager.setPageTransformer { page, position ->
            page.alpha = 1 - kotlin.math.abs(position)
            page.scaleY = 0.85f + (1 - kotlin.math.abs(position)) * 0.15f
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                sliderHandler.removeCallbacks(sliderRunnable)
                sliderHandler.postDelayed(sliderRunnable, 4000)
            }
        })
    }

    private val sliderRunnable = Runnable {
        val itemCount = viewPager.adapter?.itemCount ?: 0
        if (itemCount > 0) {
            viewPager.currentItem = (viewPager.currentItem + 1) % itemCount
        }
    }

    private fun drawSmartPreview(destinations: List<GeoPoint>, color: Int) {
        thread {
            try {
                val roadManager = OSRMRoadManager(this, packageName)
                roadManager.setMean(OSRMRoadManager.MEAN_BY_FOOT)
                val myLocation = locationOverlay.myLocation

                val mainRoad = roadManager.getRoad(ArrayList(destinations))
                val mainOverlay = RoadManager.buildRoadOverlay(mainRoad)
                mainOverlay.outlinePaint.color = color
                mainOverlay.outlinePaint.strokeWidth = 12f

                var dashOverlay: Polyline? = null
                if (myLocation != null) {
                    val connectPoints = arrayListOf(myLocation, destinations[0])
                    val dashRoad = roadManager.getRoad(connectPoints)
                    dashOverlay = RoadManager.buildRoadOverlay(dashRoad)
                    dashOverlay.outlinePaint.apply {
                        this.color = Color.GRAY
                        this.strokeWidth = 9f
                        this.pathEffect = DashPathEffect(floatArrayOf(25f, 15f), 0f)
                    }
                }

                runOnUiThread {
                    currentMainLine?.let { previewMap.overlays.remove(it) }
                    currentDashLine?.let { previewMap.overlays.remove(it) }

                    currentMainLine = mainOverlay
                    previewMap.overlays.add(mainOverlay)

                    dashOverlay?.let {
                        currentDashLine = it
                        previewMap.overlays.add(it)
                    }

                    previewMap.invalidate()


                    previewMap.controller.animateTo(myLocation ?: destinations[0])
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun setupButton(button: Button, category: String, points: List<GeoPoint>, color: Int) {
        val detector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                drawSmartPreview(points, color)
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

    private fun checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
    }

    override fun onResume() {
        super.onResume()
        previewMap.onResume()
        locationOverlay.enableMyLocation()
        sliderHandler.postDelayed(sliderRunnable, 4000)
    }

    override fun onPause() {
        super.onPause()
        previewMap.onPause()
        locationOverlay.disableMyLocation()
        sliderHandler.removeCallbacks(sliderRunnable)
    }
}