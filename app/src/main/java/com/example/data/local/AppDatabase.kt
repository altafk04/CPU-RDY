package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.ClusterConfig
import com.example.data.model.SavedReport
import com.example.data.model.VmProfile

@Database(
    entities = [
        ClusterConfig::class,
        VmProfile::class,
        SavedReport::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vmDao(): VmDao
    abstract fun clusterDao(): ClusterDao
    abstract fun savedReportDao(): SavedReportDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vmware_drs_calculator.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
