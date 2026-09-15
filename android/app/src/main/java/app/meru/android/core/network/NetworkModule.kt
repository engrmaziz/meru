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
    val awards: TripAwardsDto? = null,
    val ghost: GhostCompareResponse? = null,
)

@Serializable
data class TripAwardsDto(
    val clientTripId: String,
    val xpAwarded: Int = 0,
    val level: Int = 1,
    val title: String = "Novice Driver",
    val adventureScore: Int = 0,
    val driverRating: Int = 70,
    val qualityScore: Int = 0,
    val explorationScore: Int = 0,
    val activityScore: Int = 0,
    val integrity: Int = 90,
    val competitiveEligible: Boolean = true,
    val unlockedAchievements: List<AchievementUnlockDto> = emptyList(),
    val challengeProgress: List<ChallengeProgressDto> = emptyList(),
    val streakDays: Int = 0,
    val weightsVersion: Int = 1,
)

@Serializable
data class AchievementUnlockDto(
    val id: String,
    val title: String,
    val rarity: String,
    val xpBonus: Int = 0,
)

@Serializable
data class ChallengeProgressDto(
    val id: String,
    val title: String,
    val progress: Double = 0.0,
    val target: Double = 1.0,
    val completed: Boolean = false,
)

@Serializable
data class ScoreComponentsDto(
    val quality: Int = 0,
    val exploration: Int = 0,
    val activity: Int = 0,
)

@Serializable
data class ScoresMeResponse(
    val xpTotal: Int = 0,
    val level: Int = 1,
    val title: String = "Novice Driver",
    val xpIntoLevel: Int = 0,
    val xpForNextLevel: Int = 1000,
    val adventureScore: Int = 0,
    val driverRating: Int = 70,
    val components: ScoreComponentsDto = ScoreComponentsDto(),
    val streakDays: Int = 0,
    val tripCount: Int = 0,
    val totalDistanceM: Double = 0.0,
    val totalCells: Int = 0,
    val competitiveEligible: Boolean = true,
    val weightsVersion: Int = 1,
    val lastTripAwards: TripAwardsDto? = null,
)

@Serializable
data class AchievementItemDto(
    val id: String,
    val title: String,
    val description: String,
    val rarity: String,
    val xpBonus: Int = 0,
    val unlocked: Boolean = false,
)

@Serializable
data class AchievementsMeResponse(
    val items: List<AchievementItemDto> = emptyList(),
)

@Serializable
data class ChallengeItemDto(
    val id: String,
    val title: String,
    val description: String,
    val period: String,
    val metric: String,
    val target: Double = 0.0,
    val xpReward: Int = 0,
    val progress: Double = 0.0,
    val completed: Boolean = false,
    val periodKey: String = "",
)

@Serializable
data class ScoreWeightsDto(
    val version: Int = 1,
    val quality: Double = 0.45,
    val exploration: Double = 0.35,
    val activity: Double = 0.2,
    val xpPerQualityPoint: Double = 2.0,
    val xpPerExplorationPoint: Double = 3.0,
    val xpPerActivityPoint: Double = 1.0,
    val xpPerKm: Double = 8.0,
    val xpPerNewCell: Double = 12.0,
    val integrityCompetitiveMin: Int = 75,
    val levelXpStep: Int = 1000,
)

@Serializable
data class ChallengesResponse(
    val items: List<ChallengeItemDto> = emptyList(),
)

@Serializable
data class BoardEntryDto(
    val userId: String,
    val displayName: String,
    val rank: Int = 0,
    val prevRank: Int? = null,
    val score: Int = 0,
    val level: Int = 1,
    val driverRating: Int = 70,
    val delta: Int = 0,
)

