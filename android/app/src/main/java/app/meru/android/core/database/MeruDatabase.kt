package app.meru.android.core.database

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey val id: String,
    val vehicleId: String? = null,
    val startAtMs: Long,
    val endAtMs: Long? = null,
    val distanceM: Double = 0.0,
    val durationMs: Long = 0L,
    val avgSpeedKmh: Double = 0.0,
    val maxSpeedKmh: Double = 0.0,
    val pointCount: Int = 0,
    val rejectedJumps: Int = 0,
    val status: String = "active", // active | completed | processed
    val syncStatus: String = "pending", // pending | synced | failed
    val qualityScore: Int = 0,
    val explorationXp: Int = 0,
    val newCells: Int = 0,
    val elevationGainM: Double = 0.0,
    val elevationLossM: Double = 0.0,
    val stopCount: Int = 0,
)

@Entity(tableName = "trip_locations")
data class TripLocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: String,
    val timestampMs: Long,
    val latitude: Double,
    val longitude: Double,
    val altitudeM: Double? = null,
    val speedMps: Double? = null,
    val bearing: Float? = null,
    val accuracyM: Float? = null,
)

@Entity(tableName = "trip_events")
data class TripEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: String,
    val timestampMs: Long,
    val type: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val severity: Int = 0,
    val label: String,
)

@Entity(tableName = "explored_cells")
data class ExploredCellEntity(
    @PrimaryKey val cellId: String,
    val firstTripId: String,
    val discoveredAtMs: Long,
)

@Entity(tableName = "pending_sync")
data class PendingSyncEntity(
    @PrimaryKey val id: String,
    val type: String,
    val payloadJson: String,
    val attempts: Int = 0,
    val status: String = "pending",
)

@Dao
interface TripDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTrip(trip: TripEntity)

    @Insert
    suspend fun insertLocation(location: TripLocationEntity)

    @Insert
    suspend fun insertEvents(events: List<TripEventEntity>)

    @Query("SELECT * FROM trips WHERE id = :id LIMIT 1")
    suspend fun getTrip(id: String): TripEntity?

    @Query("SELECT * FROM trips ORDER BY startAtMs DESC")
    fun observeTrips(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE status != 'active' ORDER BY startAtMs DESC")
    fun observeCompletedTrips(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trip_locations WHERE tripId = :tripId ORDER BY timestampMs ASC")
    suspend fun locationsForTrip(tripId: String): List<TripLocationEntity>

    @Query("SELECT * FROM trip_events WHERE tripId = :tripId ORDER BY timestampMs ASC")
    suspend fun eventsForTrip(tripId: String): List<TripEventEntity>

    @Query("SELECT COUNT(*) FROM trips WHERE status != 'active'")
    suspend fun completedCount(): Int

    @Query("SELECT COALESCE(SUM(distanceM), 0) FROM trips WHERE status != 'active'")
    suspend fun totalDistanceM(): Double

    @Query("SELECT COALESCE(SUM(durationMs), 0) FROM trips WHERE status != 'active'")
    suspend fun totalDurationMs(): Long

    @Query("SELECT COALESCE(MAX(distanceM), 0) FROM trips WHERE status != 'active'")
    suspend fun longestTripM(): Double

    @Query("SELECT COALESCE(MAX(qualityScore), 0) FROM trips WHERE status != 'active'")
    suspend fun bestQuality(): Int

    @Query("UPDATE trips SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)
}

@Dao
interface ExplorationDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(cell: ExploredCellEntity): Long

    @Query("SELECT COUNT(*) FROM explored_cells")
    suspend fun cellCount(): Int

    @Query("SELECT cellId FROM explored_cells")
    suspend fun allCellIds(): List<String>
}

@Dao
interface PendingSyncDao {
    @Query("SELECT * FROM pending_sync WHERE status = 'pending'")
    suspend fun pending(): List<PendingSyncEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PendingSyncEntity)

    @Query("UPDATE pending_sync SET status = :status, attempts = attempts + 1 WHERE id = :id")
    suspend fun mark(id: String, status: String)
}

@Database(
    entities = [
        TripEntity::class,
        TripLocationEntity::class,
        TripEventEntity::class,
        ExploredCellEntity::class,
        PendingSyncEntity::class,
        VehicleEntity::class,
        VehicleServiceEntity::class,
        VehicleDocumentEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class MeruDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun explorationDao(): ExplorationDao
    abstract fun pendingSyncDao(): PendingSyncDao
    abstract fun vehicleDao(): VehicleDao
}
