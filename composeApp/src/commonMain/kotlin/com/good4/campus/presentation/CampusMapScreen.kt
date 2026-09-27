package com.good4.campus.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalAtm
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Mosque
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.good4.campus.domain.CampusPoint
import com.good4.campus.domain.CampusPointCategory
import com.good4.campus.domain.CampusPoints
import com.good4.campus.domain.search
import com.good4.core.presentation.BorderMuted
import com.good4.core.presentation.PrimaryGreen
import com.good4.core.presentation.LocalThemeController
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.Good4NestedScaffold
import com.good4.core.presentation.components.Good4TopBar
import com.good4.core.util.openWalkingDirections
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.campus_academic_pin
import good4.composeapp.generated.resources.campus_atm_pin
import good4.composeapp.generated.resources.campus_bank_pin
import good4.composeapp.generated.resources.campus_dormitory_pin
import good4.composeapp.generated.resources.campus_dining_pin
import good4.composeapp.generated.resources.campus_faculty_pin
import good4.composeapp.generated.resources.campus_library_pin
import good4.composeapp.generated.resources.campus_mosque_pin
import good4.composeapp.generated.resources.campus_shopping_pin
import good4.composeapp.generated.resources.campus_sports_pin
import good4.composeapp.generated.resources.campus_services_pin
import org.jetbrains.compose.resources.painterResource
import dev.sargunv.maplibrecompose.compose.ClickResult
import dev.sargunv.maplibrecompose.compose.MaplibreMap
import dev.sargunv.maplibrecompose.compose.layer.CircleLayer
import dev.sargunv.maplibrecompose.compose.layer.SymbolLayer
import dev.sargunv.maplibrecompose.compose.rememberCameraState
import dev.sargunv.maplibrecompose.compose.rememberStyleState
import dev.sargunv.maplibrecompose.compose.source.rememberGeoJsonSource
import dev.sargunv.maplibrecompose.core.CameraPosition
import dev.sargunv.maplibrecompose.core.MapOptions
import dev.sargunv.maplibrecompose.core.OrnamentOptions
import dev.sargunv.maplibrecompose.core.source.GeoJsonData
import dev.sargunv.maplibrecompose.core.source.GeoJsonOptions
import dev.sargunv.maplibrecompose.expressions.dsl.all
import dev.sargunv.maplibrecompose.expressions.dsl.asBoolean
import dev.sargunv.maplibrecompose.expressions.dsl.asNumber
import dev.sargunv.maplibrecompose.expressions.dsl.asString
import dev.sargunv.maplibrecompose.expressions.dsl.case
import dev.sargunv.maplibrecompose.expressions.dsl.condition
import dev.sargunv.maplibrecompose.expressions.dsl.const
import dev.sargunv.maplibrecompose.expressions.dsl.eq
import dev.sargunv.maplibrecompose.expressions.dsl.feature
import dev.sargunv.maplibrecompose.expressions.dsl.format
import dev.sargunv.maplibrecompose.expressions.dsl.image
import dev.sargunv.maplibrecompose.expressions.dsl.not
import dev.sargunv.maplibrecompose.expressions.dsl.offset
import dev.sargunv.maplibrecompose.expressions.dsl.span
import dev.sargunv.maplibrecompose.expressions.dsl.step
import dev.sargunv.maplibrecompose.expressions.dsl.switch
import dev.sargunv.maplibrecompose.expressions.value.SymbolAnchor
import io.github.dellisd.spatialk.geojson.Point
import io.github.dellisd.spatialk.geojson.Position
import kotlinx.coroutines.launch

