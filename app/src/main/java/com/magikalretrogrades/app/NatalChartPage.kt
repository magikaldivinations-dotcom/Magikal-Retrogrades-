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

private data class NatalHouse(val number: Int, val longitude: Double)

private data class NatalChartData(
    val placements: List<NatalPlacement>,
    val houses: List<NatalHouse>,
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
    // Whole Sign house cusps begin at 0° of the rising sign.
    val houses = ascSign?.let { risingSign ->
        (0..11).map { offset -> NatalHouse(offset + 1, ((risingSign + offset) % 12) * 30.0) }
    }.orEmpty()
    val aspects = buildAspects(placements)
    val dtFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a", Locale.getDefault())
    val note = if (offsets.size > 1) "This birthplace had two possible UTC offsets at that clock time; the earlier occurrence was used." else null
    return NatalChartData(
        placements, houses, angles?.first, angles?.second, aspects, place,
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
                Text("Planetary placements & meanings", color = ChartLilac, fontSize = 19.sp, fontWeight = FontWeight.Medium)
                Text("Each entry shows the planet’s zodiac sign, exact degree, and house. Open it for a personal interpretation.", color = ChartMuted, fontSize = 12.sp, lineHeight = 17.sp)
                chart.placements.forEach { placement -> PlacementCard(placement) }
                if (chart.houses.isNotEmpty()) {
                    Text("Your 12 house cusps", color = ChartLilac, fontSize = 19.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 5.dp))
                    Text("In Whole Sign houses, each house begins at 0° of its sign. The Ascendant identifies the first house.", color = ChartMuted, fontSize = 12.sp, lineHeight = 17.sp)
                    chart.houses.forEach { house -> HouseRow(house, chart) }
                }
                if (chart.aspects.isNotEmpty()) {
                    Text("Major aspects", color = ChartLilac, fontSize = 19.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 5.dp))
                    chart.aspects.take(15).forEach { aspect ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("${aspect.first.glyph} ${aspect.first.name}  ·  ${aspect.name}  ·  ${aspect.second.glyph} ${aspect.second.name}", color = aspect.color, fontSize = 12.sp)
                                Text(aspectMeaning(aspect), color = ChartMuted, fontSize = 11.sp, lineHeight = 15.sp)
                            }
                            Text(degreeMinute(aspect.orb), color = ChartMuted, fontSize = 10.sp)
                        }
                    }
                }
                Text("Planet positions are calculated on-device with Astronomy Engine. House cusps use the Whole Sign system. Birth time and historical time-zone records can affect the Ascendant, houses, and exact degrees. Astrology is for personal reflection and entertainment.", color = ChartMuted, fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 4.dp))
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
private fun PlacementCard(placement: NatalPlacement) {
    var expanded by remember(placement.name, placement.longitude) { mutableStateOf(false) }
    Surface(color = ChartPanelDark, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), onClick = { expanded = !expanded }) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(placement.glyph, color = ChartGold, fontSize = 19.sp, modifier = Modifier.width(30.dp))
                Text(placement.name, color = ChartLilac, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text("${ChartSignNames[placement.signIndex]} ${degreeMinute(placement.degreesInSign)}", color = ChartLilac, fontSize = 12.sp, textAlign = TextAlign.End)
                placement.house?.let { Text("H$it", color = ChartViolet, fontSize = 11.sp, modifier = Modifier.width(35.dp), textAlign = TextAlign.End) }
            }
            if (expanded) {
                Text(planetInterpretation(placement), color = ChartMuted, fontSize = 12.sp, lineHeight = 18.sp)
                placement.house?.let { Text("House $it: ${houseKeywords(it)}", color = ChartGold, fontSize = 11.sp, lineHeight = 16.sp) }
            }
        }
    }
}

private data class HouseGuide(
    val title: String,
    val meaning: String,
    val topics: String,
    val reflection: String
)

