package com.bigbrother.mobile.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups ORDER BY CASE WHEN isSystem = 1 THEN 0 ELSE 1 END, sortOrder, name COLLATE NOCASE")
    fun observeAll(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups")
    suspend fun getAllOnce(): List<GroupEntity>

    @Query("SELECT * FROM groups WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): GroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: GroupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<GroupEntity>)

    @Query("UPDATE groups SET name = :name WHERE id = :id")
    suspend fun rename(id: String, name: String)

    @Query("UPDATE groups SET colorArgb = :color WHERE id = :id")
    suspend fun updateColor(id: String, color: Int)

    @Query("UPDATE groups SET isDeleted = :deleted WHERE id = :id")
    suspend fun setDeleted(id: String, deleted: Boolean)

    @Query("UPDATE groups SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: String, sortOrder: Int)

    @Query("DELETE FROM groups")
    suspend fun deleteAll()

    @Delete
    suspend fun delete(entity: GroupEntity)
}

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY isDeleted, isFavorite DESC, sortOrder, name COLLATE NOCASE")
    fun observeAll(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events")
    suspend fun getAllOnce(): List<EventEntity>

    @Query("SELECT * FROM events WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): EventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: EventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<EventEntity>)

    @Query("UPDATE events SET name = :name WHERE id = :id")
    suspend fun rename(id: String, name: String)

    @Query("UPDATE events SET groupId = :groupId WHERE id = :id")
    suspend fun move(id: String, groupId: String)

    @Query("UPDATE events SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean)

    @Query("UPDATE events SET isDeleted = :deleted WHERE id = :id")
    suspend fun setDeleted(id: String, deleted: Boolean)

    @Query("DELETE FROM events")
    suspend fun deleteAll()

    @Delete
    suspend fun delete(entity: EventEntity)
}

@Dao
interface RecordDao {
    @Query("SELECT * FROM records ORDER BY startTime DESC")
    fun observeAll(): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records")
    suspend fun getAllOnce(): List<RecordEntity>

    @Query("SELECT * FROM records WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): RecordEntity?

    @Query("SELECT COUNT(*) FROM records WHERE eventId = :eventId AND endTime IS NULL")
    suspend fun countRunningByEvent(eventId: String): Int

    @Query("SELECT COUNT(*) FROM records WHERE groupIdSnapshot = :groupId AND endTime IS NULL")
    suspend fun countRunningByGroup(groupId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<RecordEntity>)

    @Query("UPDATE records SET endTime = :endTime WHERE id = :id")
    suspend fun end(id: String, endTime: Long)

    @Query("UPDATE records SET noteText = :noteText WHERE id = :id")
    suspend fun updateNoteText(id: String, noteText: String)

    @Query("UPDATE records SET eventNameSnapshot = :name WHERE eventId = :eventId")
    suspend fun syncEventName(eventId: String, name: String)

    @Query("UPDATE records SET groupNameSnapshot = :name WHERE groupIdSnapshot = :groupId")
    suspend fun syncGroupName(groupId: String, name: String)

    @Query("UPDATE records SET groupColorArgbSnapshot = :color WHERE groupIdSnapshot = :groupId")
    suspend fun syncGroupColor(groupId: String, color: Int)

    @Query("UPDATE records SET groupIdSnapshot = :groupId, groupNameSnapshot = :groupName, groupColorArgbSnapshot = :groupColor WHERE eventId = :eventId")
    suspend fun syncEventGroup(eventId: String, groupId: String, groupName: String, groupColor: Int)

    @Query("DELETE FROM records WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM records")
    suspend fun deleteAll()
}

@Dao
interface NoteImageDao {
    @Query("SELECT * FROM note_images ORDER BY recordId, sortOrder, fileName")
    fun observeAll(): Flow<List<NoteImageEntity>>

    @Query("SELECT * FROM note_images WHERE recordId = :recordId ORDER BY sortOrder, fileName")
    suspend fun getByRecord(recordId: String): List<NoteImageEntity>

    @Query("SELECT * FROM note_images")
    suspend fun getAllOnce(): List<NoteImageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: NoteImageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<NoteImageEntity>)

    @Query("DELETE FROM note_images WHERE recordId = :recordId")
    suspend fun deleteByRecord(recordId: String)

    @Query("DELETE FROM note_images")
    suspend fun deleteAll()
}

@Database(
    entities = [GroupEntity::class, EventEntity::class, RecordEntity::class, NoteImageEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun groups(): GroupDao
    abstract fun events(): EventDao
    abstract fun records(): RecordDao
    abstract fun noteImages(): NoteImageDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE records ADD COLUMN noteText TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS note_images (" +
                        "id TEXT NOT NULL PRIMARY KEY, " +
                        "recordId TEXT NOT NULL, " +
                        "fileName TEXT NOT NULL, " +
                        "sortOrder INTEGER NOT NULL)"
                )
            }
        }
    }
}
