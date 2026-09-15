package app.meru.android.core.database

import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.RoomDatabase

@Entity(tableName = "pending_sync")
data class PendingSyncEntity(
    @PrimaryKey val id: String,
    val type: String,
    val payloadJson: String,
    val attempts: Int = 0,
    val status: String = "pending",
)

@Database(
    entities = [PendingSyncEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class MeruDatabase : RoomDatabase() {
    abstract fun pendingSyncDao(): PendingSyncDao
}

@androidx.room.Dao
interface PendingSyncDao {
    @androidx.room.Query("SELECT * FROM pending_sync WHERE status = 'pending'")
    suspend fun pending(): List<PendingSyncEntity>

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PendingSyncEntity)
}