private val HouseGuides = mapOf(
    1 to HouseGuide("Self, body & approach", "The first house begins with the Ascendant. It describes the way you meet life, present yourself, and initiate experience. It is a symbolic lens on temperament and vitality, not a diagnosis or a fixed description of appearance.", "Identity, self-presentation, first impressions, personal initiative, and new beginnings.", "Where do you feel most able to act as yourself, and what helps you begin?"),
    2 to HouseGuide("Resources, values & security", "The second house concerns resources you develop, use, and protect. It includes possessions and material security, as well as values and practical abilities you rely on. It does not predict income or guarantee wealth.", "Personal finances and possessions, livelihood skills, priorities, self-worth, and stability.", "Which abilities and values help you build a sense of security over time?"),
    3 to HouseGuide("Learning, communication & local life", "The third house describes everyday exchange: how information moves through your immediate world and how you learn from it. It is traditionally associated with siblings and neighbors, short journeys, and foundational learning.", "Speaking and writing, practical learning, siblings, neighbors, local travel, messages, and daily questions.", "What conversations or skills would make your everyday world clearer and more connected?"),
    4 to HouseGuide("Home, roots & private foundations", "The fourth house describes the base from which you live: home, family roots, private life, and the conditions that help you feel grounded. It can point to themes of ancestry and belonging without deciding what family relationships must be.", "Home, family roots, ancestry, privacy, emotional foundations, and belonging.", "What does feeling at home mean to you, and which foundations do you want to strengthen?"),
    5 to HouseGuide("Creativity, joy & self-expression", "The fifth house is associated with what you create and enjoy for its own sake. It includes play, romance, pleasure, children, and creative projects. Its symbolism does not promise fertility, romance, or a particular outcome.", "Creative work, hobbies, play, celebration, romance, pleasure, children, and expressive risks.", "What kind of joy or creative expression feels worth making room for?"),
    6 to HouseGuide("Daily work, service & wellbeing habits", "The sixth house describes the work of maintaining everyday life: routines, practical service, skills, and care of the body through ordinary habits. Astrology cannot assess, diagnose, or predict medical conditions; use qualified health professionals for health concerns.", "Daily schedules, work conditions, service, useful skills, maintenance, and wellbeing habits.", "Which small routine supports you, and which one could be made more sustainable?"),
    7 to HouseGuide("Partnerships & agreements", "The seventh house focuses on one-to-one encounters and the agreements that make them work. It can describe what you seek or learn through partners, clients, collaborators, and direct opponents; it does not guarantee marriage or a specific relationship outcome.", "Committed relationships, business partnerships, clients, contracts, negotiation, and open conflict.", "What do fairness, reciprocity, and clear boundaries look like in close partnerships?"),
    8 to HouseGuide("Shared resources, trust & change", "The eighth house concerns what is shared, owed, entrusted, or experienced jointly. It is also used to reflect on intimacy, vulnerability, and transitions that call for letting go or rebuilding. It does not predict death, inheritance, debt, or financial outcomes.", "Shared finances, taxes and debts, support from others, trust, intimacy, vulnerability, and major transitions.", "Where would clearer agreements or more honest communication help you feel safer sharing?"),
    9 to HouseGuide("Beliefs, higher learning & wider horizons", "The ninth house explores how you make meaning beyond familiar routines. It is linked with advanced study, philosophy, religion or spirituality, law, publishing, and long-distance travel. It describes perspectives you may explore, not a single correct belief.", "Higher education, philosophy, spiritual or religious frameworks, law, publishing, and long journeys.", "Which belief or area of study helps you grow, and what might you still want to question?"),
    10 to HouseGuide("Vocation, contribution & public life", "The tenth house describes visible responsibilities and the contribution you build over time. It is associated with vocation, reputation, authority, and long-term aims. In Whole Sign houses, the Midheaven can fall in another house, so it is shown separately from the tenth-house sign.", "Career and vocation, public role, recognition, leadership, responsibility, and long-term direction.", "What do you want to be known for, and what kind of contribution feels meaningful to you?"),
    11 to HouseGuide("Friendship, community & future aims", "The eleventh house concerns people and groups who help shape your hopes for the future. It is associated with friends, networks, communities, shared causes, and support gained through those connections.", "Friendships, groups, networks, shared ideals, patrons, and long-range goals.", "Which communities reflect your values, and what future are you willing to help build together?"),
    12 to HouseGuide("Retreat, reflection & behind-the-scenes matters", "The twelfth house is associated with solitude, restoration, private reflection, and matters not always easy to see or control. It can help explore boundaries, inner patterns, compassionate service, and spiritual practice; it does not mean someone is destined for isolation or hidden enemies.", "Rest and retreat, private burdens, inner habits, behind-the-scenes work, institutions, compassion, and spiritual reflection.", "What helps you rest, set limits, and seek support when something feels difficult to name?")
)

