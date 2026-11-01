package com.magikalretrogrades.app

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.net.ssl.HttpsURLConnection

internal data class RetrogradeEvent(val planet: String, val startUtcMillis: Long, val endUtcMillis: Long, val zodiacSign: String = "")
internal data class RetrogradeFeed(val events: List<RetrogradeEvent>, val fetchedAtMillis: Long, val warnings: List<String>)
internal data class TrackerPlanet(val name: String, val horizonsId: String)

/** Geocentric apparent motion from NASA/JPL Horizons, cached on the device for 12 hours. */
internal object RetrogradeTracker {
    private const val ENDPOINT = "https://ssd.jpl.nasa.gov/api/horizons.api"
    private const val PREFS = "retrograde_ephemeris_cache_v2"
    private const val CACHE_KEY = "feed_v2"
    private const val CACHE_TIME_KEY = "fetched_at"
    private const val CACHE_TTL_MILLIS = 12L * 60L * 60L * 1000L
    // Covers the full duration of even the long outer-planet retrograde periods,
    // so an active period's real station date is not clipped to the query start.
    private const val LOOKBACK_DAYS = 250L
    private const val LOOKAHEAD_DAYS = 730L

    /** Curated station calendar keeps active dates and countdowns visible when the live feed is unreachable. */
    fun fallbackFeed(warnings: List<String> = listOf("Built-in station calendar; event times are converted to your selected time zone.")): RetrogradeFeed =
        RetrogradeFeed(knownEvents, System.currentTimeMillis(), warnings)

    private val knownEvents: List<RetrogradeEvent> by lazy {
        listOf(
            event("Mercury", "2026-10-24T07:15:00Z", "2026-11-13T15:52:00Z", "Scorpio"),
            event("Venus", "2026-10-03T07:10:00Z", "2026-11-14T00:21:00Z", "Scorpio → Libra"),
            event("Saturn", "2026-07-26T19:58:00Z", "2026-12-10T23:23:00Z", "Aries"),
            event("Uranus", "2026-09-10T18:40:00Z", "2027-02-08T12:24:00Z", "Gemini"),
            event("Neptune", "2026-07-07T11:21:00Z", "2026-12-12T22:13:00Z", "Aries"),
            event("Jupiter", "2026-12-13T00:59:00Z", "2027-04-13T02:13:00Z", "Leo"),
            event("Chiron", "2026-09-17T00:00:00Z", "2027-01-06T00:00:00Z", "Taurus → Aries"),
            event("Mars", "2027-01-10T13:01:00Z", "2027-04-01T14:10:00Z"),
            event("Mercury", "2027-02-09T17:36:00Z", "2027-03-03T12:35:00Z"),
            event("Pluto", "2027-05-08T12:15:00Z", "2027-10-18T03:51:00Z"),
            event("Mercury", "2027-06-10T18:19:00Z", "2027-07-04T19:40:00Z"),
            event("Neptune", "2027-07-09T22:50:00Z", "2027-12-15T09:43:00Z"),
            event("Saturn", "2027-08-09T17:53:00Z", "2027-12-24T02:50:00Z"),
            event("Uranus", "2027-09-15T09:15:00Z", "2028-02-12T23:40:00Z"),
            event("Mercury", "2027-10-07T14:40:00Z", "2027-10-28T14:10:00Z"),
            event("Jupiter", "2028-01-12T08:48:00Z", "2028-05-13T19:58:00Z"),
            event("Mercury", "2028-01-24T11:03:00Z", "2028-02-14T12:39:00Z"),
            event("Pluto", "2028-05-09T08:48:00Z", "2028-10-19T04:20:00Z"),
            event("Venus", "2028-05-10T23:02:00Z", "2028-06-22T22:13:00Z"),
            event("Mercury", "2028-05-21T08:39:00Z", "2028-06-14T06:07:00Z"),
            event("Neptune", "2028-07-11T13:18:00Z", "2028-12-16T20:55:00Z"),
            event("Saturn", "2028-08-22T22:17:00Z", "2029-01-05T12:32:00Z"),
            event("Uranus", "2028-09-19T00:06:00Z", "2029-02-16T10:50:00Z"),
            event("Mercury", "2028-09-19T16:31:00Z", "2028-10-11T10:30:00Z")
        ).sortedBy { it.startUtcMillis }
    }

    private fun event(planet: String, start: String, end: String, zodiacSign: String = "") =
        RetrogradeEvent(planet, Instant.parse(start).toEpochMilli(), Instant.parse(end).toEpochMilli(), zodiacSign)

