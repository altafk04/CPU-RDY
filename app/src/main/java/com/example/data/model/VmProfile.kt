package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vm_profiles")
data class VmProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val vCpuCount: Int = 4,
    val ramGb: Int = 16,
    val readyTimeMs: Double = 1200.0, // CPU Ready time in milliseconds during sample
    val coStopTimeMs: Double = 150.0, // CPU Co-stop time in milliseconds
    val samplePeriodSec: Int = 20, // Sample interval in seconds (20s = esxtop, 300s = vCenter 5min)
    val assignedNodeIndex: Int = 0, // Node 0, 1, 2, etc.
    val workloadType: String = "App Server", // Web Tier, Database, App Server, Analytics, VDI
    val notes: String = ""
)
