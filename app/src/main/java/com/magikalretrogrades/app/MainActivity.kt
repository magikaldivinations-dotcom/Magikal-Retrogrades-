package com.magikalretrogrades.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.location.LocationListener
import android.os.Bundle
import android.os.Build
import android.os.Looper
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.roundToInt
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.illumination as moonIllumination
import io.github.cosinekitty.astronomy.moonPhase
import io.github.cosinekitty.astronomy.moonQuartersAfter
import io.github.cosinekitty.astronomy.searchMoonPhase

private val Ink = Color(0xFF100B18)
private val Panel = Color(0xFF21172E)
private val PanelLight = Color(0xFF30213F)
private val Violet = Color(0xFFB782FF)
private val Lilac = Color(0xFFE4CBFF)
private val Muted = Color(0xFFB9AFC4)
private val Gold = Color(0xFFE8C77A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(16, 11, 24)
        window.navigationBarColor = android.graphics.Color.rgb(16, 11, 24)
        setContent { MagikalApp() }
    }
}

private enum class Page(val label: String, val icon: String) {
    Retrogrades("Retrogrades", "☿"), Moon("Moon phases", "☾"), Wheel("Sabbats", "✺"), Journal("Journal", "✎"), Chart("Natal chart", "☉")
}

@Composable
private fun MagikalApp() {
    var page by remember { mutableStateOf(Page.Retrogrades) }
    val scheme = darkColorScheme(
        primary = Violet, onPrimary = Ink, secondary = Gold, background = Ink,
        surface = Panel, onBackground = Lilac, onSurface = Lilac
    )
    MaterialTheme(colorScheme = scheme) {
        Scaffold(
            containerColor = Ink,
            topBar = {
                AppHeader()
            },
            bottomBar = {
                Column(Modifier.fillMaxWidth().background(Color(0xFF17101F))) {
                    TripleMoonMark(Modifier.align(Alignment.CenterHorizontally).padding(top = 5.dp, bottom = 1.dp).size(width = 96.dp, height = 38.dp))
                    NavigationBar(containerColor = Color(0xFF17101F), contentColor = Violet) {
                        Page.entries.forEach { item ->
                            NavigationBarItem(
                                selected = page == item,
                                onClick = { page = item },
                                icon = { Text(item.icon, fontSize = 19.sp) },
                                label = { Text(item.label, fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = Lilac, selectedTextColor = Lilac,
                                    indicatorColor = PanelLight, unselectedIconColor = Muted, unselectedTextColor = Muted)
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                when (page) {
                    Page.Retrogrades -> RetrogradesPage()
                    Page.Moon -> MoonPage()
                    Page.Wheel -> WheelPage()
                    Page.Journal -> JournalPage()
                    Page.Chart -> NatalChartPage()
                }
            }
        }
    }
}

@Composable
private fun AppHeader() {
    Column(Modifier.fillMaxWidth().background(Ink).padding(horizontal = 16.dp, vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            BrandStar(Modifier.size(40.dp))
            Column(Modifier.padding(start = 7.dp), horizontalAlignment = Alignment.Start) {
                Text("Magikal Retrogrades", color = Lilac, fontSize = 24.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Cursive,
                    fontWeight = FontWeight.Medium, maxLines = 1)
            }
        }
        HorizontalDivider(color = Color(0xFF493650), thickness = 1.dp, modifier = Modifier.padding(top = 5.dp))
    }
}

@Composable
private fun BrandStar(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val orbit = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * .06f, h * .84f)
            cubicTo(w * .01f, h * .98f, w * .20f, h * .93f, w * .42f, h * .79f)
            cubicTo(w * .67f, h * .63f, w * .92f, h * .42f, w * .79f, h * .29f)
            cubicTo(w * .72f, h * .22f, w * .68f, h * .18f, w * .69f, h * .06f)
        }
        drawPath(orbit, Gold, style = androidx.compose.ui.graphics.drawscope.Stroke(width = (w * .045f).coerceAtLeast(1.4f), cap = androidx.compose.ui.graphics.StrokeCap.Round))
        fun star(cx: Float, cy: Float, rx: Float, ry: Float) = androidx.compose.ui.graphics.Path().apply {
            moveTo(cx, cy - ry); quadraticTo(cx - rx * .13f, cy - ry * .13f, cx - rx, cy)
            quadraticTo(cx - rx * .13f, cy + ry * .13f, cx, cy + ry)
            quadraticTo(cx + rx * .13f, cy + ry * .13f, cx + rx, cy)
            quadraticTo(cx + rx * .13f, cy - ry * .13f, cx, cy - ry); close()
        }
        drawPath(star(w * .68f, h * .30f, w * .23f, h * .29f), Lilac)
        drawPath(star(w * .63f, h * .61f, w * .17f, h * .21f), Violet)
    }
}

@Composable
private fun TripleMoonMark(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = (size.minDimension * .035f).coerceAtLeast(1.2f), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        val mark = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * .08f, h * .08f)
            cubicTo(w * .39f, h * .08f, w * .39f, h * .92f, w * .08f, h * .92f)
            moveTo(w * .08f, h * .08f)
            cubicTo(w * .28f, h * .22f, w * .28f, h * .78f, w * .08f, h * .92f)
            moveTo(w * .92f, h * .08f)
            cubicTo(w * .61f, h * .08f, w * .61f, h * .92f, w * .92f, h * .92f)
            moveTo(w * .92f, h * .08f)
            cubicTo(w * .72f, h * .22f, w * .72f, h * .78f, w * .92f, h * .92f)
        }
        drawPath(mark, Lilac, style = stroke)
        drawCircle(Lilac, radius = h * .41f, center = androidx.compose.ui.geometry.Offset(w * .5f, h * .5f), style = stroke)
        drawCircle(Lilac, radius = h * .018f, center = androidx.compose.ui.geometry.Offset(w * .56f, h * .36f))
    }
}

@Composable
private fun PageColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 10.dp), content = content)
}

private data class Planet(
    val name: String, val glyph: String, val horizonsId: String?, val colors: List<Color>,
    val meaning: String, val retro: String, val practice: String, val ritual: String,
    val materials: String, val steps: List<String>, val spell: String, val avoid: String, val crystal: String
)
private val planets = listOf(
    Planet("Mercury", "☿", "199", listOf(Color(0xFFC6B7A6), Color(0xFF665D64)), "Communication, learning, and exchange", "Review conversations, plans, and details; leave room to revise.", "Pause before sending an important message and double-check travel plans.", "Write down one conversation you want to revisit, then draft a calmer version.", "Journal, a purple pen, and a glass of water.", listOf("Set the water beside your journal and take three slow breaths.", "Write the message or plan you want to review; underline the detail that needs checking.", "Draft one clear next question or correction before taking action."), "May I listen clearly, speak thoughtfully, and make room to review.", "Avoid rushing agreements or assuming a message was understood.", "Fluorite"),
    Planet("Venus", "♀", "299", listOf(Color(0xFFFFA77D), Color(0xFF8E3A65)), "Love, values, beauty, and connection", "Reflect on what you value and how you nurture relationships.", "Reconnect with a creative practice or write about your needs.", "Place rose quartz beside a written list of what care and respect look like to you.", "Rose quartz or another keepsake, paper, and a pen.", listOf("Write three qualities you want in a caring relationship, including how you want to treat yourself.", "Circle one value you can practice this week.", "Name one kind boundary that protects that value."), "May care, honesty, and self-respect guide my choices.", "Avoid making a major purchase just to soothe a passing feeling.", "Rose quartz"),
    Planet("Mars", "♂", "499", listOf(Color(0xFFFF765E), Color(0xFF7D263D)), "Drive, courage, and action", "Review how you use energy, desire, and assertiveness.", "Choose steady movement and a clear, manageable goal.", "Take three steady breaths, then choose one safe action that uses your energy well.", "Red paper, a pen, and an optional red jasper stone.", listOf("Write one goal that matters to you.", "Break it into a first step small enough to do safely.", "Schedule a pause or rest period alongside the action."), "May courage move with care and clear intention.", "Avoid escalating conflict or forcing progress through exhaustion.", "Red jasper"),
    Planet("Jupiter", "♃", "599", listOf(Color(0xFFF5C38A), Color(0xFF684C86)), "Growth, wisdom, and possibility", "Revisit beliefs, learning, and the promises you have made yourself.", "Study a subject you love and celebrate quiet progress.", "Choose a book or lesson and learn without rushing the outcome.", "A book, notebook, and an optional amethyst stone.", listOf("Write a belief or goal you want to reconsider.", "List one thing you have learned and one open question.", "Choose a realistic next step and a date to review it."), "May wisdom keep my growth generous and grounded.", "Avoid overcommitting or treating optimism as a guarantee.", "Amethyst"),
    Planet("Saturn", "♄", "699", listOf(Color(0xFFD2C2A2), Color(0xFF454047)), "Boundaries, responsibility, and structure", "Review commitments and strengthen foundations patiently.", "Tidy one small area or create a realistic routine.", "Clear one small surface and decide what belongs there.", "Paper, a pen, and a small stone as a reminder.", listOf("List the commitments currently using your time.", "Mark one boundary or obligation that needs review.", "Write one realistic routine step you can maintain."), "May steady boundaries support a life with room to breathe.", "Avoid harsh self-judgment or taking on every burden alone.", "Smoky quartz"),
    Planet("Uranus", "♅", "799", listOf(Color(0xFF9CE7DF), Color(0xFF396D88)), "Change, originality, and freedom", "Notice where you want more honesty, space, or experimentation.", "Try a low-stakes new approach and record what surprises you.", "Sketch an unconventional solution to a small problem, then test it in a reversible way.", "Paper, colored pencils, and an optional labradorite stone.", listOf("Choose a small routine you want to improve.", "Sketch two fresh approaches, including the simplest one.", "Test one reversible change and note what happens."), "May thoughtful change make space for honest freedom.", "Avoid abrupt changes made only to escape discomfort.", "Labradorite"),
    Planet("Neptune", "♆", "899", listOf(Color(0xFF9CB4FF), Color(0xFF524178)), "Dreams, intuition, and imagination", "Reflect on inspiration, boundaries, and what feels unclear.", "Keep a dream note and make time for a gentle creative ritual.", "Record impressions without treating them as proof or instruction.", "A journal, a blue or violet pencil, and a glass of water.", listOf("Write an image, dream, or idea without judging it.", "Separate what you observed from what you imagine it means.", "Pair the idea with one practical action you can verify."), "May imagination be gentle, clear, and grounded.", "Avoid ignoring practical details or treating a hunch as proof.", "Lepidolite"),
    Planet("Pluto", "♇", "999", listOf(Color(0xFF9A79C7), Color(0xFF34253F)), "Transformation, depth, and renewal", "Explore what is ready to change and what you are ready to release.", "Write a private release note and choose one supportive next step.", "Write one habit or expectation you are ready to loosen; keep the note private or tear it up when finished.", "Dark paper, a pen, and a bowl for holding the note.", listOf("Name one pattern you are ready to examine, without shaming yourself.", "Write what support or boundary would make change safer.", "Choose one small step; keep or recycle the note when you are ready."), "May I meet change with patience, support, and choice.", "Avoid pressuring yourself or others into sudden transformation.", "Obsidian"),
    Planet("Chiron", "⚷", "DES=2060", listOf(Color(0xFFB8A6CA), Color(0xFF5D4B70)), "Vulnerability, learning, and compassionate repair", "Reflect on old sensitivities with care and notice where support helps you grow.", "Name a lesson you have learned and one boundary that protects your wellbeing.", "Write about a challenge only as much as feels safe today, then name what helped you through it.", "A journal, a soft-colored pen, and an optional comforting keepsake.", listOf("Write about a challenge only as much as feels safe today.", "Note what support, skill, or kindness helped you through it.", "Choose one small act of care you can offer yourself this week."), "May I meet tender places with patience, support, and compassion.", "Avoid reopening painful memories without support or treating a crystal as a substitute for care.", "Rose quartz")
)

private const val DISPLAY_ZONE_PREFS = "magikal_display_preferences"
private const val DISPLAY_ZONE_KEY = "display_time_zone"
private fun getDisplayZone(context: Context): java.time.ZoneId {
    val saved = context.getSharedPreferences(DISPLAY_ZONE_PREFS, Context.MODE_PRIVATE).getString(DISPLAY_ZONE_KEY, null)
    return runCatching { java.time.ZoneId.of(saved ?: java.time.ZoneId.systemDefault().id) }.getOrDefault(java.time.ZoneId.systemDefault())
}
private fun saveDisplayZone(context: Context, zoneId: String) {
    context.getSharedPreferences(DISPLAY_ZONE_PREFS, Context.MODE_PRIVATE).edit().putString(DISPLAY_ZONE_KEY, zoneId).apply()
}

@Composable
private fun DisplayZonePicker(selectedId: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val zones = remember { java.util.TimeZone.getAvailableIDs().distinct().sorted() }
    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text("DISPLAY TIME ZONE · LOCATION", color = Gold, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
        Box(Modifier.fillMaxWidth().padding(top = 5.dp)) {
            Surface(color = PanelLight, shape = RoundedCornerShape(13.dp), modifier = Modifier.fillMaxWidth().clickable { expanded = true }) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(selectedId.replace('_', ' '), color = Lilac, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text("▾", color = Gold, fontSize = 18.sp)
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                zones.forEach { zone ->
                    DropdownMenuItem(text = { Text(zone.replace('_', ' '), maxLines = 1) }, onClick = { onSelect(zone); expanded = false })
                }
            }
        }
        Text("Choose any region. Retrograde and Moon event times convert to this time zone in both hemispheres.", color = Muted, fontSize = 9.sp, lineHeight = 13.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun RetrogradesPage() {
    var openPlanet by remember { mutableStateOf<String?>(null) }
    var feed by remember { mutableStateOf(RetrogradeTracker.fallbackFeed()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    var displayZoneId by remember { mutableStateOf(getDisplayZone(context).id) }
    val displayZone = runCatching { java.time.ZoneId.of(displayZoneId) }.getOrDefault(java.time.ZoneId.systemDefault())
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1_000)
        }
    }
    LaunchedEffect(refreshKey) {
        loading = true
        error = null
        try {
            feed = RetrogradeTracker.load(context, planets.mapNotNull { planet -> planet.horizonsId?.let { TrackerPlanet(planet.name, it) } }, forceRefresh = refreshKey > 0)
        } catch (failure: Exception) {
            feed = RetrogradeTracker.fallbackFeed(listOf("Using the built-in station calendar; refresh when online to check for updated ephemeris data."))
            error = null
        } finally {
            loading = false
        }
    }
    PageColumn {
        SectionIntro("Planetary retrogrades", "Explore each planet's symbolism and reflective practices.")
        DisplayZonePicker(displayZoneId) { zone -> displayZoneId = zone; saveDisplayZone(context, zone) }
        InfoCard("LIVE PLANET TRACKER", "Dates are calculated from geocentric planetary motion. Station instants are shared worldwide; displayed times use ${displayZone.id}.")
        when {
            loading -> InfoCard("UPDATING THE SKY", "Fetching planetary positions from NASA/JPL Horizons…")
            error != null -> InfoCard("TRACKING TEMPORARILY UNAVAILABLE", "${error}. Tap Refresh dates to try again. Your guide content is still available offline.")
            feed != null -> {
                val updated = formatLocalEventTime(feed!!.fetchedAtMillis, displayZone)
                InfoCard("DATA UPDATED", "Last refreshed $updated. Station times are estimated from two-day ephemeris samples.")
                if (feed!!.warnings.isNotEmpty()) {
                    InfoCard("SOME PLANETS DIDN'T UPDATE", feed!!.warnings.joinToString(" · "))
                }
            }
        }
        val activeNow = feed?.events.orEmpty().filter { it.startUtcMillis <= nowMillis && it.endUtcMillis > nowMillis }.sortedBy { it.startUtcMillis }
        val upcomingNow = feed?.events.orEmpty().filter { it.startUtcMillis > nowMillis }.sortedBy { it.startUtcMillis }
        when {
            activeNow.isNotEmpty() -> InfoCard("CURRENT RETROGRADES", activeNow.joinToString("\n") { "${it.planet}${if (it.zodiacSign.isNotBlank()) " in ${it.zodiacSign}" else ""} · ends ${formatLocalEventTime(it.endUtcMillis, displayZone)} · ${formatCountdown(it.endUtcMillis - nowMillis)} remaining" })
            upcomingNow.isNotEmpty() -> InfoCard("NO RETROGRADES RIGHT NOW · NEXT", upcomingNow.take(3).joinToString("\n") { "${it.planet}${if (it.zodiacSign.isNotBlank()) " in ${it.zodiacSign}" else ""} · starts ${formatLocalEventTime(it.startUtcMillis, displayZone)} · in ${formatCountdown(it.startUtcMillis - nowMillis)}" })
            loading -> InfoCard("CURRENT RETROGRADES", "Updating station dates in ${displayZone.id}. The built-in calendar keeps countdowns visible while data refreshes.")
            error != null -> InfoCard("TRACKING TEMPORARILY UNAVAILABLE", "${error}. Built-in current and upcoming dates remain visible.")
            feed != null -> InfoCard("NO CURRENT OR UPCOMING RETROGRADES", "No station was found in the available forecast.")
        }
        Button(onClick = { refreshKey++ }, enabled = !loading, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Violet, contentColor = Ink), shape = RoundedCornerShape(14.dp)) {
            Text(if (loading) "Updating…" else "Refresh dates", fontWeight = FontWeight.Bold)
        }
        planets.forEach { planet ->
            val periods = feed?.events?.filter { it.planet == planet.name }?.sortedBy { it.startUtcMillis }.orEmpty()
            val active = periods.firstOrNull { it.startUtcMillis <= nowMillis && it.endUtcMillis > nowMillis }
            val next = periods.firstOrNull { it.startUtcMillis > nowMillis }
            Surface(color = Panel, shape = RoundedCornerShape(19.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable { openPlanet = if (openPlanet == planet.name) null else planet.name }) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PlanetOrb(planet, Modifier.size(52.dp))
                        Column(Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(planet.name, color = Lilac, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                            Text(planet.meaning, color = Muted, fontSize = 11.sp)
                            if (loading && feed == null) Text("Checking current motion…", color = Violet, fontSize = 10.sp)
                                                        else if (feed != null) Text(
                                when {
                                    active != null -> "RETROGRADE NOW · ends ${formatLocalEventTime(active.endUtcMillis, displayZone)}"
                                    next != null -> "NEXT · starts ${formatLocalEventTime(next.startUtcMillis, displayZone)}"
                                    else -> "No station found in the 2-year forecast"
                                }, color = if (active != null) Gold else Violet, fontSize = 10.sp, lineHeight = 14.sp
                            )
                        }
                        Text(planet.glyph, color = Violet, fontSize = 24.sp)
                    }
                    if (planet.horizonsId != null && feed != null) {
                        HorizontalDivider(color = PanelLight, modifier = Modifier.padding(vertical = 10.dp))
                        when {
                            active != null -> {
                                EventCountdown("CURRENT RETROGRADE", active, nowMillis, displayZone, isActive = true)
                                val following = periods.firstOrNull { it.startUtcMillis >= active.endUtcMillis }
                                if (following != null) {
                                    Spacer(Modifier.height(10.dp))
                                    EventCountdown("NEXT RETROGRADE", following, nowMillis, displayZone, isActive = false)
                                }
                            }
                            next != null -> EventCountdown("NEXT RETROGRADE", next, nowMillis, displayZone, isActive = false)
                            else -> Text("No upcoming station was found in the current two-year forecast.", color = Muted, fontSize = 11.sp)
                        }
                    }
                    if (openPlanet == planet.name) {
                        HorizontalDivider(color = PanelLight, modifier = Modifier.padding(vertical = 12.dp))
                        InfoLine("SPIRITUAL MEANING", planet.meaning)
                        InfoLine("RETROGRADE REFLECTION", planet.retro)
                        InfoLine("PRACTICE", planet.practice)
                        InfoLine("RITUAL", planet.ritual)
                        InfoLine("WHAT YOU NEED", planet.materials)
                        planet.steps.forEachIndexed { index, step -> InfoLine("RITUAL STEP ${index + 1}", step) }
                        InfoLine("SPELL / INTENTION", planet.spell)
                        InfoLine("WHAT NOT TO DO", planet.avoid)
                        InfoLine("CRYSTAL", "${planet.crystal} (a symbolic companion, not a substitute for care)")
                    }
                }
            }
        }
    }
}

@Composable
private fun EventCountdown(title: String, event: RetrogradeEvent, nowMillis: Long, zone: java.time.ZoneId, isActive: Boolean) {
    Text(title + if (event.zodiacSign.isNotBlank()) " · ${event.zodiacSign}" else " · zodiac updates with live ephemeris", color = Gold, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
    TimelineMoment("${if (isActive) "Started" else "Starts"} · ${formatLocalEventTime(event.startUtcMillis, zone)}",
        if (isActive) "${formatCountdown(nowMillis - event.startUtcMillis)} ago" else "In ${formatCountdown(event.startUtcMillis - nowMillis)}")
    Spacer(Modifier.height(6.dp))
    TimelineMoment("Ends · ${formatLocalEventTime(event.endUtcMillis, zone)}", "In ${formatCountdown(event.endUtcMillis - nowMillis)}")
}

@Composable
private fun TimelineMoment(label: String, countdown: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Lilac, fontSize = 10.sp, lineHeight = 14.sp, modifier = Modifier.weight(1f))
        Text(countdown, color = Violet, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
    }
}