@Serializable
data class LeaderboardResponse(
    val disabled: Boolean = false,
    val seasonId: String = "",
    val seasonName: String = "",
    val geoType: String = "",
    val geoId: String = "",
    val geoName: String = "",
    val period: String = "season",
    val entries: List<BoardEntryDto> = emptyList(),
    val you: BoardEntryDto? = null,
    val podium: List<BoardEntryDto> = emptyList(),
)

@Serializable
data class RankChipDto(
    val geoType: String,
    val geoId: String,
    val geoName: String,
    val rank: Int? = null,
    val score: Int = 0,
    val delta: Int = 0,
)

@Serializable
data class RanksMeResponse(
    val seasonId: String = "",
    val boardOptIn: Boolean = true,
    val ranks: List<RankChipDto> = emptyList(),
)

@Serializable
data class FriendsBoardResponse(
    val seasonId: String = "",
    val entries: List<BoardEntryDto> = emptyList(),
    val you: BoardEntryDto? = null,
)

@Serializable
data class ExplorationMapResponse(
    val cityId: String = "",
    val cityName: String = "",
    val cellsExplored: Int = 0,
    val cityCellBudget: Int = 2500,
    val cityPercent: Double = 0.0,
    val sampleCells: List<String> = emptyList(),
)

@Serializable
data class GhostCompareResponse(
    val enabled: Boolean = true,
    val matched: Boolean = false,
    val priorQuality: Int? = null,
    val currentQuality: Int? = null,
    val delta: Int? = null,
    val message: String = "",
)

@Serializable
data class ShareCardResponse(
    val displayName: String = "Meru Driver",
    val title: String = "",
    val level: Int = 1,
    val adventureScore: Int = 0,
    val cityRank: Int? = null,
    val seasonName: String = "",
    val cityName: String = "",
    val tagline: String = "",
)

@Serializable
data class PrivacyMeResponse(
    val boardOptIn: Boolean = true,
    val homeCityId: String = "pk-pb-lhr",
    val followingCount: Int = 0,
)

@Serializable
data class PrivacyPatchRequest(
    val boardOptIn: Boolean = true,
    val displayName: String? = null,
)

@Serializable
data class SeasonCurrentResponse(
    val id: String = "",
    val name: String = "",
    val startsAt: Long = 0,
    val endsAt: Long = 0,
    val primaryPeriod: String = "season",
)

@Serializable
data class VehicleDto(
    val id: String,
    val make: String,
    val model: String,
    val year: Int,
    val variant: String? = null,
    val powertrain: String? = null,
    val nickname: String,
    val vinMasked: String? = null,
    val odometerKm: Double = 0.0,
    val purchaseAtMs: Long? = null,
    val createdAtMs: Long = 0,
    val active: Boolean = false,
)

@Serializable
data class VehiclesListResponse(
    val maxVehicles: Int = 1,
    val vehicles: List<VehicleDto> = emptyList(),
    val canAdd: Boolean = true,
)

@Serializable
data class CreateVehicleRequest(
    val make: String,
    val model: String,
    val year: Int,
    val variant: String? = null,
    val powertrain: String? = null,
    val nickname: String? = null,
    val vin: String? = null,
    val odometerKm: Double? = null,
    val purchaseAtMs: Long? = null,
)

@Serializable
data class TimelineItemDto(
    val id: String,
    val atMs: Long,
    val kind: String,
    val title: String,
    val subtitle: String? = null,
    val meta: TimelineMetaDto? = null,
)

@Serializable
data class TimelineMetaDto(
    val certified: Boolean = false,
    val nextDueAtMs: Long? = null,
    val invoiceId: String? = null,
    val source: String? = null,
)

@Serializable
data class TimelineSummaryDto(
    val lifetimeCost: Double = 0.0,
    val serviceVisits: Int = 0,
    val documentCount: Int = 0,
    val nextDueAtMs: Long? = null,
)

@Serializable
data class TimelineResponse(
    val vehicle: VehicleDto,
    val summary: TimelineSummaryDto = TimelineSummaryDto(),
    val items: List<TimelineItemDto> = emptyList(),
)

