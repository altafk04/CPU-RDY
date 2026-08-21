package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_reports")
data class SavedReport(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val reportType: String, // "CPU_RDY_CALC", "DRS_ANALYSIS", "CLUSTER_SIZING"
    val cpuReadyPercent: Double,
    val vCpuCount: Int,
    val samplePeriodSec: Int,
    val clusterNodeCount: Int,
    val nodeSku: String,
    val overcommitRatio: Double,
    val drsImbalance: Double,
    val summaryText: String
)
