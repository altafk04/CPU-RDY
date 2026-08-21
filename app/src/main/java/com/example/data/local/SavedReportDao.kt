package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.SavedReport
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedReportDao {
    @Query("SELECT * FROM saved_reports ORDER BY timestamp DESC")
    fun getAllReports(): Flow<List<SavedReport>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: SavedReport): Long

    @Delete
    suspend fun deleteReport(report: SavedReport)

    @Query("DELETE FROM saved_reports WHERE id = :id")
    suspend fun deleteReportById(id: Long)

    @Query("DELETE FROM saved_reports")
    suspend fun deleteAllReports()
}
