package com.smarttour360.app.ui.train

data class Station(
    val name: String,
    val code: String,
    val state: String = ""
)

data class TrainResult(
    val number: String,
    val name: String,
    val fromStation: Station,
    val toStation: Station,
    val departure: String,
    val arrival: String,
    val duration: String,
    val classes: List<String>,
    val runsDays: List<String>,
    val travelDate: String
)

data class TrainStop(
    val stationCode: String,
    val stationName: String,
    val scheduledArrival: String,
    val scheduledDeparture: String,
    val haltMinutes: Int,
    val distanceKm: Int,
    val day: Int
)

data class LiveStatus(
    val trainNumber: String,
    val trainName: String,
    val position: String,
    val delayMinutes: Int,
    val status: String
)

data class TrainBookingHandoff(
    val trainNumber: String,
    val trainName: String,
    val fromCode: String,
    val toCode: String,
    val travelDate: String,
    val selectedClass: String,
    val irctcUrl: String
) {
    companion object {
        fun build(
            trainNumber: String,
            trainName: String,
            fromCode: String,
            toCode: String,
            travelDate: String,
            selectedClass: String
        ): TrainBookingHandoff {
            val formattedDate = if (travelDate.length == 8) {
                "${travelDate.substring(6, 8)}/${travelDate.substring(4, 6)}/${travelDate.substring(0, 4)}"
            } else {
                travelDate
            }
            val url = buildString {
                append("https://www.irctc.co.in/nget/train-search")
                append("?fromStation=").append(fromCode)
                append("&toStation=").append(toCode)
                append("&journeyDate=").append(formattedDate)
                append("&class=").append(selectedClass)
            }
            return TrainBookingHandoff(
                trainNumber = trainNumber,
                trainName = trainName,
                fromCode = fromCode,
                toCode = toCode,
                travelDate = travelDate,
                selectedClass = selectedClass,
                irctcUrl = url
            )
        }
    }
}
