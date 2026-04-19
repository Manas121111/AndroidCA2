package com.smarttour360.app.data

import com.smarttour360.app.data.remote.ApiClient
import com.smarttour360.app.ui.train.LiveStatus
import com.smarttour360.app.ui.train.Station
import com.smarttour360.app.ui.train.TrainResult
import com.smarttour360.app.ui.train.TrainStop

class TrainRepository {
    private var stationsCache: List<Station>? = null

    fun isConfigured(): Boolean = true

    suspend fun resolveStation(query: String): Station? {
        val normalized = query.trim()
        if (normalized.length < 2) return null

        val explicitCode = extractStationCode(normalized)
        val stations = searchStations(explicitCode ?: normalized)
        if (stations.isEmpty()) return null

        explicitCode?.let { code ->
            stations.firstOrNull { it.code.equals(code, ignoreCase = true) }?.let { return it }
        }

        stations.firstOrNull { it.name.equals(normalized, ignoreCase = true) }?.let { return it }
        stations.firstOrNull { it.name.contains(normalized, ignoreCase = true) }?.let { return it }
        return stations.first()
    }

    suspend fun searchStations(query: String): List<Station> {
        val normalized = query.trim()
        if (normalized.length < 2) return emptyList()
        val catalog = loadStationsCatalog()
        return catalog.filter { station ->
            station.code.contains(normalized, ignoreCase = true) ||
                station.name.contains(normalized, ignoreCase = true)
        }.take(20)
    }

    suspend fun searchTrains(fromCode: String, toCode: String, date: String): List<TrainResult> {
        val payload = ApiClient.trainApi.getTrainsBetweenStations(fromCode, toCode).string()
        return parseErailTrains(payload, date)
    }

    suspend fun getSchedule(trainNumber: String): List<TrainStop> {
        val html = ApiClient.trainApi.getTrainRoutePage("https://amp.erail.in/train-enquiry/$trainNumber").string()
        return parseRouteFromAmpHtml(html).map { stop ->
            TrainStop(
                stationCode = stop.stationCode,
                stationName = stop.stationName,
                scheduledArrival = stop.scheduledArrival,
                scheduledDeparture = stop.scheduledDeparture,
                haltMinutes = stop.haltMinutes,
                distanceKm = stop.distanceKm,
                day = stop.day
            )
        }
    }

    suspend fun getLiveStatus(trainNumber: String, @Suppress("UNUSED_PARAMETER") date: String): LiveStatus {
        return LiveStatus(
            trainNumber = trainNumber,
            trainName = "Train $trainNumber",
            position = "Free live running status is not integrated right now.",
            delayMinutes = 0,
            status = "Schedule is available from eRail. Live status is unavailable in the current free setup."
        )
    }

    private suspend fun loadStationsCatalog(): List<Station> {
        stationsCache?.let { return it }
        val script = ApiClient.trainApi.getStationsCatalog().string()
        val startToken = "var StationsData=\""
        val startIndex = script.indexOf(startToken)
        if (startIndex == -1) return emptyList()
        val dataStart = startIndex + startToken.length
        val dataEnd = script.indexOf("\";", dataStart)
        if (dataEnd == -1) return emptyList()

        val rawData = script.substring(dataStart, dataEnd)
        val tokens = rawData.split(",")
        val stations = mutableListOf<Station>()
        var index = 0
        while (index + 1 < tokens.size) {
            val code = tokens[index].trim()
            val name = tokens[index + 1].trim()
            if (isStationCode(code) && name.isNotBlank()) {
                stations += Station(name = name, code = code)
            }
            index += 2
        }
        stationsCache = stations
        return stations
    }

    private fun parseErailTrains(payload: String, date: String): List<TrainResult> {
        if (payload.isBlank()) return emptyList()
        return payload.split("^")
            .drop(1)
            .mapNotNull { record ->
                val fields = record.split("~")
                if (fields.size < 13) return@mapNotNull null

                val trainNumber = fields.getOrNull(0).orEmpty().trim()
                val trainName = fields.getOrNull(1).orEmpty().trim()
                val fromName = fields.getOrNull(6).orEmpty().trim().ifBlank { fields.getOrNull(2).orEmpty().trim() }
                val fromCode = fields.getOrNull(7).orEmpty().trim().ifBlank { fields.getOrNull(3).orEmpty().trim() }
                val toName = fields.getOrNull(8).orEmpty().trim().ifBlank { fields.getOrNull(4).orEmpty().trim() }
                val toCode = fields.getOrNull(9).orEmpty().trim().ifBlank { fields.getOrNull(5).orEmpty().trim() }
                val departure = normalizeTime(fields.getOrNull(10))
                val arrival = normalizeTime(fields.getOrNull(11))
                val duration = normalizeDuration(fields.getOrNull(12))
                val runBits = fields.getOrNull(13).orEmpty()

                if (trainNumber.isBlank() || trainName.isBlank() || fromCode.isBlank() || toCode.isBlank()) {
                    return@mapNotNull null
                }

                TrainResult(
                    number = trainNumber,
                    name = trainName,
                    fromStation = Station(fromName, fromCode),
                    toStation = Station(toName, toCode),
                    departure = departure,
                    arrival = arrival,
                    duration = duration,
                    classes = extractClasses(record),
                    runsDays = decodeRunsDays(runBits),
                    travelDate = date
                )
            }
    }