    private fun mergeKnownEvents(events: List<RetrogradeEvent>): List<RetrogradeEvent> {
        val matched = knownEvents.map { known ->
            events.firstOrNull { live -> live.planet == known.planet && kotlin.math.abs(known.startUtcMillis - live.startUtcMillis) < 3L * 86_400_000L }
                ?.let { live -> live.copy(zodiacSign = live.zodiacSign.ifBlank { known.zodiacSign }) } ?: known
        }
        val additional = events.filterNot { live -> knownEvents.any { known -> known.planet == live.planet && kotlin.math.abs(known.startUtcMillis - live.startUtcMillis) < 3L * 86_400_000L } }
        return (matched + additional).distinctBy { Triple(it.planet, it.startUtcMillis, it.endUtcMillis) }.sortedBy { it.startUtcMillis }
    }

    suspend fun load(context: Context, bodies: List<TrackerPlanet>, forceRefresh: Boolean = false): RetrogradeFeed = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val cachedAt = prefs.getLong(CACHE_TIME_KEY, 0L)
        val cached = decodeFeed(prefs.getString(CACHE_KEY, null), cachedAt)
        if (!forceRefresh && cached != null && System.currentTimeMillis() - cachedAt < CACHE_TTL_MILLIS) {
            return@withContext cached.copy(events = mergeKnownEvents(cached.events))
        }

