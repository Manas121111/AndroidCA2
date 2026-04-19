package com.smarttour360.app.ui.search

import java.util.Locale

object AirportCodeResolver {
    private val cityToAirportCode = mapOf(
        "delhi" to "DEL",
        "new delhi" to "DEL",
        "mumbai" to "BOM",
        "bombay" to "BOM",
        "bengaluru" to "BLR",
        "bangalore" to "BLR",
        "hyderabad" to "HYD",
        "chennai" to "MAA",
        "kolkata" to "CCU",
        "calcutta" to "CCU",
        "kochi" to "COK",
        "cochin" to "COK",
        "goa" to "GOI",
        "jaipur" to "JAI",
        "amritsar" to "ATQ",
        "varanasi" to "VNS",
        "agra" to "AGR",
        "srinagar" to "SXR",
        "leh" to "IXL",
        "shimla" to "SLV",
        "manali" to "KUU",
        "udaipur" to "UDR",
        "jaisalmer" to "JSA",
        "rishikesh" to "DED",
        "dehradun" to "DED",
        "nainital" to "PGH",
        "mysuru" to "MYQ",
        "mysore" to "MYQ",
        "munnar" to "COK",
        "alleppey" to "COK",
        "alappuzha" to "COK",
        "pondicherry" to "PNY",
        "puducherry" to "PNY",
        "madurai" to "IXM",
        "darjeeling" to "IXB",
        "gangtok" to "PYG",
        "shillong" to "SHL",
        "bhubaneswar" to "BBI",
        "puri" to "BBI"
    )

    fun resolve(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.length == 3 && trimmed.all { it.isLetter() }) {
            return trimmed.uppercase(Locale.US)
        }
        val normalized = trimmed.lowercase(Locale.US)
        return cityToAirportCode[normalized]
            ?: cityToAirportCode.entries.firstOrNull { (city, _) ->
                normalized.contains(city) || city.contains(normalized)
            }?.value
    }
}
