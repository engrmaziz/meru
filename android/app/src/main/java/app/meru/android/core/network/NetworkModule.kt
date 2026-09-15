package app.meru.android.core.network

import app.meru.android.BuildConfig
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

@Serializable
data class HealthResponse(val status: String)

@Serializable
data class AuthRequest(
    val email: String,
    val password: String,
    val displayName: String? = null,
)

@Serializable
data class AuthResponse(
    val accessToken: String,
    val user: AuthUser,
)

@Serializable
data class AuthUser(
    val id: String,
    val email: String,
    val displayName: String,
)

@Serializable
data class GoogleAuthRequest(
    val idToken: String = "dev-google-token",
    val displayName: String = "Meru Driver",
    val email: String = "driver@meru.app",
)

@Serializable
data class TripLocationDto(
    val ts: Long,
    val lat: Double,
    val lon: Double,
    val alt: Double? = null,
    val speed: Double? = null,
    val bearing: Float? = null,
    val acc: Float? = null,
)

@Serializable
data class TripEventDto(
    val ts: Long,
    val type: String,
    val label: String,
    val severity: Int = 0,
    val lat: Double? = null,
    val lon: Double? = null,
)

@Serializable
data class TripUpsertRequest(
    val clientTripId: String,
    val startAtMs: Long,
    val endAtMs: Long? = null,
    val distanceM: Double = 0.0,
    val durationMs: Long = 0,
    val avgSpeedKmh: Double = 0.0,
    val maxSpeedKmh: Double = 0.0,
    val qualityScore: Int = 0,
    val explorationXp: Int = 0,
    val newCells: Int = 0,
    val elevationGainM: Double = 0.0,
    val stopCount: Int = 0,
    val pointCount: Int = 0,
    val locations: List<TripLocationDto> = emptyList(),
    val events: List<TripEventDto> = emptyList(),
)

@Serializable
data class TripUpsertResponse(
    val id: String,
    val clientTripId: String,
    val integrity: Int = 90,
    val duplicated: Boolean = false,
)

interface MeruApi {
    @GET("health")
    suspend fun health(): HealthResponse

    @POST("v1/auth/register")
    suspend fun register(@Body body: AuthRequest): AuthResponse

    @POST("v1/auth/login")
    suspend fun login(@Body body: AuthRequest): AuthResponse

    @POST("v1/auth/google")
    suspend fun google(@Body body: GoogleAuthRequest): AuthResponse

    @POST("v1/trips")
    suspend fun upsertTrip(
        @Header("Authorization") authorization: String,
        @Body body: TripUpsertRequest,
    ): TripUpsertResponse
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun json(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun okHttp(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                },
            )
            .build()

    @Provides
    @Singleton
    fun retrofit(json: Json, client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun api(retrofit: Retrofit): MeruApi = retrofit.create(MeruApi::class.java)
}
