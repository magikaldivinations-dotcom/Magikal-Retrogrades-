package com.magikalretrogrades.app

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.eclipticGeoMoon
import io.github.cosinekitty.astronomy.geoVector
import io.github.cosinekitty.astronomy.rotationEqjEct
import io.github.cosinekitty.astronomy.siderealTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

private val ChartInk = Color(0xFF100B18)
private val ChartPanel = Color(0xFF21172E)
private val ChartPanelDark = Color(0xFF17101F)
private val ChartGold = Color(0xFFE8C77A)
private val ChartLilac = Color(0xFFE4CBFF)
private val ChartMuted = Color(0xFFB9AFC4)
private val ChartViolet = Color(0xFFB782FF)
private val ChartSigns = listOf("♈", "♉", "♊", "♋", "♌", "♍", "♎", "♏", "♐", "♑", "♒", "♓")
private val ChartSignNames = listOf("Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo", "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces")
private val ChartBodies = listOf(
    Triple("Sun", "☉", Body.Sun), Triple("Moon", "☾", Body.Moon), Triple("Mercury", "☿", Body.Mercury),
    Triple("Venus", "♀", Body.Venus), Triple("Mars", "♂", Body.Mars), Triple("Jupiter", "♃", Body.Jupiter),
    Triple("Saturn", "♄", Body.Saturn), Triple("Uranus", "♅", Body.Uranus), Triple("Neptune", "♆", Body.Neptune),
    Triple("Pluto", "♇", Body.Pluto)
)

private data class BirthPlace(
    val name: String,
    val region: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val timeZone: String
) {
    val label: String get() = listOf(name, region, country).filter { it.isNotBlank() }.distinct().joinToString(", ")
}

private data class NatalPlacement(
    val name: String,
    val glyph: String,
    val longitude: Double,
    val signIndex: Int,
    val degreesInSign: Double,
    val house: Int?
)

private data class NatalAspect(
    val first: NatalPlacement,
    val second: NatalPlacement,
    val name: String,
    val orb: Double,
    val color: Color
)

private data class NatalChartData(
    val placements: List<NatalPlacement>,
    val ascendant: Double?,
    val midheaven: Double?,
    val aspects: List<NatalAspect>,
    val place: BirthPlace,
    val dateTimeLabel: String,
    val unknownTime: Boolean,
    val dstNote: String?
)