@Serializable
data class CreateServiceRequest(
    val clientServiceId: String,
    val atMs: Long? = null,
    val odometerKm: Double? = null,
    val workshopName: String? = null,
    val serviceTypeIds: List<String> = emptyList(),
    val notes: String? = null,
    val laborCost: Double = 0.0,
    val partsCost: Double = 0.0,
    val nextDueAtMs: Long? = null,
)

@Serializable
data class ServiceResponseDto(
    val id: String,
    val vehicleId: String,
    val clientServiceId: String,
    val atMs: Long,
    val odometerKm: Double = 0.0,
    val workshopName: String? = null,
    val laborCost: Double = 0.0,
    val partsCost: Double = 0.0,
    val nextDueAtMs: Long? = null,
    val duplicated: Boolean = false,
)

@Serializable
data class DocumentCreateRequest(
    val type: String = "other",
    val title: String = "Document",
    val expiresAtMs: Long? = null,
)

@Serializable
data class DocumentCreateResponse(
    val id: String,
    val vehicleId: String,
    val type: String,
    val title: String,
    val expiresAtMs: Long? = null,
    val uploadUrl: String = "",
    val uploaded: Boolean = false,
)

@Serializable
data class HistoryShareRequest(
    val scope: String = "timeline_readonly",
    val ttlHours: Int = 24,
)

@Serializable
data class HistoryShareResponse(
    val id: String,
    val token: String,
    val scope: String,
    val expiresAtMs: Long,
    val redeemHint: String = "",
)

@Serializable
data class PlayVerifyRequest(
    val purchaseToken: String,
    val sku: String = "meru_extra_vehicle_slot",
)

@Serializable
data class EntitlementResponse(
    val maxVehicles: Int = 1,
    val verified: Boolean = false,
)

@Serializable
data class CostsResponse(
    val total: Double = 0.0,
    val byMonth: List<CostMonthDto> = emptyList(),
)

@Serializable
data class CostMonthDto(
    val month: String,
    val amount: Double = 0.0,
)

@Serializable
data class ServiceTypesResponse(
    val items: List<ServiceTypeDto> = emptyList(),
)

@Serializable
data class ServiceTypeDto(
    val id: String,
    val label: String,
    val category: String = "other",
)

@Serializable
data class WorkshopsListResponse(
    val items: List<WorkshopDto> = emptyList(),
)

@Serializable
data class WorkshopDto(
    val id: String,
    val name: String,
    val kind: String = "general",
    val brands: List<String> = emptyList(),
    val cityId: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val rating: Double = 0.0,
    val jobsCompleted: Int = 0,
    val kycStatus: String = "pending",
    val verified: Boolean = false,
    val description: String = "",
    val distanceKm: Double = 0.0,
    val brandFit: Int = 0,
    val services: List<ServiceTypeDto> = emptyList(),
    val parts: List<WorkshopPartDto> = emptyList(),
    val hours: WorkshopHoursDto? = null,
    val bays: Int = 0,
)

@Serializable
data class WorkshopPartDto(
    val id: String,
    val name: String,
    val brand: String? = null,
    val price: Double = 0.0,
)

@Serializable
data class WorkshopHoursDto(
    val open: String = "09:00",
    val close: String = "18:00",
)

@Serializable
data class SlotsResponse(
    val items: List<SlotDto> = emptyList(),
)

@Serializable
data class SlotDto(
    val id: String,
    val startAtMs: Long,
    val endAtMs: Long,
    val bay: Int = 1,
)

@Serializable
data class CreateBookingRequest(
    val workshopId: String,
    val vehicleId: String,
    val slotId: String,
    val serviceIds: List<String> = emptyList(),
    val historyShareToken: String? = null,
)

