package com.inningsstudio.statussaver.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.inningsstudio.statussaver.data.dao.SavedStatusDao
import com.inningsstudio.statussaver.data.model.SavedStatusEntity

@Database(
    entities = [SavedStatusEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun savedStatusDao(): SavedStatusDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        // Migration from version 1 to 2: Add favoriteMarkedDate column
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE saved_statuses ADD COLUMN favoriteMarkedDate INTEGER")
            }
        }
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "status_saver_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 