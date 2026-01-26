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
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
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
    private lateinit var drawerLayout: DrawerLayout
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var myLocationOverlay: MyLocationNewOverlay

    private var isAudioGlobalEnabled = true
    private val visitedPlaces = mutableSetOf<String>()

    data class Place(
        val name: String, val description: String,
        val lat: Double, val lon: Double,
        val audioRes: Int, val imageRes: Int, val category: String
    )

    // Твой полный список точек
    private val places = listOf(
        Place("Иезуитский коллегиум", "Величайший памятник барокко XVII века.Основан в 1631 году, достроен к 1675. Был центром образования и культуры Полесья при иезуитах. После упразднения ордена (1773) перешёл к Эдукационной комиссии. Позже здесь были гимназия, суд, училище.В здании с 1926 года находится Пинский музей Белорусского Полесья. Можно увидеть историю региона, этнографию, природу, интерьеры старой библиотеки с фресками.", 52.11098, 26.10433, R.raw.pinsk_iseut_kol, R.drawable.photo20260, "HISTORICAL"),
        Place("Францисканский монастырь", "Действующий монастырь с уникальным органом.Основан в 1396 году по привилею князя литовского Скиргайло, что делает его одним из старейших монастырей на территории Беларуси. Каменный костёл (нынешнее здание) был возведён в 1730-х годах в стиле барокко. Монастырь был важным религиозным и образовательным центром (при нём действовала школа). После восстания 1863 года царские власти закрыли монастырь, а костёл стал обычным приходским храмом. В советское время (1953 г.) здание костёла было полностью перестроено под кинотеатр, что кардинально исказило его внешний и внутренний облик.", 52.11298, 26.10828, R.raw.monastr, R.drawable.lheight, "HISTORICAL"),
        Place("Дворец Бутримовича", "Построен в 1784–1790 гг. по заказу Матеуша Бутримовича — пинского земского судьи, предпринимателя и политика. Это был подарок его дочери Анне по случаю свадьбы с Михаилом Клецким. Проект в стиле раннего классицизма приписывают придворному архитектору короля Станислава Августа К. Шильдхаузу. Дворец сразу стал центром светской жизни города, его называли «Пинским Муром» за великолепие.\n" +
                "Это первое каменное гражданское (не культовое) здание в Пинске.", 52.11469, 26.11315, R.raw.dvorec, R.drawable.photo2026014802, "HISTORICAL"),
        Place("Полесский драмтеатр", "Здание бывшего кинотеатра 'Казино'.Основан в 1940 году как Брестский областной драматический театр, хотя базировался в Пинске — уникальный случай для советской театральной системы. Во время войны не прекращал работу, давая спектакли в госпиталях и на фронтах. После 1954 года стал Пинским драматическим театром. Расцвет связан с именем Владимира Матрунчика (1939–2006) — народного артиста Беларуси, который 30 лет руководил театром, создал его узнаваемый стиль и сильную труппу. В 2006 году театру присвоено его имя.", 52.11404, 26.10802, R.raw.dramm, R.drawable.photo20260123233619, "HISTORICAL"),
        Place("76-мм орудие на постаменте", "Памятник воинам-артиллеристам.", 52.12129, 26.12137, R.raw.opydie, R.drawable.opydie, "WAR"),
        Place("Памятник освободителям", "Легендарный бронекатер БК-92 на набережной.", 52.119598, 26.121725, R.raw.luvvoic, R.drawable.photo2026011, "WAR"),
        Place("Памятник жертвам Холокоста", "Место памяти погибших в Пинском гетто.", 52.122346, 26.112711, R.raw.cholocost, R.drawable.cholocost, "WAR"),
        Place("Мемориал Партизанам", "Памятник героям полесского сопротивления.", 52.12643, 26.10173, R.raw.memorial, R.drawable.photo2026122331, "WAR")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Важная настройка для OSM
        Configuration.getInstance().load(this, getSharedPreferences("osm_prefs", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName

        setContentView(R.layout.activity_main)

        map = findViewById(R.id.map)
        infoCard = findViewById(R.id.infoCard)
        drawerLayout = findViewById(R.id.drawerLayout)

        setupMap()
        setupMenuHandlers()

        findViewById<Button>(R.id.btnClose).setOnClickListener {
            infoCard.visibility = View.GONE
            mediaPlayer?.stop()
        }

        findViewById<ImageButton>(R.id.btnOpenMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Запуск маршрута из StartActivity
        val category = intent.getStringExtra("SELECTED_CATEGORY") ?: "HISTORICAL"
        myLocationOverlay.runOnFirstFix {
            runOnUiThread { showRoute(category) }
        }
    }

    private fun setupMap() {
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.controller.setZoom(16.0)
        map.controller.setCenter(GeoPoint(52.115, 26.107))

        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), map)
        myLocationOverlay.enableMyLocation()
        myLocationOverlay.runOnFirstFix {
            myLocationOverlay.myLocationProvider.startLocationProvider { location, _ ->
                checkProximity(GeoPoint(location.latitude, location.longitude))
            }
        }
        map.overlays.add(myLocationOverlay)
    }

    private fun setupMenuHandlers() {
        findViewById<Button>(R.id.menuHistory).setOnClickListener {
            showRoute("HISTORICAL")
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        findViewById<Button>(R.id.menuWar).setOnClickListener {
            showRoute("WAR")
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        findViewById<Button>(R.id.btnExitToStart).setOnClickListener {
            mediaPlayer?.release()
            finish()
        }

        val btnAudio = findViewById<Button>(R.id.btnToggleAudio)
        btnAudio.setOnClickListener {
            isAudioGlobalEnabled = !isAudioGlobalEnabled
            if (isAudioGlobalEnabled) {
                btnAudio.text = "Звук: ВКЛ"
                btnAudio.setBackgroundColor(Color.parseColor("#4CAF50"))
            } else {
                btnAudio.text = "Звук: ВЫКЛ"
                btnAudio.setBackgroundColor(Color.parseColor("#F44336"))
                mediaPlayer?.stop()
            }
        }
    }

    private fun showRoute(category: String) {
        val filtered = places.filter { it.category == category }
        if (filtered.isEmpty()) return

        // Очистка старых маркеров и линий
        map.overlays.removeAll { it is Marker || it is Polyline && it != myLocationOverlay }

        filtered.forEach { p ->
            val marker = Marker(map)
            marker.position = GeoPoint(p.lat, p.lon)
            marker.title = p.name
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.setOnMarkerClickListener { _, _ -> displayPlaceInfo(p); true }
            map.overlays.add(marker)
        }

        thread {
            try {
                val roadManager = OSRMRoadManager(this, packageName)
                roadManager.setMean(OSRMRoadManager.MEAN_BY_FOOT)
                val waypoints = ArrayList(filtered.map { GeoPoint(it.lat, it.lon) })
                val road = roadManager.getRoad(waypoints)
                val roadOverlay = RoadManager.buildRoadOverlay(road)

                roadOverlay.outlinePaint.color = if (category == "HISTORICAL") Color.BLUE else Color.RED
                roadOverlay.outlinePaint.strokeWidth = 12f

                runOnUiThread {
                    map.overlays.add(roadOverlay)
                    map.invalidate()
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun checkProximity(currentPos: GeoPoint) {
        places.forEach { p ->
            val dist = currentPos.distanceToAsDouble(GeoPoint(p.lat, p.lon))
            if (dist < 28.0 && !visitedPlaces.contains(p.name)) {
                runOnUiThread {
                    visitedPlaces.add(p.name)
                    displayPlaceInfo(p)
                }
            }
        }
    }

    private fun displayPlaceInfo(place: Place) {
        findViewById<TextView>(R.id.infoTitle).text = place.name
        findViewById<TextView>(R.id.infoDescription).text = place.description
        findViewById<ImageView>(R.id.infoImage).setImageResource(place.imageRes)
        infoCard.visibility = View.VISIBLE

        if (isAudioGlobalEnabled) {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(this, place.audioRes)
            mediaPlayer?.start()
        }
    }

    override fun onResume() { super.onResume(); map.onResume() }
    override fun onPause() { super.onPause(); map.onPause() }
    override fun onDestroy() { super.onDestroy(); mediaPlayer?.release() }
}