private val TraditionalSignRulers = listOf(
    "Mars", "Venus", "Mercury", "Moon", "Sun", "Mercury",
    "Venus", "Mars", "Jupiter", "Saturn", "Saturn", "Jupiter"
)

private val ModernCoRulers = mapOf(7 to "Pluto", 10 to "Uranus", 11 to "Neptune")

private fun signStyle(signIndex: Int): String = when (signIndex) {
    0 -> "directly, with an impulse to initiate"
    1 -> "steadily, emphasizing patience and tangible results"
    2 -> "through curiosity, ideas, and exchange"
    3 -> "with sensitivity to belonging, care, and emotional safety"
    4 -> "warmly, creatively, and with a wish to express individuality"
    5 -> "carefully, practically, and with attention to useful details"
    6 -> "through cooperation, fairness, and consideration of others"
    7 -> "intensely, privately, and with a desire to understand what lies beneath the surface"
    8 -> "with curiosity about meaning, possibility, and wider horizons"
    9 -> "deliberately, with attention to responsibility and lasting structure"
    10 -> "independently, with openness to unconventional solutions"
    else -> "imaginatively, with sensitivity to ideals and subtle impressions"
}

@Composable
private fun HouseRow(house: NatalHouse, chart: NatalChartData) {
    val signIndex = floor(house.longitude / 30.0).toInt().mod(12)
    val sign = ChartSignNames[signIndex]
    val guide = HouseGuides.getValue(house.number)
    val rulerName = TraditionalSignRulers[signIndex]
    val ruler = chart.placements.firstOrNull { it.name == rulerName }
    val coRulerName = ModernCoRulers[signIndex]
    val coRuler = coRulerName?.let { name -> chart.placements.firstOrNull { it.name == name } }
    val residents = chart.placements.filter { it.house == house.number }

    Surface(color = ChartPanelDark, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(house.number.toString().padStart(2, '0'), color = ChartGold, fontSize = 12.sp, modifier = Modifier.width(34.dp))
                Column(Modifier.weight(1f)) {
                    Text("House ${house.number} · ${guide.title}", color = ChartLilac, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("${sign} · Whole Sign house", color = ChartGold, fontSize = 11.sp)
                }
                Text("0°00′", color = ChartMuted, fontSize = 11.sp)
            }
            Text(guide.meaning, color = ChartLilac, fontSize = 12.sp, lineHeight = 18.sp)
            Text("LIFE AREAS", color = ChartGold, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(guide.topics, color = ChartMuted, fontSize = 11.sp, lineHeight = 16.sp)
            Text("${sign} colors these themes with an approach that is ${signStyle(signIndex)}.", color = ChartMuted, fontSize = 11.sp, lineHeight = 16.sp)

            if (ruler != null) {
                Text("HOUSE RULER · ${rulerName.uppercase()}", color = ChartGold, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text("${ruler.glyph} ${ruler.name} is in ${ChartSignNames[ruler.signIndex]} in House ${ruler.house}. This links House ${house.number} topics with ${houseKeywords(ruler.house ?: 1)}. ${planetInterpretation(ruler)}",
                    color = ChartMuted, fontSize = 11.sp, lineHeight = 16.sp)
            }
            if (coRuler != null) {
                Text("MODERN CO-RULER · ${coRuler.name.uppercase()}", color = ChartGold, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text("${coRuler.name} is in ${ChartSignNames[coRuler.signIndex]} in House ${coRuler.house}. This is an additional modern layer; the traditional ruler remains the primary ruler used here.", color = ChartMuted, fontSize = 11.sp, lineHeight = 16.sp)
            }

            Text("PLANETS IN THIS HOUSE", color = ChartGold, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            if (residents.isEmpty()) {
                Text("No major planet from this chart falls in this house. That does not mean this area is missing or unimportant: read its sign and ruler above alongside the rest of the chart.", color = ChartMuted, fontSize = 11.sp, lineHeight = 16.sp)
            } else {
                residents.forEach { planet ->
                    Text("${planet.glyph} ${planet.name} in ${ChartSignNames[planet.signIndex]}: ${planetInterpretation(planet)}", color = ChartMuted, fontSize = 11.sp, lineHeight = 16.sp)
                }
            }
            Text("REFLECTION", color = ChartGold, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(guide.reflection, color = ChartLilac, fontSize = 11.sp, lineHeight = 16.sp)
        }
    }
}
private fun planetInterpretation(p: NatalPlacement): String {
    val sign = ChartSignNames[p.signIndex]
    val style = when (p.signIndex % 4) {
        0 -> "direct, initiating, and responsive to fresh starts"
        1 -> "steady, grounded, and oriented toward what can be built"
        2 -> "curious, adaptable, and energized by ideas or exchange"
        else -> "intuitive, protective, and attentive to emotional undercurrents"
    }
    val theme = when (p.name) {
        "Sun" -> "identity, vitality, and self-expression"
        "Moon" -> "emotional needs, instincts, and sources of comfort"
        "Mercury" -> "thinking, learning, communication, and daily exchanges"
        "Venus" -> "affection, values, attraction, and what feels meaningful"
        "Mars" -> "initiative, desire, courage, and how you pursue what matters"
        "Jupiter" -> "growth, confidence, learning, and guiding beliefs"
        "Saturn" -> "responsibility, boundaries, patience, and lasting mastery"
        "Uranus" -> "independence, innovation, and the urge to change stale patterns"
        "Neptune" -> "imagination, ideals, compassion, and clear boundaries"
        else -> "deep change, renewal, and the work of letting go and rebuilding"
    }
    val house = p.house?.let { " In House $it, this expression is especially connected with ${houseKeywords(it)}." }
        ?: " The house cannot be established without a reliable birth time."
    return "${p.name} describes $theme. In $sign, it tends to express itself in a $style way.$house"
}

private fun houseKeywords(house: Int): String = when (house) {
    1 -> "identity, presence, and new beginnings"
    2 -> "resources, personal values, and self-reliance"
    3 -> "learning, communication, and nearby connections"
    4 -> "home, roots, family, and private life"
    5 -> "creativity, play, romance, and self-expression"
    6 -> "routines, skills, service, and wellbeing"
    7 -> "partnerships, agreements, and one-to-one relationships"
    8 -> "shared resources, trust, intimacy, and transformation"
    9 -> "beliefs, wider learning, travel, and meaning"
    10 -> "career, public contribution, and long-term direction"
    11 -> "friendships, communities, and future aims"
    else -> "rest, reflection, spiritual life, and behind-the-scenes matters"
}

private fun aspectMeaning(aspect: NatalAspect): String = when (aspect.name) {
    "Conjunction" -> "The two planetary themes combine and intensify one another."
    "Sextile" -> "The themes can support one another when you actively work with the opportunity."
    "Square" -> "The themes can create friction that invites practice, adjustment, and growth."
    "Trine" -> "The themes tend to flow together and can feel familiar or easy to access."
    else -> "The themes face one another, asking for awareness and a workable balance."
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
            val cusp = chart.houses.getOrNull(i)?.longitude ?: i * 30.0
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