private const val OPEN_FREE_MAP_STYLE = "https://tiles.openfreemap.org/styles/liberty"
private const val LABEL_MIN_ZOOM = 15f
private const val POINT_FOCUS_ZOOM = 16.5
private val LibraryGreen = Color(0xFF008556)
private val FacultyBlue = Color(0xFF3159A5)
private val AcademicPurple = Color(0xFF7048A8)
private val BankGold = Color(0xFFB77900)
private val AtmRed = Color(0xFFD64A3A)
private val ShoppingOrange = Color(0xFFE06B20)
private val MosqueTeal = Color(0xFF168A86)
private val SportsGreen = Color(0xFF2B8A4A)
private val DormitoryPink = Color(0xFFC33C78)
private val DiningBrown = Color(0xFF9C542C)

/** Chip, card and pin presentation of each category; colors match the pin drawables. */
private data class CategoryStyle(val chipLabel: String, val icon: ImageVector, val color: Color)

private fun CampusPointCategory.style(): CategoryStyle = when (this) {
    CampusPointCategory.LIBRARY -> CategoryStyle("Kütüphane", Icons.Outlined.LocalLibrary, LibraryGreen)
    CampusPointCategory.FACULTY -> CategoryStyle("Fakülteler", Icons.Outlined.School, FacultyBlue)
    CampusPointCategory.ACADEMIC_UNIT -> CategoryStyle("Akademik Birimler", Icons.Outlined.School, AcademicPurple)
    CampusPointCategory.SERVICE_BUILDING -> CategoryStyle("Hizmet Binaları", Icons.Outlined.AccountBalance, MosqueTeal)
    CampusPointCategory.DINING_HALL -> CategoryStyle("Yemekhaneler", Icons.Outlined.Restaurant, DiningBrown)
    CampusPointCategory.BANK -> CategoryStyle("Bankalar", Icons.Outlined.AccountBalance, BankGold)
    CampusPointCategory.ATM -> CategoryStyle("ATM'ler", Icons.Outlined.LocalAtm, AtmRed)
    CampusPointCategory.SHOPPING -> CategoryStyle("Çarşılar", Icons.Outlined.Storefront, ShoppingOrange)
    CampusPointCategory.MOSQUE -> CategoryStyle("Cami", Icons.Outlined.Mosque, MosqueTeal)
    CampusPointCategory.SPORTS -> CategoryStyle("Spor Alanları", Icons.Outlined.SportsSoccer, SportsGreen)
    CampusPointCategory.DORMITORY -> CategoryStyle("KYK Yurtları", Icons.Outlined.Home, DormitoryPink)
}

/** Categories whose names are drawn on the map once the user zooms in. */
private val LabelledCategories = setOf(
    CampusPointCategory.FACULTY,
    CampusPointCategory.ACADEMIC_UNIT,
    CampusPointCategory.SERVICE_BUILDING,
    CampusPointCategory.DINING_HALL
)