private fun formatCountdown(milliseconds: Long): String {
    val totalSeconds = (milliseconds.coerceAtLeast(0L) / 1_000L)
    val days = totalSeconds / 86_400L
    val hours = (totalSeconds % 86_400L) / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (days > 0) "${days}d ${hours}h ${minutes}m ${seconds}s" else "${hours}h ${minutes}m ${seconds}s"
}

@Composable
private fun PlanetOrb(planet: Planet, modifier: Modifier = Modifier) {
    val sprite = ImageBitmap.imageResource(R.drawable.planet_sprite)
    val index = mapOf("Mercury" to 0, "Venus" to 1, "Mars" to 2, "Jupiter" to 3, "Saturn" to 4, "Uranus" to 5, "Neptune" to 6, "Pluto" to 7, "Chiron" to 8)[planet.name] ?: 0
    val tile = sprite.width / 3
    Canvas(modifier) {
        drawImage(image = sprite, srcOffset = IntOffset((index % 3) * tile, (index / 3) * tile),
            srcSize = IntSize(tile, tile), dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.toInt(), size.height.toInt()))
    }
}

private data class MoonInfo(
    val name: String, val illumination: Int, val advice: String, val ritual: String,
    val materials: String, val steps: List<String>, val avoid: String
)
private data class MoonPhaseEvent(val name: String, val atMillis: Long)
private data class MoonSnapshot(val phase: Double, val info: MoonInfo, val ageDays: Double, val upcoming: List<MoonPhaseEvent>)
private data class MoonPosition(val altitude: Double, val azimuth: Double, val distanceKm: Double, val zodiac: String, val rightAscension: Double, val declination: Double)
private data class MoonHorizon(val riseMillis: Long?, val setMillis: Long?, val transitMillis: Long?, val transitAltitude: Double)
private val moonPhaseNames = listOf("New Moon", "Waxing Crescent", "First Quarter", "Waxing Gibbous", "Full Moon", "Waning Gibbous", "Last Quarter", "Waning Crescent")

private fun moonSnapshot(): MoonSnapshot {
    val nowMillis = System.currentTimeMillis()
    val now = Time((nowMillis - 946_728_000_000L) / 86_400_000.0)
    val phaseAngle = moonPhase(now)
    val phase = phaseAngle / 360.0
    val phaseIndex = floor(((phaseAngle + 22.5) % 360.0) / 45.0).toInt().coerceIn(0, 7)
    val illuminatedPercent = (moonIllumination(Body.Moon, now).phaseFraction * 100.0).roundToInt().coerceIn(0, 100)
    val previousNewMoon = searchMoonPhase(0.0, now, -40.0)
    val ageDays = previousNewMoon?.let { (nowMillis - it.toMillisecondsSince1970()) / 86_400_000.0 } ?: 0.0
    val upcoming = moonQuartersAfter(now).take(4).map { event ->
        val name = listOf("New Moon", "First Quarter", "Full Moon", "Last Quarter").getOrElse(event.quarter) { "Moon phase" }
        MoonPhaseEvent(name, event.time.toMillisecondsSince1970())
    }
    return MoonSnapshot(phase, moonGuidance(moonPhaseNames[phaseIndex], illuminatedPercent), ageDays, upcoming)
}

private fun moonPositionAt(timeMillis: Long, latitude: Double, longitude: Double): MoonPosition {
    val rad = Math.PI / 180.0
    val d = timeMillis / 86_400_000.0 - 10_957.5
    val l = rad * (218.316 + 13.176396 * d)
    val m = rad * (134.963 + 13.064993 * d)
    val f = rad * (93.272 + 13.229350 * d)
    val lambda = l + rad * 6.289 * sin(m)
    val beta = rad * 5.128 * sin(f)
    val distanceKm = 385_001.0 - 20_905.0 * cos(m)
    val obliquity = rad * 23.4397
    val rightAscension = atan2(sin(lambda) * cos(obliquity) - kotlin.math.tan(beta) * sin(obliquity), cos(lambda))
    val declination = kotlin.math.asin(sin(beta) * cos(obliquity) + cos(beta) * sin(lambda) * sin(obliquity))
    val latitudeRad = latitude * rad
    val hourAngle = rad * (280.16 + 360.9856235 * d + longitude) - rightAscension
    val altitude = kotlin.math.asin(sin(latitudeRad) * sin(declination) + cos(latitudeRad) * cos(declination) * cos(hourAngle)) / rad
    val azimuth = ((atan2(sin(hourAngle), cos(hourAngle) * sin(latitudeRad) - kotlin.math.tan(declination) * cos(latitudeRad)) / rad + 180.0) % 360.0 + 360.0) % 360.0
    val eclipticLongitude = ((atan2(sin(rightAscension) * cos(obliquity) + kotlin.math.tan(declination) * sin(obliquity), cos(rightAscension)) / rad % 360.0) + 360.0) % 360.0
    val signs = listOf("Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo", "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces")
    return MoonPosition(altitude, azimuth, distanceKm, signs[(eclipticLongitude / 30.0).toInt().coerceIn(0, 11)], rightAscension, declination)
}

private fun moonBrightLimbAngle(timeMillis: Long, latitude: Double, longitude: Double): Double {
    val rad = Math.PI / 180.0
    val d = timeMillis / 86_400_000.0 - 10_957.5
    val l = rad * (280.46 + .9856474 * d)
    val g = rad * (357.528 + .9856003 * d)
    val solarLongitude = l + rad * (1.915 * sin(g) + .020 * sin(2 * g))
    val obliquity = rad * (23.4393 - .0000004 * d)
    val sunRa = atan2(cos(obliquity) * sin(solarLongitude), cos(solarLongitude))
    val sunDec = kotlin.math.asin(sin(obliquity) * sin(solarLongitude))
    val moon = moonPositionAt(timeMillis, latitude, longitude)
    val delta = sunRa - moon.rightAscension
    val brightLimb = atan2(cos(sunDec) * sin(delta), sin(sunDec) * cos(moon.declination) - cos(sunDec) * sin(moon.declination) * cos(delta))
    val hourAngle = rad * (280.16 + 360.9856235 * d + longitude) - moon.rightAscension
    val phi = latitude * rad
    val parallactic = atan2(sin(hourAngle), kotlin.math.tan(phi) * cos(moon.declination) - sin(moon.declination) * cos(hourAngle))
    return (brightLimb - parallactic) / rad
}

@Suppress("DEPRECATION")
private fun requestCurrentObserverLocation(context: Context, callback: (Location?) -> Unit) {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val provider = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .firstOrNull { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
    if (provider == null) { callback(lastKnownObserverLocation(context)); return }
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            manager.getCurrentLocation(provider, null, context.mainExecutor) { location ->
                callback(location ?: lastKnownObserverLocation(context))
            }
        } else {
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    runCatching { manager.removeUpdates(this) }
                    callback(location)
                }
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            }
            manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
        }
    } catch (_: SecurityException) { callback(lastKnownObserverLocation(context)) }
      catch (_: Exception) { callback(lastKnownObserverLocation(context)) }
}

private fun moonHorizon(nowMillis: Long, latitude: Double, longitude: Double): MoonHorizon {
    val threshold = -0.3
    val sampleStep = 30L * 60L * 1000L
    var previousTime = nowMillis
    var previousAltitude = moonPositionAt(previousTime, latitude, longitude).altitude - threshold
    var rise: Long? = null
    var set: Long? = null
    var transit: Long? = null
    var maxAltitude = -90.0
    var t = nowMillis
    while (t <= nowMillis + 48L * 60L * 60L * 1000L) {
        val altitude = moonPositionAt(t, latitude, longitude).altitude
        if (t <= nowMillis + 24L * 60L * 60L * 1000L && altitude > maxAltitude) { maxAltitude = altitude; transit = t }
        val adjusted = altitude - threshold
        if (previousAltitude <= 0.0 && adjusted > 0.0 && rise == null) {
            val fraction = (previousAltitude / (previousAltitude - adjusted)).coerceIn(0.0, 1.0)
            rise = previousTime + (sampleStep * fraction).toLong()
        } else if (previousAltitude >= 0.0 && adjusted < 0.0 && set == null) {
            val fraction = (previousAltitude / (previousAltitude - adjusted)).coerceIn(0.0, 1.0)
            set = previousTime + (sampleStep * fraction).toLong()
        }
        previousAltitude = adjusted
        previousTime = t
        t += sampleStep
    }
    return MoonHorizon(rise, set, transit, maxAltitude)
}

private fun lastKnownObserverLocation(context: Context): Location? = try {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    manager.getProviders(true).mapNotNull { provider -> manager.getLastKnownLocation(provider) }
        .minByOrNull { it.accuracy }
} catch (_: SecurityException) { null } catch (_: Exception) { null }

private fun moonGuidance(name: String, illumination: Int): MoonInfo = when (name) {
    "New Moon" -> MoonInfo(name, illumination, "Set gentle intentions and make room for a fresh beginning.", "Intention-seed ritual", "A small seed, a cup or pot with soil, water, and paper with a pen.", listOf("Write one intention in a single clear sentence.", "Plant the seed or place it beside the note as a symbol of beginning.", "Choose one small action you can take this week and date it."), "Avoid demanding immediate results from a new goal.")
    "Waxing Crescent" -> MoonInfo(name, illumination, "Nurture plans with curiosity and steady effort.", "First-step candle or LED ritual", "A candle used safely or an LED candle, paper, and a pen.", listOf("Write the next small step toward a goal.", "Light the candle safely, or switch on the LED candle, while naming the step.", "Place the note where it can remind you to follow through."), "Avoid comparing your beginning with someone else's progress.")
    "First Quarter" -> MoonInfo(name, illumination, "Meet a challenge with a thoughtful choice.", "Two-path clarity ritual", "Two small stones or objects, paper, and a pen.", listOf("Name one obstacle without judging yourself.", "Set one object beside each practical way forward.", "Choose the safer next step and write when you will try it."), "Avoid treating every delay as a sign to give up.")
    "Waxing Gibbous" -> MoonInfo(name, illumination, "Refine, prepare, and tend what is already growing.", "Intention tune-up", "Your original intention, a journal, and a pen.", listOf("Read an intention you have been working with.", "Underline what still fits and cross out what needs changing.", "Write one useful adjustment and put it into practice."), "Avoid overworking a plan that already needs rest.")
    "Full Moon" -> MoonInfo(name, illumination, "Celebrate, notice what is illuminated, and release what feels complete.", "Gratitude and release ritual", "A bowl of water, paper, a pen, and an optional LED candle.", listOf("Write three things you appreciate and one thing you are ready to release.", "Read the gratitude aloud; tear or recycle the release note.", "Pour the water into a plant or down the sink, then choose a restful closing moment."), "Avoid making emotionally charged choices in haste.")
    "Waning Gibbous" -> MoonInfo(name, illumination, "Share gratitude and integrate what you have learned.", "Lesson and thanks ritual", "A journal, a pen, and an optional cup of tea.", listOf("Write one lesson from the past month.", "Name a person, place, or practice that supported you.", "Choose one way to carry that learning forward."), "Avoid holding on to a routine that no longer serves you.")
    "Last Quarter" -> MoonInfo(name, illumination, "Clear space and reconsider what deserves your energy.", "Gentle clearing ritual", "A cloth, a small bowl, paper, and a pen.", listOf("Clear one small surface or tidy one drawer.", "Write one boundary that would protect your time or energy.", "Keep the note in your journal and revisit it next week."), "Avoid turning reflection into self-criticism.")
    else -> MoonInfo("Waning Crescent", illumination, "Rest, restore, and prepare for a quieter reset.", "Quiet reset ritual", "A comfortable blanket, water or calming tea, and a journal if useful.", listOf("Settle somewhere comfortable and take several unhurried breaths.", "Write down one thing you can leave until later.", "Choose one restorative activity and give yourself time for it."), "Avoid filling every moment before the next cycle begins.")
}

@Composable
private fun MoonOrb(illumination: Int, phaseName: String, modifier: Modifier = Modifier) {
    Box(modifier.clip(CircleShape)) {
        Image(painterResource(R.drawable.moon_globe), contentDescription = "Still cratered lunar surface", contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize())
        Canvas(Modifier.fillMaxSize().clip(CircleShape)) {
            val r = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val lit = illumination.coerceIn(0, 100) / 100f
            if (lit < .98f) {
                val waxing = phaseName.startsWith("Waxing") || phaseName == "First Quarter"
                val offset = lit * r * 2f
                val shadowX = center.x + if (waxing) offset else -offset
                drawCircle(Brush.radialGradient(listOf(Color(0xFF30283A).copy(alpha = .96f), Color(0xFF15101B)),
                    center = Offset(shadowX, center.y), radius = r * 1.65f), r, Offset(shadowX, center.y))
            }
            drawCircle(Color.White.copy(alpha = .16f), r, center, style = Stroke(width = 1.dp.toPx()))
        }
    }
}

