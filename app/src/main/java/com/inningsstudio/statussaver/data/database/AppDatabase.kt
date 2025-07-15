package com.inningsstudio.statussaver.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.inningsstudio.statussaver.data.dao.SavedStatusDao
import com.inningsstudio.statussaver.data.model.SavedStatusEntity

@Database(
    entities = [SavedStatusEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun savedStatusDao(): SavedStatusDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "status_saver_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
} 