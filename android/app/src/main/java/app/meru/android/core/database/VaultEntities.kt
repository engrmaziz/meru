package app.meru.android.core.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey val id: String,
    val make: String,
    val model: String,
    val year: Int,
    val variant: String? = null,
    val powertrain: String? = null,
    val nickname: String,
    val vinMasked: String? = null,
    val odometerKm: Double = 0.0,
    val active: Boolean = false,
    val syncStatus: String = "synced",
)

@Entity(tableName = "vehicle_services")
data class VehicleServiceEntity(
    @PrimaryKey val id: String,
    val vehicleId: String,
    val clientServiceId: String,
    val atMs: Long,
    val odometerKm: Double = 0.0,
    val workshopName: String? = null,
    val serviceTypeIdsJson: String = "[]",
    val notes: String? = null,
    val laborCost: Double = 0.0,
    val partsCost: Double = 0.0,
    val nextDueAtMs: Long? = null,
    val syncStatus: String = "pending",
)

@Entity(tableName = "vehicle_documents")
data class VehicleDocumentEntity(
    @PrimaryKey val id: String,
    val vehicleId: String,
    val type: String,
    val title: String,
    val expiresAtMs: Long? = null,
    val uploadUrl: String? = null,
    val uploaded: Boolean = false,
)

@Dao
interface VehicleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(vehicle: VehicleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(vehicles: List<VehicleEntity>)

    @Query("SELECT * FROM vehicles ORDER BY active DESC, nickname ASC")
    fun observeAll(): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles WHERE id = :id LIMIT 1")
    suspend fun get(id: String): VehicleEntity?

    @Query("SELECT * FROM vehicles WHERE active = 1 LIMIT 1")
    suspend fun active(): VehicleEntity?

    @Query("SELECT COUNT(*) FROM vehicles")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertService(service: VehicleServiceEntity)

    @Query("SELECT * FROM vehicle_services WHERE vehicleId = :vehicleId ORDER BY atMs DESC")
    suspend fun servicesFor(vehicleId: String): List<VehicleServiceEntity>

    @Query("SELECT * FROM vehicle_services WHERE syncStatus = 'pending'")
    suspend fun pendingServices(): List<VehicleServiceEntity>

    @Query("UPDATE vehicle_services SET syncStatus = :status WHERE id = :id")
    suspend fun markService(id: String, status: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDocument(doc: VehicleDocumentEntity)

    @Query("SELECT * FROM vehicle_documents WHERE vehicleId = :vehicleId ORDER BY title ASC")
    suspend fun documentsFor(vehicleId: String): List<VehicleDocumentEntity>
}