@Composable
private fun MoonPage() {
    var snapshot by remember { mutableStateOf(moonSnapshot()) }
    var selectedPhase by remember { mutableStateOf(snapshot.info.name) }
    var observerLocation by remember { mutableStateOf<Location?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    var displayZoneId by remember { mutableStateOf(getDisplayZone(context).id) }
    val displayZone = runCatching { java.time.ZoneId.of(displayZoneId) }.getOrDefault(java.time.ZoneId.systemDefault())
    var locationAccessDenied by remember { mutableStateOf(false) }
    var showLocationSettings by remember { mutableStateOf(false) }
    var requestingLocation by remember { mutableStateOf(false) }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true || grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            requestingLocation = true
            requestCurrentObserverLocation(context) { location -> observerLocation = location; requestingLocation = false; locationAccessDenied = location == null }
        } else { requestingLocation = false; locationAccessDenied = true; showLocationSettings = true }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            snapshot = moonSnapshot()
        }
    }
    LaunchedEffect(snapshot.info.name) { selectedPhase = snapshot.info.name }
    val selectedIsCurrent = selectedPhase == snapshot.info.name
    val selectedInfo = if (selectedIsCurrent) snapshot.info else moonGuidance(selectedPhase, approximateMidIllumination(selectedPhase))
    PageColumn {
        SectionIntro("Moon phases", "Explore every phase, its spiritual themes, and practices to try.")
        DisplayZonePicker(displayZoneId) { zone -> displayZoneId = zone; saveDisplayZone(context, zone) }
        Surface(color = Panel, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("MOON AT A GLANCE", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                Text(formatLocalEventTime(System.currentTimeMillis(), displayZone), color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp, bottom = 10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("LUNAR AGE" to "${"%.1f".format(snapshot.ageDays)} days", "ILLUMINATION" to "${snapshot.info.illumination}%").forEach { (label, value) ->
                        Column(Modifier.weight(1f).background(PanelLight, RoundedCornerShape(12.dp)).padding(10.dp)) {
                            Text(label, color = Gold, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
                            Text(value, color = Lilac, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
                Text("CURRENT PHASE BY HEMISPHERE", color = Gold, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp, modifier = Modifier.padding(top = 12.dp))
                Text(if (observerLocation == null) "The lunar phase is the same worldwide. Allow location access to calculate the Moon’s local viewing angle for your area." else "Location-based view updated for your device. The phase is shared worldwide; the Moon’s orientation is calculated for this place and time.", color = Muted, fontSize = 10.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 4.dp, bottom = 7.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Northern hemisphere" to 1, "Southern hemisphere" to -1).forEach { (hemisphereName, sign) ->
                        Surface(color = PanelLight, shape = RoundedCornerShape(13.dp), modifier = Modifier.weight(1f)) {
                            Row(Modifier.padding(9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val latitude = observerLocation?.latitude?.let { kotlin.math.abs(it) * sign } ?: (35.0 * sign)
                                val longitude = observerLocation?.longitude ?: 0.0
                                val viewAngle = moonBrightLimbAngle(System.currentTimeMillis(), latitude, longitude)
                                MoonOrb(snapshot.info.illumination, snapshot.info.name, Modifier.size(38.dp).graphicsLayer(rotationZ = viewAngle.toFloat()))
                                Column {
                                    Text(hemisphereName, color = Gold, fontSize = 9.sp, lineHeight = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text(snapshot.info.name, color = Lilac, fontSize = 11.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 3.dp))
                                    Text("${snapshot.info.illumination}% illuminated", color = Muted, fontSize = 8.sp, lineHeight = 11.sp)
                                }
                            }
                        }
                    }
                }
                val nextFull = snapshot.upcoming.firstOrNull { it.name == "Full Moon" }
                val nextNew = snapshot.upcoming.firstOrNull { it.name == "New Moon" }
                if (nextFull != null) InfoLine("NEXT FULL MOON", formatLocalEventTime(nextFull.atMillis, displayZone))
                if (nextNew != null) InfoLine("NEXT NEW MOON", formatLocalEventTime(nextNew.atMillis, displayZone))
                Text("NEXT PHASES", color = Gold, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp, modifier = Modifier.padding(top = 10.dp, bottom = 3.dp))
                snapshot.upcoming.forEach { phase -> TimelineMoment(phase.name, formatLocalEventTime(phase.atMillis, displayZone)) }
                Button(onClick = {
                    val hasFine = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    val hasCoarse = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    if (locationAccessDenied && !hasFine && !hasCoarse) showLocationSettings = true
                    else if (hasFine || hasCoarse) {
                        requestingLocation = true
                        requestCurrentObserverLocation(context) { location -> observerLocation = location; requestingLocation = false; locationAccessDenied = location == null }
                    } else locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PanelLight, contentColor = Lilac), shape = RoundedCornerShape(13.dp)) {
                    Text(when { requestingLocation -> "Getting your device location…"; observerLocation == null -> "Use my location for local sky details"; else -> "Refresh local sky details" }, fontSize = 12.sp)
                }
                val location = observerLocation
                if (location != null) {
                    val moonNow = moonPositionAt(System.currentTimeMillis(), location.latitude, location.longitude)
                    val horizon = moonHorizon(System.currentTimeMillis(), location.latitude, location.longitude)
                    InfoLine("MOONRISE", horizon.riseMillis?.let { formatLocalEventTime(it, displayZone) } ?: "No rise in the next 48 hours")
                    InfoLine("MOONSET", horizon.setMillis?.let { formatLocalEventTime(it, displayZone) } ?: "No set in the next 48 hours")
                    if (horizon.transitMillis != null) InfoLine("MERIDIAN PASSING", "${formatLocalEventTime(horizon.transitMillis, displayZone)} · ${"%.0f".format(horizon.transitAltitude)}° altitude")
                    InfoLine("CURRENT ALTITUDE / AZIMUTH", "${"%.0f".format(moonNow.altitude)}° / ${"%.0f".format(moonNow.azimuth)}°")
                    InfoLine("DISTANCE", "${moonNow.distanceKm.toInt()} km")
                    InfoLine("TROPICAL ZODIAC", moonNow.zodiac)
                } else {
                    Text("Optional location access adds approximate local moonrise, moonset, sky position, distance, and zodiac details.", color = Muted, fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 7.dp))
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            moonPhaseNames.forEach { phaseName ->
                Surface(color = if (selectedPhase == phaseName) PanelLight else Panel,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedPhase == phaseName) Violet else Color(0xFF493650)),
                    modifier = Modifier.fillMaxWidth().clickable { selectedPhase = phaseName }) {
                    Text(phaseName, color = if (selectedPhase == phaseName) Lilac else Muted, fontSize = 13.sp,
                        maxLines = 1, modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp))
                }
            }
        }
        Surface(color = Panel, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                MoonOrb(if (selectedIsCurrent) snapshot.info.illumination else approximateMidIllumination(selectedPhase), selectedPhase, Modifier.size(142.dp))
                Text(selectedPhase, color = Lilac, fontSize = 25.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp))
                if (selectedIsCurrent) Text("Current phase · about ${snapshot.info.illumination}% illuminated", color = Muted, fontSize = 13.sp)
                else Text("Phase guide · select the current phase to see its illumination", color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(14.dp))
        InfoCard("THE THEME", selectedInfo.advice)
        InfoCard("A RITUAL TO TRY", selectedInfo.ritual)
        InfoCard("WHAT YOU NEED", selectedInfo.materials)
        selectedInfo.steps.forEachIndexed { index, step -> InfoCard("RITUAL STEP ${index + 1}", step) }
        InfoCard("GIVE YOURSELF SPACE FROM", selectedInfo.avoid)
        Text("Phase names, illumination, lunar age, and quarter times use the Astronomy Engine lunar ephemeris. Event times use the selected display time zone.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(vertical = 12.dp))
    }
    if (showLocationSettings) AlertDialog(
        onDismissRequest = { showLocationSettings = false },
        title = { Text("Allow location access") },
        text = { Text("Location access is blocked for Magikal Retrogrades. Enable location in the app settings, then tap Refresh local sky details.") },
        confirmButton = { TextButton(onClick = {
            showLocationSettings = false
            runCatching { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }) { Text("Open app settings") } },
        dismissButton = { TextButton(onClick = { showLocationSettings = false; locationAccessDenied = false }) { Text("Not now") } }
    )
}

private fun approximateMidIllumination(name: String): Int {
    val index = moonPhaseNames.indexOf(name).coerceAtLeast(0)
    return ((1 - cos(((index + .5) / 8.0) * 2 * Math.PI)) * 50).toInt().coerceIn(0, 100)
}

private data class Sabbat(
    val name: String, val northDate: String, val southDate: String, val theme: String,
    val practice: String, val symbol: String, val ritualName: String, val materials: String,
    val steps: List<String>, val spell: String
)
private val sabbats = listOf(
    Sabbat("Samhain", "Oct 31", "Apr 30–May 1", "Honoring ancestors, endings, and remembrance", "Remember loved ones and reflect on what you are ready to release.", "☽", "Ancestral remembrance and release", "A memory or photo, a cup of water, paper, pen, and an optional LED candle.", listOf("Tidy a small space and place the memory item and water there.", "Take a few steady breaths and remember a lesson or quality you want to honor.", "Write one gratitude and one boundary or burden you are ready to release.", "Read your intention aloud; pour the water into a plant or down the sink and keep or recycle the note."), "May loving memories offer wisdom; may I release what is not mine to carry."),
    Sabbat("Yule", "Dec 20–23", "Jun 20–23", "Rest, returning light, and renewal", "Welcome the returning light with a restful seasonal intention.", "❄", "Returning light intention", "An evergreen sprig, paper, pen, and an optional LED candle.", listOf("Choose a quiet place and set the evergreen beside the LED candle.", "Write one hope you want to nurture as the light returns.", "Name one small, practical step you can take toward it.", "Read both aloud and place the note where you will see it during the season."), "As the light returns, may patience and hope grow in me."),
    Sabbat("Imbolc", "Feb 1–2", "Aug 1–2", "Inspiration, purification, and first stirrings", "Make room for a fresh idea and tend one small beginning.", "✧", "Seed of inspiration spell", "A seed, a small pot with soil, water, paper, and pen.", listOf("Clear a small surface and place the seed beside the pot.", "Write an idea or skill you want to begin nurturing.", "Plant the seed and water it gently, using its care as a reminder of your intention.", "Read your spell and choose a date to check on both the plant and your first step."), "I welcome new growth with care, curiosity, and steady effort."),
    Sabbat("Ostara", "Mar 20–23", "Sep 20–23", "Balance, growth, and spring renewal", "Notice what is growing and restore balance with a small act of care.", "卯", "Balance and renewal ritual", "Two small stones or objects, a seed or small plant, paper, and pen.", listOf("Place the two objects side by side and name one thing you are nurturing and one thing needing care.", "Write one realistic action to support each side of that balance.", "Plant or tend the seed or plant as a symbol of patient growth.", "Read your spell and revisit the two actions in a week."), "May I welcome growth while tending balance and care."),
    Sabbat("Beltane", "May 1", "Oct 31–Nov 1", "Vitality, creativity, and connection", "Celebrate creativity and connections rooted in care and consent.", "✿", "Joy and creative spark spell", "A ribbon, a flower or leaf, paper, and pen.", listOf("Set the flower or leaf beside the ribbon in a calm space.", "Write one creative joy you want to make room for and one connection you want to nurture respectfully.", "Tie the ribbon loosely around the note or a jar as a reminder of your intention.", "Speak your spell, then take one kind, practical action that supports it."), "May joy grow where care, honesty, and consent meet."),
    Sabbat("Litha", "Jun 20–23", "Dec 20–23", "Abundance, warmth, and the sun's peak", "Honor what gives you energy and share appreciation.", "☼", "Sunlight gratitude charm", "A cup of water, paper, pen, and a leaf or flower.", listOf("Place the water and leaf near a safe patch of daylight.", "Write three sources of warmth, support, or joy in your life.", "Add one action that will help you protect your energy this season.", "Read your spell, water a plant with some of the water, and recycle the note when ready."), "I honor the light in my life and use my energy with care."),
    Sabbat("Lammas / Lughnasadh", "Aug 1", "Feb 1", "First harvest, craft, and gratitude", "Celebrate a skill you have practiced and the effort behind it.", "🌾", "First harvest and craft spell", "A piece of bread or oats, paper, pen, and a safe plate; skip food if it is unsuitable for you.", listOf("Set the food on a plate and pause to recognize the work behind it.", "Write one skill, project, or effort you are proud to have grown.", "Name one next step that would help you keep developing it.", "Read your spell; enjoy or share the food only if safe and welcome, or simply leave it as a symbol."), "May I honor what I have grown and keep learning with gratitude."),
    Sabbat("Mabon", "Sep 20–23", "Mar 20–23", "Equilibrium, harvest, and reflection", "Name what you have gathered and what you are ready to set down.", "⚖", "Gratitude and release balance", "An apple or other seasonal produce, paper, pen, and two small bowls or spaces.", listOf("Set the two bowls apart and label them ‘keep nurturing’ and ‘ready to release.’", "Write one gratitude for the season and place it in the first bowl.", "Write one expectation or habit you are ready to loosen and place it in the second.", "Read your spell, then recycle the release note and choose one gentle action for the coming season."), "I give thanks for what sustains me and release what I can lay down.")
)

@Composable
private fun WheelPage() {
    var selected by remember { mutableIntStateOf(1) }
    var southernHemisphere by remember { mutableStateOf(false) }
    PageColumn {
        SectionIntro("Wheel of the Year", "Eight seasonal celebrations, rituals, and reflections.")
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Northern hemisphere" to false, "Southern hemisphere" to true).forEach { (label, isSouthern) ->
                FilterChip(selected = southernHemisphere == isSouthern, onClick = { southernHemisphere = isSouthern }, label = { Text(label, fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PanelLight, selectedLabelColor = Lilac, labelColor = Muted))
            }
        }
        WheelGraphic(Modifier.fillMaxWidth().height(300.dp)) { selected = it }
        Text("CHOOSE A SABBAT", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold,
            letterSpacing = 1.3.sp, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
        sabbats.chunked(2).forEachIndexed { rowIndex, rowSabbats ->
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowSabbats.forEachIndexed { columnIndex, sabbat ->
                    val sabbatIndex = rowIndex * 2 + columnIndex
                    Surface(
                        color = if (selected == sabbatIndex) PanelLight else Panel,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected == sabbatIndex) Violet else Color(0xFF493650)),
                        modifier = Modifier.weight(1f).heightIn(min = 54.dp).clickable { selected = sabbatIndex }
                    ) {
                        Row(Modifier.fillMaxSize().padding(horizontal = 11.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(sabbat.symbol, color = Gold, fontSize = 17.sp, modifier = Modifier.padding(end = 8.dp))
                            Text(sabbat.name, color = if (selected == sabbatIndex) Lilac else Muted, fontSize = 12.sp,
                                fontWeight = if (selected == sabbatIndex) FontWeight.SemiBold else FontWeight.Medium, lineHeight = 15.sp)
                        }
                    }
                }
            }
        }
        val chosen = sabbats[selected]
        Surface(color = Panel, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("${chosen.symbol}  ${chosen.name}", color = Lilac, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Text(if (southernHemisphere) chosen.southDate else chosen.northDate, color = Gold, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, bottom = 10.dp))
                InfoLine("THEME", chosen.theme)
                InfoLine("ACTIVITY TO TRY", chosen.practice)
                InfoLine("RITUAL", chosen.ritualName)
                InfoLine("WHAT YOU NEED", chosen.materials)
                chosen.steps.forEachIndexed { index, step -> InfoLine("STEP ${index + 1}", step) }
                InfoLine("SPELL / INTENTION", "“${chosen.spell}”")
            }
        }
        Text("Traditional dates and seasonal associations can vary by tradition and hemisphere. These are common Wheel of the Year observances.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(vertical = 12.dp))
    }
}

@Composable
private fun WheelGraphic(modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    Box(modifier.pointerInput(Unit) { detectTapGestures { offset ->
        val centerX = size.width / 2f; val centerY = size.height / 2f
        val angle = (atan2(offset.y - centerY, offset.x - centerX) * 180.0 / Math.PI + 360.0 + 90.0) % 360.0
        onSelect((floor((angle + 22.5) / 45.0).toInt() + 1) % 8)
    } }, contentAlignment = Alignment.Center) {
        Image(painter = painterResource(R.drawable.wheel_of_the_year), contentDescription = "Wheel of the Year with the eight sabbats, seasonal artwork, zodiac ring, and pentagram center",
            modifier = Modifier.fillMaxHeight().aspectRatio(1f), contentScale = ContentScale.Fit)
    }
}

private data class JournalEntry(val tag: String, val timestamp: String, val text: String)

@Composable
private fun JournalPage() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("magikal_journal", android.content.Context.MODE_PRIVATE) }
    var category by remember { mutableStateOf("Retrograde") }
    var target by remember { mutableStateOf(planets.first().name) }
    var note by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf(loadEntries(prefs)) }
    val categories = listOf("Retrograde", "Moon phase", "Sabbat", "General")
    val targets = when (category) {
        "Retrograde" -> planets.map { it.name }
        "Moon phase" -> moonPhaseNames
        "Sabbat" -> sabbats.map { it.name }
        else -> listOf("General")
    }
    LaunchedEffect(category) { target = targets.first() }
    val entryTag = if (category == "General") "General" else "$category · $target"
    PageColumn {
        SectionIntro("Witch's journal", "Keep your reflections close. Entries are stored on this device.")
        Text("WHAT ARE YOU JOURNALING?", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            categories.forEach { option ->
                FilterChip(selected = category == option, onClick = { category = option }, label = { Text(option, fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PanelLight, selectedLabelColor = Lilac, labelColor = Muted))
            }
        }
        if (category != "General") {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 2.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                targets.forEach { option ->
                    FilterChip(selected = target == option, onClick = { target = option }, label = { Text(option, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PanelLight, selectedLabelColor = Lilac, labelColor = Muted))
                }
            }
        }
        OutlinedTextField(value = note, onValueChange = { note = it }, modifier = Modifier.fillMaxWidth(), minLines = 4,
            placeholder = { Text("What are you noticing, learning, or setting an intention for?", color = Muted) },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Violet, unfocusedBorderColor = PanelLight, focusedTextColor = Lilac, unfocusedTextColor = Lilac, cursorColor = Violet))
        Button(onClick = {
            if (note.isNotBlank()) {
                val entry = JournalEntry(entryTag, LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a")), note.trim())
                entries = listOf(entry) + entries
                saveEntries(prefs, entries)
                note = ""
            }
        }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp), colors = ButtonDefaults.buttonColors(containerColor = Violet, contentColor = Ink), shape = RoundedCornerShape(14.dp)) {
            Text("Save journal entry", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("YOUR ENTRIES", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
            JournalTripleMoonPentacle(Modifier.size(width = 88.dp, height = 34.dp))
        }
        if (entries.isEmpty()) {
            Text("Your saved notes will appear here.", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp, bottom = 20.dp))
        }
        entries.forEachIndexed { index, entry ->
            Surface(color = Panel, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(entry.tag.uppercase(), color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(entry.timestamp, color = Muted, fontSize = 10.sp)
                    }
                    Text(entry.text, color = Lilac, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 8.dp))
                    Text("Delete", color = Violet, fontSize = 11.sp, modifier = Modifier.align(Alignment.End).clickable {
                        entries = entries.filterIndexed { i, _ -> i != index }; saveEntries(prefs, entries)
                    }.padding(top = 8.dp))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Journal notes stay on this device in this first build. There is no account sync or cloud backup yet.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
    }
}

@Composable
private fun JournalTripleMoonPentacle(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val cY = h * .5f
        val outer = Path().apply {
            moveTo(w * .03f, h * .10f)
            cubicTo(w * .32f, h * .12f, w * .32f, h * .88f, w * .03f, h * .90f)
            cubicTo(w * .20f, h * .73f, w * .20f, h * .27f, w * .03f, h * .10f)
            close()
            moveTo(w * .97f, h * .10f)
            cubicTo(w * .68f, h * .12f, w * .68f, h * .88f, w * .97f, h * .90f)
            cubicTo(w * .80f, h * .73f, w * .80f, h * .27f, w * .97f, h * .10f)
            close()
        }
        drawPath(outer, Gold)
        val center = Offset(w * .5f, cY)
        val radius = h * .43f
        drawCircle(Gold, radius, center, style = Stroke(width = (size.minDimension * .055f).coerceAtLeast(1.2f)))
        val points = (0..4).map { i ->
            val angle = Math.toRadians(-90.0 + i * 72.0)
            Offset(center.x + cos(angle).toFloat() * radius * .82f, center.y + sin(angle).toFloat() * radius * .82f)
        }
        val starOrder = listOf(0, 2, 4, 1, 3)
        val star = Path().apply {
            moveTo(points[starOrder.first()].x, points[starOrder.first()].y)
            starOrder.drop(1).forEach { index -> lineTo(points[index].x, points[index].y) }
            close()
        }
        drawPath(star, Gold, style = Stroke(width = (size.minDimension * .055f).coerceAtLeast(1.2f)))
    }
}
@Composable
private fun SectionIntro(title: String, subtitle: String) {
    Text(title, color = Lilac, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
    Text(subtitle, color = Muted, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))
}

@Composable
private fun InfoCard(title: String, body: String) {
    Surface(color = Panel, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Text(body, color = Lilac, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 5.dp))
        }
    }
}

@Composable
private fun InfoLine(title: String, body: String) {
    Column(Modifier.padding(bottom = 10.dp)) {
        Text(title, color = Gold, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text(body, color = Lilac, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 3.dp))
    }
}
package com.magikalretrogrades.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.location.LocationListener
import android.os.Bundle
import android.os.Build
import android.os.Looper
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.roundToInt
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.illumination
import io.github.cosinekitty.astronomy.moonPhase
import io.github.cosinekitty.astronomy.moonQuartersAfter
import io.github.cosinekitty.astronomy.searchMoonPhase

private val Ink = Color(0xFF100B18)
private val Panel = Color(0xFF21172E)
private val PanelLight = Color(0xFF30213F)
private val Violet = Color(0xFFB782FF)
private val Lilac = Color(0xFFE4CBFF)
private val Muted = Color(0xFFB9AFC4)
private val Gold = Color(0xFFE8C77A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(16, 11, 24)
        window.navigationBarColor = android.graphics.Color.rgb(16, 11, 24)
        setContent { MagikalApp() }
    }
}

private enum class Page(val label: String, val icon: String) {
    Retrogrades("Retrogrades", "☿"), Moon("Moon phases", "☾"), Wheel("Sabbats", "✺"), Journal("Journal", "✎"), Chart("Natal chart", "☉")
}

@Composable
private fun MagikalApp() {
    var page by remember { mutableStateOf(Page.Retrogrades) }
    val scheme = darkColorScheme(
        primary = Violet, onPrimary = Ink, secondary = Gold, background = Ink,
        surface = Panel, onBackground = Lilac, onSurface = Lilac
    )
    MaterialTheme(colorScheme = scheme) {
        Scaffold(
            containerColor = Ink,
            topBar = {
                AppHeader()
            },
            bottomBar = {
                Column(Modifier.fillMaxWidth().background(Color(0xFF17101F))) {
                    TripleMoonMark(Modifier.align(Alignment.CenterHorizontally).padding(top = 5.dp, bottom = 1.dp).size(width = 96.dp, height = 38.dp))
                    NavigationBar(containerColor = Color(0xFF17101F), contentColor = Violet) {
                        Page.entries.forEach { item ->
                            NavigationBarItem(
                                selected = page == item,
                                onClick = { page = item },
                                icon = { Text(item.icon, fontSize = 19.sp) },
                                label = { Text(item.label, fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = Lilac, selectedTextColor = Lilac,
                                    indicatorColor = PanelLight, unselectedIconColor = Muted, unselectedTextColor = Muted)
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                when (page) {
                    Page.Retrogrades -> RetrogradesPage()
                    Page.Moon -> MoonPage()
                    Page.Wheel -> WheelPage()
                    Page.Journal -> JournalPage()
                    Page.Chart -> NatalChartPage()
                }
            }
        }
    }
}

@Composable
private fun AppHeader() {
    Column(Modifier.fillMaxWidth().background(Ink).padding(horizontal = 16.dp, vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            BrandStar(Modifier.size(40.dp))
            Column(Modifier.padding(start = 7.dp), horizontalAlignment = Alignment.Start) {
                Text("Magikal Retrogrades", color = Lilac, fontSize = 24.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Cursive,
                    fontWeight = FontWeight.Medium, maxLines = 1)
            }
        }
        HorizontalDivider(color = Color(0xFF493650), thickness = 1.dp, modifier = Modifier.padding(top = 5.dp))
    }
}

@Composable
private fun BrandStar(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val orbit = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * .06f, h * .84f)
            cubicTo(w * .01f, h * .98f, w * .20f, h * .93f, w * .42f, h * .79f)
            cubicTo(w * .67f, h * .63f, w * .92f, h * .42f, w * .79f, h * .29f)
            cubicTo(w * .72f, h * .22f, w * .68f, h * .18f, w * .69f, h * .06f)
        }
        drawPath(orbit, Gold, style = androidx.compose.ui.graphics.drawscope.Stroke(width = (w * .045f).coerceAtLeast(1.4f), cap = androidx.compose.ui.graphics.StrokeCap.Round))
        fun star(cx: Float, cy: Float, rx: Float, ry: Float) = androidx.compose.ui.graphics.Path().apply {
            moveTo(cx, cy - ry); quadraticTo(cx - rx * .13f, cy - ry * .13f, cx - rx, cy)
            quadraticTo(cx - rx * .13f, cy + ry * .13f, cx, cy + ry)
            quadraticTo(cx + rx * .13f, cy + ry * .13f, cx + rx, cy)
            quadraticTo(cx + rx * .13f, cy - ry * .13f, cx, cy - ry); close()
        }
        drawPath(star(w * .68f, h * .30f, w * .23f, h * .29f), Lilac)
        drawPath(star(w * .63f, h * .61f, w * .17f, h * .21f), Violet)
    }
}

@Composable
private fun TripleMoonMark(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = (size.minDimension * .035f).coerceAtLeast(1.2f), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        val mark = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * .08f, h * .08f)
            cubicTo(w * .39f, h * .08f, w * .39f, h * .92f, w * .08f, h * .92f)
            moveTo(w * .08f, h * .08f)
            cubicTo(w * .28f, h * .22f, w * .28f, h * .78f, w * .08f, h * .92f)
            moveTo(w * .92f, h * .08f)
            cubicTo(w * .61f, h * .08f, w * .61f, h * .92f, w * .92f, h * .92f)
            moveTo(w * .92f, h * .08f)
            cubicTo(w * .72f, h * .22f, w * .72f, h * .78f, w * .92f, h * .92f)
        }
        drawPath(mark, Lilac, style = stroke)
        drawCircle(Lilac, radius = h * .41f, center = androidx.compose.ui.geometry.Offset(w * .5f, h * .5f), style = stroke)
        drawCircle(Lilac, radius = h * .018f, center = androidx.compose.ui.geometry.Offset(w * .56f, h * .36f))
    }
}

@Composable
private fun PageColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 10.dp), content = content)
}