    private fun extractClasses(record: String): List<String> {
        val classOrder = listOf("1A", "2A", "3A", "3E", "CC", "EC", "EV", "SL", "2S", "GN")
        return classOrder.filter { record.contains("$it:", ignoreCase = false) }
    }

    private fun decodeRunsDays(bits: String): List<String> {
        val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        if (bits.length < 7 || bits.any { it !in listOf('0', '1') }) return emptyList()
        return labels.mapIndexedNotNull { index, day ->
            if (bits.getOrNull(index) == '1') day else null
        }
    }

    private fun normalizeTime(raw: String?): String {
        val value = raw.orEmpty().trim()
        return if (value.isBlank()) "--" else value.replace('.', ':')
    }

    private fun normalizeDuration(raw: String?): String {
        val value = raw.orEmpty().trim()
        if (value.isBlank()) return "--"
        return value.replace('.', 'h') + "m"
    }

    private fun parseRouteFromAmpHtml(html: String): List<TrainStop> {
        val tableMatch = Regex("""<table class="table[^"]*">(.*?)</table>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .find(html)
            ?: return emptyList()
        val tableHtml = tableMatch.groupValues[1]
        val rowRegex = Regex("""<tr><td>(\d+)</td><td[^>]*>(.*?)</td><td>(.*?)</td><td>(.*?)</td><td>(\d+)</td><td>(.*?)</td></tr>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        val dayRegex = Regex("""day\s*(\d+)""", RegexOption.IGNORE_CASE)

        return rowRegex.findAll(tableHtml).map { match ->
            val stationRaw = cleanupHtml(match.groupValues[2])
            val stationName = stationRaw.substringBefore(" (RL)").trim()
            val stationCode = extractStationCodeFromName(stationName)
            val arrival = normalizeAmpTime(match.groupValues[3])
            val departure = normalizeAmpTime(match.groupValues[4])
            val distance = match.groupValues[5].toIntOrNull() ?: 0
            val extra = cleanupHtml(match.groupValues[6])
            val day = dayRegex.find(extra)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 1

            TrainStop(
                stationCode = stationCode,
                stationName = stationName,
                scheduledArrival = arrival,
                scheduledDeparture = departure,
                haltMinutes = calculateAmpHalt(arrival, departure),
                distanceKm = distance,
                day = day
            )
        }.toList()
    }

    private fun cleanupHtml(value: String): String {
        return value
            .replace(Regex("<.*?>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .trim()
    }

    private fun normalizeAmpTime(value: String): String {
        val clean = cleanupHtml(value)
        return when (clean.lowercase()) {
            "first" -> "Source"
            "last" -> "Destination"
            else -> clean.replace('.', ':')
        }
    }

    private fun calculateAmpHalt(arrival: String, departure: String): Int {
        val arr = parseClock(arrival) ?: return 0
        val dep = parseClock(departure) ?: return 0
        val diff = dep - arr
        return if (diff > 0) diff else 0
    }

    private fun parseClock(value: String): Int? {
        if (!Regex("""\d{1,2}:\d{2}""").matches(value)) return null
        val parts = value.split(":")
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        return hour * 60 + minute
    }

    private fun extractStationCodeFromName(name: String): String {
        return name.uppercase()
            .split(" ")
            .filter { it.isNotBlank() }
            .map { it.take(1) }
            .joinToString("")
            .take(4)
            .ifBlank { name.take(4).uppercase() }
    }

    private fun extractStationCode(query: String): String? {
        val open = query.lastIndexOf('(')
        val close = query.lastIndexOf(')')
        if (open == -1 || close <= open + 1) return null
        return query.substring(open + 1, close).trim().takeIf { it.isNotEmpty() }
    }

    private fun isStationCode(value: String): Boolean {
        return value.length in 2..5 && value.all { it.isLetterOrDigit() }
    }
}