@Composable
fun CampusMapScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allCategoryNames = remember { CampusPointCategory.entries.map { it.name } }
    var visibleCategoryNames by rememberSaveable { mutableStateOf(allCategoryNames) }
    val visibleCategories = visibleCategoryNames.map(CampusPointCategory::valueOf).toSet()
    val allVisible = visibleCategories.size == CampusPointCategory.entries.size
    var query by rememberSaveable { mutableStateOf("") }
    var selectedPoints by remember { mutableStateOf(listOf(CampusPoints.library)) }
    var bottomCardHeight by remember { mutableStateOf(0) }
    var showFeedback by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val markers: Map<CampusPointCategory, Painter> = mapOf(
        CampusPointCategory.LIBRARY to painterResource(Res.drawable.campus_library_pin),
        CampusPointCategory.FACULTY to painterResource(Res.drawable.campus_faculty_pin),
        CampusPointCategory.ACADEMIC_UNIT to painterResource(Res.drawable.campus_academic_pin),
        CampusPointCategory.SERVICE_BUILDING to painterResource(Res.drawable.campus_services_pin),
        CampusPointCategory.DINING_HALL to painterResource(Res.drawable.campus_dining_pin),
        CampusPointCategory.BANK to painterResource(Res.drawable.campus_bank_pin),
        CampusPointCategory.ATM to painterResource(Res.drawable.campus_atm_pin),
        CampusPointCategory.SHOPPING to painterResource(Res.drawable.campus_shopping_pin),
        CampusPointCategory.MOSQUE to painterResource(Res.drawable.campus_mosque_pin),
        CampusPointCategory.SPORTS to painterResource(Res.drawable.campus_sports_pin),
        CampusPointCategory.DORMITORY to painterResource(Res.drawable.campus_dormitory_pin)
    )
    val visibleGeoJson = CampusPoints.all.filter { it.category in visibleCategories }.toGeoJson()
    val searchResults = remember(query) { CampusPoints.search(query) }

    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(latitude = 36.8965, longitude = 30.6585),
            zoom = 13.4
        )
    )
    val styleState = rememberStyleState()

    fun focusOn(point: CampusPoint) {
        if (point.category !in visibleCategories) {
            visibleCategoryNames = visibleCategoryNames + point.category.name
        }
        selectedPoints = listOf(point)
        query = ""
        focusManager.clearFocus()
        scope.launch {
            cameraState.animateTo(
                CameraPosition(
                    target = Position(latitude = point.latitude, longitude = point.longitude),
                    zoom = maxOf(cameraState.position.zoom, POINT_FOCUS_ZOOM)
                )
            )
        }
    }

    fun setVisibleCategories(categories: Set<CampusPointCategory>) {
        visibleCategoryNames = CampusPointCategory.entries.filter { it in categories }.map { it.name }
        selectedPoints = selectedPoints.filter { it.category in categories }
    }

    Good4NestedScaffold(
        modifier = modifier,
        topBar = {
            Good4TopBar(
                title = "Kampüs Haritası",
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showFeedback = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Feedback,
                            contentDescription = "Harita hakkında geri bildirim"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (showFeedback) {
            CampusMapFeedbackDialog(
                selectedPoint = selectedPoints.singleOrNull(),
                onDismiss = { showFeedback = false }
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val cardHeight = if (selectedPoints.isEmpty()) 0.dp else with(density) { bottomCardHeight.toDp() }
            val ornamentBottom = cardHeight + 20.dp
            MaplibreMap(
                styleUri = OPEN_FREE_MAP_STYLE,
                cameraState = cameraState,
                styleState = styleState,
                options = MapOptions(
                    ornamentOptions = OrnamentOptions(
                        padding = PaddingValues(start = 8.dp, top = 120.dp, end = 8.dp, bottom = ornamentBottom),
                        isScaleBarEnabled = false
                    )
                ),
                onMapClick = { _, _ ->
                    selectedPoints = emptyList()
                    focusManager.clearFocus()
                    ClickResult.Pass
                },
                modifier = Modifier.fillMaxSize()
            ) {
                val pointsSource = rememberGeoJsonSource(
                    id = "campus-points",
                    data = GeoJsonData.JsonString(visibleGeoJson),
                    options = GeoJsonOptions(
                        cluster = true,
                        clusterRadius = 36,
                        clusterMaxZoom = 15
                    )
                )
                val isCluster = feature.has("point_count")
                CircleLayer(
                    id = "campus-clusters",
                    source = pointsSource,
                    filter = isCluster,
                    color = const(PrimaryGreen),
                    radius = step(
                        feature.get("point_count").asNumber(),
                        const(17.dp),
                        10 to const(21.dp)
                    ),
                    strokeColor = const(Color.White),
                    strokeWidth = const(2.5.dp),
                    onClick = { features ->
                        val center = (features.firstOrNull()?.geometry as? Point)?.coordinates
                        if (center != null) {
                            scope.launch {
                                cameraState.animateTo(
                                    CameraPosition(
                                        target = center,
                                        zoom = minOf(cameraState.position.zoom + 2, 17.0)
                                    )
                                )
                            }
                        }
                        ClickResult.Consume
                    }
                )
                SymbolLayer(
                    id = "campus-cluster-counts",
                    source = pointsSource,
                    filter = isCluster,
                    textField = format(span(feature.get("point_count_abbreviated").asString())),
                    textFont = const(listOf("Noto Sans Regular")),
                    textSize = const(13.sp),
                    textColor = const(Color.White),
                    textAllowOverlap = const(true),
                    textIgnorePlacement = const(true)
                )
                val selectedName = selectedPoints.singleOrNull()?.name.orEmpty()
                SymbolLayer(
                    id = "campus-pins",
                    source = pointsSource,
                    filter = !isCluster,
                    iconImage = switch(
                        feature.get("category").asString(),
                        *CampusPointCategory.entries.map { category ->
                            case(category.name.lowercase(), image(markers.getValue(category)))
                        }.toTypedArray(),
                        fallback = image(markers.getValue(CampusPointCategory.FACULTY))
                    ),
                    iconSize = switch(
                        condition(feature.get("name").asString() eq const(selectedName), const(1.25f)),
                        fallback = const(1f)
                    ),
                    iconAnchor = const(SymbolAnchor.Bottom),
                    iconAllowOverlap = const(true),
                    onClick = { features ->
                        selectedPoints = features.selectedCampusPoints()
                        focusManager.clearFocus()
                        ClickResult.Consume
                    }
                )
                SymbolLayer(
                    id = "campus-labels",
                    source = pointsSource,
                    minZoom = LABEL_MIN_ZOOM,
                    filter = all(!isCluster, feature.get("labelled").asBoolean()),
                    textField = format(span(feature.get("name").asString())),
                    textFont = const(listOf("Noto Sans Regular")),
                    textSize = const(11.sp),
                    textColor = switch(
                        feature.get("category").asString(),
                        case("academic_unit", const(Color(0xFF35204F))),
                        case("service_building", const(Color(0xFF0D5D5A))),
                        case("dining_hall", const(Color(0xFF65351C))),
                        fallback = const(Color(0xFF17324D))
                    ),
                    textHaloColor = const(Color.White),
                    textHaloWidth = const(1.5.dp),
                    textAnchor = const(SymbolAnchor.Top),
                    textOffset = offset(0.em, 0.45.em),
                    textMaxWidth = const(13.em)
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
            ) {
                CampusSearchField(
                    query = query,
                    onQueryChange = { query = it },
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp)
                )
                if (query.isBlank()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        CampusFilterChip(
                            selected = allVisible,
                            onClick = { setVisibleCategories(CampusPointCategory.entries.toSet()) },
                            label = "Tümü",
                            icon = Icons.Outlined.Map,
                            color = PrimaryGreen
                        )
                        CampusPointCategory.entries.forEach { category ->
                            val style = category.style()
                            CampusFilterChip(
                                selected = !allVisible && category in visibleCategories,
                                onClick = {
                                    val next = when {
                                        // From "Tümü", the first tap shows only that category.
                                        allVisible -> setOf(category)
                                        category in visibleCategories -> visibleCategories - category
                                        else -> visibleCategories + category
                                    }
                                    setVisibleCategories(next.ifEmpty { CampusPointCategory.entries.toSet() })
                                },
                                label = "${style.chipLabel} · ${CampusPoints.all.count { it.category == category }}",
                                icon = style.icon,
                                color = style.color
                            )
                        }
                    }
                } else {
                    CampusSearchResults(
                        results = searchResults,
                        onPointClick = ::focusOn,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp)
                    )
                }
            }

            if (selectedPoints.isNotEmpty()) {
                CampusPointInfoCard(
                    points = selectedPoints,
                    onPointClick = ::focusOn,
                    onClose = { selectedPoints = emptyList() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .onSizeChanged { bottomCardHeight = it.height + with(density) { 16.dp.roundToPx() } }
                )
            }
        }
    }
}

@Composable
private fun CampusSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Fakülte, yemekhane, ATM ara", color = TextSecondary) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = TextSecondary) },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "Aramayı temizle", tint = TextSecondary)
                }
            }
        } else null,
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SurfaceDefault,
            unfocusedContainerColor = SurfaceDefault,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = BorderMuted.copy(alpha = 0.35f)
        )
    )
}

