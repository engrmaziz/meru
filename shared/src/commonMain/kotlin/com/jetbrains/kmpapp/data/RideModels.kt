package com.jetbrains.kmpapp.data

import kotlinx.serialization.Serializable

@Serializable
data class DriverProfile(
    val id: String,
    val name: String,
    val email: String,
    val city: String,
    val province: String,
    val country: String,
    val carName: String,
    val make: String,
    val model: String,
    val year: Int,
    val plate: String,
    val referralCode: String,
    val referredBy: String? = null,
)

@Serializable
data class RideSample(
    val t: Long,
    val lat: Double,
    val lon: Double,
    val alt: Double? = null,
    val speedKmh: Double = 0.0,
    val bearing: Float = 0f,
    val longG: Double = 0.0,
    val latG: Double = 0.0,
)

@Serializable
data class RideMark(
    val t: Long,
    val kind: String,
    val lat: Double,
    val lon: Double,
    val note: String,
)

@Serializable
data class SavedDrive(
    val id: String,
    val startedAt: Long,
    val endedAt: Long,
    val points: List<RideSample> = emptyList(),
    val marks: List<RideMark> = emptyList(),
    val distanceM: Double = 0.0,
    val maxSpeedKmh: Double = 0.0,
    val avgSpeedKmh: Double = 0.0,
    val peakG: Double = 0.0,
    val elevationGainM: Double = 0.0,
    val brakeCount: Int = 0,
    val laneChanges: Int = 0,
    val score: Int = 0,
    val weather: String = "",
    val tempC: Double? = null,
    val aqi: Int? = null,
    val windKmh: Double? = null,
    val city: String = "",
    val province: String = "",
    val country: String = "",
    val storyTitle: String = "",
    val storyBody: String = "",
)

@Serializable
data class SavedMember(val userId: String, val name: String)

@Serializable
data class SavedConvoy(
    val id: String,
    val code: String,
    val name: String,
    val oath: String,
    val leaderId: String,
    val members: List<SavedMember> = emptyList(),
)

@Serializable
data class MeruSnapshot(
    val profile: DriverProfile? = null,
    val pinHash: String = "",
    val drives: List<SavedDrive> = emptyList(),
    val points: Int = 0,
    val pledgedRituals: List<String> = emptyList(),
    val checkpoint: SavedDrive? = null,
    val signedIn: Boolean = true,
    val care: List<CareMark> = emptyList(),
    val embarked: List<String> = emptyList(),
    val memberships: List<String> = emptyList(),
    val convoys: List<SavedConvoy> = emptyList(),
)

data class RideFix(
    val t: Long,
    val lat: Double,
    val lon: Double,
    val alt: Double?,
    val speedMps: Float,
    val bearing: Float,
    val longG: Float,
    val latG: Float,
    val synthetic: Boolean = false,
)

data class Sky(
    val label: String = "Waiting for the sky",
    val tempC: Double? = null,
    val aqi: Int? = null,
    val windKmh: Double? = null,
)

data class Here(
    val city: String = "",
    val province: String = "",
    val country: String = "",
)

data class BoardRow(
    val rank: Int,
    val name: String,
    val car: String,
    val points: Int,
    val place: String,
    val you: Boolean,
)

interface MeruFiles {
    fun read(): String?
    fun write(text: String)
}

expect fun meruFiles(): MeruFiles
expect fun platformHasGps(): Boolean
expect fun startHardwareRide()
expect fun stopHardwareRide()
