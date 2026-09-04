package com.travelcompanion.app.data.memory

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * A memory's metadata row.
 *
 * The audio itself is a file under `files/memories/`, named by this id (brief
 * §22). Keeping the two apart is what lets a finalized recording survive a
 * failure to write the row: the bytes are already on disk and nothing here can
 * take them back.
 */
@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey val id: String,
    val fileName: String,
    val recordedAtEpochMs: Long,
    val durationMs: Long,
    val participantId: String,
    val participantName: String,
    val cityName: String?,
    val placeName: String?,
)

@Dao
interface MemoryDao {
    /** Newest first, which is the order the list on screen 12 is read in. */
    @Query("SELECT * FROM memories ORDER BY recordedAtEpochMs DESC")
    fun memories(): Flow<List<MemoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(memory: MemoryEntity)

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun delete(id: String)
}

@Database(entities = [MemoryEntity::class], version = 1, exportSchema = false)
abstract class MemoryDatabase : RoomDatabase() {
    abstract fun memories(): MemoryDao
}

/** The Room-backed [MemoryRepository]. Room stops here and goes no further. */
class RoomMemoryRepository(private val dao: MemoryDao) : MemoryRepository {

    override fun memories(): Flow<List<Memory>> =
        dao.memories().map { rows -> rows.map(MemoryEntity::toMemory) }

    override suspend fun save(memory: Memory) = dao.insert(memory.toEntity())

    override suspend fun delete(id: String) = dao.delete(id)
}

private fun MemoryEntity.toMemory() = Memory(
    id = id,
    fileName = fileName,
    recordedAtEpochMs = recordedAtEpochMs,
    durationMs = durationMs,
    participantId = participantId,
    participantName = participantName,
    cityName = cityName,
    placeName = placeName,
)

private fun Memory.toEntity() = MemoryEntity(
    id = id,
    fileName = fileName,
    recordedAtEpochMs = recordedAtEpochMs,
    durationMs = durationMs,
    participantId = participantId,
    participantName = participantName,
    cityName = cityName,
    placeName = placeName,
)
