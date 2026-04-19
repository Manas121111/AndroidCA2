package com.smarttour360.app.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class PressureReading(
    val pressureHpa: Float,
    val riskContribution: Float,
    val label: String
)

object BarometerReader {

    fun readPressure(context: Context): Flow<PressureReading?> = callbackFlow {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val barometer = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

        if (barometer == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type != Sensor.TYPE_PRESSURE) return
                trySend(mapPressureToReading(event.values[0]))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.registerListener(listener, barometer, SensorManager.SENSOR_DELAY_NORMAL)
        awaitClose { sensorManager.unregisterListener(listener) }
    }

    private fun mapPressureToReading(hpa: Float): PressureReading {
        val risk: Float
        val label: String

        when {
            hpa >= 1013f -> {
                risk = 0.1f
                label = "High pressure · Clear weather"
            }
            hpa >= 1000f -> {
                risk = 0.2f
                label = "Normal pressure · Stable"
            }
            hpa >= 990f -> {
                risk = 0.35f
                label = "Falling pressure · Possible rain"
            }
            hpa >= 980f -> {
                risk = 0.5f
                label = "Low pressure · Wind and rain likely"
            }
            hpa >= 960f -> {
                risk = 0.7f
                label = "Very low pressure · Storm risk"
            }
            else -> {
                risk = 0.9f
                label = "Severe low pressure · Dangerous conditions"
            }
        }

        return PressureReading(
            pressureHpa = hpa,
            riskContribution = risk,
            label = label
        )
    }
}