@Composable
internal fun NatalChartPage() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var birthDate by remember { mutableStateOf<LocalDate?>(null) }
    var birthTime by remember { mutableStateOf<LocalTime?>(null) }
    var timeUnknown by remember { mutableStateOf(false) }
    var placeQuery by remember { mutableStateOf("") }
    var choices by remember { mutableStateOf<List<BirthPlace>>(emptyList()) }
    var selectedPlace by remember { mutableStateOf<BirthPlace?>(null) }
    var searching by remember { mutableStateOf(false) }
    var calculating by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var chart by remember { mutableStateOf<NatalChartData?>(null) }

    Column(
        Modifier.fillMaxSize().background(ChartInk).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("✦ YOUR COSMIC BLUEPRINT", color = ChartGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.7.sp)
            Text("Natal birth chart", color = ChartLilac, fontSize = 28.sp, fontWeight = FontWeight.Medium)
            Text("Enter your birth date, time, and birthplace to calculate your chart.", color = ChartMuted, fontSize = 14.sp, lineHeight = 21.sp)
        }

        Surface(color = ChartPanel, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Text("BIRTH DETAILS", color = ChartGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                OutlinedButton(
                    onClick = {
                        val initial = birthDate ?: LocalDate.now()
                        DatePickerDialog(context, { _, year, month, day ->
                            birthDate = LocalDate.of(year, month + 1, day)
                            chart = null
                            message = null
                        }, initial.year, initial.monthValue - 1, initial.dayOfMonth).show()
                    }, modifier = Modifier.fillMaxWidth()
                ) { Text(birthDate?.format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault())) ?: "Choose birth date") }

                OutlinedButton(
                    onClick = {
                        val initial = birthTime ?: LocalTime.NOON
                        TimePickerDialog(context, { _, hour, minute ->
                            birthTime = LocalTime.of(hour, minute)
                            timeUnknown = false
                            chart = null
                            message = null
                        }, initial.hour, initial.minute, DateFormat.is24HourFormat(context)).show()
                    }, enabled = !timeUnknown, modifier = Modifier.fillMaxWidth()
                ) { Text(birthTime?.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())) ?: "Choose birth time") }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = timeUnknown, onCheckedChange = {
                        timeUnknown = it
                        if (it) birthTime = null
                        chart = null
                        message = null
                    }, colors = CheckboxDefaults.colors(checkedColor = ChartViolet))
                    Column(Modifier.padding(start = 4.dp)) {
                        Text("I don’t know my birth time", color = ChartLilac, fontSize = 13.sp)
                        Text("Without it, the chart uses noon and omits houses and angles.", color = ChartMuted, fontSize = 11.sp, lineHeight = 15.sp)
                    }
                }

                OutlinedTextField(
                    value = placeQuery,
                    onValueChange = { placeQuery = it; selectedPlace = null; choices = emptyList(); chart = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Birth city or place") },
                    placeholder = { Text("Example: Beaumont, Texas") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    colors = chartFieldColors()
                )
                Button(
                    onClick = {
                        searching = true; message = null; choices = emptyList(); selectedPlace = null; chart = null
                        scope.launch {
                            try { choices = searchBirthPlaces(placeQuery) }
                            catch (e: Exception) { message = e.message ?: "Could not search for that place. Try adding the state or country." }
                            finally { searching = false }
                        }
                    }, enabled = placeQuery.trim().length >= 2 && !searching,
                    modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = ChartViolet, contentColor = ChartInk)
                ) {
                    if (searching) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = ChartInk)
                    else Text("Find birthplace")
                }

                if (choices.isNotEmpty()) {
                    Text("CHOOSE THE MATCHING PLACE", color = ChartGold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    choices.forEach { candidate ->
                        OutlinedButton(onClick = {
                            selectedPlace = candidate
                            placeQuery = candidate.label
                            choices = emptyList()
                            chart = null
                        }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
                            Text("${candidate.label}\n${candidate.timeZone}", textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth(), lineHeight = 18.sp)
                        }
                    }
                }
                selectedPlace?.let { Text("Selected: ${it.label} · ${it.timeZone}", color = ChartGold, fontSize = 12.sp, lineHeight = 18.sp) }

                Button(
                    onClick = {
                        val date = birthDate
                        val time = birthTime
                        val location = selectedPlace
                        if (date == null || location == null || (!timeUnknown && time == null)) {
                            message = "Choose your birth date, birth time (or mark it unknown), and a matching birthplace."
                        } else {
                            calculating = true; message = null
                            scope.launch {
                                try {
                                    chart = withContext(Dispatchers.Default) { calculateNatalChart(date, time ?: LocalTime.NOON, location, timeUnknown) }
                                } catch (e: Exception) {
                                    message = e.message ?: "The chart could not be calculated. Check the date, time, and selected place."
                                } finally { calculating = false }
                            }
                        }
                    }, enabled = !calculating && selectedPlace != null && birthDate != null && (birthTime != null || timeUnknown),
                    modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = ChartGold, contentColor = ChartInk)
                ) {
                    if (calculating) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = ChartInk)
                    else Text("Generate free natal chart", fontWeight = FontWeight.Bold)
                }
                Text(
                    "Your birth date and time are calculated on this device. Place search sends only the place name to Open-Meteo’s geocoder to find coordinates and its time zone (location data based on GeoNames).",
                    color = ChartMuted, fontSize = 10.sp, lineHeight = 15.sp
                )
            }
        }

        message?.let {
            Surface(color = Color(0xFF382443), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Text(it, color = ChartLilac, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(14.dp))
            }
        }
        chart?.let { NatalChartResult(it) }
    }
}

