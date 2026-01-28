package com.example.pinsk

import android.content.Intent
import android.graphics.Color
import android.graphics.DashPathEffect
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
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
    private lateinit var infoSlider: androidx.viewpager2.widget.ViewPager2
    private val sliderHandler = android.os.Handler(android.os.Looper.getMainLooper())


    private val sliderRunnable by lazy {
        object : Runnable {
            override fun run() {
                val itemCount = infoSlider.adapter?.itemCount ?: 0
                if (itemCount > 1) {
                    val nextItem = (infoSlider.currentItem + 1) % itemCount
                    infoSlider.setCurrentItem(nextItem, true)
                    sliderHandler.postDelayed(this, 3000)
                }
            }
        }
    }
    data class Place(
        val name: String, val description: String,
        val lat: Double, val lon: Double,
        val audioRes: Int,
        val images: List<Int>,
        val category: String
    )

    private val places = listOf(
        //  ИСТОРИЯ
        Place("Кафедральный собор Святого великомученика Феодора Тирона", "Собор ведёт свою историю от деревянной церкви, известной с XVI века. Существующее каменное здание было возведено значительно позже — в 1936–1938 годах по проекту архитектора Ю. Лисецкого на месте старого храма. Его строительство в межвоенный период (в составе Польши) стало актом духовного укрепления православной общины города. Освящён во имя святого Феодора Тирона — раннехристианского мученика, почитаемого как покровитель воинов. В 1992 году храм получил статус кафедрального собора Пинской епархии Белорусского экзархата Русской православной церкви.", 52.127968, 26.069469, R.raw.sobor1, listOf(R.drawable.sobor,R.drawable.sobor1,R.drawable.sobor2, R.drawable.sobor3), "HISTORICAL"),
        Place("Кладбище «Спокойное»", "(иногда встречаются названия «Спокоевское», «Завищенское») является старейшим из сохранившихся городских некрополей Пинска. Его официальное открытие относится к концу XVIII – началу XIX века, хотя захоронения на этом месте, возможно, велись и ранее. Кладбище стало последним пристанищем для представителей разных сословий, конфессий и национальностей Пинска, отразив в себе сложную социальную ткань города в досоветский период.", 52.127496, 26.088354, R.raw.kladbicha, listOf(R.drawable.klad,R.drawable.kladbicha1,R.drawable.kladbicha2), "HISTORICAL"),
        Place("Каролинский замок", "Каролинский замок был возведён в первой половине XVIII века (около 1720-х годов) как резиденция великого гетмана литовского Михаила Сервация Вишневецкого. Замок был построен на землях деревни Каролин, которую гетман купил и назвал в честь своей жены — княгини Катажины из рода Дольских. Это была не оборонительная крепость, а репрезентативная магнатская резиденция в стиле барокко, символизирующая могущество одного из самых влиятельных родов Речи Посполитой.", 52.115631, 26.098187, R.raw.kpepoct, listOf(R.drawable.kpepoct,R.drawable.zamok1), "HISTORICAL"),
        Place("Варваринский монастырь", "Женский монастырь во имя святой великомученицы Варвары ведёт свою историю с основания в 1728 году. После упразднения ордена бригиток в XIX веке и пожара 1848 года, на средства православной церкви в 1856–1861 годах был возведён каменный Варваринский собор в русско-византийском стиле. При нём в 1885 году был учреждён православный женский монастырь, ставший важным центром духовности и благотворительности.", 52.11385, 26.10238, R.raw.barbara, listOf(R.drawable.costel12,R.drawable.costel13,R.drawable.barbara,R.drawable.barbara1), "HISTORICAL"),
        Place("Иезуитский коллегиум", "Величайший памятник барокко XVII века. Основан в 1631 году, достроен к 1675. Был центром образования и культуры Полесья при иезуитах. В здании с 1926 года находится Пинский музей Белорусского Полесья. Можно увидеть историю региона, этнографию, природу, интерьеры старой библиотеки с фресками.", 52.11098, 26.10433, R.raw.pinsk_iseut_kol, listOf(R.drawable.photo20260,R.drawable.v_kollege,R.drawable.photo_2026,R.drawable.photo_20263_28,R.drawable.kolegium,R.drawable.star_kol), "HISTORICAL"),
        Place("Памятник полешуку", "Памятник был торжественно открыт 17 июня 1997 года. Он стал одним из первых в Беларуси крупных скульптурных произведений, посвящённых собирательному образу коренного жителя региона — полешука, носителя уникальной культуры Полесья. Автор — белорусский скульптор Юрий Павлович Гумировский.", 52.1121, 26.1052, R.raw.polishyk, listOf(R.drawable.polishyk,R.drawable.foto2), "HISTORICAL"),
        Place("Францисканский монастырь", "Действующий монастырь с уникальным органом. Основан в 1396 году. Каменный костёл (нынешнее здание) был возведён в 1730-х годах в стиле барокко. В советское время (1953 г.) здание костёла было полностью перестроено под кинотеатр, что кардинально исказило его внешний и внутренний облик.", 52.11298, 26.10828, R.raw.monastr, listOf(R.drawable.lheight,R.drawable.photo_20264727,R.drawable.photo_28,R.drawable.kost_1,R.drawable.kost_2), "HISTORICAL"),
        Place("Полесский драмтеатр", "Здание бывшего кинотеатра 'Казино'. Основан в 1940 году. Расцвет связан с именем Владимира Матрунчика (1939–2006) — народного артиста Беларуси, который 30 лет руководил театром, создал его узнаваемый стиль и сильную труппу. В 2006 году театру присвоено его имя.", 52.11404, 26.10802, R.raw.dramm, listOf(R.drawable.photo20260123233619,R.drawable.photo_20260123_233638,R.drawable.teatry), "HISTORICAL"),
        Place("Дворец Бутримовича", "Построен в 1784–1790 гг. по заказу Матеуша Бутримовича. Проект в стиле раннего классицизма приписывают архитектору К. Шильдхаузу. Дворец называли «Пинским Муром» за великолепие. Это первое каменное гражданское здание в Пинске.", 52.11469, 26.11315, R.raw.dvorec, listOf(R.drawable.photo2026014802,R.drawable.dvorez,R.drawable.dvorez1,R.drawable.dvorez2), "HISTORICAL"),
        Place("Костёл Святого Карла Борромео", "Костёл был возведён в 1902–1910 годах на тогдашней окраине города. Его строительство инициировал меценат Кароль Лопатинский. Храм освящён во имя святого Карла Борромео — кардинала Милана XVI века. Это был акт укрепления католической веры в промышленном районе.", 52.120089, 26.115355, R.raw.kotel, listOf(R.drawable.kotal,R.drawable.costel14,R.drawable.costel15), "HISTORICAL"),

        //  ВОЙНА
        Place("Мемориал Партизанам", "Мемориал был открыт в 2002 году. Пинская область была краем непроходимых лесов и болот, которые стали естественной крепостью для десятков партизанских бригад. Мемориал увековечивает память тысяч бойцов и подпольщиков, сражавшихся в этом регионе.", 52.12643, 26.10173, R.raw.partizan, listOf(R.drawable.photo2026122331,R.drawable.pam,R.drawable.pam2), "WAR"),
        Place("Памятный знак жертвам Пинского гетто", "Установлен на месте, где в 1942 году находилось Пинское гетто — один из крупнейших центров уничтожения еврейского населения. В нём содержалось до 26 000 человек. Массовые расстрелы происходили в урочищах Добрая Воля и у Полесского драматического театра.", 52.1168, 26.1095, R.raw.znak, listOf(R.drawable.getto,R.drawable.png1), "WAR"),
        Place("Памятник жертвам Холокоста", "Мемориал установлен на историческом месте трагедии 1942 года. В ходе Холокоста в Пинске, где до войны евреи составляли более 70% населения, было уничтожено почти всё еврейское население города — около 26 000 человек.", 52.122346, 26.112711, R.raw.pamatnik, listOf(R.drawable.cholocost,R.drawable.pn1,R.drawable.pn2), "WAR"),
        Place("Памятник воинам интернационалистам", "Открыт в 1998 году. Посвящён жителям Пинска, погибшим в Афганистане (29 человек), а также в других локальных конфликтах (Ангола, Эфиопия). Мемориал стал актом памяти о солдатах, чья служба долгое время оставалась в тени.", 52.11934, 26.12084, R.raw.inter, listOf(R.drawable.inter,R.drawable.int1,R.drawable.int2,R.drawable.int3), "WAR"),
        Place("Памятник освободителям", "Комплекс посвящён воинам, погибшим при освобождении Пинска в июле 1944 года. Вечный огонь открыт в 1975 году, а легендарный бронекатер БК-92 установлен в 1985 году. Это место памяти о пехоте, артиллерии и моряках Днепровской флотилии.", 52.119598, 26.121725, R.raw.komplex, listOf(R.drawable.foto1,R.drawable.photo2026011,R.drawable.bp,R.drawable.ogon), "WAR"),
        Place("76-мм орудие на постаменте", "Установлено в 1975 году. Посвящено воинам-артиллеристам 61-й армии, которые в июле 1944 года освобождали Пинск. Это подлинное фронтовое орудие, один из главных «рабочих инструментов» Победы.", 52.12129, 26.12137, R.raw.opydie1, listOf(R.drawable.opydie), "WAR"),
        Place("ДОТ Молчанова", "Железобетонный пулемётный ДОТ Пинского укрепрайона («Линия Сталина»), построенный в 1938–1939 годах. Это один из немногих материально сохранившихся объектов укрепрайона. Представляет собой типичное фортификационное сооружение предвоенного периода.", 52.120189, 26.124274, R.raw.molchanov, listOf(R.drawable.molchanov,R.drawable.molchanov1), "WAR")
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(this, getSharedPreferences("osm_prefs", MODE_PRIVATE))
        setContentView(R.layout.activity_main)

        map = findViewById(R.id.map)
        infoCard = findViewById(R.id.infoCard)
        drawerLayout = findViewById(R.id.drawerLayout)
        infoSlider = findViewById(R.id.infoSlider)

        setupMap()
        setupMenuHandlers()

        findViewById<Button>(R.id.btnClose).setOnClickListener {
            infoCard.visibility = View.GONE
            mediaPlayer?.stop()
            sliderHandler.removeCallbacks(sliderRunnable)
        }

        findViewById<ImageButton>(R.id.btnOpenMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        val category = intent.getStringExtra("SELECTED_CATEGORY") ?: "HISTORICAL"
        myLocationOverlay.runOnFirstFix {
            runOnUiThread { showRoute(category) }
        }
    }

    private fun displayPlaceInfo(place: Place) {
        findViewById<TextView>(R.id.infoTitle).text = place.name
        findViewById<TextView>(R.id.infoDescription).text = place.description

        infoSlider.adapter = SliderAdapter(place.images)
        infoSlider.setCurrentItem(0, false) // Сброс на первую картинку без анимации

        infoCard.visibility = View.VISIBLE

        sliderHandler.removeCallbacks(sliderRunnable)
        if (place.images.size > 1) {
            sliderHandler.postDelayed(sliderRunnable, 3000)
        }

        if (isAudioGlobalEnabled) {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(this, place.audioRes)
            mediaPlayer?.start()
        }
    }

    private fun setupMap() {
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.controller.setZoom(15.0)
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

    private fun showRoute(category: String) {
        val filtered = places.filter { it.category == category }
        if (filtered.isEmpty()) return

        map.overlays.removeAll { it is Marker || (it is Polyline && it != myLocationOverlay) }

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

                val userLoc = myLocationOverlay.myLocation
                val destPoints = ArrayList(filtered.map { GeoPoint(it.lat, it.lon) })

                if (userLoc != null && destPoints.isNotEmpty()) {
                    val roadToFirst = roadManager.getRoad(arrayListOf(userLoc, destPoints[0]))
                    val dashOverlay = RoadManager.buildRoadOverlay(roadToFirst)
                    dashOverlay.outlinePaint.color = Color.GRAY
                    dashOverlay.outlinePaint.strokeWidth = 10f
                    dashOverlay.outlinePaint.pathEffect = DashPathEffect(floatArrayOf(20f, 20f), 0f)
                    runOnUiThread { map.overlays.add(dashOverlay) }
                }

                if (destPoints.size > 1) {
                    val mainRoad = roadManager.getRoad(destPoints)
                    val mainOverlay = RoadManager.buildRoadOverlay(mainRoad)
                    mainOverlay.outlinePaint.color = if (category == "HISTORICAL") Color.BLUE else Color.RED
                    mainOverlay.outlinePaint.strokeWidth = 14f
                    runOnUiThread { map.overlays.add(mainOverlay) }
                }
                runOnUiThread { map.invalidate() }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun checkProximity(currentPos: GeoPoint) {
        places.forEach { p ->
            val dist = currentPos.distanceToAsDouble(GeoPoint(p.lat, p.lon))
            if (dist < 30.0 && !visitedPlaces.contains(p.name)) {
                runOnUiThread {
                    visitedPlaces.add(p.name)
                    displayPlaceInfo(p)
                }
            }
        }
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
        findViewById<Button>(R.id.btnExitToStart).setOnClickListener { finish() }
        findViewById<Button>(R.id.menuVirtual).setOnClickListener {
            val intent = Intent(this, PanoramaActivity::class.java)
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        val btnAudio = findViewById<Button>(R.id.btnToggleAudio)
        btnAudio.setOnClickListener {
            isAudioGlobalEnabled = !isAudioGlobalEnabled
            if (isAudioGlobalEnabled) {
                btnAudio.text = "Звук: ВКЛ"
                btnAudio.setBackgroundColor(Color.parseColor("#4CAF50"))
            } else {
                btnAudio.text = "Звук: ВЫКЛ"
                btnAudio.setBackgroundColor(Color.BLACK)
                mediaPlayer?.stop()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
        sliderHandler.removeCallbacks(sliderRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        sliderHandler.removeCallbacks(sliderRunnable)
    }
}