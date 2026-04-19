package com.smarttour360.app.data

import android.content.Context
import org.json.JSONObject
import kotlin.math.ln
import kotlin.math.roundToInt

class CrimeBaselineRepository(private val context: Context) {
    private val dataset: CrimeDataset by lazy { loadDataset() }

    fun crimeSafetyScoreFor(city: String, region: String): Int {
        return resolveCrimeBaseline(city, region)?.let { (100 - it.normalizedRisk).coerceIn(20, 96) } ?: 68
    }

    fun crimeSourceLabelFor(city: String, region: String): String {
        return resolveCrimeBaseline(city, region)?.sourceLabel ?: "NCRB 2023 baseline unavailable"
    }

    private fun resolveCrimeBaseline(city: String, region: String): CrimeBaseline? {
        val normalizedCity = normalize(city)
        val cityBaseline = dataset.cityRiskByName[normalizedCity]
        if (cityBaseline != null) {
            return cityBaseline
        }

        return dataset.stateRiskByName[normalize(region)]
            ?: dataset.stateRiskByName[normalize(regionAlias(region))]
            ?: dataset.stateRiskByName[normalize(cityAliasRegion(normalizedCity))]
    }

    fun hasOfficialBaseline(city: String, region: String): Boolean {
        return resolveCrimeBaseline(city, region) != null
    }

    private fun loadDataset(): CrimeDataset {
        val raw = context.assets.open("crime/ncrb_violent_crime_2023.json")
            .bufferedReader()
            .use { it.readText() }
        val json = JSONObject(raw)

        val stateEntries = mutableListOf<Pair<String, Int>>()
        val states = json.getJSONArray("states")
        for (index in 0 until states.length()) {
            val row = states.getJSONObject(index)
            stateEntries += row.getString("name") to row.getInt("totalViolentCrimes")
        }

        val cityEntries = mutableListOf<Pair<String, Int>>()
        val cities = json.getJSONArray("cities")
        for (index in 0 until cities.length()) {
            val row = cities.getJSONObject(index)
            cityEntries += row.getString("name") to row.getInt("totalViolentCrimes")
        }

        return CrimeDataset(
            stateRiskByName = normalizeRiskMap(stateEntries, "NCRB 2023 state/UT violent-crime baseline"),
            cityRiskByName = normalizeRiskMap(cityEntries, "NCRB 2023 metro-city violent-crime baseline")
        )
    }

    private fun normalizeRiskMap(entries: List<Pair<String, Int>>, sourceLabel: String): Map<String, CrimeBaseline> {
        if (entries.isEmpty()) return emptyMap()
        val logged = entries.associate { normalize(it.first) to ln((it.second + 1).toDouble()) }
        val min = logged.minOf { it.value }
        val max = logged.maxOf { it.value }

        return logged.mapValues { (_, value) ->
            val normalizedRisk = if (max == min) {
                50
            } else {
                (((value - min) / (max - min)) * 100.0).roundToInt().coerceIn(0, 100)
            }
            CrimeBaseline(normalizedRisk = normalizedRisk, sourceLabel = sourceLabel)
        }
    }

    private fun normalize(value: String): String {
        return value.lowercase()
            .replace("&", " and ")
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun regionAlias(region: String): String {
        return when (normalize(region)) {
            else -> region
        }
    }

    private fun cityAliasRegion(normalizedCity: String): String {
        return when (normalizedCity) {
            "pondicherry", "puducherry" -> "Puducherry"
            else -> normalizedCity
        }
    }
}

private data class CrimeDataset(
    val stateRiskByName: Map<String, CrimeBaseline>,
    val cityRiskByName: Map<String, CrimeBaseline>
)

private data class CrimeBaseline(
    val normalizedRisk: Int,
    val sourceLabel: String
)