@Composable
private fun chartFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = ChartLilac, unfocusedTextColor = ChartLilac,
    focusedBorderColor = ChartViolet, unfocusedBorderColor = Color(0xFF5A4668),
    focusedLabelColor = ChartGold, unfocusedLabelColor = ChartMuted,
    cursorColor = ChartGold
)

private suspend fun searchBirthPlaces(query: String): List<BirthPlace> = withContext(Dispatchers.IO) {
    val encoded = URLEncoder.encode(query.trim(), "UTF-8")
    val connection = (URL("https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=6&language=en&format=json").openConnection() as HttpURLConnection)
    connection.connectTimeout = 9000
    connection.readTimeout = 9000
    connection.requestMethod = "GET"
    try {
        if (connection.responseCode !in 200..299) throw IllegalStateException("Place search is unavailable right now. Please try again.")
        val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        val results = json.optJSONArray("results") ?: return@withContext emptyList()
        (0 until results.length()).mapNotNull { index ->
            val item = results.optJSONObject(index) ?: return@mapNotNull null
            val timeZone = item.optString("timezone")
            if (timeZone.isBlank() || !item.has("latitude") || !item.has("longitude")) return@mapNotNull null
            BirthPlace(
                name = item.optString("name"), region = item.optString("admin1"), country = item.optString("country"),
                latitude = item.optDouble("latitude"), longitude = item.optDouble("longitude"), timeZone = timeZone
            )
        }
    } finally { connection.disconnect() }
}

private fun calculateNatalChart(date: LocalDate, time: LocalTime, place: BirthPlace, unknownTime: Boolean): NatalChartData {
    val zone = try { ZoneId.of(place.timeZone) } catch (_: Exception) { throw IllegalArgumentException("The selected place did not return a usable time zone.") }
    val local = LocalDateTime.of(date, time)
    val offsets = zone.rules.getValidOffsets(local)
    if (offsets.isEmpty()) throw IllegalArgumentException("That local birth time did not occur because of a clock change. Choose another time or mark the time unknown.")
    val instant = local.toInstant(offsets.first())
    val utc = instant.atOffset(ZoneOffset.UTC)
    val astroTime = Time(utc.year, utc.monthValue, utc.dayOfMonth, utc.hour, utc.minute, utc.second.toDouble() + utc.nano / 1_000_000_000.0)

    val raw = ChartBodies.map { (name, glyph, body) ->
        val longitude = if (body == Body.Moon) {
            eclipticGeoMoon(astroTime).lon
        } else {
            val vector = rotationEqjEct(astroTime).rotate(geoVector(body, astroTime, Aberration.Corrected))
            positiveDegrees(Math.toDegrees(atan2(vector.y, vector.x)))
        }
        Triple(name, glyph, longitude)
    }

    val angles = if (!unknownTime) {
        val epsilon = meanObliquityDegrees(astroTime) + astroTime.nutationEps()
        val theta = positiveDegrees(siderealTime(astroTime) * 15.0 + place.longitude)
        val latitude = Math.toRadians(place.latitude.coerceIn(-89.8, 89.8))
        val thetaRad = Math.toRadians(theta)
        val epsilonRad = Math.toRadians(epsilon)
        val ascendant = positiveDegrees(Math.toDegrees(atan2(
            -cos(thetaRad),
            sin(epsilonRad) * tan(latitude) + cos(epsilonRad) * sin(thetaRad)
        )))
        val midheaven = positiveDegrees(Math.toDegrees(atan2(sin(thetaRad), cos(thetaRad) * cos(epsilonRad))))
        ascendant to midheaven
    } else null
    val ascSign = angles?.first?.let { floor(it / 30.0).toInt() }
    val placements = raw.map { (name, glyph, longitude) ->
        val sign = floor(positiveDegrees(longitude) / 30.0).toInt().coerceIn(0, 11)
        val house = ascSign?.let { (sign - it + 12) % 12 + 1 }
        NatalPlacement(name, glyph, positiveDegrees(longitude), sign, positiveDegrees(longitude) % 30.0, house)
    }
    val aspects = buildAspects(placements)
    val dtFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a", Locale.getDefault())
    val note = if (offsets.size > 1) "This birthplace had two possible UTC offsets at that clock time; the earlier occurrence was used." else null
    return NatalChartData(
        placements, angles?.first, angles?.second, aspects, place,
        local.format(dtFormatter) + if (unknownTime) " (time unknown; noon used)" else " ${zone.id}", unknownTime, note
    )
}

