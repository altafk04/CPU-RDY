package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cluster_configs")
data class ClusterConfig(
    @PrimaryKey val id: String = "primary_cluster",
    val clusterName: String = "AVS-SDDC-Cluster-01",
    val skuCode: String = "AV36",
    val nodeCount: Int = 4, // 3 to 16 nodes in AVS standard
    val haFailoverNodesReserved: Int = 1, // N+1 failover
    // Custom specs if skuCode == "CUSTOM"
    val customSockets: Int = 2,
    val customCoresPerSocket: Int = 18,
    val customTotalRamGb: Int = 576,
    val customClockGhz: Double = 2.4,
    val customReservedCores: Int = 2,
    val customReservedRamGb: Int = 36,
    // Target Overcommit Guideline
    val targetVcpuToPcoreRatio: Double = 2.5,
    val drsMigrationThreshold: Int = 3 // 1 (Conservative) to 5 (Aggressive)
)