private data class Planet(
    val name: String, val glyph: String, val horizonsId: String?, val colors: List<Color>,
    val meaning: String, val retro: String, val practice: String, val ritual: String,
    val materials: String, val steps: List<String>, val spell: String, val avoid: String, val crystal: String
)
private val planets = listOf(
    Planet("Mercury", "☿", "199", listOf(Color(0xFFC6B7A6), Color(0xFF665D64)), "Communication, learning, and exchange", "Review conversations, plans, and details; leave room to revise.", "Pause before sending an important message and double-check travel plans.", "Write down one conversation you want to revisit, then draft a calmer version.", "Journal, a purple pen, and a glass of water.", listOf("Set the water beside your journal and take three slow breaths.", "Write the message or plan you want to review; underline the detail that needs checking.", "Draft one clear next question or correction before taking action."), "May I listen clearly, speak thoughtfully, and make room to review.", "Avoid rushing agreements or assuming a message was understood.", "Fluorite"),
    Planet("Venus", "♀", "299", listOf(Color(0xFFFFA77D), Color(0xFF8E3A65)), "Love, values, beauty, and connection", "Reflect on what you value and how you nurture relationships.", "Reconnect with a creative practice or write about your needs.", "Place rose quartz beside a written list of what care and respect look like to you.", "Rose quartz or another keepsake, paper, and a pen.", listOf("Write three qualities you want in a caring relationship, including how you want to treat yourself.", "Circle one value you can practice this week.", "Name one kind boundary that protects that value."), "May care, honesty, and self-respect guide my choices.", "Avoid making a major purchase just to soothe a passing feeling.", "Rose quartz"),
    Planet("Mars", "♂", "499", listOf(Color(0xFFFF765E), Color(0xFF7D263D)), "Drive, courage, and action", "Review how you use energy, desire, and assertiveness.", "Choose steady movement and a clear, manageable goal.", "Take three steady breaths, then choose one safe action that uses your energy well.", "Red paper, a pen, and an optional red jasper stone.", listOf("Write one goal that matters to you.", "Break it into a first step small enough to do safely.", "Schedule a pause or rest period alongside the action."), "May courage move with care and clear intention.", "Avoid escalating conflict or forcing progress through exhaustion.", "Red jasper"),
    Planet("Jupiter", "♃", "599", listOf(Color(0xFFF5C38A), Color(0xFF684C86)), "Growth, wisdom, and possibility", "Revisit beliefs, learning, and the promises you have made yourself.", "Study a subject you love and celebrate quiet progress.", "Choose a book or lesson and learn without rushing the outcome.", "A book, notebook, and an optional amethyst stone.", listOf("Write a belief or goal you want to reconsider.", "List one thing you have learned and one open question.", "Choose a realistic next step and a date to review it."), "May wisdom keep my growth generous and grounded.", "Avoid overcommitting or treating optimism as a guarantee.", "Amethyst"),
    Planet("Saturn", "♄", "699", listOf(Color(0xFFD2C2A2), Color(0xFF454047)), "Boundaries, responsibility, and structure", "Review commitments and strengthen foundations patiently.", "Tidy one small area or create a realistic routine.", "Clear one small surface and decide what belongs there.", "Paper, a pen, and a small stone as a reminder.", listOf("List the commitments currently using your time.", "Mark one boundary or obligation that needs review.", "Write one realistic routine step you can maintain."), "May steady boundaries support a life with room to breathe.", "Avoid harsh self-judgment or taking on every burden alone.", "Smoky quartz"),
    Planet("Uranus", "♅", "799", listOf(Color(0xFF9CE7DF), Color(0xFF396D88)), "Change, originality, and freedom", "Notice where you want more honesty, space, or experimentation.", "Try a low-stakes new approach and record what surprises you.", "Sketch an unconventional solution to a small problem, then test it in a reversible way.", "Paper, colored pencils, and an optional labradorite stone.", listOf("Choose a small routine you want to improve.", "Sketch two fresh approaches, including the simplest one.", "Test one reversible change and note what happens."), "May thoughtful change make space for honest freedom.", "Avoid abrupt changes made only to escape discomfort.", "Labradorite"),
    Planet("Neptune", "♆", "899", listOf(Color(0xFF9CB4FF), Color(0xFF524178)), "Dreams, intuition, and imagination", "Reflect on inspiration, boundaries, and what feels unclear.", "Keep a dream note and make time for a gentle creative ritual.", "Record impressions without treating them as proof or instruction.", "A journal, a blue or violet pencil, and a glass of water.", listOf("Write an image, dream, or idea without judging it.", "Separate what you observed from what you imagine it means.", "Pair the idea with one practical action you can verify."), "May imagination be gentle, clear, and grounded.", "Avoid ignoring practical details or treating a hunch as proof.", "Lepidolite"),
    Planet("Pluto", "♇", "999", listOf(Color(0xFF9A79C7), Color(0xFF34253F)), "Transformation, depth, and renewal", "Explore what is ready to change and what you are ready to release.", "Write a private release note and choose one supportive next step.", "Write one habit or expectation you are ready to loosen; keep the note private or tear it up when finished.", "Dark paper, a pen, and a bowl for holding the note.", listOf("Name one pattern you are ready to examine, without shaming yourself.", "Write what support or boundary would make change safer.", "Choose one small step; keep or recycle the note when you are ready."), "May I meet change with patience, support, and choice.", "Avoid pressuring yourself or others into sudden transformation.", "Obsidian"),
    Planet("Chiron", "⚷", "DES=2060", listOf(Color(0xFFB8A6CA), Color(0xFF5D4B70)), "Vulnerability, learning, and compassionate repair", "Reflect on old sensitivities with care and notice where support helps you grow.", "Name a lesson you have learned and one boundary that protects your wellbeing.", "Write about a challenge only as much as feels safe today, then name what helped you through it.", "A journal, a soft-colored pen, and an optional comforting keepsake.", listOf("Write about a challenge only as much as feels safe today.", "Note what support, skill, or kindness helped you through it.", "Choose one small act of care you can offer yourself this week."), "May I meet tender places with patience, support, and compassion.", "Avoid reopening painful memories without support or treating a crystal as a substitute for care.", "Rose quartz")
)

private const val DISPLAY_ZONE_PREFS = "magikal_display_preferences"
private const val DISPLAY_ZONE_KEY = "display_time_zone"
private fun getDisplayZone(context: Context): java.time.ZoneId {
    val saved = context.getSharedPreferences(DISPLAY_ZONE_PREFS, Context.MODE_PRIVATE).getString(DISPLAY_ZONE_KEY, null)
    return runCatching { java.time.ZoneId.of(saved ?: java.time.ZoneId.systemDefault().id) }.getOrDefault(java.time.ZoneId.systemDefault())
}
private fun saveDisplayZone(context: Context, zoneId: String) {
    context.getSharedPreferences(DISPLAY_ZONE_PREFS, Context.MODE_PRIVATE).edit().putString(DISPLAY_ZONE_KEY, zoneId).apply()
}

@Composable
private fun DisplayZonePicker(selectedId: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val zones = remember { java.util.TimeZone.getAvailableIDs().distinct().sorted() }
    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text("DISPLAY TIME ZONE · LOCATION", color = Gold, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
        Box(Modifier.fillMaxWidth().padding(top = 5.dp)) {
            Surface(color = PanelLight, shape = RoundedCornerShape(13.dp), modifier = Modifier.fillMaxWidth().clickable { expanded = true }) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(selectedId.replace('_', ' '), color = Lilac, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text("▾", color = Gold, fontSize = 18.sp)
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                zones.forEach { zone ->
                    DropdownMenuItem(text = { Text(zone.replace('_', ' '), maxLines = 1) }, onClick = { onSelect(zone); expanded = false })
                }
            }
        }
        Text("Choose any region. Retrograde and Moon event times convert to this time zone in both hemispheres.", color = Muted, fontSize = 9.sp, lineHeight = 13.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun RetrogradesPage() {
    var openPlanet by remember { mutableStateOf<String?>(null) }
    var feed by remember { mutableStateOf(RetrogradeTracker.fallbackFeed()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    var displayZoneId by remember { mutableStateOf(getDisplayZone(context).id) }
    val displayZone = runCatching { java.time.ZoneId.of(displayZoneId) }.getOrDefault(java.time.ZoneId.systemDefault())
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1_000)
        }
    }
    LaunchedEffect(refreshKey) {
        loading = true
        error = null
        try {
            feed = RetrogradeTracker.load(context, planets.mapNotNull { planet -> planet.horizonsId?.let { TrackerPlanet(planet.name, it) } }, forceRefresh = refreshKey > 0)
        } catch (failure: Exception) {
            feed = RetrogradeTracker.fallbackFeed(listOf("Using the built-in station calendar; refresh when online to check for updated ephemeris data."))
            error = null
        } finally {
            loading = false
        }
    }
    PageColumn {
        SectionIntro("Planetary retrogrades", "Explore each planet's symbolism and reflective practices.")
        DisplayZonePicker(displayZoneId) { zone -> displayZoneId = zone; saveDisplayZone(context, zone) }
        InfoCard("LIVE PLANET TRACKER", "Dates are calculated from geocentric planetary motion. Station instants are shared worldwide; displayed times use ${displayZone.id}.")
        when {
            loading -> InfoCard("UPDATING THE SKY", "Fetching planetary positions from NASA/JPL Horizons…")
            error != null -> InfoCard("TRACKING TEMPORARILY UNAVAILABLE", "${error}. Tap Refresh dates to try again. Your guide content is still available offline.")
            feed != null -> {
                val updated = formatLocalEventTime(feed!!.fetchedAtMillis, displayZone)
                InfoCard("DATA UPDATED", "Last refreshed $updated. Station times are estimated from two-day ephemeris samples.")
                if (feed!!.warnings.isNotEmpty()) {
                    InfoCard("SOME PLANETS DIDN'T UPDATE", feed!!.warnings.joinToString(" · "))
                }
            }
        }
        val activeNow = feed?.events.orEmpty().filter { it.startUtcMillis <= nowMillis && it.endUtcMillis > nowMillis }.sortedBy { it.startUtcMillis }
        val upcomingNow = feed?.events.orEmpty().filter { it.startUtcMillis > nowMillis }.sortedBy { it.startUtcMillis }
        when {
            activeNow.isNotEmpty() -> InfoCard("CURRENT RETROGRADES", activeNow.joinToString("\n") { "${it.planet}${if (it.zodiacSign.isNotBlank()) " in ${it.zodiacSign}" else ""} · ends ${formatLocalEventTime(it.endUtcMillis, displayZone)} · ${formatCountdown(it.endUtcMillis - nowMillis)} remaining" })
            upcomingNow.isNotEmpty() -> InfoCard("NO RETROGRADES RIGHT NOW · NEXT", upcomingNow.take(3).joinToString("\n") { "${it.planet}${if (it.zodiacSign.isNotBlank()) " in ${it.zodiacSign}" else ""} · starts ${formatLocalEventTime(it.startUtcMillis, displayZone)} · in ${formatCountdown(it.startUtcMillis - nowMillis)}" })
            loading -> InfoCard("CURRENT RETROGRADES", "Updating station dates in ${displayZone.id}. The built-in calendar keeps countdowns visible while data refreshes.")
            error != null -> InfoCard("TRACKING TEMPORARILY UNAVAILABLE", "${error}. Built-in current and upcoming dates remain visible.")
            feed != null -> InfoCard("NO CURRENT OR UPCOMING RETROGRADES", "No station was found in the available forecast.")
        }
        Button(onClick = { refreshKey++ }, enabled = !loading, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Violet, contentColor = Ink), shape = RoundedCornerShape(14.dp)) {
            Text(if (loading) "Updating…" else "Refresh dates", fontWeight = FontWeight.Bold)
        }
        planets.forEach { planet ->
            val periods = feed?.events?.filter { it.planet == planet.name }?.sortedBy { it.startUtcMillis }.orEmpty()
            val active = periods.firstOrNull { it.startUtcMillis <= nowMillis && it.endUtcMillis > nowMillis }
            val next = periods.firstOrNull { it.startUtcMillis > nowMillis }
            Surface(color = Panel, shape = RoundedCornerShape(19.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable { openPlanet = if (openPlanet == planet.name) null else planet.name }) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PlanetOrb(planet, Modifier.size(52.dp))
                        Column(Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(planet.name, color = Lilac, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                            Text(planet.meaning, color = Muted, fontSize = 11.sp)
                            if (loading && feed == null) Text("Checking current motion…", color = Violet, fontSize = 10.sp)
                                                        else if (feed != null) Text(
                                when {
                                    active != null -> "RETROGRADE NOW · ends ${formatLocalEventTime(active.endUtcMillis, displayZone)}"
                                    next != null -> "NEXT · starts ${formatLocalEventTime(next.startUtcMillis, displayZone)}"
                                    else -> "No station found in the 2-year forecast"
                                }, color = if (active != null) Gold else Violet, fontSize = 10.sp, lineHeight = 14.sp
                            )
                        }
                        Text(planet.glyph, color = Violet, fontSize = 24.sp)
                    }
                    if (planet.horizonsId != null && feed != null) {
                        HorizontalDivider(color = PanelLight, modifier = Modifier.padding(vertical = 10.dp))
                        when {
                            active != null -> {
                                EventCountdown("CURRENT RETROGRADE", active, nowMillis, displayZone, isActive = true)
                                val following = periods.firstOrNull { it.startUtcMillis >= active.endUtcMillis }
                                if (following != null) {
                                    Spacer(Modifier.height(10.dp))
                                    EventCountdown("NEXT RETROGRADE", following, nowMillis, displayZone, isActive = false)
                                }
                            }
                            next != null -> EventCountdown("NEXT RETROGRADE", next, nowMillis, displayZone, isActive = false)
                            else -> Text("No upcoming station was found in the current two-year forecast.", color = Muted, fontSize = 11.sp)
                        }
                    }
                    if (openPlanet == planet.name) {
                        HorizontalDivider(color = PanelLight, modifier = Modifier.padding(vertical = 12.dp))
                        InfoLine("SPIRITUAL MEANING", planet.meaning)
                        InfoLine("RETROGRADE REFLECTION", planet.retro)
                        InfoLine("PRACTICE", planet.practice)
                        InfoLine("RITUAL", planet.ritual)
                        InfoLine("WHAT YOU NEED", planet.materials)
                        planet.steps.forEachIndexed { index, step -> InfoLine("RITUAL STEP ${index + 1}", step) }
                        InfoLine("SPELL / INTENTION", planet.spell)
                        InfoLine("WHAT NOT TO DO", planet.avoid)
                        InfoLine("CRYSTAL", "${planet.crystal} (a symbolic companion, not a substitute for care)")
                    }
                }
            }
        }
    }
}

@Composable
private fun EventCountdown(title: String, event: RetrogradeEvent, nowMillis: Long, zone: java.time.ZoneId, isActive: Boolean) {
    Text(title + if (event.zodiacSign.isNotBlank()) " · ${event.zodiacSign}" else " · zodiac updates with live ephemeris", color = Gold, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
    TimelineMoment("${if (isActive) "Started" else "Starts"} · ${formatLocalEventTime(event.startUtcMillis, zone)}",
        if (isActive) "${formatCountdown(nowMillis - event.startUtcMillis)} ago" else "In ${formatCountdown(event.startUtcMillis - nowMillis)}")
    Spacer(Modifier.height(6.dp))
    TimelineMoment("Ends · ${formatLocalEventTime(event.endUtcMillis, zone)}", "In ${formatCountdown(event.endUtcMillis - nowMillis)}")
}

@Composable
private fun TimelineMoment(label: String, countdown: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Lilac, fontSize = 10.sp, lineHeight = 14.sp, modifier = Modifier.weight(1f))
        Text(countdown, color = Violet, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
    }
}

private fun formatCountdown(milliseconds: Long): String {
    val totalSeconds = (milliseconds.coerceAtLeast(0L) / 1_000L)
    val days = totalSeconds / 86_400L
    val hours = (totalSeconds % 86_400L) / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (days > 0) "${days}d ${hours}h ${minutes}m ${seconds}s" else "${hours}h ${minutes}m ${seconds}s"
}

@Composable
private fun PlanetOrb(planet: Planet, modifier: Modifier = Modifier) {
    val sprite = ImageBitmap.imageResource(R.drawable.planet_sprite)
    val index = mapOf("Mercury" to 0, "Venus" to 1, "Mars" to 2, "Jupiter" to 3, "Saturn" to 4, "Uranus" to 5, "Neptune" to 6, "Pluto" to 7, "Chiron" to 8)[planet.name] ?: 0
    val tile = sprite.width / 3
    Canvas(modifier) {
        drawImage(image = sprite, srcOffset = IntOffset((index % 3) * tile, (index / 3) * tile),
            srcSize = IntSize(tile, tile), dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.toInt(), size.height.toInt()))
    }
}

private data class MoonInfo(
    val name: String, val illumination: Int, val advice: String, val ritual: String,
    val materials: String, val steps: List<String>, val avoid: String
)
private data class MoonPhaseEvent(val name: String, val atMillis: Long)
private data class MoonSnapshot(val phase: Double, val info: MoonInfo, val ageDays: Double, val upcoming: List<MoonPhaseEvent>)
private data class MoonPosition(val altitude: Double, val azimuth: Double, val distanceKm: Double, val zodiac: String, val rightAscension: Double, val declination: Double)
private data class MoonHorizon(val riseMillis: Long?, val setMillis: Long?, val transitMillis: Long?, val transitAltitude: Double)
private val moonPhaseNames = listOf("New Moon", "Waxing Crescent", "First Quarter", "Waxing Gibbous", "Full Moon", "Waning Gibbous", "Last Quarter", "Waning Crescent")

private fun moonSnapshot(): MoonSnapshot {
    val nowMillis = System.currentTimeMillis()
    val now = Time((nowMillis - 946_728_000_000L) / 86_400_000.0)
    val phaseAngle = moonPhase(now)
    val phase = phaseAngle / 360.0
    val phaseIndex = floor(((phaseAngle + 22.5) % 360.0) / 45.0).toInt().coerceIn(0, 7)
    val illumination = (illumination(Body.Moon, now).phaseFraction * 100.0).roundToInt().coerceIn(0, 100)
    val previousNewMoon = searchMoonPhase(0.0, now, -40.0)
    val ageDays = previousNewMoon?.let { (nowMillis - it.toMillisecondsSince1970()) / 86_400_000.0 } ?: 0.0
    val upcoming = moonQuartersAfter(now).take(4).map { event ->
        val name = listOf("New Moon", "First Quarter", "Full Moon", "Last Quarter").getOrElse(event.quarter) { "Moon phase" }
        MoonPhaseEvent(name, event.time.toMillisecondsSince1970())
    }
    return MoonSnapshot(phase, moonGuidance(moonPhaseNames[phaseIndex], illumination), ageDays, upcoming)
}

private fun moonPositionAt(timeMillis: Long, latitude: Double, longitude: Double): MoonPosition {
    val rad = Math.PI / 180.0
    val d = timeMillis / 86_400_000.0 - 10_957.5
    val l = rad * (218.316 + 13.176396 * d)
    val m = rad * (134.963 + 13.064993 * d)
    val f = rad * (93.272 + 13.229350 * d)
    val lambda = l + rad * 6.289 * sin(m)
    val beta = rad * 5.128 * sin(f)
    val distanceKm = 385_001.0 - 20_905.0 * cos(m)
    val obliquity = rad * 23.4397
    val rightAscension = atan2(sin(lambda) * cos(obliquity) - kotlin.math.tan(beta) * sin(obliquity), cos(lambda))
    val declination = kotlin.math.asin(sin(beta) * cos(obliquity) + cos(beta) * sin(lambda) * sin(obliquity))
    val latitudeRad = latitude * rad
    val hourAngle = rad * (280.16 + 360.9856235 * d + longitude) - rightAscension
    val altitude = kotlin.math.asin(sin(latitudeRad) * sin(declination) + cos(latitudeRad) * cos(declination) * cos(hourAngle)) / rad
    val azimuth = ((atan2(sin(hourAngle), cos(hourAngle) * sin(latitudeRad) - kotlin.math.tan(declination) * cos(latitudeRad)) / rad + 180.0) % 360.0 + 360.0) % 360.0
    val eclipticLongitude = ((atan2(sin(rightAscension) * cos(obliquity) + kotlin.math.tan(declination) * sin(obliquity), cos(rightAscension)) / rad % 360.0) + 360.0) % 360.0
    val signs = listOf("Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo", "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces")
    return MoonPosition(altitude, azimuth, distanceKm, signs[(eclipticLongitude / 30.0).toInt().coerceIn(0, 11)], rightAscension, declination)
}

private fun moonBrightLimbAngle(timeMillis: Long, latitude: Double, longitude: Double): Double {
    val rad = Math.PI / 180.0
    val d = timeMillis / 86_400_000.0 - 10_957.5
    val l = rad * (280.46 + .9856474 * d)
    val g = rad * (357.528 + .9856003 * d)
    val solarLongitude = l + rad * (1.915 * sin(g) + .020 * sin(2 * g))
    val obliquity = rad * (23.4393 - .0000004 * d)
    val sunRa = atan2(cos(obliquity) * sin(solarLongitude), cos(solarLongitude))
    val sunDec = kotlin.math.asin(sin(obliquity) * sin(solarLongitude))
    val moon = moonPositionAt(timeMillis, latitude, longitude)
    val delta = sunRa - moon.rightAscension
    val brightLimb = atan2(cos(sunDec) * sin(delta), sin(sunDec) * cos(moon.declination) - cos(sunDec) * sin(moon.declination) * cos(delta))
    val hourAngle = rad * (280.16 + 360.9856235 * d + longitude) - moon.rightAscension
    val phi = latitude * rad
    val parallactic = atan2(sin(hourAngle), kotlin.math.tan(phi) * cos(moon.declination) - sin(moon.declination) * cos(hourAngle))
    return (brightLimb - parallactic) / rad
}

@Suppress("DEPRECATION")
private fun requestCurrentObserverLocation(context: Context, callback: (Location?) -> Unit) {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val provider = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .firstOrNull { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
    if (provider == null) { callback(lastKnownObserverLocation(context)); return }
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            manager.getCurrentLocation(provider, null, context.mainExecutor) { location ->
                callback(location ?: lastKnownObserverLocation(context))
            }
        } else {
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    runCatching { manager.removeUpdates(this) }
                    callback(location)
                }
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            }
            manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
        }
    } catch (_: SecurityException) { callback(lastKnownObserverLocation(context)) }
      catch (_: Exception) { callback(lastKnownObserverLocation(context)) }
}

