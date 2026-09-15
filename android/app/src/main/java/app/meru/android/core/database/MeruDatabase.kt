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
    val status: String = "active", // active | completed
    val syncStatus: String = "pending", // pending | synced | failed
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

    @Query("SELECT * FROM trips WHERE id = :id LIMIT 1")
    suspend fun getTrip(id: String): TripEntity?

    @Query("SELECT * FROM trips ORDER BY startAtMs DESC")
    fun observeTrips(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips ORDER BY startAtMs DESC")
    suspend fun listTrips(): List<TripEntity>

    @Query("SELECT * FROM trip_locations WHERE tripId = :tripId ORDER BY timestampMs ASC")
    suspend fun locationsForTrip(tripId: String): List<TripLocationEntity>

    @Query("SELECT COUNT(*) FROM trip_locations WHERE tripId = :tripId")
    suspend fun locationCount(tripId: String): Int
}

@Dao
interface PendingSyncDao {
    @Query("SELECT * FROM pending_sync WHERE status = 'pending'")
    suspend fun pending(): List<PendingSyncEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PendingSyncEntity)
}

@Database(
    entities = [TripEntity::class, TripLocationEntity::class, PendingSyncEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class MeruDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun pendingSyncDao(): PendingSyncDao
}
