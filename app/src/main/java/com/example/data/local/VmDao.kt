package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.VmProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface VmDao {
    @Query("SELECT * FROM vm_profiles ORDER BY id ASC")
    fun getAllVms(): Flow<List<VmProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVm(vm: VmProfile): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVms(vms: List<VmProfile>)

    @Update
    suspend fun updateVm(vm: VmProfile)

    @Delete
    suspend fun deleteVm(vm: VmProfile)

    @Query("DELETE FROM vm_profiles WHERE id = :id")
    suspend fun deleteVmById(id: Long)

    @Query("DELETE FROM vm_profiles")
    suspend fun deleteAllVms()

    @Query("SELECT COUNT(*) FROM vm_profiles")
    suspend fun getVmCount(): Int
}