private fun meanObliquityDegrees(time: Time): Double {
    val centuries = time.tt / 36525.0
    return 23.439291111 - 0.013004167 * centuries - 0.000000164 * centuries * centuries + 0.000000504 * centuries * centuries * centuries
}

private fun positiveDegrees(value: Double): Double = ((value % 360.0) + 360.0) % 360.0

private fun buildAspects(placements: List<NatalPlacement>): List<NatalAspect> {
    val aspects = listOf(0.0 to "Conjunction", 60.0 to "Sextile", 90.0 to "Square", 120.0 to "Trine", 180.0 to "Opposition")
    val result = mutableListOf<NatalAspect>()
    for (i in placements.indices) for (j in i + 1 until placements.size) {
        val a = placements[i]; val b = placements[j]
        val difference = abs(a.longitude - b.longitude).let { minOf(it, 360.0 - it) }
        val orbLimit = if (a.name in listOf("Sun", "Moon") || b.name in listOf("Sun", "Moon")) 7.0 else 5.0
        val match = aspects.map { (angle, name) -> Triple(angle, name, abs(difference - angle)) }.minByOrNull { it.third }
        if (match != null && match.third <= orbLimit) {
            val name = match.second
            val orb = match.third
            val tint = if (name in listOf("Trine", "Sextile")) Color(0xFF86C9C2) else Color(0xFFE49A9A)
            result += NatalAspect(a, b, name, orb, tint)
        }
    }
    return result.sortedBy { it.orb }
}