@Composable
private fun CampusSearchResults(
    results: List<CampusPoint>,
    onPointClick: (CampusPoint) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDefault),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        if (results.isEmpty()) {
            Text(
                text = "Sonuç bulunamadı",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                items(results, key = { it.name }) { point ->
                    CampusPointRow(point = point, onClick = { onPointClick(point) })
                    if (point != results.last()) {
                        HorizontalDivider(color = BorderMuted.copy(alpha = 0.2f))
                    }
                }
            }
        }
    }
}

/** Lift category colors on dark UI surfaces without changing their map pins. */
@Composable
private fun Color.onMapSurface(): Color =
    if (LocalThemeController.current.isDark) lerp(this, Color.White, 0.4f) else this

@Composable
private fun CampusPointRow(point: CampusPoint, onClick: () -> Unit) {
    val style = point.category.style()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(style.icon, contentDescription = null, tint = style.color.onMapSurface(), modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = point.name,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(text = point.category.label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

@Composable
private fun CampusFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    icon: ImageVector,
    color: Color
) {
    val contentColor = color.onMapSurface()
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
        shape = RoundedCornerShape(16.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = SurfaceDefault,
            labelColor = TextSecondary,
            iconColor = contentColor,
            selectedContainerColor = color.copy(alpha = 0.14f),
            selectedLabelColor = TextPrimary,
            selectedLeadingIconColor = contentColor
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = BorderMuted.copy(alpha = 0.35f),
            selectedBorderColor = contentColor,
            selectedBorderWidth = 1.5.dp
        )
    )
}

private fun List<CampusPoint>.toGeoJson(): String {
    val features = joinToString(separator = ",") { point ->
        """
            {
              "type": "Feature",
              "geometry": {
                "type": "Point",
                "coordinates": [${point.longitude}, ${point.latitude}]
              },
              "properties": {
                "name": "${point.name}",
                "category": "${point.category.name.lowercase()}",
                "labelled": ${point.category in LabelledCategories}
              }
            }
        """.trimIndent()
    }
    return """{"type":"FeatureCollection","features":[$features]}"""
}

private fun List<io.github.dellisd.spatialk.geojson.Feature>.selectedCampusPoints(): List<CampusPoint> {
    val selectedNames = mapNotNull { feature -> feature.getStringProperty("name") }.distinct()
    return selectedNames.mapNotNull { name -> CampusPoints.all.firstOrNull { point -> point.name == name } }
}

@Composable
private fun CampusPointInfoCard(
    points: List<CampusPoint>,
    onPointClick: (CampusPoint) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDefault),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        if (points.size == 1) {
            val point = points.single()
            val style = point.category.style()
            Column(modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Icon(imageVector = style.icon, contentDescription = null, tint = style.color.onMapSurface())
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = point.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(text = point.category.label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Kapat", tint = TextSecondary)
                    }
                }
                Button(
                    onClick = { openWalkingDirections(point.latitude, point.longitude) },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp, end = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.AutoMirrored.Outlined.DirectionsWalk, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text("Yol tarifi al", modifier = Modifier.padding(start = 8.dp))
                }
            }
        } else {
            // Overlapping pins: let the user pick one instead of listing names without actions.
            Column {
                Row(
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bu noktada ${points.size} yer var",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Kapat", tint = TextSecondary)
                    }
                }
                points.forEach { point ->
                    HorizontalDivider(color = BorderMuted.copy(alpha = 0.2f))
                    CampusPointRow(point = point, onClick = { onPointClick(point) })
                }
            }
        }
    }
}

