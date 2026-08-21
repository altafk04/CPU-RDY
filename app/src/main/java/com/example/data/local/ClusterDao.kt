package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ClusterConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface ClusterDao {
    @Query("SELECT * FROM cluster_configs WHERE id = 'primary_cluster' LIMIT 1")
    fun getPrimaryCluster(): Flow<ClusterConfig?>

    @Query("SELECT * FROM cluster_configs WHERE id = 'primary_cluster' LIMIT 1")
    suspend fun getPrimaryClusterDirect(): ClusterConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveClusterConfig(config: ClusterConfig)

    @Update
    suspend fun updateClusterConfig(config: ClusterConfig)
}