private fun moonHorizon(nowMillis: Long, latitude: Double, longitude: Double): MoonHorizon {
    val threshold = -0.3
    val sampleStep = 30L * 60L * 1000L
    var previousTime = nowMillis
    var previousAltitude = moonPositionAt(previousTime, latitude, longitude).altitude - threshold
    var rise: Long? = null
    var set: Long? = null
    var transit: Long? = null
    var maxAltitude = -90.0
    var t = nowMillis
    while (t <= nowMillis + 48L * 60L * 60L * 1000L) {
        val altitude = moonPositionAt(t, latitude, longitude).altitude
        if (t <= nowMillis + 24L * 60L * 60L * 1000L && altitude > maxAltitude) { maxAltitude = altitude; transit = t }
        val adjusted = altitude - threshold
        if (previousAltitude <= 0.0 && adjusted > 0.0 && rise == null) {
            val fraction = (previousAltitude / (previousAltitude - adjusted)).coerceIn(0.0, 1.0)
            rise = previousTime + (sampleStep * fraction).toLong()
        } else if (previousAltitude >= 0.0 && adjusted < 0.0 && set == null) {
            val fraction = (previousAltitude / (previousAltitude - adjusted)).coerceIn(0.0, 1.0)
            set = previousTime + (sampleStep * fraction).toLong()
        }
        previousAltitude = adjusted
        previousTime = t
        t += sampleStep
    }
    return MoonHorizon(rise, set, transit, maxAltitude)
}

private fun lastKnownObserverLocation(context: Context): Location? = try {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    manager.getProviders(true).mapNotNull { provider -> manager.getLastKnownLocation(provider) }
        .minByOrNull { it.accuracy }
} catch (_: SecurityException) { null } catch (_: Exception) { null }

private fun moonGuidance(name: String, illumination: Int): MoonInfo = when (name) {
    "New Moon" -> MoonInfo(name, illumination, "Set gentle intentions and make room for a fresh beginning.", "Intention-seed ritual", "A small seed, a cup or pot with soil, water, and paper with a pen.", listOf("Write one intention in a single clear sentence.", "Plant the seed or place it beside the note as a symbol of beginning.", "Choose one small action you can take this week and date it."), "Avoid demanding immediate results from a new goal.")
    "Waxing Crescent" -> MoonInfo(name, illumination, "Nurture plans with curiosity and steady effort.", "First-step candle or LED ritual", "A candle used safely or an LED candle, paper, and a pen.", listOf("Write the next small step toward a goal.", "Light the candle safely, or switch on the LED candle, while naming the step.", "Place the note where it can remind you to follow through."), "Avoid comparing your beginning with someone else's progress.")
    "First Quarter" -> MoonInfo(name, illumination, "Meet a challenge with a thoughtful choice.", "Two-path clarity ritual", "Two small stones or objects, paper, and a pen.", listOf("Name one obstacle without judging yourself.", "Set one object beside each practical way forward.", "Choose the safer next step and write when you will try it."), "Avoid treating every delay as a sign to give up.")
    "Waxing Gibbous" -> MoonInfo(name, illumination, "Refine, prepare, and tend what is already growing.", "Intention tune-up", "Your original intention, a journal, and a pen.", listOf("Read an intention you have been working with.", "Underline what still fits and cross out what needs changing.", "Write one useful adjustment and put it into practice."), "Avoid overworking a plan that already needs rest.")
    "Full Moon" -> MoonInfo(name, illumination, "Celebrate, notice what is illuminated, and release what feels complete.", "Gratitude and release ritual", "A bowl of water, paper, a pen, and an optional LED candle.", listOf("Write three things you appreciate and one thing you are ready to release.", "Read the gratitude aloud; tear or recycle the release note.", "Pour the water into a plant or down the sink, then choose a restful closing moment."), "Avoid making emotionally charged choices in haste.")
    "Waning Gibbous" -> MoonInfo(name, illumination, "Share gratitude and integrate what you have learned.", "Lesson and thanks ritual", "A journal, a pen, and an optional cup of tea.", listOf("Write one lesson from the past month.", "Name a person, place, or practice that supported you.", "Choose one way to carry that learning forward."), "Avoid holding on to a routine that no longer serves you.")
    "Last Quarter" -> MoonInfo(name, illumination, "Clear space and reconsider what deserves your energy.", "Gentle clearing ritual", "A cloth, a small bowl, paper, and a pen.", listOf("Clear one small surface or tidy one drawer.", "Write one boundary that would protect your time or energy.", "Keep the note in your journal and revisit it next week."), "Avoid turning reflection into self-criticism.")
    else -> MoonInfo("Waning Crescent", illumination, "Rest, restore, and prepare for a quieter reset.", "Quiet reset ritual", "A comfortable blanket, water or calming tea, and a journal if useful.", listOf("Settle somewhere comfortable and take several unhurried breaths.", "Write down one thing you can leave until later.", "Choose one restorative activity and give yourself time for it."), "Avoid filling every moment before the next cycle begins.")
}

@Composable
private fun MoonOrb(illumination: Int, phaseName: String, modifier: Modifier = Modifier) {
    Box(modifier.clip(CircleShape)) {
        Image(painterResource(R.drawable.moon_globe), contentDescription = "Still cratered lunar surface", contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize())
        Canvas(Modifier.fillMaxSize().clip(CircleShape)) {
            val r = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val lit = illumination.coerceIn(0, 100) / 100f
            if (lit < .98f) {
                val waxing = phaseName.startsWith("Waxing") || phaseName == "First Quarter"
                val offset = lit * r * 2f
                val shadowX = center.x + if (waxing) offset else -offset
                drawCircle(Brush.radialGradient(listOf(Color(0xFF30283A).copy(alpha = .96f), Color(0xFF15101B)),
                    center = Offset(shadowX, center.y), radius = r * 1.65f), r, Offset(shadowX, center.y))
            }
            drawCircle(Color.White.copy(alpha = .16f), r, center, style = Stroke(width = 1.dp.toPx()))
        }
    }
}

@Composable
private fun MoonPage() {
    var snapshot by remember { mutableStateOf(moonSnapshot()) }
    var selectedPhase by remember { mutableStateOf(snapshot.info.name) }
    var observerLocation by remember { mutableStateOf<Location?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    var displayZoneId by remember { mutableStateOf(getDisplayZone(context).id) }
    val displayZone = runCatching { java.time.ZoneId.of(displayZoneId) }.getOrDefault(java.time.ZoneId.systemDefault())
    var locationAccessDenied by remember { mutableStateOf(false) }
    var showLocationSettings by remember { mutableStateOf(false) }
    var requestingLocation by remember { mutableStateOf(false) }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true || grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            requestingLocation = true
            requestCurrentObserverLocation(context) { location -> observerLocation = location; requestingLocation = false; locationAccessDenied = location == null }
        } else { requestingLocation = false; locationAccessDenied = true; showLocationSettings = true }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            snapshot = moonSnapshot()
        }
    }
    LaunchedEffect(snapshot.info.name) { selectedPhase = snapshot.info.name }
    val selectedIsCurrent = selectedPhase == snapshot.info.name
    val selectedInfo = if (selectedIsCurrent) snapshot.info else moonGuidance(selectedPhase, approximateMidIllumination(selectedPhase))
    PageColumn {
        SectionIntro("Moon phases", "Explore every phase, its spiritual themes, and practices to try.")
        DisplayZonePicker(displayZoneId) { zone -> displayZoneId = zone; saveDisplayZone(context, zone) }
        Surface(color = Panel, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("MOON AT A GLANCE", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                Text(formatLocalEventTime(System.currentTimeMillis(), displayZone), color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp, bottom = 10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("LUNAR AGE" to "${"%.1f".format(snapshot.ageDays)} days", "ILLUMINATION" to "${snapshot.info.illumination}%").forEach { (label, value) ->
                        Column(Modifier.weight(1f).background(PanelLight, RoundedCornerShape(12.dp)).padding(10.dp)) {
                            Text(label, color = Gold, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
                            Text(value, color = Lilac, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
                Text("CURRENT PHASE BY HEMISPHERE", color = Gold, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp, modifier = Modifier.padding(top = 12.dp))
                Text(if (observerLocation == null) "The lunar phase is the same worldwide. Allow location access to calculate the Moon’s local viewing angle for your area." else "Location-based view updated for your device. The phase is shared worldwide; the Moon’s orientation is calculated for this place and time.", color = Muted, fontSize = 10.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 4.dp, bottom = 7.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Northern hemisphere" to 1, "Southern hemisphere" to -1).forEach { (hemisphereName, sign) ->
                        Surface(color = PanelLight, shape = RoundedCornerShape(13.dp), modifier = Modifier.weight(1f)) {
                            Row(Modifier.padding(9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val latitude = observerLocation?.latitude?.let { kotlin.math.abs(it) * sign } ?: (35.0 * sign)
                                val longitude = observerLocation?.longitude ?: 0.0
                                val viewAngle = moonBrightLimbAngle(System.currentTimeMillis(), latitude, longitude)
                                MoonOrb(snapshot.info.illumination, snapshot.info.name, Modifier.size(38.dp).graphicsLayer(rotationZ = viewAngle.toFloat()))
                                Column {
                                    Text(hemisphereName, color = Gold, fontSize = 9.sp, lineHeight = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text(snapshot.info.name, color = Lilac, fontSize = 11.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 3.dp))
                                    Text("${snapshot.info.illumination}% illuminated", color = Muted, fontSize = 8.sp, lineHeight = 11.sp)
                                }
                            }
                        }
                    }
                }
                val nextFull = snapshot.upcoming.firstOrNull { it.name == "Full Moon" }
                val nextNew = snapshot.upcoming.firstOrNull { it.name == "New Moon" }
                if (nextFull != null) InfoLine("NEXT FULL MOON", formatLocalEventTime(nextFull.atMillis, displayZone))
                if (nextNew != null) InfoLine("NEXT NEW MOON", formatLocalEventTime(nextNew.atMillis, displayZone))
                Text("NEXT PHASES", color = Gold, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp, modifier = Modifier.padding(top = 10.dp, bottom = 3.dp))
                snapshot.upcoming.forEach { phase -> TimelineMoment(phase.name, formatLocalEventTime(phase.atMillis, displayZone)) }
                Button(onClick = {
                    val hasFine = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    val hasCoarse = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    if (locationAccessDenied && !hasFine && !hasCoarse) showLocationSettings = true
                    else if (hasFine || hasCoarse) {
                        requestingLocation = true
                        requestCurrentObserverLocation(context) { location -> observerLocation = location; requestingLocation = false; locationAccessDenied = location == null }
                    } else locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PanelLight, contentColor = Lilac), shape = RoundedCornerShape(13.dp)) {
                    Text(when { requestingLocation -> "Getting your device location…"; observerLocation == null -> "Use my location for local sky details"; else -> "Refresh local sky details" }, fontSize = 12.sp)
                }
                val location = observerLocation
                if (location != null) {
                    val moonNow = moonPositionAt(System.currentTimeMillis(), location.latitude, location.longitude)
                    val horizon = moonHorizon(System.currentTimeMillis(), location.latitude, location.longitude)
                    InfoLine("MOONRISE", horizon.riseMillis?.let { formatLocalEventTime(it, displayZone) } ?: "No rise in the next 48 hours")
                    InfoLine("MOONSET", horizon.setMillis?.let { formatLocalEventTime(it, displayZone) } ?: "No set in the next 48 hours")
                    if (horizon.transitMillis != null) InfoLine("MERIDIAN PASSING", "${formatLocalEventTime(horizon.transitMillis, displayZone)} · ${"%.0f".format(horizon.transitAltitude)}° altitude")
                    InfoLine("CURRENT ALTITUDE / AZIMUTH", "${"%.0f".format(moonNow.altitude)}° / ${"%.0f".format(moonNow.azimuth)}°")
                    InfoLine("DISTANCE", "${moonNow.distanceKm.toInt()} km")
                    InfoLine("TROPICAL ZODIAC", moonNow.zodiac)
                } else {
                    Text("Optional location access adds approximate local moonrise, moonset, sky position, distance, and zodiac details.", color = Muted, fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 7.dp))
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            moonPhaseNames.forEach { phaseName ->
                Surface(color = if (selectedPhase == phaseName) PanelLight else Panel,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedPhase == phaseName) Violet else Color(0xFF493650)),
                    modifier = Modifier.fillMaxWidth().clickable { selectedPhase = phaseName }) {
                    Text(phaseName, color = if (selectedPhase == phaseName) Lilac else Muted, fontSize = 13.sp,
                        maxLines = 1, modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp))
                }
            }
        }
        Surface(color = Panel, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                MoonOrb(if (selectedIsCurrent) snapshot.info.illumination else approximateMidIllumination(selectedPhase), selectedPhase, Modifier.size(142.dp))
                Text(selectedPhase, color = Lilac, fontSize = 25.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp))
                if (selectedIsCurrent) Text("Current phase · about ${snapshot.info.illumination}% illuminated", color = Muted, fontSize = 13.sp)
                else Text("Phase guide · select the current phase to see its illumination", color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(14.dp))
        InfoCard("THE THEME", selectedInfo.advice)
        InfoCard("A RITUAL TO TRY", selectedInfo.ritual)
        InfoCard("WHAT YOU NEED", selectedInfo.materials)
        selectedInfo.steps.forEachIndexed { index, step -> InfoCard("RITUAL STEP ${index + 1}", step) }
        InfoCard("GIVE YOURSELF SPACE FROM", selectedInfo.avoid)
        Text("Phase names, illumination, lunar age, and quarter times use the Astronomy Engine lunar ephemeris. Event times use the selected display time zone.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(vertical = 12.dp))
    }
    if (showLocationSettings) AlertDialog(
        onDismissRequest = { showLocationSettings = false },
        title = { Text("Allow location access") },
        text = { Text("Location access is blocked for Magikal Retrogrades. Enable location in the app settings, then tap Refresh local sky details.") },
        confirmButton = { TextButton(onClick = {
            showLocationSettings = false
            runCatching { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }) { Text("Open app settings") } },
        dismissButton = { TextButton(onClick = { showLocationSettings = false; locationAccessDenied = false }) { Text("Not now") } }
    )
}

private fun approximateMidIllumination(name: String): Int {
    val index = moonPhaseNames.indexOf(name).coerceAtLeast(0)
    return ((1 - cos(((index + .5) / 8.0) * 2 * Math.PI)) * 50).toInt().coerceIn(0, 100)
}

private data class Sabbat(
    val name: String, val northDate: String, val southDate: String, val theme: String,
    val practice: String, val symbol: String, val ritualName: String, val materials: String,
    val steps: List<String>, val spell: String
)
private val sabbats = listOf(
    Sabbat("Samhain", "Oct 31", "Apr 30–May 1", "Honoring ancestors, endings, and remembrance", "Remember loved ones and reflect on what you are ready to release.", "☽", "Ancestral remembrance and release", "A memory or photo, a cup of water, paper, pen, and an optional LED candle.", listOf("Tidy a small space and place the memory item and water there.", "Take a few steady breaths and remember a lesson or quality you want to honor.", "Write one gratitude and one boundary or burden you are ready to release.", "Read your intention aloud; pour the water into a plant or down the sink and keep or recycle the note."), "May loving memories offer wisdom; may I release what is not mine to carry."),
    Sabbat("Yule", "Dec 20–23", "Jun 20–23", "Rest, returning light, and renewal", "Welcome the returning light with a restful seasonal intention.", "❄", "Returning light intention", "An evergreen sprig, paper, pen, and an optional LED candle.", listOf("Choose a quiet place and set the evergreen beside the LED candle.", "Write one hope you want to nurture as the light returns.", "Name one small, practical step you can take toward it.", "Read both aloud and place the note where you will see it during the season."), "As the light returns, may patience and hope grow in me."),
    Sabbat("Imbolc", "Feb 1–2", "Aug 1–2", "Inspiration, purification, and first stirrings", "Make room for a fresh idea and tend one small beginning.", "✧", "Seed of inspiration spell", "A seed, a small pot with soil, water, paper, and pen.", listOf("Clear a small surface and place the seed beside the pot.", "Write an idea or skill you want to begin nurturing.", "Plant the seed and water it gently, using its care as a reminder of your intention.", "Read your spell and choose a date to check on both the plant and your first step."), "I welcome new growth with care, curiosity, and steady effort."),
    Sabbat("Ostara", "Mar 20–23", "Sep 20–23", "Balance, growth, and spring renewal", "Notice what is growing and restore balance with a small act of care.", "卯", "Balance and renewal ritual", "Two small stones or objects, a seed or small plant, paper, and pen.", listOf("Place the two objects side by side and name one thing you are nurturing and one thing needing care.", "Write one realistic action to support each side of that balance.", "Plant or tend the seed or plant as a symbol of patient growth.", "Read your spell and revisit the two actions in a week."), "May I welcome growth while tending balance and care."),
    Sabbat("Beltane", "May 1", "Oct 31–Nov 1", "Vitality, creativity, and connection", "Celebrate creativity and connections rooted in care and consent.", "✿", "Joy and creative spark spell", "A ribbon, a flower or leaf, paper, and pen.", listOf("Set the flower or leaf beside the ribbon in a calm space.", "Write one creative joy you want to make room for and one connection you want to nurture respectfully.", "Tie the ribbon loosely around the note or a jar as a reminder of your intention.", "Speak your spell, then take one kind, practical action that supports it."), "May joy grow where care, honesty, and consent meet."),
    Sabbat("Litha", "Jun 20–23", "Dec 20–23", "Abundance, warmth, and the sun's peak", "Honor what gives you energy and share appreciation.", "☼", "Sunlight gratitude charm", "A cup of water, paper, pen, and a leaf or flower.", listOf("Place the water and leaf near a safe patch of daylight.", "Write three sources of warmth, support, or joy in your life.", "Add one action that will help you protect your energy this season.", "Read your spell, water a plant with some of the water, and recycle the note when ready."), "I honor the light in my life and use my energy with care."),
    Sabbat("Lammas / Lughnasadh", "Aug 1", "Feb 1", "First harvest, craft, and gratitude", "Celebrate a skill you have practiced and the effort behind it.", "🌾", "First harvest and craft spell", "A piece of bread or oats, paper, pen, and a safe plate; skip food if it is unsuitable for you.", listOf("Set the food on a plate and pause to recognize the work behind it.", "Write one skill, project, or effort you are proud to have grown.", "Name one next step that would help you keep developing it.", "Read your spell; enjoy or share the food only if safe and welcome, or simply leave it as a symbol."), "May I honor what I have grown and keep learning with gratitude."),
    Sabbat("Mabon", "Sep 20–23", "Mar 20–23", "Equilibrium, harvest, and reflection", "Name what you have gathered and what you are ready to set down.", "⚖", "Gratitude and release balance", "An apple or other seasonal produce, paper, pen, and two small bowls or spaces.", listOf("Set the two bowls apart and label them ‘keep nurturing’ and ‘ready to release.’", "Write one gratitude for the season and place it in the first bowl.", "Write one expectation or habit you are ready to loosen and place it in the second.", "Read your spell, then recycle the release note and choose one gentle action for the coming season."), "I give thanks for what sustains me and release what I can lay down.")
)

@Composable
private fun WheelPage() {
    var selected by remember { mutableIntStateOf(1) }
    var southernHemisphere by remember { mutableStateOf(false) }
    PageColumn {
        SectionIntro("Wheel of the Year", "Eight seasonal celebrations, rituals, and reflections.")
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Northern hemisphere" to false, "Southern hemisphere" to true).forEach { (label, isSouthern) ->
                FilterChip(selected = southernHemisphere == isSouthern, onClick = { southernHemisphere = isSouthern }, label = { Text(label, fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PanelLight, selectedLabelColor = Lilac, labelColor = Muted))
            }
        }
        WheelGraphic(Modifier.fillMaxWidth().height(300.dp)) { selected = it }
        Text("CHOOSE A SABBAT", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold,
            letterSpacing = 1.3.sp, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
        sabbats.chunked(2).forEachIndexed { rowIndex, rowSabbats ->
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowSabbats.forEachIndexed { columnIndex, sabbat ->
                    val sabbatIndex = rowIndex * 2 + columnIndex
                    Surface(
                        color = if (selected == sabbatIndex) PanelLight else Panel,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected == sabbatIndex) Violet else Color(0xFF493650)),
                        modifier = Modifier.weight(1f).heightIn(min = 54.dp).clickable { selected = sabbatIndex }
                    ) {
                        Row(Modifier.fillMaxSize().padding(horizontal = 11.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(sabbat.symbol, color = Gold, fontSize = 17.sp, modifier = Modifier.padding(end = 8.dp))
                            Text(sabbat.name, color = if (selected == sabbatIndex) Lilac else Muted, fontSize = 12.sp,
                                fontWeight = if (selected == sabbatIndex) FontWeight.SemiBold else FontWeight.Medium, lineHeight = 15.sp)
                        }
                    }
                }
            }
        }
        val chosen = sabbats[selected]
        Surface(color = Panel, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("${chosen.symbol}  ${chosen.name}", color = Lilac, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Text(if (southernHemisphere) chosen.southDate else chosen.northDate, color = Gold, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, bottom = 10.dp))
                InfoLine("THEME", chosen.theme)
                InfoLine("ACTIVITY TO TRY", chosen.practice)
                InfoLine("RITUAL", chosen.ritualName)
                InfoLine("WHAT YOU NEED", chosen.materials)
                chosen.steps.forEachIndexed { index, step -> InfoLine("STEP ${index + 1}", step) }
                InfoLine("SPELL / INTENTION", "“${chosen.spell}”")
            }
        }
        Text("Traditional dates and seasonal associations can vary by tradition and hemisphere. These are common Wheel of the Year observances.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(vertical = 12.dp))
    }
}

@Composable
private fun WheelGraphic(modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    Box(modifier.pointerInput(Unit) { detectTapGestures { offset ->
        val centerX = size.width / 2f; val centerY = size.height / 2f
        val angle = (atan2(offset.y - centerY, offset.x - centerX) * 180.0 / Math.PI + 360.0 + 90.0) % 360.0
        onSelect((floor((angle + 22.5) / 45.0).toInt() + 1) % 8)
    } }, contentAlignment = Alignment.Center) {
        Image(painter = painterResource(R.drawable.wheel_of_the_year), contentDescription = "Wheel of the Year with the eight sabbats, seasonal artwork, zodiac ring, and pentagram center",
            modifier = Modifier.fillMaxHeight().aspectRatio(1f), contentScale = ContentScale.Fit)
    }
}

private data class JournalEntry(val tag: String, val timestamp: String, val text: String)

@Composable
private fun JournalPage() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("magikal_journal", android.content.Context.MODE_PRIVATE) }
    var category by remember { mutableStateOf("Retrograde") }
    var target by remember { mutableStateOf(planets.first().name) }
    var note by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf(loadEntries(prefs)) }
    val categories = listOf("Retrograde", "Moon phase", "Sabbat", "General")
    val targets = when (category) {
        "Retrograde" -> planets.map { it.name }
        "Moon phase" -> moonPhaseNames
        "Sabbat" -> sabbats.map { it.name }
        else -> listOf("General")
    }
    LaunchedEffect(category) { target = targets.first() }
    val entryTag = if (category == "General") "General" else "$category · $target"
    PageColumn {
        SectionIntro("Witch's journal", "Keep your reflections close. Entries are stored on this device.")
        Text("WHAT ARE YOU JOURNALING?", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            categories.forEach { option ->
                FilterChip(selected = category == option, onClick = { category = option }, label = { Text(option, fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PanelLight, selectedLabelColor = Lilac, labelColor = Muted))
            }
        }
        if (category != "General") {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 2.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                targets.forEach { option ->
                    FilterChip(selected = target == option, onClick = { target = option }, label = { Text(option, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PanelLight, selectedLabelColor = Lilac, labelColor = Muted))
                }
            }
        }
        OutlinedTextField(value = note, onValueChange = { note = it }, modifier = Modifier.fillMaxWidth(), minLines = 4,
            placeholder = { Text("What are you noticing, learning, or setting an intention for?", color = Muted) },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Violet, unfocusedBorderColor = PanelLight, focusedTextColor = Lilac, unfocusedTextColor = Lilac, cursorColor = Violet))
        Button(onClick = {
            if (note.isNotBlank()) {
                val entry = JournalEntry(entryTag, LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a")), note.trim())
                entries = listOf(entry) + entries
                saveEntries(prefs, entries)
                note = ""
            }
        }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp), colors = ButtonDefaults.buttonColors(containerColor = Violet, contentColor = Ink), shape = RoundedCornerShape(14.dp)) {
            Text("Save journal entry", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("YOUR ENTRIES", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
            JournalTripleMoonPentacle(Modifier.size(width = 88.dp, height = 34.dp))
        }
        if (entries.isEmpty()) {
            Text("Your saved notes will appear here.", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp, bottom = 20.dp))
        }
        entries.forEachIndexed { index, entry ->
            Surface(color = Panel, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(entry.tag.uppercase(), color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(entry.timestamp, color = Muted, fontSize = 10.sp)
                    }
                    Text(entry.text, color = Lilac, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 8.dp))
                    Text("Delete", color = Violet, fontSize = 11.sp, modifier = Modifier.align(Alignment.End).clickable {
                        entries = entries.filterIndexed { i, _ -> i != index }; saveEntries(prefs, entries)
                    }.padding(top = 8.dp))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Journal notes stay on this device in this first build. There is no account sync or cloud backup yet.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
    }
}

@Composable
private fun JournalTripleMoonPentacle(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val cY = h * .5f
        val outer = Path().apply {
            moveTo(w * .03f, h * .10f)
            cubicTo(w * .32f, h * .12f, w * .32f, h * .88f, w * .03f, h * .90f)
            cubicTo(w * .20f, h * .73f, w * .20f, h * .27f, w * .03f, h * .10f)
            close()
            moveTo(w * .97f, h * .10f)
            cubicTo(w * .68f, h * .12f, w * .68f, h * .88f, w * .97f, h * .90f)
            cubicTo(w * .80f, h * .73f, w * .80f, h * .27f, w * .97f, h * .10f)
            close()
        }
        drawPath(outer, Gold)
        val center = Offset(w * .5f, cY)
        val radius = h * .43f
        drawCircle(Gold, radius, center, style = Stroke(width = (size.minDimension * .055f).coerceAtLeast(1.2f)))
        val points = (0..4).map { i ->
            val angle = Math.toRadians(-90.0 + i * 72.0)
            Offset(center.x + cos(angle).toFloat() * radius * .82f, center.y + sin(angle).toFloat() * radius * .82f)
        }
        val starOrder = listOf(0, 2, 4, 1, 3)
        val star = Path().apply {
            moveTo(points[starOrder.first()].x, points[starOrder.first()].y)
            starOrder.drop(1).forEach { index -> lineTo(points[index].x, points[index].y) }
            close()
        }
        drawPath(star, Gold, style = Stroke(width = (size.minDimension * .055f).coerceAtLeast(1.2f)))
    }
}
@Composable
private fun SectionIntro(title: String, subtitle: String) {
    Text(title, color = Lilac, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
    Text(subtitle, color = Muted, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))
}