@Serializable
data class BookingDto(
    val id: String,
    val workshopId: String,
    val vehicleId: String,
    val slotId: String,
    val serviceIds: List<String> = emptyList(),
    val historyShareToken: String? = null,
    val status: String = "confirmed",
    val createdAtMs: Long = 0,
    val workshopName: String? = null,
    val startAtMs: Long? = null,
    val endAtMs: Long? = null,
    val statusLabel: String? = null,
    val holdTtlMs: Long? = null,
)

@Serializable
data class BookingsListResponse(
    val items: List<BookingDto> = emptyList(),
)

@Serializable
data class NotificationsResponse(
    val items: List<NotifDto> = emptyList(),
)

@Serializable
data class NotifDto(
    val id: String,
    val type: String = "",
    val title: String = "",
    val body: String = "",
    val atMs: Long = 0,
    val read: Boolean = false,
)

@Serializable
data class JobDto(
    val id: String,
    val bookingId: String = "",
    val workshopId: String = "",
    val vehicleId: String = "",
    val status: String = "confirmed",
    val workshopName: String? = null,
    val extras: List<ExtraWorkDto> = emptyList(),
    val invoice: JobInvoiceSummaryDto? = null,
    val historyShareToken: String? = null,
)

@Serializable
data class ExtraWorkDto(
    val id: String,
    val description: String = "",
    val estimatedCost: Double = 0.0,
    val status: String = "pending",
)

@Serializable
data class JobInvoiceSummaryDto(
    val id: String,
    val total: Double = 0.0,
    val status: String = "issued",
    val pdfUrl: String = "",
)

@Serializable
data class JobsListResponse(
    val items: List<JobDto> = emptyList(),
)

@Serializable
data class InvoiceDto(
    val id: String,
    val jobId: String = "",
    val vehicleId: String = "",
    val workshopId: String = "",
    val lines: List<InvoiceLineDto> = emptyList(),
    val laborTotal: Double = 0.0,
    val partsTotal: Double = 0.0,
    val feesTotal: Double = 0.0,
    val total: Double = 0.0,
    val pdfUrl: String = "",
    val status: String = "issued",
    val createdAtMs: Long = 0,
    val serviceRecordId: String? = null,
)

@Serializable
data class InvoiceLineDto(
    val kind: String = "labor",
    val label: String = "",
    val qty: Double = 1.0,
    val unitPrice: Double = 0.0,
    val serviceTypeId: String? = null,
)

@Serializable
data class InvoicesListResponse(
    val items: List<InvoiceDto> = emptyList(),
)

@Serializable
data class ConfirmInvoiceResponse(
    val invoice: InvoiceDto,
    val writeback: WritebackDto? = null,
    val duplicated: Boolean = false,
)

@Serializable
data class WritebackDto(
    val id: String,
    val certified: Boolean = false,
    val invoiceId: String? = null,
    val workshopName: String? = null,
    val serviceTypeIds: List<String> = emptyList(),
)

@Serializable
data class ExtraDecisionRequest(
    val approve: Boolean,
)

@Serializable
data class ReviewRequest(
    val jobId: String,
    val rating: Int,
    val comment: String? = null,
)

@Serializable
data class ReviewDto(
    val id: String,
    val jobId: String,
    val rating: Int,
    val comment: String? = null,
)

@Serializable
data class DisputeRequest(
    val reason: String = "owner_dispute",
)

@Serializable
data class PublicFlagsDto(
    val softLaunchCityId: String = "pk-pb-lhr",
    val softLaunchCityName: String = "Lahore",
    val s2Leaderboards: Boolean = true,
    val s3Garage: Boolean = true,
    val s4Marketplace: Boolean = true,
    val ghostDriver: Boolean = true,
    val challengesEnabled: Boolean = true,
    val accountDeletionEnabled: Boolean = true,
    val bookingsEnabled: Boolean = true,
    val integrityCompetitiveMin: Int = 75,
    val weightsVersion: Int = 1,
)

