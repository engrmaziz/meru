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

interface MeruApi {
    @GET("health")
    suspend fun health(): HealthResponse

    @POST("v1/auth/register")
    suspend fun register(@Body body: AuthRequest): AuthResponse

    @POST("v1/auth/login")
    suspend fun login(@Body body: AuthRequest): AuthResponse

    @POST("v1/auth/google")
    suspend fun google(@Body body: GoogleAuthRequest): AuthResponse
}

@Serializable
data class GoogleAuthRequest(
    val idToken: String = "dev-google-token",
    val displayName: String = "Meru Driver",
    val email: String = "driver@meru.app",
)

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun json(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
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