@Composable
private fun InfoCard(title: String, body: String) {
    Surface(color = Panel, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Text(body, color = Lilac, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 5.dp))
        }
    }
}

@Composable
private fun InfoLine(title: String, body: String) {
    Column(Modifier.padding(bottom = 10.dp)) {
        Text(title, color = Gold, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text(body, color = Lilac, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 3.dp))
    }
}
package com.magikalretrogrades.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.location.LocationListener
import android.os.Bundle
import android.os.Build
import android.os.Looper
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

private val Ink = Color(0xFF100B18)
private val Panel = Color(0xFF21172E)
private val PanelLight = Color(0xFF30213F)
private val Violet = Color(0xFFB782FF)
private val Lilac = Color(0xFFE4CBFF)
private val Muted = Color(0xFFB9AFC4)
private val Gold = Color(0xFFE8C77A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(16, 11, 24)
        window.navigationBarColor = android.graphics.Color.rgb(16, 11, 24)
        setContent { MagikalApp() }
    }
}

private enum class Page(val label: String, val icon: String) {
    Retrogrades("Retrogrades", "☿"), Moon("Moon phases", "☾"), Wheel("Sabbats", "✺"), Journal("Journal", "✎"), Chart("Natal chart", "☉")
}

@Composable
private fun MagikalApp() {
    var page by remember { mutableStateOf(Page.Retrogrades) }
    val scheme = darkColorScheme(
        primary = Violet, onPrimary = Ink, secondary = Gold, background = Ink,
        surface = Panel, onBackground = Lilac, onSurface = Lilac
    )
    MaterialTheme(colorScheme = scheme) {
        Scaffold(
            containerColor = Ink,
            topBar = {
                AppHeader()
            },
            bottomBar = {
                Column(Modifier.fillMaxWidth().background(Color(0xFF17101F))) {
                    TripleMoonMark(Modifier.align(Alignment.CenterHorizontally).padding(top = 5.dp, bottom = 1.dp).size(width = 96.dp, height = 38.dp))
                    NavigationBar(containerColor = Color(0xFF17101F), contentColor = Violet) {
                        Page.entries.forEach { item ->
                            NavigationBarItem(
                                selected = page == item,
                                onClick = { page = item },
                                icon = { Text(item.icon, fontSize = 19.sp) },
                                label = { Text(item.label, fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = Lilac, selectedTextColor = Lilac,
                                    indicatorColor = PanelLight, unselectedIconColor = Muted, unselectedTextColor = Muted)
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                when (page) {
                    Page.Retrogrades -> RetrogradesPage()
                    Page.Moon -> MoonPage()
                    Page.Wheel -> WheelPage()
                    Page.Journal -> JournalPage()
                    Page.Chart -> NatalChartPage()
                }
            }
        }
    }
}

@Composable
private fun AppHeader() {
    Column(Modifier.fillMaxWidth().background(Ink).padding(horizontal = 16.dp, vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            BrandStar(Modifier.size(40.dp))
            Column(Modifier.padding(start = 7.dp), horizontalAlignment = Alignment.Start) {
                Text("Magikal Retrogrades", color = Lilac, fontSize = 24.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Cursive,
                    fontWeight = FontWeight.Medium, maxLines = 1)
            }
        }
        HorizontalDivider(color = Color(0xFF493650), thickness = 1.dp, modifier = Modifier.padding(top = 5.dp))
    }
}

@Composable
private fun BrandStar(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val orbit = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * .06f, h * .84f)
            cubicTo(w * .01f, h * .98f, w * .20f, h * .93f, w * .42f, h * .79f)
            cubicTo(w * .67f, h * .63f, w * .92f, h * .42f, w * .79f, h * .29f)
            cubicTo(w * .72f, h * .22f, w * .68f, h * .18f, w * .69f, h * .06f)
        }
        drawPath(orbit, Gold, style = androidx.compose.ui.graphics.drawscope.Stroke(width = (w * .045f).coerceAtLeast(1.4f), cap = androidx.compose.ui.graphics.StrokeCap.Round))
        fun star(cx: Float, cy: Float, rx: Float, ry: Float) = androidx.compose.ui.graphics.Path().apply {
            moveTo(cx, cy - ry); quadraticTo(cx - rx * .13f, cy - ry * .13f, cx - rx, cy)
            quadraticTo(cx - rx * .13f, cy + ry * .13f, cx, cy + ry)
            quadraticTo(cx + rx * .13f, cy + ry * .13f, cx + rx, cy)
            quadraticTo(cx + rx * .13f, cy - ry * .13f, cx, cy - ry); close()
        }
        drawPath(star(w * .68f, h * .30f, w * .23f, h * .29f), Lilac)
        drawPath(star(w * .63f, h * .61f, w * .17f, h * .21f), Violet)
    }
}

@Composable
private fun TripleMoonMark(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = (size.minDimension * .035f).coerceAtLeast(1.2f), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        val mark = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * .08f, h * .08f)
            cubicTo(w * .39f, h * .08f, w * .39f, h * .92f, w * .08f, h * .92f)
            moveTo(w * .08f, h * .08f)
            cubicTo(w * .28f, h * .22f, w * .28f, h * .78f, w * .08f, h * .92f)
            moveTo(w * .92f, h * .08f)
            cubicTo(w * .61f, h * .08f, w * .61f, h * .92f, w * .92f, h * .92f)
            moveTo(w * .92f, h * .08f)
            cubicTo(w * .72f, h * .22f, w * .72f, h * .78f, w * .92f, h * .92f)
        }
        drawPath(mark, Lilac, style = stroke)
        drawCircle(Lilac, radius = h * .41f, center = androidx.compose.ui.geometry.Offset(w * .5f, h * .5f), style = stroke)
        drawCircle(Lilac, radius = h * .018f, center = androidx.compose.ui.geometry.Offset(w * .56f, h * .36f))
    }
}

@Composable
private fun PageColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 10.dp), content = content)
}

private data class Planet(
    val name: String, val glyph: String, val horizonsId: String?, val colors: List<Color>,
    val meaning: String, val retro: String, val practice: String, val ritual: String,
    val materials: String, val steps: List<String>, val spell: String, val avoid: String, val crystal: String
)
private val planets = listOf(
    Planet("Mercury", "☿", "199", listOf(Color(0xFFC6B7A6), Color(0xFF665D64)), "Communication, learning, and exchange", "Review conversations, plans, and details; leave room to revise.", "Pause before sending an important message and double-check travel plans.", "Write down one conversation you want to revisit, then draft a calmer version.", "Journal, a purple pen, and a glass of water.", listOf("Set the water beside your journal and take three slow breaths.", "Write the message or plan you want to review; underline the detail that needs checking.", "Draft one clear next question or correction before taking action."), "May I listen clearly, speak thoughtfully, and make room to review.", "Avoid rushing agreements or assuming a message was understood.", "Fluorite"),
    Planet("Venus", "♀", "299", listOf(Color(0xFFFFA77D), Color(0xFF8E3A65)), "Love, values, beauty, and connection", "Reflect on what you value and how you nurture relationships.", "Reconnect with a creative practice or write about your needs.", "Place rose quartz beside a written list of what care and respect look like to you.", "Rose quartz or another keepsake, paper, and a pen.", listOf("Write three qualities you want in a caring relationship, including how you want to treat yourself.", "Circle one value you can practice this week.", "Name one kind boundary that protects that value."), "May care, honesty, and self-respect guide my choices.", "Avoid making a major purchase just to soothe a passing feeling.", "Rose quartz"),
    Planet("Mars", "♂", "499", listOf(Color(0xFFFF765E), Color(0xFF7D263D)), "Drive, courage, and action", "Review how you use energy, desire, and assertiveness.", "Choose steady movement and a clear, manageable goal.", "Take three steady breaths, then choose one safe action that uses your energy well.", "Red paper, a pen, and an optional red jasper stone.", listOf("Write one goal that matters to you.", "Break it into a first step small enough to do safely.", "Schedule a pause or rest period alongside the action."), "May courage move with care and clear intention.", "Avoid escalating conflict or forcing progress through exhaustion.", "Red jasper"),
    Planet("Jupiter", "♃", "599", listOf(Color(0xFFF5C38A), Color(0xFF684C86)), "Growth, wisdom, and possibility", "Revisit beliefs, learning, and the promises you have made yourself.", "Study a subject you love and celebrate quiet progress.", "Choose a book or lesson and learn without rushing the outcome.", "A book, notebook, and an optional amethyst stone.", listOf("Write a belief or goal you want to reconsider.", "List one thing you have learned and one open question.", "Choose a realistic next step and a date to review it."), "May wisdom keep my growth generous and grounded.", "Avoid overcommitting or treating optimism as a guarantee.", "Amethyst"),
    Planet("Saturn", "♄", "699", listOf(Color(0xFFD2C2A2), Color(0xFF454047)), "Boundaries, responsibility, and structure", "Review commitments and strengthen foundations patiently.", "Tidy one small area or create a realistic routine.", "Clear one small surface and decide what belongs there.", "Paper, a pen, and a small stone as a reminder.", listOf("List the commitments currently using your time.", "Mark one boundary or obligation that needs review.", "Write one realistic routine step you can maintain."), "May steady boundaries support a life with room to breathe.", "Avoid harsh self-judgment or taking on every burden alone.", "Smoky quartz"),
    Planet("Uranus", "♅", "799", listOf(Color(0xFF9CE7DF), Color(0xFF396D88)), "Change, originality, and freedom", "Notice where you want more honesty, space, or experimentation.", "Try a low-stakes new approach and record what surprises you.", "Sketch an unconventional solution to a small problem, then test it in a reversible way.", "Paper, colored pencils, and an optional labradorite stone.", listOf("Choose a small routine you want to improve.", "Sketch two fresh approaches, including the simplest one.", "Test one reversible change and note what happens."), "May thoughtful change make space for honest freedom.", "Avoid abrupt changes made only to escape discomfort.", "Labradorite"),
    Planet("Neptune", "♆", "899", listOf(Color(0xFF9CB4FF), Color(0xFF524178)), "Dreams, intuition, and imagination", "Reflect on inspiration, boundaries, and what feels unclear.", "Keep a dream note and make time for a gentle creative ritual.", "Record impressions without treating them as proof or instruction.", "A journal, a blue or violet pencil, and a glass of water.", listOf("Write an image, dream, or idea without judging it.", "Separate what you observed from what you imagine it means.", "Pair the idea with one practical action you can verify."), "May imagination be gentle, clear, and grounded.", "Avoid ignoring practical details or treating a hunch as proof.", "Lepidolite"),
    Planet("Pluto", "♇", "999", listOf(Color(0xFF9A79C7), Color(0xFF34253F)), "Transformation, depth, and renewal", "Explore what is ready to change and what you are ready to release.", "Write a private release note and choose one supportive next step.", "Write one habit or expectation you are ready to loosen; keep the note private or tear it up when finished.", "Dark paper, a pen, and a bowl for holding the note.", listOf("Name one pattern you are ready to examine, without shaming yourself.", "Write what support or boundary would make change safer.", "Choose one small step; keep or recycle the note when you are ready."), "May I meet change with patience, support, and choice.", "Avoid pressuring yourself or others into sudden transformation.", "Obsidian"),
    Planet("Chiron", "⚷", "DES=2060", listOf(Color(0xFFB8A6CA), Color(0xFF5D4B70)), "Vulnerability, learning, and compassionate repair", "Reflect on old sensitivities with care and notice where support helps you grow.", "Name a lesson you have learned and one boundary that protects your wellbeing.", "Write about a challenge only as much as feels safe today, then name what helped you through it.", "A journal, a soft-colored pen, and an optional comforting keepsake.", listOf("Write about a challenge only as much as feels safe today.", "Note what support, skill, or kindness helped you through it.", "Choose one small act of care you can offer yourself this week."), "May I meet tender places with patience, support, and compassion.", "Avoid reopening painful memories without support or treating a crystal as a substitute for care.", "Rose quartz")
)

private const val DISPLAY_ZONE_PREFS = "magikal_display_preferences"
private const val DISPLAY_ZONE_KEY = "display_time_zone"
private fun getDisplayZone(context: Context): java.time.ZoneId {
    val saved = context.getSharedPreferences(DISPLAY_ZONE_PREFS, Context.MODE_PRIVATE).getString(DISPLAY_ZONE_KEY, null)
    return runCatching { java.time.ZoneId.of(saved ?: java.time.ZoneId.systemDefault().id) }.getOrDefault(java.time.ZoneId.systemDefault())
}
private fun saveDisplayZone(context: Context, zoneId: String) {
    context.getSharedPreferences(DISPLAY_ZONE_PREFS, Context.MODE_PRIVATE).edit().putString(DISPLAY_ZONE_KEY, zoneId).apply()
}

