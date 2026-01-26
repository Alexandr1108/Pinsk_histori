package com.example.pinsk

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private lateinit var infoCard: CardView
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var myLocationOverlay: MyLocationNewOverlay
    private var currentRoadOverlay: Polyline? = null
    private val visitedPlaces = mutableSetOf<String>()
    private var connectingDashOverlay: Polyline? = null
    data class Place(
        val name: String,
        val description: String,
        val lat: Double,
        val lon: Double,
        val audioRes: Int,
        val imageRes: Int,
        val category: String
    )
    private val place = listOf(
        Place("Иезуитский коллегиум", "Величайший памятник барокко XVII века.Основан в 1631 году, достроен к 1675. Был центром образования и культуры Полесья при иезуитах. После упразднения ордена (1773) перешёл к Эдукационной комиссии. Позже здесь были гимназия, суд, училище. Сейчас \n" +
                "В здании с 1926 года находится Пинский музей Белорусского Полесья. Можно увидеть историю региона, этнографию, природу, интерьеры старой библиотеки с фресками.", 52.11098, 26.10433, R.raw.kollege, R.drawable.photo20260, "HISTORICAL"),
        Place("Францисканский монастырь", "Действующий монастырь с уникальным органом.", 52.11298, 26.10828, R.raw.frank, R.drawable.lheight, "HISTORICAL"),
        Place("Дворец Бутримовича", "Построен в 1794 году, жемчужина архитектуры.", 52.11469, 26.11315, R.raw.butrimovichi, R.drawable.photo2026014802, "HISTORICAL"),
        Place("Полесский драмтеатр", "Здание бывшего кинотеатра 'Казино'.", 52.11404, 26.10802, R.raw.dram_tatr, R.drawable.photo20260123233619, "HISTORICAL"),
        Place("76-мм орудие на постаменте", "Памятник воинам-артиллеристам.", 52.12129, 26.12137, R.raw.opydie, R.drawable.opydie, "WAR"),
        Place("Памятник освободителям", "Легендарный бронекатер БК-92 на набережной.", 52.119598, 26.121725, R.raw.luvvoic, R.drawable.photo2026011, "WAR"),
        Place("Памятник жертвам Холокоста", "Место памяти погибших в Пинском гетто.", 52.122346, 26.112711, R.raw.cholocost, R.drawable.cholocost, "WAR"),
        Place("Мемориал Партизанам", "Памятник героям полесского сопротивления.", 52.12643, 26.10173, R.raw.memorial, R.drawable.photo2026122331, "WAR")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Загрузка конфигурации OSM
        Configuration.getInstance().load(this, getSharedPreferences("osm_prefs", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName
        setContentView(R.layout.activity_main)

        // Инициализация UI
        map = findViewById(R.id.map)
        infoCard = findViewById(R.id.infoCard)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.controller.setZoom(17.0)
        map.controller.setCenter(GeoPoint(52.115, 26.107))

        // Настройка GPS
        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), map)
        myLocationOverlay.enableMyLocation()
        myLocationOverlay.enableFollowLocation()

        myLocationOverlay.runOnFirstFix {
            myLocationOverlay.myLocationProvider.startLocationProvider { location, _ ->
                val currentGeoPoint = GeoPoint(location.latitude, location.longitude)
                checkProximity(currentGeoPoint)
            }

            // АВТОМАТИЧЕСКОЕ ПОСТРОЕНИЕ: Если пришли со StartActivity
            val selectedCategory = intent.getStringExtra("SELECTED_CATEGORY")
            if (selectedCategory != null) {
                runOnUiThread {
                    showRoute(selectedCategory)
                }
            }
        }
        map.overlays.add(myLocationOverlay)

        // Кнопки управления
        findViewById<Button>(R.id.btnExit).setOnClickListener {
            mediaPlayer?.stop()
            finish()
        }
        findViewById<Button>(R.id.btnClose).setOnClickListener {
            infoCard.visibility = View.GONE
            mediaPlayer?.stop()
        }

        checkPermissions()
    }

    private fun checkProximity(currentPos: GeoPoint) {
        place.forEach { place ->
            val targetPos = GeoPoint(place.lat, place.lon)
            val distance = currentPos.distanceToAsDouble(targetPos)

            if (distance < 27.0 && !visitedPlaces.contains(place.name)) {
                runOnUiThread {
                    visitedPlaces.add(place.name)
                    displayPlaceInfo(place)
                    Toast.makeText(this, "Вы прибыли: ${place.name}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showRoute(category: String) {
        visitedPlaces.clear()

        // Удаляем старые линии
        currentRoadOverlay?.let { map.overlays.remove(it) }
        connectingDashOverlay?.let { map.overlays.remove(it) }

        map.overlays.removeAll { it is Marker }
        map.overlays.add(myLocationOverlay)

        val myPos = myLocationOverlay.myLocation
        val filteredPlaces = place.filter { it.category == category }

        if (filteredPlaces.isEmpty()) return

        // Создаем маркеры для мест
        filteredPlaces.forEach { place ->
            val point = GeoPoint(place.lat, place.lon)
            val marker = Marker(map)
            marker.position = point
            marker.title = place.name
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.setOnMarkerClickListener { _, _ ->
                displayPlaceInfo(place)
                true
            }
            map.overlays.add(marker)
        }

        thread {
            try {
                val roadManager = OSRMRoadManager(this, packageName)
                roadManager.setMean(OSRMRoadManager.MEAN_BY_FOOT)

                // --- ЧАСТЬ 1: Основной маршрут (между точками достопримечательностей) ---
                val routeWaypoints = ArrayList<GeoPoint>()
                filteredPlaces.forEach { routeWaypoints.add(GeoPoint(it.lat, it.lon)) }

                val mainRoad = roadManager.getRoad(routeWaypoints)
                val mainRoadOverlay = RoadManager.buildRoadOverlay(mainRoad)
                mainRoadOverlay.outlinePaint.color = if (category == "HISTORICAL") Color.BLUE else Color.RED
                mainRoadOverlay.outlinePaint.strokeWidth = 12f

                // --- ЧАСТЬ 2: Пунктирный путь (от GPS до первой точки) ---
                var dashOverlay: Polyline? = null
                if (myPos != null) {
                    val connectingPoints = ArrayList<GeoPoint>()
                    connectingPoints.add(myPos)
                    connectingPoints.add(GeoPoint(filteredPlaces[0].lat, filteredPlaces[0].lon))

                    val connectingRoad = roadManager.getRoad(connectingPoints)
                    dashOverlay = RoadManager.buildRoadOverlay(connectingRoad)

                    // Настройка ПУНКТИРА
                    dashOverlay.outlinePaint.apply {
                        color = Color.GRAY
                        strokeWidth = 8f
                        // Массив: 20px линия, 20px пропуск
                        pathEffect = android.graphics.DashPathEffect(floatArrayOf(20f, 20f), 0f)
                    }
                }

                runOnUiThread {
                    currentRoadOverlay = mainRoadOverlay
                    map.overlays.add(mainRoadOverlay)

                    dashOverlay?.let {
                        connectingDashOverlay = it
                        map.overlays.add(it)
                    }

                    map.invalidate()
                    if (myPos != null) map.controller.animateTo(myPos)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun displayPlaceInfo(place: Place) {
        findViewById<TextView>(R.id.infoTitle).text = place.name
        findViewById<TextView>(R.id.infoDescription).text = place.description
        findViewById<ImageView>(R.id.infoImage).setImageResource(place.imageRes)
        infoCard.visibility = View.VISIBLE

        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(this, place.audioRes)
        mediaPlayer?.start()
    }

    private fun checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
    }

    override fun onResume() { super.onResume(); map.onResume(); myLocationOverlay.enableMyLocation() }
    override fun onPause() { super.onPause(); map.onPause(); myLocationOverlay.disableMyLocation() }
    override fun onDestroy() { super.onDestroy(); mediaPlayer?.release() }
}