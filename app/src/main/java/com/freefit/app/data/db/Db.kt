package com.freefit.app.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** yyyy-MM-dd */
    val date: String,
    val startTime: Long,
    val planName: String,
    val durationSec: Int,
    val doneSets: Int,
    val totalSets: Int,
)

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(e: SessionEntity)

    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    fun observeAll(): Flow<List<SessionEntity>>

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun delete(id: Long)
}

@Database(entities = [SessionEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "freefit.db",
                ).build().also { instance = it }
            }
    }
}
