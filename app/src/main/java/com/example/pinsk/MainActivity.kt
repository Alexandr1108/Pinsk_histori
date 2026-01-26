package com.example.pinsk

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.DashPathEffect
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

    private val places = listOf(
        //ИСТОРИЯ
        Place("Кафедральный собор Святого великомученика Феодора Тирона", "Собор ведёт свою историю от деревянной церкви, известной с XVI века. Существующее каменное здание было возведено значительно позже — в 1936–1938 годах по проекту архитектора Ю. Лисецкого на месте старого храма. Его строительство в межвоенный период (в составе Польши) стало актом духовного укрепления православной общины города. Освящён во имя святого Феодора Тирона — раннехристианского мученика, почитаемого как покровитель воинов. В 1992 году храм получил статус кафедрального собора Пинской епархии Белорусского экзархата Русской православной церкви.", 52.127968,  26.069469, R.raw.sobor1, R.drawable.sobor, "HISTORICAL"),
        Place("Кладбище «Спокойное»", "(иногда встречаются названия «Спокоевское», «Завищенское») является старейшим из сохранившихся городских некрополей Пинска. Его официальное открытие относится к концу XVIII – началу XIX века, хотя захоронения на этом месте, возможно, велись и ранее. Кладбище стало последним пристанищем для представителей разных сословий, конфессий и национальностей Пинска, отразив в себе сложную социальную ткань города в досоветский период.", 52.127496, 26.088354, R.raw.kladbicha, R.drawable.klad, "HISTORICAL"),
        Place("Каролинский замок", "Каролинский замок был возведён в первой половине XVIII века (около 1720-х годов) как резиденция великого гетмана литовского Михаила Сервация Вишневецкого (также известного как «гетман Вишневецкий»). Замок был построен на землях деревни Каролин (позже давшей название всему предместью), которую гетман купил и назвал в честь своей жены — княгини Катажины (Катерины) из рода Дольских. Это была не оборонительная крепость, а репрезентативная магнатская резиденция («палац») в стиле барокко, символизирующая могущество одного из самых влиятельных родов Речи Посполитой.",  52.115631,  26.098187, R.raw.kpepoct, R.drawable.kpepoct, "HISTORICAL"),
        Place("Варваринский монастырь ", "Женский монастырь во имя святой великомученицы Варвары ведёт свою историю с основания в 1728 году, когда пинский маршалок (городской голова) Ян Леонович пригласил в город монашествующих сестёр ордена бригиток и передал им деревянный костёл Святой Варвары. После упразднения ордена в XIX веке и пожара 1848 года, уничтожившего деревянные постройки, на средства православной церкви и верующих в 1856–1861 годах был возведён каменный Варваринский собор в русско-византийском стиле. При нём официально в 1885 году был учреждён православный женский монастырь, ставший важным центром духовности и благотворительности.",52.11385,26.10238 , R.raw.barbara, R.drawable.barbara, "HISTORICAL"),
        Place("Иезуитский коллегиум", "Величайший памятник барокко XVII века. Основан в 1631 году, достроен к 1675. Был центром образования и культуры Полесья при иезуитах. После упразднения ордена (1773) перешёл к Эдукационной комиссии. Позже здесь были гимназия, суд, училище. В здании с 1926 года находится Пинский музей Белорусского Полесья. Можно увидеть историю региона, этнографию, природу, интерьеры старой библиотеки с фресками.", 52.11098, 26.10433, R.raw.pinsk_iseut_kol, R.drawable.photo20260, "HISTORICAL"),
        Place("Памятник полешуку", "Памятник был торжественно открыт 17 июня 1997 года во время празднования Дня города. Он стал одним из первых в Беларуси крупных скульптурных произведений, посвящённых не историческому событию или герою, а собирательному образу коренного жителя региона — полешука, носителя уникальной культуры Полесья. Автор — белорусский скульптор Юрий Павлович Гумировский.", 52.1121, 26.1052, R.raw.polishyk, R.drawable.polishyk, "HISTORICAL"),
        Place("Францисканский монастырь", "Действующий монастырь с уникальным органом. Основан в 1396 году по привилею князя литовского Скиргайло, что делает его одним из старейших монастырей на территории Беларуси. Каменный костёл (нынешнее здание) был возведён в 1730-х годах в стиле барокко. Монастырь был важным религиозным и образовательным центром (при нём действовала школа). После восстания 1863 года царские власти закрыли монастырь, а костёл стал обычным приходским храмом. В советское время (1953 г.) здание костёла было полностью перестроено под кинотеатр, что кардинально исказило его внешний и внутренний облик.", 52.11298, 26.10828, R.raw.monastr, R.drawable.lheight, "HISTORICAL"),
        Place("Полесский драмтеатр", "Здание бывшего кинотеатра 'Казино'. Основан в 1940 году как Брестский областной драматический театр, хотя базировался в Пинске — уникальный случай для советской театральной системы. Во время войны не прекращал работу, давая спектакли в госпиталях и на фронтах. После 1954 года стал Пинским драматическим театром. Расцвет связан с именем Владимира Матрунчика (1939–2006) — народного артиста Беларуси, который 30 лет руководил театром, создал его узнаваемый стиль и сильную труппу. В 2006 году театру присвоено его имя.", 52.11404, 26.10802, R.raw.dramm, R.drawable.photo20260123233619, "HISTORICAL"),
        Place("Дворец Бутримовича", "Построен в 1784–1790 гг. по заказу Матеуша Бутримовича — пинского земского судьи, предпринимателя и политика. Это был подарок его дочери Анне по случаю свадьбы с Михаилом Клецким. Проект в стиле раннего классицизма приписывают придворному архитектору короля Станислава Августа К. Шильдхаузу. Дворец сразу стал центром светской жизни города, его называли «Пинским Муром» за великолепие.Это первое каменное гражданское (не культовое) здание в Пинске.", 52.11469, 26.11315, R.raw.dvorec, R.drawable.photo2026014802, "HISTORICAL"),
        Place("Костёл Святого Карла Борромео ", "Костёл был возведён сравнительно поздно, в 1902–1910 годах, на тогдашней окраине города — Каролинской слободе (ныне район «Каролин»). Его строительство инициировал и финансировал местный предприниматель и меценат Кароль Лопатинский, владелец пивоваренного завода. Храм освящён во имя святого Карла (Карло) Борромео — кардинала и архиепископа Милана XVI века, деятеля Контрреформации, покровителя катехизаторов. Это был акт укрепления католической веры в промышленном районе, а также дань уважения небесному покровителю основателя.", 52.120089, 26.115355 , R.raw.kotel, R.drawable.kotal, "HISTORICAL"),
       //ВОЙНА
        Place("Мемориал Партизанам", "Памятник героям полесского сопротивления.Мемориал был торжественно открыт в 2002 году. Он посвящён не просто отдельным бойцам, а целому народному феномену — массовому партизанскому движению на территории Белорусского Полесья в годы Великой Отечественной войны (1941-1944 гг.). Пинская область (нынешняя Брестская) была краем непроходимых лесов и болот, которые стали естественной крепостью для десятков партизанских бригад и отрядов. Мемориал увековечивает память тысяч бойцов и подпольщиков, сражавшихся в этом регионе.", 52.12643, 26.10173, R.raw.partizan, R.drawable.photo2026122331, "WAR"),
        Place("Памятный знак жертвам Пинского гетто", "Памятный знак установлен на месте, где в годы Великой Отечественной войны (1941–1944) находилось Пинское гетто — один из крупнейших центров уничтожения еврейского населения в Беларуси. Гетто было создано нацистами в мае 1942 года и просуществовало до конца октября того же года. В нём содержалось до 26 000 человек. Массовые расстрелы узников происходили в урочищах Добрая Воля и у Полесского драматического театра. Знак был установлен в 1990-е годы по инициативе уцелевших узников и их потомков как акт исторической памяти и скорби.", 52.1168, 26.1095, R.raw.znak, R.drawable.getto, "WAR"),
        Place("Памятник жертвам Холокоста", "Место памяти погибших в Пинском гетто.Мемориал установлен на историческом месте трагедии 1942 года. В ходе Холокоста в Пинске, где до войны евреи составляли более 70% населения, было уничтожено почти всё еврейское население города — около 26 000 человек.", 52.122346, 26.112711, R.raw.pamatnik, R.drawable.cholocost, "WAR"),
        Place("Памятник воинам  интернационалистам", "Памятник был открыт в 1998 году по инициативе и на средства общественной организации «Белорусский союз ветеранов войны в Афганистане» и родственников погибших. Он посвящён жителям Пинска и Пинского района, погибшим при исполнении интернационального долга за пределами Беларуси. В первую очередь — 29 воинам-афганцам, а также тем, кто пал в других локальных конфликтах (Ангола, Эфиопия и др.). Мемориал стал важным актом общественного признания и памяти о солдатах, чья служба и гибель долгое время оставались в тени официальной истории.", 52.11934,26.12084, R.raw.inter, R.drawable.inter, "WAR"),
        Place("Памятник освободителям", "Комплекс был создан в два этапа и посвящён воинам, погибшим при освобождении Пинска и Пинской области в июле 1944 года.Вечный огонь и центральный памятник были торжественно открыты 9 мая 1975 года к 30-летию Победы.Бронекатер БК-92 был установлен рядом в 1985 году к 40-летию Победы, дополнив и обогатив комплекс.Это место памяти о всех родах войск, сражавшихся за город: пехоте, артиллерии, лётчиках и, что уникально, о моряках Днепровской военной флотилии, чья роль в боях за Полесье была исключительно важна.", 52.119598, 26.121725, R.raw.komplex, R.drawable.photo2026011, "WAR"),
        Place("76-мм орудие на постаменте", "Памятник был установлен в 1975 году в честь 30-летия Победы в Великой Отечественной войне. Он посвящён воинам-артиллеристам 61-й армии 1-го Белорусского фронта, которые в июле 1944 года огнём своих орудий прокладывали путь пехоте и танкам в боях за освобождение Пинска и Пинской области. Это не символическая скульптура, а подлинное фронтовое орудие, один из главных «рабочих инструментов» Победы.", 52.12129, 26.12137, R.raw.opydie1, R.drawable.opydie, "WAR"),
        Place("ДОТ Молчанова", "Этот железобетонный пулемётный ДОТ (долговременная огневая точка) является частью Пинского укрепрайона (Пинского УРа), входившего в систему советских приграничных укреплений, известную как «Линия Сталина». Строительство укрепрайона велось в 1938–1939 годах на западной границе СССР для прикрытия важного Пинского операционного направления. После присоединения Западной Беларуси к СССР в 1939 году и переноса границы укрепления утратили актуальность, а их оборудование было снято. Название «ДОТ Молчанова» — неофициальное, данное по фамилии местного жителя или краеведа.Это один из немногих материально сохранившихся объектов Пинского укрепрайона, доступных для осмотра. Он представляет собой типичное небольшое пехотное фортификационное сооружение предвоенного периода, рассчитанное на круговую оборону силами гарнизона в несколько человек. Его ценность — в подлинности и наглядности как памятника военно-инженерной мысли конца 1930-х годов.", 52.120189, 26.124274, R.raw.molchanov, R.drawable.molchanov, "WAR"),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(this, getSharedPreferences("osm_prefs", MODE_PRIVATE))
        setContentView(R.layout.activity_main)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }

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

        val category = intent.getStringExtra("SELECTED_CATEGORY") ?: "HISTORICAL"
        myLocationOverlay.runOnFirstFix {
            runOnUiThread { showRoute(category) }
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
            finish()
        }

        val btnAudio = findViewById<Button>(R.id.btnToggleAudio)
        btnAudio.setBackgroundColor(Color.parseColor("#4CAF50"))
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

    private fun showRoute(category: String) {
        val filtered = places.filter { it.category == category }
        if (filtered.isEmpty()) return

        // Удаляем все старые слои, кроме GPS-метки
        map.overlays.removeAll { it is Marker || (it is Polyline && it != myLocationOverlay) }

        // Расставляем маркеры
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

                // 1. ШТРИХОВАЯ ЛИНИЯ (СЕРАЯ) ОТ ВАС ДО ПЕРВОЙ ТОЧКИ
                if (userLoc != null && destPoints.isNotEmpty()) {
                    val roadToFirst = roadManager.getRoad(arrayListOf(userLoc, destPoints[0]))
                    val dashOverlay = RoadManager.buildRoadOverlay(roadToFirst)
                    dashOverlay.outlinePaint.color = Color.GRAY
                    dashOverlay.outlinePaint.strokeWidth = 10f
                    dashOverlay.outlinePaint.pathEffect = DashPathEffect(floatArrayOf(20f, 20f), 0f)
                    runOnUiThread { map.overlays.add(dashOverlay) }
                }

                // 2. СПЛОШНАЯ ЛИНИЯ МЕЖДУ ТОЧКАМИ МАРШРУТА
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