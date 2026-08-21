package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusNormal
import com.example.ui.theme.StatusOptimal
import com.example.ui.theme.StatusWarning

enum class ContentionSeverity(
    val title: String,
    val color: Color,
    val description: String
) {
    OPTIMAL(
        title = "Optimal (< 2.5%)",
        color = StatusOptimal,
        description = "Negligible scheduling wait. ESXi physical CPU cores are immediately available."
    ),
    NORMAL(
        title = "Normal (2.5% - 5%)",
        color = StatusNormal,
        description = "Minor multi-tenant queueing. Normal performance under typical cloud production loads."
    ),
    WARNING(
        title = "Warning Contention (5% - 10%)",
        color = StatusWarning,
        description = "Noticeable CPU queue contention. User-facing latency jitter; assess vCPU allocations."
    ),
    CRITICAL(
        title = "Critical Bottleneck (> 10%)",
        color = StatusCritical,
        description = "Severe CPU starvation. High co-scheduling delays, performance degradation; immediate DRS or right-sizing needed."
    );

    companion object {
        fun fromPercent(percent: Double): ContentionSeverity {
            return when {
                percent < 2.5 -> OPTIMAL
                percent < 5.0 -> NORMAL
                percent < 10.0 -> WARNING
                else -> CRITICAL
            }
        }
    }
}

data class SampleIntervalPreset(
    val title: String,
    val seconds: Int,
    val description: String
) {
    val totalMilliseconds: Long
        get() = seconds * 1000L

    companion object {
        val PRESETS = listOf(
            SampleIntervalPreset("esxtop Real-Time", 20, "20-second default sampling window (20,000 ms)"),
            SampleIntervalPreset("vCenter 5-Min Rollup", 300, "Standard 5-minute past-day average (300,000 ms)"),
            SampleIntervalPreset("vCenter 30-Min Rollup", 1800, "30-minute past-week interval (1,800,000 ms)"),
            SampleIntervalPreset("vCenter 2-Hour Rollup", 7200, "2-hour past-month interval (7,200,000 ms)"),
            SampleIntervalPreset("vCenter 24-Hour Rollup", 86400, "1-day past-year rollup (86,400,000 ms)")
        )
    }
}

data class CpuReadyResult(
    val totalReadyMs: Double,
    val samplePeriodSec: Int,
    val vCpuCount: Int,
    val totalReadyPercent: Double,      // (ReadyMs / (SampleSec * 1000)) * 100
    val perVcpuReadyPercent: Double,    // Total % / vCPUs
    val coStopMs: Double,
    val coStopPercent: Double,          // (CstpMs / (SampleSec * 1000)) * 100
    val contentionSeverity: ContentionSeverity,
    val latencyDelayMsPerSec: Double,   // Estimated ms waited per wall-clock second
    val coSchedulingSkewFactor: Double, // Contention multiplier based on vCPU width
    val actionableInsights: List<String>
)

data class HostWorkload(
    val hostIndex: Int,
    val hostName: String,
    val cpuUsagePercent: Double,
    val ramUsagePercent: Double,
    val totalAssignedVcpus: Int,
    val totalAssignedRamGb: Int,
    val assignedVms: List<VmProfile>,
    val averageReadyPercent: Double,
    val isMaintenanceMode: Boolean = false
)

data class ClusterMetricSummary(
    val totalNodes: Int,
    val activeNodes: Int,
    val totalPhysicalCores: Int,
    val totalUsablePhysicalCores: Int,
    val totalLogicalVcpus: Int,
    val totalRamGb: Int,
    val totalUsableRamGb: Int,
    val totalAllocatedVcpus: Int,
    val totalAllocatedRamGb: Int,
    val vCpuToPhysicalCoreRatio: Double,
    val vCpuToLogicalCoreRatio: Double,
    val memoryAllocationPercent: Double,
    val clusterImbalanceStdDev: Double, // DRS Imbalance metric
    val isHaCompliant: Boolean,
    val nPlusOneFailoverLoadPercent: Double,
    val clusterHealthScore: Int // 0 - 100
)

data class DrsRecommendation(
    val id: String,
    val vmId: Long,
    val vmName: String,
    val vmVcpus: Int,
    val vmRamGb: Int,
    val sourceHostIndex: Int,
    val sourceHostName: String,
    val targetHostIndex: Int,
    val targetHostName: String,
    val priority: Int, // 1 (lowest) to 5 (highest / critical)
    val reason: String,
    val sourceCpuBefore: Double,
    val sourceCpuAfter: Double,
    val targetCpuBefore: Double,
    val targetCpuAfter: Double,
    val sourceRdyBefore: Double,
    val sourceRdyAfter: Double,
    val estimatedImbalanceImprovement: Double
)

enum class WhatIfScenarioType {
    NODE_MAINTENANCE_EVACUATION,
    CLUSTER_SCALE_OUT,
    VM_VCPU_RIGHT_SIZING
}

data class WhatIfSimulationResult(
    val scenarioType: WhatIfScenarioType,
    val scenarioTitle: String,
    val beforeImbalance: Double,
    val afterImbalance: Double,
    val beforeAvgCpuRdy: Double,
    val afterAvgCpuRdy: Double,
    val beforeMaxHostCpu: Double,
    val afterMaxHostCpu: Double,
    val summaryDescription: String,
    val hostLoadDeltas: List<Pair<String, Pair<Double, Double>>> // Host -> (Before %, After %)
)