@Composable
private fun DisplayZonePicker(selectedId: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val zones = remember { java.util.TimeZone.getAvailableIDs().distinct().sorted() }
    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text("DISPLAY TIME ZONE · LOCATION", color = Gold, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
        Box(Modifier.fillMaxWidth().padding(top = 5.dp)) {
            Surface(color = PanelLight, shape = RoundedCornerShape(13.dp), modifier = Modifier.fillMaxWidth().clickable { expanded = true }) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(selectedId.replace('_', ' '), color = Lilac, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text("▾", color = Gold, fontSize = 18.sp)
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                zones.forEach { zone ->
                    DropdownMenuItem(text = { Text(zone.replace('_', ' '), maxLines = 1) }, onClick = { onSelect(zone); expanded = false })
                }
            }
        }
        Text("Choose any region. Retrograde and Moon event times convert to this time zone in both hemispheres.", color = Muted, fontSize = 9.sp, lineHeight = 13.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun RetrogradesPage() {
    var openPlanet by remember { mutableStateOf<String?>(null) }
    var feed by remember { mutableStateOf(RetrogradeTracker.fallbackFeed()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    var displayZoneId by remember { mutableStateOf(getDisplayZone(context).id) }
    val displayZone = runCatching { java.time.ZoneId.of(displayZoneId) }.getOrDefault(java.time.ZoneId.systemDefault())
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1_000)
        }
    }
    LaunchedEffect(refreshKey) {
        loading = true
        error = null
        try {
            feed = RetrogradeTracker.load(context, planets.mapNotNull { planet -> planet.horizonsId?.let { TrackerPlanet(planet.name, it) } }, forceRefresh = refreshKey > 0)
        } catch (failure: Exception) {
            feed = RetrogradeTracker.fallbackFeed(listOf("Using the built-in station calendar; refresh when online to check for updated ephemeris data."))
            error = null
        } finally {
            loading = false
        }
    }
    PageColumn {
        SectionIntro("Planetary retrogrades", "Explore each planet's symbolism and reflective practices.")
        DisplayZonePicker(displayZoneId) { zone -> displayZoneId = zone; saveDisplayZone(context, zone) }
        InfoCard("LIVE PLANET TRACKER", "Dates are calculated from geocentric planetary motion. Station instants are shared worldwide; displayed times use ${displayZone.id}.")
        when {
            loading -> InfoCard("UPDATING THE SKY", "Fetching planetary positions from NASA/JPL Horizons…")
            error != null -> InfoCard("TRACKING TEMPORARILY UNAVAILABLE", "${error}. Tap Refresh dates to try again. Your guide content is still available offline.")
            feed != null -> {
                val updated = formatLocalEventTime(feed!!.fetchedAtMillis, displayZone)
                InfoCard("DATA UPDATED", "Last refreshed $updated. Station times are estimated from two-day ephemeris samples.")
                if (feed!!.warnings.isNotEmpty()) {
                    InfoCard("SOME PLANETS DIDN'T UPDATE", feed!!.warnings.joinToString(" · "))
                }
            }
        }
        val activeNow = feed?.events.orEmpty().filter { it.startUtcMillis <= nowMillis && it.endUtcMillis > nowMillis }.sortedBy { it.startUtcMillis }
        val upcomingNow = feed?.events.orEmpty().filter { it.startUtcMillis > nowMillis }.sortedBy { it.startUtcMillis }
        when {
            activeNow.isNotEmpty() -> InfoCard("CURRENT RETROGRADES", activeNow.joinToString("\n") { "${it.planet}${if (it.zodiacSign.isNotBlank()) " in ${it.zodiacSign}" else ""} · ends ${formatLocalEventTime(it.endUtcMillis, displayZone)} · ${formatCountdown(it.endUtcMillis - nowMillis)} remaining" })
            upcomingNow.isNotEmpty() -> InfoCard("NO RETROGRADES RIGHT NOW · NEXT", upcomingNow.take(3).joinToString("\n") { "${it.planet}${if (it.zodiacSign.isNotBlank()) " in ${it.zodiacSign}" else ""} · starts ${formatLocalEventTime(it.startUtcMillis, displayZone)} · in ${formatCountdown(it.startUtcMillis - nowMillis)}" })
            loading -> InfoCard("CURRENT RETROGRADES", "Updating station dates in ${displayZone.id}. The built-in calendar keeps countdowns visible while data refreshes.")
            error != null -> InfoCard("TRACKING TEMPORARILY UNAVAILABLE", "${error}. Built-in current and upcoming dates remain visible.")
            feed != null -> InfoCard("NO CURRENT OR UPCOMING RETROGRADES", "No station was found in the available forecast.")
        }
        Button(onClick = { refreshKey++ }, enabled = !loading, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Violet, contentColor = Ink), shape = RoundedCornerShape(14.dp)) {
            Text(if (loading) "Updating…" else "Refresh dates", fontWeight = FontWeight.Bold)
        }
        planets.forEach { planet ->
            val periods = feed?.events?.filter { it.planet == planet.name }?.sortedBy { it.startUtcMillis }.orEmpty()
            val active = periods.firstOrNull { it.startUtcMillis <= nowMillis && it.endUtcMillis > nowMillis }
            val next = periods.firstOrNull { it.startUtcMillis > nowMillis }
            Surface(color = Panel, shape = RoundedCornerShape(19.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable { openPlanet = if (openPlanet == planet.name) null else planet.name }) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PlanetOrb(planet, Modifier.size(52.dp))
                        Column(Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(planet.name, color = Lilac, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                            Text(planet.meaning, color = Muted, fontSize = 11.sp)
                            if (loading && feed == null) Text("Checking current motion…", color = Violet, fontSize = 10.sp)
                                                        else if (feed != null) Text(
                                when {
                                    active != null -> "RETROGRADE NOW · ends ${formatLocalEventTime(active.endUtcMillis, displayZone)}"
                                    next != null -> "NEXT · starts ${formatLocalEventTime(next.startUtcMillis, displayZone)}"
                                    else -> "No station found in the 2-year forecast"
                                }, color = if (active != null) Gold else Violet, fontSize = 10.sp, lineHeight = 14.sp
                            )
                        }
                        Text(planet.glyph, color = Violet, fontSize = 24.sp)
                    }
                    if (planet.horizonsId != null && feed != null) {
                        HorizontalDivider(color = PanelLight, modifier = Modifier.padding(vertical = 10.dp))
                        when {
                            active != null -> {
                                EventCountdown("CURRENT RETROGRADE", active, nowMillis, displayZone, isActive = true)
                                val following = periods.firstOrNull { it.startUtcMillis >= active.endUtcMillis }
                                if (following != null) {
                                    Spacer(Modifier.height(10.dp))
                                    EventCountdown("NEXT RETROGRADE", following, nowMillis, displayZone, isActive = false)
                                }
                            }
                            next != null -> EventCountdown("NEXT RETROGRADE", next, nowMillis, displayZone, isActive = false)
                            else -> Text("No upcoming station was found in the current two-year forecast.", color = Muted, fontSize = 11.sp)
                        }
                    }
                    if (openPlanet == planet.name) {
                        HorizontalDivider(color = PanelLight, modifier = Modifier.padding(vertical = 12.dp))
                        InfoLine("SPIRITUAL MEANING", planet.meaning)
                        InfoLine("RETROGRADE REFLECTION", planet.retro)
                        InfoLine("PRACTICE", planet.practice)
                        InfoLine("RITUAL", planet.ritual)
                        InfoLine("WHAT YOU NEED", planet.materials)
                        planet.steps.forEachIndexed { index, step -> InfoLine("RITUAL STEP ${index + 1}", step) }
                        InfoLine("SPELL / INTENTION", planet.spell)
                        InfoLine("WHAT NOT TO DO", planet.avoid)
                        InfoLine("CRYSTAL", "${planet.crystal} (a symbolic companion, not a substitute for care)")
                    }
                }
            }
        }
    }
}

@Composable
private fun EventCountdown(title: String, event: RetrogradeEvent, nowMillis: Long, zone: java.time.ZoneId, isActive: Boolean) {
    Text(title + if (event.zodiacSign.isNotBlank()) " · ${event.zodiacSign}" else " · zodiac updates with live ephemeris", color = Gold, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
    TimelineMoment("${if (isActive) "Started" else "Starts"} · ${formatLocalEventTime(event.startUtcMillis, zone)}",
        if (isActive) "${formatCountdown(nowMillis - event.startUtcMillis)} ago" else "In ${formatCountdown(event.startUtcMillis - nowMillis)}")
    Spacer(Modifier.height(6.dp))
    TimelineMoment("Ends · ${formatLocalEventTime(event.endUtcMillis, zone)}", "In ${formatCountdown(event.endUtcMillis - nowMillis)}")
}

@Composable
private fun TimelineMoment(label: String, countdown: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Lilac, fontSize = 10.sp, lineHeight = 14.sp, modifier = Modifier.weight(1f))
        Text(countdown, color = Violet, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
    }
}

private fun formatCountdown(milliseconds: Long): String {
    val totalSeconds = (milliseconds.coerceAtLeast(0L) / 1_000L)
    val days = totalSeconds / 86_400L
    val hours = (totalSeconds % 86_400L) / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (days > 0) "${days}d ${hours}h ${minutes}m ${seconds}s" else "${hours}h ${minutes}m ${seconds}s"
}

@Composable
private fun PlanetOrb(planet: Planet, modifier: Modifier = Modifier) {
    val sprite = ImageBitmap.imageResource(R.drawable.planet_sprite)
    val index = mapOf("Mercury" to 0, "Venus" to 1, "Mars" to 2, "Jupiter" to 3, "Saturn" to 4, "Uranus" to 5, "Neptune" to 6, "Pluto" to 7, "Chiron" to 8)[planet.name] ?: 0
    val tile = sprite.width / 3
    Canvas(modifier) {
        drawImage(image = sprite, srcOffset = IntOffset((index % 3) * tile, (index / 3) * tile),
            srcSize = IntSize(tile, tile), dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.toInt(), size.height.toInt()))
    }
}

private data class MoonInfo(
    val name: String, val illumination: Int, val advice: String, val ritual: String,
    val materials: String, val steps: List<String>, val avoid: String
)
private data class MoonPhaseEvent(val name: String, val atMillis: Long)
private data class MoonSnapshot(val phase: Double, val info: MoonInfo, val ageDays: Double, val upcoming: List<MoonPhaseEvent>)
private data class MoonPosition(val altitude: Double, val azimuth: Double, val distanceKm: Double, val zodiac: String, val rightAscension: Double, val declination: Double)
private data class MoonHorizon(val riseMillis: Long?, val setMillis: Long?, val transitMillis: Long?, val transitAltitude: Double)
private val moonPhaseNames = listOf("New Moon", "Waxing Crescent", "First Quarter", "Waxing Gibbous", "Full Moon", "Waning Gibbous", "Last Quarter", "Waning Crescent")

private fun moonSnapshot(): MoonSnapshot {
    val nowMillis = System.currentTimeMillis()
    val knownNewMoonMillis = 947182440_000.0 // 2000-01-06 18:14 UTC
    val cycleMillis = 29.530588853 * 86_400_000.0
    val phase = (((nowMillis - knownNewMoonMillis) % cycleMillis) + cycleMillis) % cycleMillis / cycleMillis
    val illumination = ((1 - cos(phase * 2 * Math.PI)) * 50).toInt().coerceIn(0, 100)
    val phaseIndex = floor(phase * 8).toInt().coerceIn(0, 7)
    val ageDays = phase * 29.530588853
    val upcoming = upcomingMajorMoonPhases(nowMillis, knownNewMoonMillis, cycleMillis)
    return MoonSnapshot(phase, moonGuidance(moonPhaseNames[phaseIndex], illumination), ageDays, upcoming)
}

private fun upcomingMajorMoonPhases(nowMillis: Long, epochMillis: Double, cycleMillis: Double): List<MoonPhaseEvent> {
    val names = listOf("New Moon", "First Quarter", "Full Moon", "Last Quarter")
    val currentCycle = floor((nowMillis - epochMillis) / cycleMillis).toLong()
    return (currentCycle..currentCycle + 2).flatMap { cycle ->
        names.mapIndexed { quarter, name -> MoonPhaseEvent(name, (epochMillis + (cycle + quarter / 4.0) * cycleMillis).toLong()) }
    }.filter { it.atMillis > nowMillis }.sortedBy { it.atMillis }.take(4)
}

private fun moonPositionAt(timeMillis: Long, latitude: Double, longitude: Double): MoonPosition {
    val rad = Math.PI / 180.0
    val d = timeMillis / 86_400_000.0 - 10_957.5
    val l = rad * (218.316 + 13.176396 * d)
    val m = rad * (134.963 + 13.064993 * d)
    val f = rad * (93.272 + 13.229350 * d)
    val lambda = l + rad * 6.289 * sin(m)
    val beta = rad * 5.128 * sin(f)
    val distanceKm = 385_001.0 - 20_905.0 * cos(m)
    val obliquity = rad * 23.4397
    val rightAscension = atan2(sin(lambda) * cos(obliquity) - kotlin.math.tan(beta) * sin(obliquity), cos(lambda))
    val declination = kotlin.math.asin(sin(beta) * cos(obliquity) + cos(beta) * sin(lambda) * sin(obliquity))
    val latitudeRad = latitude * rad
    val hourAngle = rad * (280.16 + 360.9856235 * d + longitude) - rightAscension
    val altitude = kotlin.math.asin(sin(latitudeRad) * sin(declination) + cos(latitudeRad) * cos(declination) * cos(hourAngle)) / rad
    val azimuth = ((atan2(sin(hourAngle), cos(hourAngle) * sin(latitudeRad) - kotlin.math.tan(declination) * cos(latitudeRad)) / rad + 180.0) % 360.0 + 360.0) % 360.0
    val eclipticLongitude = ((atan2(sin(rightAscension) * cos(obliquity) + kotlin.math.tan(declination) * sin(obliquity), cos(rightAscension)) / rad % 360.0) + 360.0) % 360.0
    val signs = listOf("Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo", "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces")
    return MoonPosition(altitude, azimuth, distanceKm, signs[(eclipticLongitude / 30.0).toInt().coerceIn(0, 11)], rightAscension, declination)
}

private fun moonBrightLimbAngle(timeMillis: Long, latitude: Double, longitude: Double): Double {
    val rad = Math.PI / 180.0
    val d = timeMillis / 86_400_000.0 - 10_957.5
    val l = rad * (280.46 + .9856474 * d)
    val g = rad * (357.528 + .9856003 * d)
    val solarLongitude = l + rad * (1.915 * sin(g) + .020 * sin(2 * g))
    val obliquity = rad * (23.4393 - .0000004 * d)
    val sunRa = atan2(cos(obliquity) * sin(solarLongitude), cos(solarLongitude))
    val sunDec = kotlin.math.asin(sin(obliquity) * sin(solarLongitude))
    val moon = moonPositionAt(timeMillis, latitude, longitude)
    val delta = sunRa - moon.rightAscension
    val brightLimb = atan2(cos(sunDec) * sin(delta), sin(sunDec) * cos(moon.declination) - cos(sunDec) * sin(moon.declination) * cos(delta))
    val hourAngle = rad * (280.16 + 360.9856235 * d + longitude) - moon.rightAscension
    val phi = latitude * rad
    val parallactic = atan2(sin(hourAngle), kotlin.math.tan(phi) * cos(moon.declination) - sin(moon.declination) * cos(hourAngle))
    return (brightLimb - parallactic) / rad
}

@Suppress("DEPRECATION")
private fun requestCurrentObserverLocation(context: Context, callback: (Location?) -> Unit) {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val provider = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .firstOrNull { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
    if (provider == null) { callback(lastKnownObserverLocation(context)); return }
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            manager.getCurrentLocation(provider, null, context.mainExecutor) { location ->
                callback(location ?: lastKnownObserverLocation(context))
            }
        } else {
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    runCatching { manager.removeUpdates(this) }
                    callback(location)
                }
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            }
            manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
        }
    } catch (_: SecurityException) { callback(lastKnownObserverLocation(context)) }
      catch (_: Exception) { callback(lastKnownObserverLocation(context)) }
}

private fun moonHorizon(nowMillis: Long, latitude: Double, longitude: Double): MoonHorizon {
    val threshold = -0.3
    val sampleStep = 30L * 60L * 1000L
    var previousTime = nowMillis
    var previousAltitude = moonPositionAt(previousTime, latitude, longitude).altitude - threshold
    var rise: Long? = null
    var set: Long? = null
    var transit: Long? = null
    var maxAltitude = -90.0
    var t = nowMillis
    while (t <= nowMillis + 48L * 60L * 60L * 1000L) {
        val altitude = moonPositionAt(t, latitude, longitude).altitude
        if (t <= nowMillis + 24L * 60L * 60L * 1000L && altitude > maxAltitude) { maxAltitude = altitude; transit = t }
        val adjusted = altitude - threshold
        if (previousAltitude <= 0.0 && adjusted > 0.0 && rise == null) {
            val fraction = (previousAltitude / (previousAltitude - adjusted)).coerceIn(0.0, 1.0)
            rise = previousTime + (sampleStep * fraction).toLong()
        } else if (previousAltitude >= 0.0 && adjusted < 0.0 && set == null) {
            val fraction = (previousAltitude / (previousAltitude - adjusted)).coerceIn(0.0, 1.0)
            set = previousTime + (sampleStep * fraction).toLong()
        }
        previousAltitude = adjusted
        previousTime = t
        t += sampleStep
    }
    return MoonHorizon(rise, set, transit, maxAltitude)
}

private fun lastKnownObserverLocation(context: Context): Location? = try {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    manager.getProviders(true).mapNotNull { provider -> manager.getLastKnownLocation(provider) }
        .minByOrNull { it.accuracy }
} catch (_: SecurityException) { null } catch (_: Exception) { null }

private fun moonGuidance(name: String, illumination: Int): MoonInfo = when (name) {
    "New Moon" -> MoonInfo(name, illumination, "Set gentle intentions and make room for a fresh beginning.", "Intention-seed ritual", "A small seed, a cup or pot with soil, water, and paper with a pen.", listOf("Write one intention in a single clear sentence.", "Plant the seed or place it beside the note as a symbol of beginning.", "Choose one small action you can take this week and date it."), "Avoid demanding immediate results from a new goal.")
    "Waxing Crescent" -> MoonInfo(name, illumination, "Nurture plans with curiosity and steady effort.", "First-step candle or LED ritual", "A candle used safely or an LED candle, paper, and a pen.", listOf("Write the next small step toward a goal.", "Light the candle safely, or switch on the LED candle, while naming the step.", "Place the note where it can remind you to follow through."), "Avoid comparing your beginning with someone else's progress.")
    "First Quarter" -> MoonInfo(name, illumination, "Meet a challenge with a thoughtful choice.", "Two-path clarity ritual", "Two small stones or objects, paper, and a pen.", listOf("Name one obstacle without judging yourself.", "Set one object beside each practical way forward.", "Choose the safer next step and write when you will try it."), "Avoid treating every delay as a sign to give up.")
    "Waxing Gibbous" -> MoonInfo(name, illumination, "Refine, prepare, and tend what is already growing.", "Intention tune-up", "Your original intention, a journal, and a pen.", listOf("Read an intention you have been working with.", "Underline what still fits and cross out what needs changing.", "Write one useful adjustment and put it into practice."), "Avoid overworking a plan that already needs rest.")
    "Full Moon" -> MoonInfo(name, illumination, "Celebrate, notice what is illuminated, and release what feels complete.", "Gratitude and release ritual", "A bowl of water, paper, a pen, and an optional LED candle.", listOf("Write three things you appreciate and one thing you are ready to release.", "Read the gratitude aloud; tear or recycle the release note.", "Pour the water into a plant or down the sink, then choose a restful closing moment."), "Avoid making emotionally charged choices in haste.")
    "Waning Gibbous" -> MoonInfo(name, illumination, "Share gratitude and integrate what you have learned.", "Lesson and thanks ritual", "A journal, a pen, and an optional cup of tea.", listOf("Write one lesson from the past month.", "Name a person, place, or practice that supported you.", "Choose one way to carry that learning forward."), "Avoid holding on to a routine that no longer serves you.")
    "Last Quarter" -> MoonInfo(name, illumination, "Clear space and reconsider what deserves your energy.", "Gentle clearing ritual", "A cloth, a small bowl, paper, and a pen.", listOf("Clear one small surface or tidy one drawer.", "Write one boundary that would protect your time or energy.", "Keep the note in your journal and revisit it next week."), "Avoid turning reflection into self-criticism.")
    else -> MoonInfo("Waning Crescent", illumination, "Rest, restore, and prepare for a quieter reset.", "Quiet reset ritual", "A comfortable blanket, water or calming tea, and a journal if useful.", listOf("Settle somewhere comfortable and take several unhurried breaths.", "Write down one thing you can leave until later.", "Choose one restorative activity and give yourself time for it."), "Avoid filling every moment before the next cycle begins.")
}

@Composable
private fun MoonOrb(illumination: Int, phaseName: String, modifier: Modifier = Modifier) {
    Box(modifier.clip(CircleShape)) {
        Image(painterResource(R.drawable.moon_globe), contentDescription = "Still cratered lunar surface", contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize())
        Canvas(Modifier.fillMaxSize().clip(CircleShape)) {
            val r = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val lit = illumination.coerceIn(0, 100) / 100f
            if (lit < .98f) {
                val waxing = phaseName.startsWith("Waxing") || phaseName == "First Quarter"
                val offset = lit * r * 2f
                val shadowX = center.x + if (waxing) offset else -offset
                drawCircle(Brush.radialGradient(listOf(Color(0xFF30283A).copy(alpha = .96f), Color(0xFF15101B)),
                    center = Offset(shadowX, center.y), radius = r * 1.65f), r, Offset(shadowX, center.y))
            }
            drawCircle(Color.White.copy(alpha = .16f), r, center, style = Stroke(width = 1.dp.toPx()))
        }
    }
}