@Composable
private fun NatalChartResult(chart: NatalChartData) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(color = ChartPanel, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("YOUR NATAL CHART", color = ChartGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                Text(chart.dateTimeLabel, color = ChartLilac, fontSize = 14.sp)
                Text(chart.place.label, color = ChartMuted, fontSize = 12.sp)
                Text("Tropical zodiac · Whole Sign houses", color = ChartViolet, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                if (chart.dstNote != null) Text(chart.dstNote, color = ChartGold, fontSize = 11.sp, lineHeight = 16.sp)
                if (chart.unknownTime) Text("Birth time unknown: planetary positions are calculated for local noon. The Ascendant, Midheaven, and houses are not shown because they depend on the exact time.", color = ChartMuted, fontSize = 12.sp, lineHeight = 18.sp)
                if (chart.ascendant != null && chart.midheaven != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        AngleCard("ASCENDANT", chart.ascendant, Modifier.weight(1f))
                        AngleCard("MIDHEAVEN", chart.midheaven, Modifier.weight(1f))
                    }
                }
                NatalWheel(chart, Modifier.fillMaxWidth().aspectRatio(1f))
                Text("Planetary positions", color = ChartLilac, fontSize = 19.sp, fontWeight = FontWeight.Medium)
                chart.placements.forEach { placement -> PlacementRow(placement) }
                if (chart.aspects.isNotEmpty()) {
                    Text("Major aspects", color = ChartLilac, fontSize = 19.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 5.dp))
                    chart.aspects.take(15).forEach { aspect ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${aspect.first.glyph} ${aspect.first.name}  ·  ${aspect.name}  ·  ${aspect.second.glyph} ${aspect.second.name}", color = aspect.color, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            Text("${aspect.orb.roundToInt()}° orb", color = ChartMuted, fontSize = 10.sp)
                        }
                    }
                }
                Text("Planet positions use Astronomy Engine’s on-device ephemeris. Its stated planetary-position accuracy is about ±1 arcminute. Houses use the Whole Sign system and the selected place’s time zone.", color = ChartMuted, fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun AngleCard(label: String, longitude: Double, modifier: Modifier = Modifier) {
    Surface(color = ChartPanelDark, shape = RoundedCornerShape(13.dp), modifier = modifier) {
        Column(Modifier.padding(11.dp)) {
            Text(label, color = ChartGold, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text("${ChartSignNames[floor(longitude / 30.0).toInt()]} ${degreeMinute(longitude % 30.0)}", color = ChartLilac, fontSize = 12.sp)
        }
    }
}

@Composable
private fun PlacementRow(placement: NatalPlacement) {
    Row(Modifier.fillMaxWidth().background(ChartPanelDark, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(placement.glyph, color = ChartGold, fontSize = 19.sp, modifier = Modifier.width(30.dp))
        Text(placement.name, color = ChartLilac, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text("${ChartSignNames[placement.signIndex]} ${degreeMinute(placement.degreesInSign)}", color = ChartLilac, fontSize = 12.sp, textAlign = TextAlign.End)
        placement.house?.let { Text("H$it", color = ChartViolet, fontSize = 11.sp, modifier = Modifier.width(35.dp), textAlign = TextAlign.End) }
    }
}

private fun degreeMinute(value: Double): String {
    val totalMinutes = (value * 60.0).roundToInt().coerceIn(0, 1799)
    return "${totalMinutes / 60}°${(totalMinutes % 60).toString().padStart(2, '0')}′"
}

@Composable
private fun NatalWheel(chart: NatalChartData, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * .47f
        val inner = radius * .52f
        drawCircle(Color(0xFF170F20), radius, center)
        drawCircle(ChartGold.copy(alpha = .85f), radius, center, style = Stroke(size.minDimension * .009f))
        drawCircle(Color(0xFF674D73), inner, center, style = Stroke(size.minDimension * .006f))
        val asc = chart.ascendant ?: 0.0
        fun point(longitude: Double, scale: Float): Offset {
            val angle = Math.toRadians(180.0 + longitude - asc)
            val r = radius * scale
            return Offset(center.x + (kotlin.math.cos(angle) * r).toFloat(), center.y + (kotlin.math.sin(angle) * r).toFloat())
        }
        for (i in 0..11) {
            val cusp = if (chart.ascendant == null) i * 30.0 else floor(asc / 30.0) * 30.0 + i * 30.0
            val outerPoint = point(cusp, 1f)
            val innerPoint = point(cusp, .52f)
            drawLine(Color(0xFF5A4668), innerPoint, outerPoint, strokeWidth = size.minDimension * .0035f)
            val labelPoint = point(cusp + 15.0, .82f)
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = ChartGold.toArgb(); textAlign = android.graphics.Paint.Align.CENTER; textSize = size.minDimension * .055f
                typeface = android.graphics.Typeface.create("serif", android.graphics.Typeface.NORMAL)
            }
            drawContext.canvas.nativeCanvas.drawText(ChartSigns[(floor(cusp / 30.0).toInt() % 12 + 12) % 12], labelPoint.x, labelPoint.y + paint.textSize * .34f, paint)
        }
        val aspectsToDraw = chart.aspects.take(20)
        aspectsToDraw.forEach { aspect ->
            drawLine(
                aspect.color.copy(alpha = .45f), point(aspect.first.longitude, .49f), point(aspect.second.longitude, .49f),
                strokeWidth = size.minDimension * .003f
            )
        }
        chart.placements.forEachIndexed { index, placement ->
            val p = point(placement.longitude, if (index % 2 == 0) .64f else .72f)
            drawCircle(ChartViolet, size.minDimension * .018f, p)
            drawCircle(ChartGold, size.minDimension * .009f, p)
        }
        drawCircle(ChartGold.copy(alpha = .9f), size.minDimension * .004f, center)
    }
}
