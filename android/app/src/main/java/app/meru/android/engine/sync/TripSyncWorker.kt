package app.meru.android.engine.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.meru.android.core.database.PendingSyncDao
import app.meru.android.core.database.TripDao
import app.meru.android.core.database.VehicleDao
import app.meru.android.core.datastore.ProgressionStore
import app.meru.android.core.datastore.SessionStore
import app.meru.android.core.network.CreateServiceRequest
import app.meru.android.core.network.MeruApi
import app.meru.android.core.network.TripUpsertRequest
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

@HiltWorker
class TripSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val pendingSyncDao: PendingSyncDao,
    private val tripDao: TripDao,
    private val vehicleDao: VehicleDao,
    private val api: MeruApi,
    private val sessionStore: SessionStore,
    private val progressionStore: ProgressionStore,
    private val json: Json,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val token = sessionStore.session.first().accessToken ?: return Result.retry()
        var failures = 0

        val pending = pendingSyncDao.pending().filter { it.type == "trip_complete" }
        for (item in pending) {
            runCatching {
                val body = json.decodeFromString(TripUpsertRequest.serializer(), item.payloadJson)
                val res = api.upsertTrip("Bearer $token", body)
                res.awards?.let { awards ->
                    if (!res.duplicated) {
                        progressionStore.applyAwards(awards, res.ghost?.message)
                    }
                    val trip = tripDao.getTrip(item.id)
                    if (trip != null) {
                        tripDao.upsertTrip(
                            trip.copy(
                                qualityScore = awards.qualityScore,
                                explorationXp = awards.xpAwarded,
                                syncStatus = "synced",
                            ),
                        )
                    } else {
                        tripDao.updateSyncStatus(item.id, "synced")
                    }
                } ?: tripDao.updateSyncStatus(item.id, "synced")
                pendingSyncDao.mark(item.id, "synced")
                runCatching {
                    progressionStore.applyScores(api.scoresMe("Bearer $token"))
                }
            }.onFailure {
                failures++
                tripDao.updateSyncStatus(item.id, "failed")
            }
        }

        for (svc in vehicleDao.pendingServices()) {
            runCatching {
                api.createService(
                    "Bearer $token",
                    svc.vehicleId,
                    CreateServiceRequest(
                        clientServiceId = svc.clientServiceId,
                        atMs = svc.atMs,
                        odometerKm = svc.odometerKm,
                        workshopName = svc.workshopName,
                        serviceTypeIds = listOf("oil_change"),
                        laborCost = svc.laborCost,
                        partsCost = svc.partsCost,
                        nextDueAtMs = svc.nextDueAtMs,
                    ),
                )
                vehicleDao.markService(svc.id, "synced")
            }.onFailure {
                failures++
                vehicleDao.markService(svc.id, "failed")
            }
        }

        return if (failures == 0) Result.success() else Result.retry()
    }

    companion object {
        private const val UNIQUE = "meru_trip_sync"

        fun enqueue(context: Context) {
            val req = OneTimeWorkRequestBuilder<TripSyncWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE,
                ExistingWorkPolicy.KEEP,
                req,
            )
        }
    }
}