@Composable
private fun MoonPage() {
    var snapshot by remember { mutableStateOf(moonSnapshot()) }
    var selectedPhase by remember { mutableStateOf(snapshot.info.name) }
    var observerLocation by remember { mutableStateOf<Location?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    var displayZoneId by remember { mutableStateOf(getDisplayZone(context).id) }
    val displayZone = runCatching { java.time.ZoneId.of(displayZoneId) }.getOrDefault(java.time.ZoneId.systemDefault())
    var locationAccessDenied by remember { mutableStateOf(false) }
    var showLocationSettings by remember { mutableStateOf(false) }
    var requestingLocation by remember { mutableStateOf(false) }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true || grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            requestingLocation = true
            requestCurrentObserverLocation(context) { location -> observerLocation = location; requestingLocation = false; locationAccessDenied = location == null }
        } else { requestingLocation = false; locationAccessDenied = true; showLocationSettings = true }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            snapshot = moonSnapshot()
        }
    }
    LaunchedEffect(snapshot.info.name) { selectedPhase = snapshot.info.name }
    val selectedIsCurrent = selectedPhase == snapshot.info.name
    val selectedInfo = if (selectedIsCurrent) snapshot.info else moonGuidance(selectedPhase, approximateMidIllumination(selectedPhase))
    PageColumn {
        SectionIntro("Moon phases", "Explore every phase, its spiritual themes, and practices to try.")
        DisplayZonePicker(displayZoneId) { zone -> displayZoneId = zone; saveDisplayZone(context, zone) }
        Surface(color = Panel, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("MOON AT A GLANCE", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                Text(formatLocalEventTime(System.currentTimeMillis(), displayZone), color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp, bottom = 10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("LUNAR AGE" to "${"%.1f".format(snapshot.ageDays)} days", "ILLUMINATION" to "${snapshot.info.illumination}%").forEach { (label, value) ->
                        Column(Modifier.weight(1f).background(PanelLight, RoundedCornerShape(12.dp)).padding(10.dp)) {
                            Text(label, color = Gold, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
                            Text(value, color = Lilac, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
                Text("CURRENT PHASE BY HEMISPHERE", color = Gold, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp, modifier = Modifier.padding(top = 12.dp))
                Text(if (observerLocation == null) "The lunar phase is the same worldwide. Allow location access to calculate the Moon’s local viewing angle for your area." else "Location-based view updated for your device. The phase is shared worldwide; the Moon’s orientation is calculated for this place and time.", color = Muted, fontSize = 10.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 4.dp, bottom = 7.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Northern hemisphere" to 1, "Southern hemisphere" to -1).forEach { (hemisphereName, sign) ->
                        Surface(color = PanelLight, shape = RoundedCornerShape(13.dp), modifier = Modifier.weight(1f)) {
                            Row(Modifier.padding(9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val latitude = observerLocation?.latitude?.let { kotlin.math.abs(it) * sign } ?: (35.0 * sign)
                                val longitude = observerLocation?.longitude ?: 0.0
                                val viewAngle = moonBrightLimbAngle(System.currentTimeMillis(), latitude, longitude)
                                MoonOrb(snapshot.info.illumination, snapshot.info.name, Modifier.size(38.dp).graphicsLayer(rotationZ = viewAngle.toFloat()))
                                Column {
                                    Text(hemisphereName, color = Gold, fontSize = 9.sp, lineHeight = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text(snapshot.info.name, color = Lilac, fontSize = 11.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 3.dp))
                                    Text("${snapshot.info.illumination}% illuminated", color = Muted, fontSize = 8.sp, lineHeight = 11.sp)
                                }
                            }
                        }
                    }
                }
                val nextFull = snapshot.upcoming.firstOrNull { it.name == "Full Moon" }
                val nextNew = snapshot.upcoming.firstOrNull { it.name == "New Moon" }
                if (nextFull != null) InfoLine("NEXT FULL MOON", formatLocalEventTime(nextFull.atMillis, displayZone))
                if (nextNew != null) InfoLine("NEXT NEW MOON", formatLocalEventTime(nextNew.atMillis, displayZone))
                Text("NEXT PHASES", color = Gold, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp, modifier = Modifier.padding(top = 10.dp, bottom = 3.dp))
                snapshot.upcoming.forEach { phase -> TimelineMoment(phase.name, formatLocalEventTime(phase.atMillis, displayZone)) }
                Button(onClick = {
                    val hasFine = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    val hasCoarse = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    if (locationAccessDenied && !hasFine && !hasCoarse) showLocationSettings = true
                    else if (hasFine || hasCoarse) {
                        requestingLocation = true
                        requestCurrentObserverLocation(context) { location -> observerLocation = location; requestingLocation = false; locationAccessDenied = location == null }
                    } else locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PanelLight, contentColor = Lilac), shape = RoundedCornerShape(13.dp)) {
                    Text(when { requestingLocation -> "Getting your device location…"; observerLocation == null -> "Use my location for local sky details"; else -> "Refresh local sky details" }, fontSize = 12.sp)
                }
                val location = observerLocation
                if (location != null) {
                    val moonNow = moonPositionAt(System.currentTimeMillis(), location.latitude, location.longitude)
                    val horizon = moonHorizon(System.currentTimeMillis(), location.latitude, location.longitude)
                    InfoLine("MOONRISE", horizon.riseMillis?.let { formatLocalEventTime(it, displayZone) } ?: "No rise in the next 48 hours")
                    InfoLine("MOONSET", horizon.setMillis?.let { formatLocalEventTime(it, displayZone) } ?: "No set in the next 48 hours")
                    if (horizon.transitMillis != null) InfoLine("MERIDIAN PASSING", "${formatLocalEventTime(horizon.transitMillis, displayZone)} · ${"%.0f".format(horizon.transitAltitude)}° altitude")
                    InfoLine("CURRENT ALTITUDE / AZIMUTH", "${"%.0f".format(moonNow.altitude)}° / ${"%.0f".format(moonNow.azimuth)}°")
                    InfoLine("DISTANCE", "${moonNow.distanceKm.toInt()} km")
                    InfoLine("TROPICAL ZODIAC", moonNow.zodiac)
                } else {
                    Text("Optional location access adds approximate local moonrise, moonset, sky position, distance, and zodiac details.", color = Muted, fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 7.dp))
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            moonPhaseNames.forEach { phaseName ->
                Surface(color = if (selectedPhase == phaseName) PanelLight else Panel,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedPhase == phaseName) Violet else Color(0xFF493650)),
                    modifier = Modifier.fillMaxWidth().clickable { selectedPhase = phaseName }) {
                    Text(phaseName, color = if (selectedPhase == phaseName) Lilac else Muted, fontSize = 13.sp,
                        maxLines = 1, modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp))
                }
            }
        }
        Surface(color = Panel, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                MoonOrb(if (selectedIsCurrent) snapshot.info.illumination else approximateMidIllumination(selectedPhase), selectedPhase, Modifier.size(142.dp))
                Text(selectedPhase, color = Lilac, fontSize = 25.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp))
                if (selectedIsCurrent) Text("Current phase · about ${snapshot.info.illumination}% illuminated", color = Muted, fontSize = 13.sp)
                else Text("Phase guide · select the current phase to see its illumination", color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(14.dp))
        InfoCard("THE THEME", selectedInfo.advice)
        InfoCard("A RITUAL TO TRY", selectedInfo.ritual)
        InfoCard("WHAT YOU NEED", selectedInfo.materials)
        selectedInfo.steps.forEachIndexed { index, step -> InfoCard("RITUAL STEP ${index + 1}", step) }
        InfoCard("GIVE YOURSELF SPACE FROM", selectedInfo.avoid)
        Text("Phase timing and illumination are estimates based on the average lunar cycle. Event times use the selected display time zone.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(vertical = 12.dp))
    }
    if (showLocationSettings) AlertDialog(
        onDismissRequest = { showLocationSettings = false },
        title = { Text("Allow location access") },
        text = { Text("Location access is blocked for Magikal Retrogrades. Enable location in the app settings, then tap Refresh local sky details.") },
        confirmButton = { TextButton(onClick = {
            showLocationSettings = false
            runCatching { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }) { Text("Open app settings") } },
        dismissButton = { TextButton(onClick = { showLocationSettings = false; locationAccessDenied = false }) { Text("Not now") } }
    )
}

private fun approximateMidIllumination(name: String): Int {
    val index = moonPhaseNames.indexOf(name).coerceAtLeast(0)
    return ((1 - cos(((index + .5) / 8.0) * 2 * Math.PI)) * 50).toInt().coerceIn(0, 100)
}

private data class Sabbat(
    val name: String, val northDate: String, val southDate: String, val theme: String,
    val practice: String, val symbol: String, val ritualName: String, val materials: String,
    val steps: List<String>, val spell: String
)
private val sabbats = listOf(
    Sabbat("Samhain", "Oct 31", "Apr 30–May 1", "Honoring ancestors, endings, and remembrance", "Remember loved ones and reflect on what you are ready to release.", "☽", "Ancestral remembrance and release", "A memory or photo, a cup of water, paper, pen, and an optional LED candle.", listOf("Tidy a small space and place the memory item and water there.", "Take a few steady breaths and remember a lesson or quality you want to honor.", "Write one gratitude and one boundary or burden you are ready to release.", "Read your intention aloud; pour the water into a plant or down the sink and keep or recycle the note."), "May loving memories offer wisdom; may I release what is not mine to carry."),
    Sabbat("Yule", "Dec 20–23", "Jun 20–23", "Rest, returning light, and renewal", "Welcome the returning light with a restful seasonal intention.", "❄", "Returning light intention", "An evergreen sprig, paper, pen, and an optional LED candle.", listOf("Choose a quiet place and set the evergreen beside the LED candle.", "Write one hope you want to nurture as the light returns.", "Name one small, practical step you can take toward it.", "Read both aloud and place the note where you will see it during the season."), "As the light returns, may patience and hope grow in me."),
    Sabbat("Imbolc", "Feb 1–2", "Aug 1–2", "Inspiration, purification, and first stirrings", "Make room for a fresh idea and tend one small beginning.", "✧", "Seed of inspiration spell", "A seed, a small pot with soil, water, paper, and pen.", listOf("Clear a small surface and place the seed beside the pot.", "Write an idea or skill you want to begin nurturing.", "Plant the seed and water it gently, using its care as a reminder of your intention.", "Read your spell and choose a date to check on both the plant and your first step."), "I welcome new growth with care, curiosity, and steady effort."),
    Sabbat("Ostara", "Mar 20–23", "Sep 20–23", "Balance, growth, and spring renewal", "Notice what is growing and restore balance with a small act of care.", "卯", "Balance and renewal ritual", "Two small stones or objects, a seed or small plant, paper, and pen.", listOf("Place the two objects side by side and name one thing you are nurturing and one thing needing care.", "Write one realistic action to support each side of that balance.", "Plant or tend the seed or plant as a symbol of patient growth.", "Read your spell and revisit the two actions in a week."), "May I welcome growth while tending balance and care."),
    Sabbat("Beltane", "May 1", "Oct 31–Nov 1", "Vitality, creativity, and connection", "Celebrate creativity and connections rooted in care and consent.", "✿", "Joy and creative spark spell", "A ribbon, a flower or leaf, paper, and pen.", listOf("Set the flower or leaf beside the ribbon in a calm space.", "Write one creative joy you want to make room for and one connection you want to nurture respectfully.", "Tie the ribbon loosely around the note or a jar as a reminder of your intention.", "Speak your spell, then take one kind, practical action that supports it."), "May joy grow where care, honesty, and consent meet."),
    Sabbat("Litha", "Jun 20–23", "Dec 20–23", "Abundance, warmth, and the sun's peak", "Honor what gives you energy and share appreciation.", "☼", "Sunlight gratitude charm", "A cup of water, paper, pen, and a leaf or flower.", listOf("Place the water and leaf near a safe patch of daylight.", "Write three sources of warmth, support, or joy in your life.", "Add one action that will help you protect your energy this season.", "Read your spell, water a plant with some of the water, and recycle the note when ready."), "I honor the light in my life and use my energy with care."),
    Sabbat("Lammas / Lughnasadh", "Aug 1", "Feb 1", "First harvest, craft, and gratitude", "Celebrate a skill you have practiced and the effort behind it.", "🌾", "First harvest and craft spell", "A piece of bread or oats, paper, pen, and a safe plate; skip food if it is unsuitable for you.", listOf("Set the food on a plate and pause to recognize the work behind it.", "Write one skill, project, or effort you are proud to have grown.", "Name one next step that would help you keep developing it.", "Read your spell; enjoy or share the food only if safe and welcome, or simply leave it as a symbol."), "May I honor what I have grown and keep learning with gratitude."),
    Sabbat("Mabon", "Sep 20–23", "Mar 20–23", "Equilibrium, harvest, and reflection", "Name what you have gathered and what you are ready to set down.", "⚖", "Gratitude and release balance", "An apple or other seasonal produce, paper, pen, and two small bowls or spaces.", listOf("Set the two bowls apart and label them ‘keep nurturing’ and ‘ready to release.’", "Write one gratitude for the season and place it in the first bowl.", "Write one expectation or habit you are ready to loosen and place it in the second.", "Read your spell, then recycle the release note and choose one gentle action for the coming season."), "I give thanks for what sustains me and release what I can lay down.")
)

@Composable
private fun WheelPage() {
    var selected by remember { mutableIntStateOf(1) }
    var southernHemisphere by remember { mutableStateOf(false) }
    PageColumn {
        SectionIntro("Wheel of the Year", "Eight seasonal celebrations, rituals, and reflections.")
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Northern hemisphere" to false, "Southern hemisphere" to true).forEach { (label, isSouthern) ->
                FilterChip(selected = southernHemisphere == isSouthern, onClick = { southernHemisphere = isSouthern }, label = { Text(label, fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PanelLight, selectedLabelColor = Lilac, labelColor = Muted))
            }
        }
        WheelGraphic(Modifier.fillMaxWidth().height(300.dp)) { selected = it }
        Text("CHOOSE A SABBAT", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold,
            letterSpacing = 1.3.sp, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
        sabbats.chunked(2).forEachIndexed { rowIndex, rowSabbats ->
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowSabbats.forEachIndexed { columnIndex, sabbat ->
                    val sabbatIndex = rowIndex * 2 + columnIndex
                    Surface(
                        color = if (selected == sabbatIndex) PanelLight else Panel,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected == sabbatIndex) Violet else Color(0xFF493650)),
                        modifier = Modifier.weight(1f).heightIn(min = 54.dp).clickable { selected = sabbatIndex }
                    ) {
                        Row(Modifier.fillMaxSize().padding(horizontal = 11.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(sabbat.symbol, color = Gold, fontSize = 17.sp, modifier = Modifier.padding(end = 8.dp))
                            Text(sabbat.name, color = if (selected == sabbatIndex) Lilac else Muted, fontSize = 12.sp,
                                fontWeight = if (selected == sabbatIndex) FontWeight.SemiBold else FontWeight.Medium, lineHeight = 15.sp)
                        }
                    }
                }
            }
        }
        val chosen = sabbats[selected]
        Surface(color = Panel, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("${chosen.symbol}  ${chosen.name}", color = Lilac, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Text(if (southernHemisphere) chosen.southDate else chosen.northDate, color = Gold, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, bottom = 10.dp))
                InfoLine("THEME", chosen.theme)
                InfoLine("ACTIVITY TO TRY", chosen.practice)
                InfoLine("RITUAL", chosen.ritualName)
                InfoLine("WHAT YOU NEED", chosen.materials)
                chosen.steps.forEachIndexed { index, step -> InfoLine("STEP ${index + 1}", step) }
                InfoLine("SPELL / INTENTION", "“${chosen.spell}”")
            }
        }
        Text("Traditional dates and seasonal associations can vary by tradition and hemisphere. These are common Wheel of the Year observances.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(vertical = 12.dp))
    }
}

@Composable
private fun WheelGraphic(modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    Box(modifier.pointerInput(Unit) { detectTapGestures { offset ->
        val centerX = size.width / 2f; val centerY = size.height / 2f
        val angle = (atan2(offset.y - centerY, offset.x - centerX) * 180.0 / Math.PI + 360.0 + 90.0) % 360.0
        onSelect((floor((angle + 22.5) / 45.0).toInt() + 1) % 8)
    } }, contentAlignment = Alignment.Center) {
        Image(painter = painterResource(R.drawable.wheel_of_the_year), contentDescription = "Wheel of the Year with the eight sabbats, seasonal artwork, zodiac ring, and pentagram center",
            modifier = Modifier.fillMaxHeight().aspectRatio(1f), contentScale = ContentScale.Fit)
    }
}

private data class JournalEntry(val tag: String, val timestamp: String, val text: String)

@Composable
private fun JournalPage() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("magikal_journal", android.content.Context.MODE_PRIVATE) }
    var category by remember { mutableStateOf("Retrograde") }
    var target by remember { mutableStateOf(planets.first().name) }
    var note by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf(loadEntries(prefs)) }
    val categories = listOf("Retrograde", "Moon phase", "Sabbat", "General")
    val targets = when (category) {
        "Retrograde" -> planets.map { it.name }
        "Moon phase" -> moonPhaseNames
        "Sabbat" -> sabbats.map { it.name }
        else -> listOf("General")
    }
    LaunchedEffect(category) { target = targets.first() }
    val entryTag = if (category == "General") "General" else "$category · $target"
    PageColumn {
        SectionIntro("Witch's journal", "Keep your reflections close. Entries are stored on this device.")
        Text("WHAT ARE YOU JOURNALING?", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            categories.forEach { option ->
                FilterChip(selected = category == option, onClick = { category = option }, label = { Text(option, fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PanelLight, selectedLabelColor = Lilac, labelColor = Muted))
            }
        }
        if (category != "General") {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 2.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                targets.forEach { option ->
                    FilterChip(selected = target == option, onClick = { target = option }, label = { Text(option, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PanelLight, selectedLabelColor = Lilac, labelColor = Muted))
                }
            }
        }
        OutlinedTextField(value = note, onValueChange = { note = it }, modifier = Modifier.fillMaxWidth(), minLines = 4,
            placeholder = { Text("What are you noticing, learning, or setting an intention for?", color = Muted) },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Violet, unfocusedBorderColor = PanelLight, focusedTextColor = Lilac, unfocusedTextColor = Lilac, cursorColor = Violet))
        Button(onClick = {
            if (note.isNotBlank()) {
                val entry = JournalEntry(entryTag, LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a")), note.trim())
                entries = listOf(entry) + entries
                saveEntries(prefs, entries)
                note = ""
            }
        }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp), colors = ButtonDefaults.buttonColors(containerColor = Violet, contentColor = Ink), shape = RoundedCornerShape(14.dp)) {
            Text("Save journal entry", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("YOUR ENTRIES", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
            JournalTripleMoonPentacle(Modifier.size(width = 88.dp, height = 34.dp))
        }
        if (entries.isEmpty()) {
            Text("Your saved notes will appear here.", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp, bottom = 20.dp))
        }
        entries.forEachIndexed { index, entry ->
            Surface(color = Panel, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(entry.tag.uppercase(), color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(entry.timestamp, color = Muted, fontSize = 10.sp)
                    }
                    Text(entry.text, color = Lilac, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 8.dp))
                    Text("Delete", color = Violet, fontSize = 11.sp, modifier = Modifier.align(Alignment.End).clickable {
                        entries = entries.filterIndexed { i, _ -> i != index }; saveEntries(prefs, entries)
                    }.padding(top = 8.dp))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Journal notes stay on this device in this first build. There is no account sync or cloud backup yet.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
    }
}

@Composable
private fun JournalTripleMoonPentacle(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val cY = h * .5f
        val outer = Path().apply {
            moveTo(w * .03f, h * .10f)
            cubicTo(w * .32f, h * .12f, w * .32f, h * .88f, w * .03f, h * .90f)
            cubicTo(w * .20f, h * .73f, w * .20f, h * .27f, w * .03f, h * .10f)
            close()
            moveTo(w * .97f, h * .10f)
            cubicTo(w * .68f, h * .12f, w * .68f, h * .88f, w * .97f, h * .90f)
            cubicTo(w * .80f, h * .73f, w * .80f, h * .27f, w * .97f, h * .10f)
            close()
        }
        drawPath(outer, Gold)
        val center = Offset(w * .5f, cY)
        val radius = h * .43f
        drawCircle(Gold, radius, center, style = Stroke(width = (size.minDimension * .055f).coerceAtLeast(1.2f)))
        val points = (0..4).map { i ->
            val angle = Math.toRadians(-90.0 + i * 72.0)
            Offset(center.x + cos(angle).toFloat() * radius * .82f, center.y + sin(angle).toFloat() * radius * .82f)
        }
        val starOrder = listOf(0, 2, 4, 1, 3)
        val star = Path().apply {
            moveTo(points[starOrder.first()].x, points[starOrder.first()].y)
            starOrder.drop(1).forEach { index -> lineTo(points[index].x, points[index].y) }
            close()
        }
        drawPath(star, Gold, style = Stroke(width = (size.minDimension * .055f).coerceAtLeast(1.2f)))
    }
}
@Composable
private fun SectionIntro(title: String, subtitle: String) {
    Text(title, color = Lilac, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
    Text(subtitle, color = Muted, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))
}

@Composable
private fun InfoCard(title: String, body: String) {
    Surface(color = Panel, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Text(body, color = Lilac, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 5.dp))
        }
    }
}

@Composable
private fun InfoLine(title: String, body: String) {
    Column(Modifier.padding(bottom = 10.dp)) {
        Text(title, color = Gold, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text(body, color = Lilac, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 3.dp))
    }
}


private fun loadEntries(prefs: android.content.SharedPreferences): List<JournalEntry> = try {
    val stored = JSONArray(prefs.getString("entries", "[]") ?: "[]")
    List(stored.length()) { index ->
        val item = stored.getJSONObject(index)
        JournalEntry(item.optString("tag"), item.optString("timestamp"), item.optString("text"))
    }
} catch (_: Exception) {
    emptyList()
}

private fun saveEntries(prefs: android.content.SharedPreferences, entries: List<JournalEntry>) {
    val stored = JSONArray()
    entries.forEach { entry ->
        stored.put(JSONObject().put("tag", entry.tag).put("timestamp", entry.timestamp).put("text", entry.text))
    }
    prefs.edit().putString("entries", stored.toString()).apply()
}