@Serializable
data class DeleteAccountResponse(
    val deleted: Boolean = false,
    val retentionNote: String = "",
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

    @GET("v1/scores/me")
    suspend fun scoresMe(@Header("Authorization") authorization: String): ScoresMeResponse

    @GET("v1/scores/config")
    suspend fun scoresConfig(): ScoreWeightsDto

    @GET("v1/achievements/me")
    suspend fun achievementsMe(@Header("Authorization") authorization: String): AchievementsMeResponse

    @GET("v1/challenges")
    suspend fun challenges(@Header("Authorization") authorization: String): ChallengesResponse

    @GET("v1/seasons/current")
    suspend fun seasonCurrent(): SeasonCurrentResponse

    @GET("v1/leaderboards/{geoType}/{geoId}")
    suspend fun leaderboard(
        @Header("Authorization") authorization: String,
        @retrofit2.http.Path("geoType") geoType: String,
        @retrofit2.http.Path("geoId") geoId: String,
        @retrofit2.http.Query("period") period: String = "season",
    ): LeaderboardResponse

    @GET("v1/ranks/me")
    suspend fun ranksMe(@Header("Authorization") authorization: String): RanksMeResponse

    @GET("v1/friends/leaderboard")
    suspend fun friendsLeaderboard(@Header("Authorization") authorization: String): FriendsBoardResponse

    @POST("v1/friends/{targetId}/follow")
    suspend fun follow(
        @Header("Authorization") authorization: String,
        @retrofit2.http.Path("targetId") targetId: String,
    ): kotlinx.serialization.json.JsonObject

    @GET("v1/exploration/map")
    suspend fun explorationMap(@Header("Authorization") authorization: String): ExplorationMapResponse

    @GET("v1/share/card")
    suspend fun shareCard(@Header("Authorization") authorization: String): ShareCardResponse

    @GET("v1/privacy/me")
    suspend fun privacyMe(@Header("Authorization") authorization: String): PrivacyMeResponse

    @retrofit2.http.PATCH("v1/privacy/me")
    suspend fun privacyPatch(
        @Header("Authorization") authorization: String,
        @Body body: PrivacyPatchRequest,
    ): PrivacyMeResponse

    @GET("v1/service-types")
    suspend fun serviceTypes(): ServiceTypesResponse

    @GET("v1/vehicles")
    suspend fun vehicles(@Header("Authorization") authorization: String): VehiclesListResponse

    @POST("v1/vehicles")
    suspend fun createVehicle(
        @Header("Authorization") authorization: String,
        @Body body: CreateVehicleRequest,
    ): VehicleDto

    @GET("v1/vehicles/{id}/timeline")
    suspend fun vehicleTimeline(
        @Header("Authorization") authorization: String,
        @retrofit2.http.Path("id") id: String,
    ): TimelineResponse

    @POST("v1/vehicles/{id}/services")
    suspend fun createService(
        @Header("Authorization") authorization: String,
        @retrofit2.http.Path("id") id: String,
        @Body body: CreateServiceRequest,
    ): ServiceResponseDto

    @POST("v1/vehicles/{id}/documents")
    suspend fun createDocument(
        @Header("Authorization") authorization: String,
        @retrofit2.http.Path("id") id: String,
        @Body body: DocumentCreateRequest,
    ): DocumentCreateResponse

    @POST("v1/vehicles/{id}/documents/{docId}/confirm")
    suspend fun confirmDocument(
        @Header("Authorization") authorization: String,
        @retrofit2.http.Path("id") id: String,
        @retrofit2.http.Path("docId") docId: String,
    ): DocumentCreateResponse

    @GET("v1/vehicles/{id}/costs")
    suspend fun vehicleCosts(
        @Header("Authorization") authorization: String,
        @retrofit2.http.Path("id") id: String,
    ): CostsResponse

    @POST("v1/vehicles/{id}/history-shares")
    suspend fun createHistoryShare(
        @Header("Authorization") authorization: String,
        @retrofit2.http.Path("id") id: String,
        @Body body: HistoryShareRequest = HistoryShareRequest(),
    ): HistoryShareResponse

    @POST("v1/billing/play/verify")
    suspend fun verifyPlayPurchase(
        @Header("Authorization") authorization: String,
        @Body body: PlayVerifyRequest,
    ): EntitlementResponse

    @GET("v1/billing/entitlement")
    suspend fun entitlement(@Header("Authorization") authorization: String): EntitlementResponse

    @GET("v1/workshops")
    suspend fun workshops(
        @retrofit2.http.Query("make") make: String? = null,
        @retrofit2.http.Query("lat") lat: Double? = null,
        @retrofit2.http.Query("lon") lon: Double? = null,
        @retrofit2.http.Query("q") q: String? = null,
        @retrofit2.http.Query("verifiedOnly") verifiedOnly: String? = "true",
    ): WorkshopsListResponse

    @GET("v1/workshops/{id}")
    suspend fun workshop(@retrofit2.http.Path("id") id: String): WorkshopDto

    @GET("v1/workshops/{id}/slots")
    suspend fun workshopSlots(
        @retrofit2.http.Path("id") id: String,
        @retrofit2.http.Query("fromMs") fromMs: Long? = null,
    ): SlotsResponse

    @POST("v1/bookings")
    suspend fun createBooking(
        @Header("Authorization") authorization: String,
        @Body body: CreateBookingRequest,
    ): BookingDto

    @GET("v1/bookings")
    suspend fun bookings(@Header("Authorization") authorization: String): BookingsListResponse

    @POST("v1/bookings/{id}/cancel")
    suspend fun cancelBooking(
        @Header("Authorization") authorization: String,
        @retrofit2.http.Path("id") id: String,
    ): BookingDto

    @GET("v1/notifications")
    suspend fun notifications(@Header("Authorization") authorization: String): NotificationsResponse

    @GET("v1/jobs/mine")
    suspend fun myJobs(@Header("Authorization") authorization: String): JobsListResponse

    @GET("v1/jobs/{id}")
    suspend fun job(
        @Header("Authorization") authorization: String,
        @retrofit2.http.Path("id") id: String,
    ): JobDto

    @POST("v1/jobs/{id}/extras/{eid}/decision")
    suspend fun decideExtra(
        @Header("Authorization") authorization: String,
        @retrofit2.http.Path("id") id: String,
        @retrofit2.http.Path("eid") eid: String,
        @Body body: ExtraDecisionRequest,
    ): ExtraWorkDto

    @GET("v1/invoices")
    suspend fun invoices(@Header("Authorization") authorization: String): InvoicesListResponse

    @GET("v1/invoices/{id}")
    suspend fun invoice(
        @Header("Authorization") authorization: String,
        @retrofit2.http.Path("id") id: String,
    ): InvoiceDto

    @POST("v1/invoices/{id}/confirm")
    suspend fun confirmInvoice(
        @Header("Authorization") authorization: String,
        @retrofit2.http.Path("id") id: String,
    ): ConfirmInvoiceResponse

    @POST("v1/invoices/{id}/dispute")
    suspend fun disputeInvoice(
        @Header("Authorization") authorization: String,
        @retrofit2.http.Path("id") id: String,
        @Body body: DisputeRequest = DisputeRequest(),
    ): InvoiceDto

    @POST("v1/reviews")
    suspend fun submitReview(
        @Header("Authorization") authorization: String,
        @Body body: ReviewRequest,
    ): ReviewDto

    @GET("v1/flags")
    suspend fun publicFlags(): PublicFlagsDto

    @retrofit2.http.HTTP(method = "DELETE", path = "v1/account", hasBody = false)
    suspend fun deleteAccount(@Header("Authorization") authorization: String): DeleteAccountResponse
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