        val start = LocalDate.now(ZoneId.of("UTC")).minusDays(LOOKBACK_DAYS)
        val stop = start.plusDays(LOOKBACK_DAYS + LOOKAHEAD_DAYS)
        val results = coroutineScope {
            bodies.map { body ->
                async {
                    try {
                        fetchPlanet(body, start, stop) to null
                    } catch (error: Exception) {
                        emptyList<RetrogradeEvent>() to "${body.name}: ${error.message ?: "data unavailable"}"
                    }
                }
            }.awaitAll()
        }
        val allEvents = results.flatMap { it.first }
        val warnings = results.mapNotNull { it.second }
        if (allEvents.isEmpty()) {
            if (cached != null) return@withContext cached.copy(events = mergeKnownEvents(cached.events), warnings = cached.warnings + "Live refresh failed; showing saved station dates.")
            return@withContext fallbackFeed(listOf("Live ephemeris unavailable; showing the built-in station calendar.") + warnings.take(2))
        }
        val fetchedAt = System.currentTimeMillis()
        val feed = RetrogradeFeed(mergeKnownEvents(allEvents), fetchedAt, warnings)
        prefs.edit().putLong(CACHE_TIME_KEY, fetchedAt).putString(CACHE_KEY, encodeFeed(feed)).apply()
        feed
    }

    private fun fetchPlanet(body: TrackerPlanet, start: LocalDate, stop: LocalDate): List<RetrogradeEvent> {
        val uri = Uri.parse(ENDPOINT).buildUpon()
            .appendQueryParameter("format", "json")
            .appendQueryParameter("COMMAND", body.horizonsId)
            .appendQueryParameter("OBJ_DATA", "NO")
            .appendQueryParameter("MAKE_EPHEM", "YES")
            .appendQueryParameter("EPHEM_TYPE", "VECTORS")
            .appendQueryParameter("CENTER", "500@399")
            .appendQueryParameter("START_TIME", start.toString())
            .appendQueryParameter("STOP_TIME", stop.toString())
            .appendQueryParameter("STEP_SIZE", "2 d")
            .appendQueryParameter("TIME_TYPE", "UT")
            .appendQueryParameter("REF_PLANE", "ECLIPTIC")
            .appendQueryParameter("OUT_UNITS", "AU-D")
            .appendQueryParameter("VEC_TABLE", "2")
            .appendQueryParameter("VEC_CORR", "LT")
            .appendQueryParameter("CSV_FORMAT", "YES")
            .appendQueryParameter("VEC_LABELS", "NO")
            .build()
        val connection = URL(uri.toString()).openConnection() as HttpsURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 5_000
        connection.readTimeout = 5_000
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "MagikalRetrogrades/0.1 Android")
        try {
            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val json = stream.bufferedReader().use { JSONObject(it.readText()) }
            val apiError = json.optString("error").takeIf { it.isNotBlank() }
            if (responseCode !in 200..299 || apiError != null) throw IllegalStateException(apiError ?: "JPL returned HTTP $responseCode")
            val signature = json.optJSONObject("signature")
            if (signature?.optString("source")?.contains("NASA/JPL Horizons", ignoreCase = true) != true) {
                throw IllegalStateException("The response source could not be verified.")
            }
            val result = json.optString("result")
            return eventsFromResult(body.name, result)
        } finally {
            connection.disconnect()
        }
    }

    private data class Position(val jd: Double, val longitudeRate: Double, val longitude: Double)

    private fun eventsFromResult(planet: String, result: String): List<RetrogradeEvent> {
        val soe = result.indexOf("\$\$SOE")
        val eoe = result.indexOf("\$\$EOE")
        if (soe < 0 || eoe <= soe) throw IllegalStateException("No ephemeris table was returned.")
        val positions = result.substring(soe + 5, eoe).lineSequence().mapNotNull { line ->
            val fields = line.trim().split(',')
            if (fields.size < 8) return@mapNotNull null
            val jd = fields[0].trim().toDoubleOrNull() ?: return@mapNotNull null
            val x = fields[2].trim().toDoubleOrNull() ?: return@mapNotNull null
            val y = fields[3].trim().toDoubleOrNull() ?: return@mapNotNull null
            val vx = fields[5].trim().toDoubleOrNull() ?: return@mapNotNull null
            val vy = fields[6].trim().toDoubleOrNull() ?: return@mapNotNull null
            val radiusSquared = x * x + y * y
            if (radiusSquared == 0.0) return@mapNotNull null
            val longitudeRate = Math.toDegrees((x * vy - y * vx) / radiusSquared)
            Position(jd, longitudeRate, (Math.toDegrees(kotlin.math.atan2(y, x)) + 360.0) % 360.0)
        }.sortedBy { it.jd }.toList()
        if (positions.size < 3) throw IllegalStateException("Too few ephemeris samples were returned.")

        val events = mutableListOf<RetrogradeEvent>()
        val signNames = listOf("Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo", "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces")
        fun signAt(jd: Double): String {
            val rightIndex = positions.indexOfFirst { it.jd >= jd }
            if (rightIndex <= 0) return signNames[((positions.firstOrNull()?.longitude ?: 0.0) / 30.0).toInt().coerceIn(0, 11)]
            val before = positions[rightIndex - 1]
            val after = positions[rightIndex]
            val delta = ((after.longitude - before.longitude + 540.0) % 360.0) - 180.0
            val longitude = (before.longitude + delta * (jd - before.jd) / (after.jd - before.jd) + 360.0) % 360.0
            return signNames[(longitude / 30.0).toInt().coerceIn(0, 11)]
        }
        fun zodiacPath(start: Double, end: Double): String {
            val timeline = listOf(start to signAt(start)) +
                positions.filter { it.jd > start && it.jd < end }.map { it.jd to signNames[(it.longitude / 30.0).toInt().coerceIn(0, 11)] } +
                listOf(end to signAt(end))
            val path = mutableListOf<String>()
            timeline.sortedBy { it.first }.forEach { (_, sign) -> if (path.lastOrNull() != sign) path += sign }
            return path.joinToString(" → ")
        }
        var startJd: Double? = null
        positions.forEachIndexed { index, sample ->
            val jd = sample.jd
            val rate = sample.longitudeRate
            if (rate < 0.0 && startJd == null) {
                startJd = if (index == 0) positions.first().jd else crossingJd(positions[index - 1], sample)
            } else if (rate >= 0.0 && startJd != null) {
                val endJd = if (index == 0) jd else crossingJd(positions[index - 1], sample)
                val start = startJd!!
                if (endJd > start) events += RetrogradeEvent(planet, jdMillis(start), jdMillis(endJd), zodiacPath(start, endJd))
                startJd = null
            }
        }
        startJd?.let { start ->
            val end = positions.last().jd
            if (end > start) events += RetrogradeEvent(planet, jdMillis(start), jdMillis(end), zodiacPath(start, end))
        }
        return events
    }

    private fun crossingJd(before: Position, after: Position): Double {
        val denominator = before.longitudeRate - after.longitudeRate
        if (denominator == 0.0) return (before.jd + after.jd) / 2.0
        val fraction = (before.longitudeRate / denominator).coerceIn(0.0, 1.0)
        return before.jd + (after.jd - before.jd) * fraction
    }

    private fun jdMillis(jd: Double): Long = ((jd - 2440587.5) * 86_400_000.0).toLong()

    private fun encodeFeed(feed: RetrogradeFeed): String {
        val items = JSONArray()
        feed.events.forEach { items.put(JSONObject().put("planet", it.planet).put("start", it.startUtcMillis).put("end", it.endUtcMillis).put("zodiac", it.zodiacSign)) }
        val warnings = JSONArray()
        feed.warnings.forEach { warnings.put(it) }
        return JSONObject().put("events", items).put("warnings", warnings).toString()
    }

    private fun decodeFeed(raw: String?, fetchedAt: Long): RetrogradeFeed? {
        if (raw == null) return null
        return try {
            val root = JSONObject(raw)
            val eventArray = root.getJSONArray("events")
            val warningArray = root.optJSONArray("warnings") ?: JSONArray()
            val events = (0 until eventArray.length()).map { index ->
                val item = eventArray.getJSONObject(index)
                RetrogradeEvent(item.getString("planet"), item.getLong("start"), item.getLong("end"), item.optString("zodiac", ""))
            }
            val warnings = (0 until warningArray.length()).map { warningArray.getString(it) }
            RetrogradeFeed(events, fetchedAt, warnings)
        } catch (_: Exception) { null }
    }
}

internal fun formatLocalEventTime(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a z")
        .withZone(zone).format(Instant.ofEpochMilli(epochMillis))
