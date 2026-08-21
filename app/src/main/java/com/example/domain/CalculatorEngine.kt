package com.example.domain

import com.example.data.model.AvsNodeSpec
import com.example.data.model.ClusterConfig
import com.example.data.model.ClusterMetricSummary
import com.example.data.model.ContentionSeverity
import com.example.data.model.CpuReadyResult
import com.example.data.model.DrsRecommendation
import com.example.data.model.HostWorkload
import com.example.data.model.SummationBreakdown
import com.example.data.model.SummationInputMode
import com.example.data.model.VmProfile
import com.example.data.model.WhatIfScenarioType
import com.example.data.model.WhatIfSimulationResult
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

object CalculatorEngine {

    /**
     * Calculates VMware CPU RDY % and Co-Stop % with latency metrics and actionable insights
     */
    fun calculateCpuReady(
        readyMs: Double,
        samplePeriodSec: Int,
        vCpuCount: Int,
        coStopMs: Double = 0.0
    ): CpuReadyResult {
        val safeSampleSec = max(1, samplePeriodSec)
        val safeVcpus = max(1, vCpuCount)
        val samplePeriodMs = safeSampleSec * 1000.0

        val totalReadyPercent = ((readyMs / samplePeriodMs) * 100.0).coerceIn(0.0, 100.0 * safeVcpus)
        val perVcpuReadyPercent = (totalReadyPercent / safeVcpus).coerceIn(0.0, 100.0)
        val coStopPercent = ((coStopMs / samplePeriodMs) * 100.0).coerceIn(0.0, 100.0 * safeVcpus)

        // Co-scheduling penalty factor grows exponentially with vCPU width
        val coSchedulingSkewFactor = 1.0 + ((safeVcpus - 1) * 0.18)

        // Latency delayed per wall-clock second in milliseconds
        val latencyDelayMsPerSec = (perVcpuReadyPercent / 100.0) * 1000.0

        val severity = ContentionSeverity.fromPercent(perVcpuReadyPercent)

        val insights = mutableListOf<String>()

        when (severity) {
            ContentionSeverity.OPTIMAL -> {
                insights.add("CPU scheduler is highly responsive with negligible wait time (< 2.5%).")
                insights.add("Current vCPU allocation is well-balanced for the host capacity.")
            }
            ContentionSeverity.NORMAL -> {
                insights.add("CPU Ready is within normal multi-tenant virtualization operating parameters (2.5% - 5%).")
                insights.add("Monitor trends during peak business hours.")
            }
            ContentionSeverity.WARNING -> {
                insights.add("Noticeable CPU queue contention (5% - 10%). Virtual machine is waiting ${latencyDelayMsPerSec.roundToInt()}ms per second.")
                if (safeVcpus >= 8) {
                    insights.add("High vCPU count ($safeVcpus vCPUs): Reducing to ${safeVcpus / 2} vCPUs will dramatically eliminate co-scheduling latency.")
                } else {
                    insights.add("Host is experiencing high overcommit. Check DRS rebalancing or migrate noisy neighbor VMs.")
                }
            }
            ContentionSeverity.CRITICAL -> {
                insights.add("CRITICAL Bottleneck (> 10% RDY). Severe CPU starvation causing heavy application latency.")
                insights.add("ESXi CPU scheduler queue depth is saturated.")
                if (coStopPercent > 3.0) {
                    insights.add("High Co-Stop (${String.format("%.1f", coStopPercent)}% CSTP): VM is suffering from multi-vCPU co-scheduling lockups.")
                }
                insights.add("Immediate action: vMotion to colder ESXi node, scale out cluster, or right-size vCPUs.")
            }
        }

        if (coStopPercent > 3.0 && safeVcpus >= 4) {
            insights.add("Co-Stop Alert: %CSTP is ${String.format("%.1f", coStopPercent)}%. Multi-threaded co-scheduling penalty is degrading performance.")
        }

        return CpuReadyResult(
            totalReadyMs = readyMs,
            samplePeriodSec = safeSampleSec,
            vCpuCount = safeVcpus,
            totalReadyPercent = totalReadyPercent,
            perVcpuReadyPercent = perVcpuReadyPercent,
            coStopMs = coStopMs,
            coStopPercent = coStopPercent,
            contentionSeverity = severity,
            latencyDelayMsPerSec = latencyDelayMsPerSec,
            coSchedulingSkewFactor = coSchedulingSkewFactor,
            actionableInsights = insights
        )
    }

    /**
     * Calculates comprehensive Summation vs Per-vCPU conversion metrics, steps, and mathematical breakdown
     */
    fun calculateSummationBreakdown(
        inputMode: SummationInputMode,
        inputValue: Double,
        samplePeriodSec: Int,
        vCpuCount: Int
    ): SummationBreakdown {
        val safeSampleSec = max(1, samplePeriodSec)
        val safeVcpus = max(1, vCpuCount)
        val samplePeriodMs = safeSampleSec * 1000.0

        val summationMs: Double
        val avgPerVcpuMs: Double
        val perVcpuPercent: Double
        val totalPercent: Double
        val formulaExplanation: String
        val steps = mutableListOf<String>()

        when (inputMode) {
            SummationInputMode.SUMMATION_MS -> {
                summationMs = max(0.0, inputValue)
                avgPerVcpuMs = summationMs / safeVcpus
                totalPercent = (summationMs / samplePeriodMs) * 100.0
                perVcpuPercent = totalPercent / safeVcpus
                formulaExplanation = "%RDY = (Summation_ms / (Sample_sec * 1000 * num_vCPUs)) * 100"

                steps.add("1. Sample Interval: $safeSampleSec sec = ${samplePeriodMs.toInt()} ms total wall-clock per core.")
                steps.add("2. vCenter Summation Counter: ${String.format("%.1f", summationMs)} ms accumulated across all $safeVcpus vCPUs.")
                steps.add("3. Average Ready Time per vCPU = ${String.format("%.1f", summationMs)} ms ÷ $safeVcpus = ${String.format("%.1f", avgPerVcpuMs)} ms.")
                steps.add("4. Normalized %RDY per vCPU = (${String.format("%.1f", summationMs)} ÷ (${samplePeriodMs.toInt()} × $safeVcpus)) × 100 = ${String.format("%.2f", perVcpuPercent)}%.")
                steps.add("5. Summed Contention % across VM = ${String.format("%.2f", totalPercent)}%.")
            }
            SummationInputMode.AVG_PER_VCPU_MS -> {
                avgPerVcpuMs = max(0.0, inputValue)
                summationMs = avgPerVcpuMs * safeVcpus
                perVcpuPercent = (avgPerVcpuMs / samplePeriodMs) * 100.0
                totalPercent = perVcpuPercent * safeVcpus
                formulaExplanation = "Summation_ms = (Avg_Per_vCPU_ms * num_vCPUs); %RDY = (Avg_Per_vCPU_ms / (Sample_sec * 1000)) * 100"

                steps.add("1. Average per-vCPU wait: ${String.format("%.1f", avgPerVcpuMs)} ms on $safeVcpus vCPUs.")
                steps.add("2. Converted Total Summation = ${String.format("%.1f", avgPerVcpuMs)} ms × $safeVcpus = ${String.format("%.1f", summationMs)} ms.")
                steps.add("3. Sample Window: $safeSampleSec sec (${samplePeriodMs.toInt()} ms).")
                steps.add("4. Calculated %RDY = (${String.format("%.1f", avgPerVcpuMs)} ÷ ${samplePeriodMs.toInt()}) × 100 = ${String.format("%.2f", perVcpuPercent)}%.")
            }
            SummationInputMode.PERCENT_RDY_TARGET -> {
                perVcpuPercent = max(0.0, inputValue)
                totalPercent = perVcpuPercent * safeVcpus
                summationMs = (perVcpuPercent / 100.0) * samplePeriodMs * safeVcpus
                avgPerVcpuMs = summationMs / safeVcpus
                formulaExplanation = "Summation_ms = (%RDY / 100) * (Sample_sec * 1000) * num_vCPUs"

                steps.add("1. Target Per-vCPU %RDY: ${String.format("%.2f", perVcpuPercent)}% on $safeVcpus vCPUs.")
                steps.add("2. Total Sample Capacity: $safeSampleSec sec × 1000 ms × $safeVcpus vCPUs = ${(samplePeriodMs * safeVcpus).toInt()} ms total.")
                steps.add("3. Required Summation ms = (${String.format("%.2f", perVcpuPercent)} / 100) × ${(samplePeriodMs * safeVcpus).toInt()} ms = ${String.format("%.1f", summationMs)} ms.")
                steps.add("4. Equivalent Avg ms per vCPU = ${String.format("%.1f", avgPerVcpuMs)} ms.")
            }
        }

        return SummationBreakdown(
            inputMode = inputMode,
            inputValue = inputValue,
            summationMs = summationMs,
            avgPerVcpuMs = avgPerVcpuMs,
            totalReadyPercent = totalPercent,
            perVcpuReadyPercent = perVcpuPercent,
            samplePeriodSec = safeSampleSec,
            samplePeriodMs = samplePeriodMs,
            vCpuCount = safeVcpus,
            formulaExplanation = formulaExplanation,
            mathSteps = steps
        )
    }

    /**
     * Resolves the active node spec from cluster configuration
     */
    fun resolveNodeSpec(config: ClusterConfig): AvsNodeSpec {
        val preset = AvsNodeSpec.ALL_PRESETS.find { it.skuCode.equals(config.skuCode, ignoreCase = true) }
        return preset ?: AvsNodeSpec.createCustom(
            name = "Custom Node Spec",
            sockets = config.customSockets,
            coresPerSocket = config.customCoresPerSocket,
            totalRamGb = config.customTotalRamGb,
            baseClockGhz = config.customClockGhz,
            reservedCores = config.customReservedCores,
            reservedRamGb = config.customReservedRamGb
        )
    }

    /**
     * Calculates Cluster-wide capacity, overcommit ratios, DRS imbalance metric, and health score
     */
    fun calculateClusterSummary(
        config: ClusterConfig,
        vms: List<VmProfile>,
        nodeSpec: AvsNodeSpec
    ): Pair<ClusterMetricSummary, List<HostWorkload>> {
        val totalNodes = max(1, config.nodeCount)
        val totalPhysicalCores = totalNodes * nodeSpec.physicalCores
        val totalUsablePhysicalCores = totalNodes * nodeSpec.usablePhysicalCores
        val totalLogicalVcpus = totalNodes * nodeSpec.logicalCores
        val totalRamGb = totalNodes * nodeSpec.totalRamGb
        val totalUsableRamGb = totalNodes * nodeSpec.usableRamGb

        val totalAllocatedVcpus = vms.sumOf { it.vCpuCount }
        val totalAllocatedRamGb = vms.sumOf { it.ramGb }

        val vCpuToPhysicalRatio = if (totalUsablePhysicalCores > 0) {
            totalAllocatedVcpus.toDouble() / totalUsablePhysicalCores.toDouble()
        } else 1.0

        val vCpuToLogicalRatio = if (totalLogicalVcpus > 0) {
            totalAllocatedVcpus.toDouble() / totalLogicalVcpus.toDouble()
        } else 1.0

        val memoryAllocationPercent = if (totalUsableRamGb > 0) {
            (totalAllocatedRamGb.toDouble() / totalUsableRamGb.toDouble() * 100.0).coerceIn(0.0, 200.0)
        } else 0.0

        // Build per-host workloads
        val hostWorkloads = mutableListOf<HostWorkload>()
        for (i in 0 until totalNodes) {
            val hostVms = vms.filter { it.assignedNodeIndex % totalNodes == i }
            val hostVcpus = hostVms.sumOf { it.vCpuCount }
            val hostRam = hostVms.sumOf { it.ramGb }

            val hostUsableCores = nodeSpec.usablePhysicalCores
            val hostUsableRam = nodeSpec.usableRamGb

            // Estimate CPU utilization percentage based on VM vCPU allocation and active loads
            val baseLoad = if (hostUsableCores > 0) (hostVcpus.toDouble() / (hostUsableCores * 1.5)) * 60.0 else 0.0
            val cpuLoad = baseLoad.coerceIn(12.0, 98.0)

            val ramLoad = if (hostUsableRam > 0) (hostRam.toDouble() / hostUsableRam) * 100.0 else 0.0

            // Calculate host average Ready %
            val avgRdy = if (hostVms.isNotEmpty()) {
                hostVms.map { vm ->
                    (vm.readyTimeMs / (vm.samplePeriodSec * 1000.0)) * 100.0 / max(1, vm.vCpuCount)
                }.average()
            } else {
                // Background idle ready
                0.4
            }

            hostWorkloads.add(
                HostWorkload(
                    hostIndex = i,
                    hostName = "AVS-Node-0${i + 1}",
                    cpuUsagePercent = cpuLoad,
                    ramUsagePercent = ramLoad.coerceIn(5.0, 100.0),
                    totalAssignedVcpus = hostVcpus,
                    totalAssignedRamGb = hostRam,
                    assignedVms = hostVms,
                    averageReadyPercent = avgRdy
                )
            )
        }

        // Calculate DRS Cluster Imbalance (Standard Deviation of CPU Load)
        val meanCpu = hostWorkloads.map { it.cpuUsagePercent }.average()
        val variance = hostWorkloads.map { (it.cpuUsagePercent - meanCpu).pow(2) }.average()
        val stdDev = sqrt(variance)

        // N+1 Failover check: If 1 node fails, remaining nodes (N-1) absorb load
        val survivingNodes = max(1, totalNodes - config.haFailoverNodesReserved)
        val failoverSurvivingPhysicalCores = survivingNodes * nodeSpec.usablePhysicalCores
        val failoverRatio = if (failoverSurvivingPhysicalCores > 0) {
            totalAllocatedVcpus.toDouble() / failoverSurvivingPhysicalCores.toDouble()
        } else 1.0
        val isHaCompliant = failoverRatio <= 3.5 && (memoryAllocationPercent * (totalNodes.toDouble() / survivingNodes)) <= 100.0
        val nPlusOneLoadPercent = min(100.0, (meanCpu * totalNodes) / survivingNodes)

        // Calculate 0-100 Cluster Health Score
        var health = 100
        if (vCpuToPhysicalRatio > 3.0) health -= 20
        else if (vCpuToPhysicalRatio > 2.0) health -= 10

        if (stdDev > 15.0) health -= 20
        else if (stdDev > 8.0) health -= 10

        if (!isHaCompliant) health -= 25
        if (memoryAllocationPercent > 85.0) health -= 15

        val finalHealthScore = health.coerceIn(15, 100)

        val summary = ClusterMetricSummary(
            totalNodes = totalNodes,
            activeNodes = totalNodes,
            totalPhysicalCores = totalPhysicalCores,
            totalUsablePhysicalCores = totalUsablePhysicalCores,
            totalLogicalVcpus = totalLogicalVcpus,
            totalRamGb = totalRamGb,
            totalUsableRamGb = totalUsableRamGb,
            totalAllocatedVcpus = totalAllocatedVcpus,
            totalAllocatedRamGb = totalAllocatedRamGb,
            vCpuToPhysicalCoreRatio = vCpuToPhysicalRatio,
            vCpuToLogicalCoreRatio = vCpuToLogicalRatio,
            memoryAllocationPercent = memoryAllocationPercent,
            clusterImbalanceStdDev = stdDev,
            isHaCompliant = isHaCompliant,
            nPlusOneFailoverLoadPercent = nPlusOneLoadPercent,
            clusterHealthScore = finalHealthScore
        )

        return Pair(summary, hostWorkloads)
    }

    /**
     * Evaluates DRS Migration Recommendations based on threshold and host load variance
     */
    fun evaluateDrsRecommendations(
        hostWorkloads: List<HostWorkload>,
        drsThreshold: Int, // 1 (Conservative) to 5 (Aggressive)
        nodeSpec: AvsNodeSpec
    ): List<DrsRecommendation> {
        if (hostWorkloads.size < 2) return emptyList()

        val recommendations = mutableListOf<DrsRecommendation>()
        val sortedByCpu = hostWorkloads.sortedByDescending { it.cpuUsagePercent }
        val avgClusterCpu = hostWorkloads.map { it.cpuUsagePercent }.average()

        val triggerThresholdDiff = when (drsThreshold) {
            1 -> 35.0 // Only massive imbalances
            2 -> 25.0
            3 -> 15.0 // Standard DRS default
            4 -> 10.0
            5 -> 5.0  // Highly aggressive
            else -> 15.0
        }

        val hottestHost = sortedByCpu.firstOrNull() ?: return emptyList()
        val coldestHost = sortedByCpu.lastOrNull() ?: return emptyList()

        if (hottestHost.cpuUsagePercent - coldestHost.cpuUsagePercent >= triggerThresholdDiff) {
            // Find best candidate VM to migrate from hot to cold
            val candidateVms = hottestHost.assignedVms.sortedByDescending { it.vCpuCount }
            for (vm in candidateVms) {
                // Calculate impact of moving this VM
                val vmCoreWeight = (vm.vCpuCount.toDouble() / max(1, nodeSpec.usablePhysicalCores)) * 25.0
                val sourceCpuAfter = (hottestHost.cpuUsagePercent - vmCoreWeight).coerceAtLeast(15.0)
                val targetCpuAfter = (coldestHost.cpuUsagePercent + vmCoreWeight).coerceAtMost(95.0)

                // Projected RDY % drop
                val sourceRdyBefore = hottestHost.averageReadyPercent
                val sourceRdyAfter = (sourceRdyBefore * 0.45).coerceAtLeast(0.8)

                val priority = when {
                    hottestHost.cpuUsagePercent > 85.0 || sourceRdyBefore > 6.0 -> 5
                    hottestHost.cpuUsagePercent > 75.0 -> 4
                    hottestHost.cpuUsagePercent - coldestHost.cpuUsagePercent > 25.0 -> 3
                    hottestHost.cpuUsagePercent - coldestHost.cpuUsagePercent > 15.0 -> 2
                    else -> 1
                }

                if (priority >= (6 - drsThreshold).coerceAtLeast(1)) {
                    val imbalanceReduction = (hottestHost.cpuUsagePercent - coldestHost.cpuUsagePercent) - (max(sourceCpuAfter, targetCpuAfter) - min(sourceCpuAfter, targetCpuAfter))

                    recommendations.add(
                        DrsRecommendation(
                            id = "DRS-REC-${vm.id}-${System.currentTimeMillis() % 10000}",
                            vmId = vm.id,
                            vmName = vm.name,
                            vmVcpus = vm.vCpuCount,
                            vmRamGb = vm.ramGb,
                            sourceHostIndex = hottestHost.hostIndex,
                            sourceHostName = hottestHost.hostName,
                            targetHostIndex = coldestHost.hostIndex,
                            targetHostName = coldestHost.hostName,
                            priority = priority,
                            reason = "Rebalance CPU Contention: ${hottestHost.hostName} is at ${hottestHost.cpuUsagePercent.roundToInt()}% CPU with ${String.format("%.1f", sourceRdyBefore)}% avg RDY. Target ${coldestHost.hostName} is cold at ${coldestHost.cpuUsagePercent.roundToInt()}% CPU.",
                            sourceCpuBefore = hottestHost.cpuUsagePercent,
                            sourceCpuAfter = sourceCpuAfter,
                            targetCpuBefore = coldestHost.cpuUsagePercent,
                            targetCpuAfter = targetCpuAfter,
                            sourceRdyBefore = sourceRdyBefore,
                            sourceRdyAfter = sourceRdyAfter,
                            estimatedImbalanceImprovement = max(0.0, imbalanceReduction)
                        )
                    )
                }

                if (recommendations.size >= 4) break
            }
        }

        return recommendations
    }

    /**
     * Executes What-If scenario simulations (Host Maintenance Mode Evacuation, Cluster Scale, Right-Sizing)
     */
    fun simulateWhatIf(
        scenarioType: WhatIfScenarioType,
        config: ClusterConfig,
        vms: List<VmProfile>,
        nodeSpec: AvsNodeSpec,
        targetHostIndex: Int = 0
    ): WhatIfSimulationResult {
        val (initialSummary, initialHosts) = calculateClusterSummary(config, vms, nodeSpec)

        return when (scenarioType) {
            WhatIfScenarioType.NODE_MAINTENANCE_EVACUATION -> {
                val hostToEvacuate = initialHosts.getOrNull(targetHostIndex) ?: initialHosts.first()
                val survivingHosts = initialHosts.filter { it.hostIndex != hostToEvacuate.hostIndex }
                val vmsToEvacuate = hostToEvacuate.assignedVms

                val newHostDeltas = mutableListOf<Pair<String, Pair<Double, Double>>>()

                // Distribute evacuated VMs across surviving hosts
                val simulatedLoads = survivingHosts.map { it.cpuUsagePercent }.toMutableList()
                val loadPerVm = if (survivingHosts.isNotEmpty()) {
                    (vmsToEvacuate.sumOf { it.vCpuCount }.toDouble() / (nodeSpec.usablePhysicalCores * survivingHosts.size)) * 30.0
                } else 0.0

                survivingHosts.forEachIndexed { idx, host ->
                    val afterLoad = min(98.0, host.cpuUsagePercent + loadPerVm)
                    simulatedLoads[idx] = afterLoad
                    newHostDeltas.add(Pair(host.hostName, Pair(host.cpuUsagePercent, afterLoad)))
                }
                newHostDeltas.add(Pair("${hostToEvacuate.hostName} (Maintenance)", Pair(hostToEvacuate.cpuUsagePercent, 0.0)))

                val newMean = simulatedLoads.average()
                val newVariance = simulatedLoads.map { (it - newMean).pow(2) }.average()
                val newStdDev = sqrt(newVariance)
                val newMaxCpu = simulatedLoads.maxOrNull() ?: 0.0
                val afterAvgRdy = (initialSummary.vCpuToPhysicalCoreRatio * 1.8).coerceAtMost(18.0)

                WhatIfSimulationResult(
                    scenarioType = scenarioType,
                    scenarioTitle = "Maintenance Evacuation: ${hostToEvacuate.hostName}",
                    beforeImbalance = initialSummary.clusterImbalanceStdDev,
                    afterImbalance = newStdDev,
                    beforeAvgCpuRdy = initialHosts.map { it.averageReadyPercent }.average(),
                    afterAvgCpuRdy = afterAvgRdy,
                    beforeMaxHostCpu = initialHosts.maxOfOrNull { it.cpuUsagePercent } ?: 0.0,
                    afterMaxHostCpu = newMaxCpu,
                    summaryDescription = "Evacuating ${vmsToEvacuate.size} VMs (${vmsToEvacuate.sumOf { it.vCpuCount }} vCPUs) from ${hostToEvacuate.hostName} into ${survivingHosts.size} surviving nodes increases average host load from ${initialHosts.map { it.cpuUsagePercent }.average().roundToInt()}% to ${newMean.roundToInt()}%.",
                    hostLoadDeltas = newHostDeltas
                )
            }

            WhatIfScenarioType.CLUSTER_SCALE_OUT -> {
                val scaledNodeCount = config.nodeCount + 2
                val scaledConfig = config.copy(nodeCount = scaledNodeCount)
                val (scaledSummary, scaledHosts) = calculateClusterSummary(scaledConfig, vms, nodeSpec)

                val newHostDeltas = initialHosts.map { host ->
                    val afterLoad = (host.cpuUsagePercent * (config.nodeCount.toDouble() / scaledNodeCount)).coerceAtLeast(15.0)
                    Pair(host.hostName, Pair(host.cpuUsagePercent, afterLoad))
                }.toMutableList()

                newHostDeltas.add(Pair("AVS-Node-0${config.nodeCount + 1} (New)", Pair(0.0, scaledSummary.vCpuToPhysicalCoreRatio * 18.0)))
                newHostDeltas.add(Pair("AVS-Node-0${config.nodeCount + 2} (New)", Pair(0.0, scaledSummary.vCpuToPhysicalCoreRatio * 18.0)))

                WhatIfSimulationResult(
                    scenarioType = scenarioType,
                    scenarioTitle = "Scale-Out Cluster (+2 AVS Nodes)",
                    beforeImbalance = initialSummary.clusterImbalanceStdDev,
                    afterImbalance = scaledSummary.clusterImbalanceStdDev * 0.5,
                    beforeAvgCpuRdy = initialHosts.map { it.averageReadyPercent }.average(),
                    afterAvgCpuRdy = (initialHosts.map { it.averageReadyPercent }.average() * 0.35).coerceAtLeast(0.6),
                    beforeMaxHostCpu = initialHosts.maxOfOrNull { it.cpuUsagePercent } ?: 0.0,
                    afterMaxHostCpu = (initialHosts.maxOfOrNull { it.cpuUsagePercent } ?: 0.0) * 0.65,
                    summaryDescription = "Adding 2 AVS ${nodeSpec.skuCode} nodes expands capacity by ${nodeSpec.usablePhysicalCores * 2} physical cores and ${nodeSpec.usableRamGb * 2} GB RAM. Overcommit drops to ${String.format("%.2f", scaledSummary.vCpuToPhysicalCoreRatio)}:1.",
                    hostLoadDeltas = newHostDeltas
                )
            }

            WhatIfScenarioType.VM_VCPU_RIGHT_SIZING -> {
                // Simulate right-sizing wide VMs (>= 8 vCPUs down to 4 vCPUs)
                val rightSizedVms = vms.map { vm ->
                    if (vm.vCpuCount >= 8) vm.copy(vCpuCount = vm.vCpuCount / 2, readyTimeMs = vm.readyTimeMs * 0.3, coStopTimeMs = vm.coStopTimeMs * 0.15)
                    else vm
                }
                val (rightSizedSummary, rightSizedHosts) = calculateClusterSummary(config, rightSizedVms, nodeSpec)

                val newHostDeltas = initialHosts.mapIndexed { idx, host ->
                    val afterHost = rightSizedHosts.getOrNull(idx)
                    Pair(host.hostName, Pair(host.cpuUsagePercent, afterHost?.cpuUsagePercent ?: host.cpuUsagePercent))
                }

                val beforeAvgRdy = initialHosts.map { it.averageReadyPercent }.average()
                val afterAvgRdy = rightSizedHosts.map { it.averageReadyPercent }.average()

                WhatIfSimulationResult(
                    scenarioType = scenarioType,
                    scenarioTitle = "Right-Size Wide Multi-vCPU VMs",
                    beforeImbalance = initialSummary.clusterImbalanceStdDev,
                    afterImbalance = rightSizedSummary.clusterImbalanceStdDev,
                    beforeAvgCpuRdy = beforeAvgRdy,
                    afterAvgCpuRdy = afterAvgRdy,
                    beforeMaxHostCpu = initialHosts.maxOfOrNull { it.cpuUsagePercent } ?: 0.0,
                    afterMaxHostCpu = rightSizedHosts.maxOfOrNull { it.cpuUsagePercent } ?: 0.0,
                    summaryDescription = "Downsizing bloated 8+ vCPU VMs eliminates ESXi scheduler co-stop skew. Total cluster vCPUs reduced from ${initialSummary.totalAllocatedVcpus} to ${rightSizedSummary.totalAllocatedVcpus}, slashing CPU Ready by ${String.format("%.1f", (beforeAvgRdy - afterAvgRdy) / max(0.1, beforeAvgRdy) * 100.0)}%.",
                    hostLoadDeltas = newHostDeltas
                )
            }
        }
    }

    /**
     * Generates standard sample enterprise workloads for initial fleet population
     */
    fun generateSampleVmFleet(): List<VmProfile> {
        return listOf(
            VmProfile(
                name = "db-prod-sql-01",
                vCpuCount = 8,
                ramGb = 64,
                readyTimeMs = 1850.0,
                coStopTimeMs = 380.0,
                samplePeriodSec = 20,
                assignedNodeIndex = 0,
                workloadType = "Database",
                notes = "Core production SQL database engine with high transactional query load."
            ),
            VmProfile(
                name = "app-api-gateway-01",
                vCpuCount = 4,
                ramGb = 16,
                readyTimeMs = 920.0,
                coStopTimeMs = 45.0,
                samplePeriodSec = 20,
                assignedNodeIndex = 0,
                workloadType = "App Server",
                notes = "Reverse proxy & REST API gateway."
            ),
            VmProfile(
                name = "analytics-worker-01",
                vCpuCount = 12,
                ramGb = 96,
                readyTimeMs = 2600.0,
                coStopTimeMs = 920.0,
                samplePeriodSec = 20,
                assignedNodeIndex = 1,
                workloadType = "Analytics",
                notes = "Large memory spark compute worker node with wide vCPU allocation."
            ),
            VmProfile(
                name = "web-frontend-cluster-01",
                vCpuCount = 2,
                ramGb = 8,
                readyTimeMs = 320.0,
                coStopTimeMs = 10.0,
                samplePeriodSec = 20,
                assignedNodeIndex = 1,
                workloadType = "Web Tier",
                notes = "Nginx frontend web server."
            ),
            VmProfile(
                name = "vdi-desktop-pool-01",
                vCpuCount = 4,
                ramGb = 16,
                readyTimeMs = 1100.0,
                coStopTimeMs = 120.0,
                samplePeriodSec = 20,
                assignedNodeIndex = 2,
                workloadType = "VDI",
                notes = "Virtual desktop pool instance."
            ),
            VmProfile(
                name = "cache-redis-cluster-01",
                vCpuCount = 4,
                ramGb = 32,
                readyTimeMs = 480.0,
                coStopTimeMs = 25.0,
                samplePeriodSec = 20,
                assignedNodeIndex = 2,
                workloadType = "Database",
                notes = "Distributed in-memory caching cluster."
            ),
            VmProfile(
                name = "auth-identity-service",
                vCpuCount = 2,
                ramGb = 8,
                readyTimeMs = 280.0,
                coStopTimeMs = 8.0,
                samplePeriodSec = 20,
                assignedNodeIndex = 3,
                workloadType = "App Server",
                notes = "OAuth2 / SAML authentication identity broker."
            ),
            VmProfile(
                name = "logging-elk-indexer",
                vCpuCount = 6,
                ramGb = 32,
                readyTimeMs = 1450.0,
                coStopTimeMs = 210.0,
                samplePeriodSec = 20,
                assignedNodeIndex = 3,
                workloadType = "Analytics",
                notes = "Elasticsearch indexing and log aggregation node."
            )
        )
    }

    /**
     * Parses raw CSV, TSV, esxtop, RVTools, or PowerCLI metrics text and extracts Virtual Machines
     */
    fun parseLivePerformanceData(text: String, defaultNodeCount: Int = 4): List<VmProfile> {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val parsedVms = mutableListOf<VmProfile>()
        val safeNodes = max(1, defaultNodeCount)

        var vmIndex = 1
        for (line in lines) {
            // Skip comments or table headers
            if (line.startsWith("#") || line.startsWith("//") || line.lowercase().startsWith("vm name") || line.lowercase().startsWith("name,") || line.lowercase().startsWith("id,")) {
                continue
            }

            // Split by comma, tab, or semicolon
            val tokens = if (line.contains(",")) {
                line.split(",").map { it.trim() }
            } else if (line.contains("\t")) {
                line.split("\t").map { it.trim() }
            } else if (line.contains(";")) {
                line.split(";").map { it.trim() }
            } else {
                line.split(Regex("\\s{2,}")).map { it.trim() }
            }

            if (tokens.isEmpty()) continue

            var name = "imported-vm-$vmIndex"
            var vcpus = 4
            var ramGb = 16
            var readyMs = 800.0
            var cstpMs = 40.0
            var sampleSec = 20
            var workload = "App Server"
            var notes = "Imported from live telemetry"

            if (tokens.size >= 1 && tokens[0].isNotBlank()) {
                name = tokens[0]
            }

            // Try to extract vCPUs (usually 2nd column or numeric token)
            if (tokens.size >= 2) {
                tokens[1].toIntOrNull()?.let { if (it > 0) vcpus = it }
            }

            // RAM GB (3rd column)
            if (tokens.size >= 3) {
                tokens[2].toIntOrNull()?.let { if (it > 0) ramGb = it }
            }

            // Ready Time ms or %RDY
            if (tokens.size >= 4) {
                val rdyVal = tokens[3].toDoubleOrNull()
                if (rdyVal != null) {
                    if (rdyVal < 100.0 && tokens[3].contains(".")) {
                        // Might be in percentage form, convert to ms for 20s window
                        readyMs = (rdyVal / 100.0) * (20 * 1000.0) * vcpus
                    } else {
                        readyMs = rdyVal
                    }
                }
            }

            // Co-Stop Time ms
            if (tokens.size >= 5) {
                tokens[4].toDoubleOrNull()?.let { cstpMs = it }
            }

            // Workload or tags
            if (tokens.size >= 6 && tokens[5].isNotBlank()) {
                workload = tokens[5]
            }

            if (tokens.size >= 7 && tokens[6].isNotBlank()) {
                notes = tokens[6]
            }

            parsedVms.add(
                VmProfile(
                    name = name,
                    vCpuCount = vcpus,
                    ramGb = ramGb,
                    readyTimeMs = readyMs,
                    coStopTimeMs = cstpMs,
                    samplePeriodSec = sampleSec,
                    assignedNodeIndex = (vmIndex - 1) % safeNodes,
                    workloadType = workload,
                    notes = notes
                )
            )
            vmIndex++
        }

        return parsedVms
    }

    /**
     * Generates real-world live environment presets for fast one-tap import
     */
    fun getEnvironmentPresets(): List<Pair<String, List<VmProfile>>> {
        return listOf(
            "Enterprise SQL Contention (High %RDY)" to listOf(
                VmProfile(name = "prd-sql-primary-01", vCpuCount = 16, ramGb = 128, readyTimeMs = 3800.0, coStopTimeMs = 1240.0, samplePeriodSec = 20, assignedNodeIndex = 0, workloadType = "Database", notes = "Heavy transactional OLTP SQL engine"),
                VmProfile(name = "prd-sql-secondary-01", vCpuCount = 16, ramGb = 128, readyTimeMs = 3200.0, coStopTimeMs = 980.0, samplePeriodSec = 20, assignedNodeIndex = 0, workloadType = "Database", notes = "Always-On sync replica"),
                VmProfile(name = "prd-reporting-ssrs-01", vCpuCount = 8, ramGb = 64, readyTimeMs = 2100.0, coStopTimeMs = 450.0, samplePeriodSec = 20, assignedNodeIndex = 1, workloadType = "Analytics", notes = "ETL pipeline batch processor"),
                VmProfile(name = "prd-api-gateway-01", vCpuCount = 4, ramGb = 16, readyTimeMs = 600.0, coStopTimeMs = 30.0, samplePeriodSec = 20, assignedNodeIndex = 1, workloadType = "App Server", notes = "Edge ingress reverse proxy"),
                VmProfile(name = "prd-api-gateway-02", vCpuCount = 4, ramGb = 16, readyTimeMs = 550.0, coStopTimeMs = 25.0, samplePeriodSec = 20, assignedNodeIndex = 2, workloadType = "App Server", notes = "Edge ingress reverse proxy"),
                VmProfile(name = "prd-redis-cache-01", vCpuCount = 4, ramGb = 32, readyTimeMs = 400.0, coStopTimeMs = 15.0, samplePeriodSec = 20, assignedNodeIndex = 2, workloadType = "Database", notes = "Session state memory cache")
            ),
            "AVS AV36 Balanced Hybrid Pool" to listOf(
                VmProfile(name = "avs-web-tier-01", vCpuCount = 2, ramGb = 8, readyTimeMs = 180.0, coStopTimeMs = 5.0, samplePeriodSec = 20, assignedNodeIndex = 0, workloadType = "Web Tier", notes = "Stateless IIS web pool"),
                VmProfile(name = "avs-web-tier-02", vCpuCount = 2, ramGb = 8, readyTimeMs = 190.0, coStopTimeMs = 6.0, samplePeriodSec = 20, assignedNodeIndex = 1, workloadType = "Web Tier", notes = "Stateless IIS web pool"),
                VmProfile(name = "avs-app-microservice-01", vCpuCount = 4, ramGb = 16, readyTimeMs = 350.0, coStopTimeMs = 18.0, samplePeriodSec = 20, assignedNodeIndex = 2, workloadType = "App Server", notes = "Spring Boot container host"),
                VmProfile(name = "avs-app-microservice-02", vCpuCount = 4, ramGb = 16, readyTimeMs = 320.0, coStopTimeMs = 15.0, samplePeriodSec = 20, assignedNodeIndex = 3, workloadType = "App Server", notes = "Spring Boot container host"),
                VmProfile(name = "avs-db-postgres-01", vCpuCount = 8, ramGb = 64, readyTimeMs = 820.0, coStopTimeMs = 75.0, samplePeriodSec = 20, assignedNodeIndex = 0, workloadType = "Database", notes = "Primary PostgreSQL database cluster"),
                VmProfile(name = "avs-msg-kafka-broker-01", vCpuCount = 6, ramGb = 32, readyTimeMs = 620.0, coStopTimeMs = 45.0, samplePeriodSec = 20, assignedNodeIndex = 1, workloadType = "Messaging", notes = "Kafka streaming broker")
            ),
            "VDI Horizon Desktop Pool (Burst Contention)" to listOf(
                VmProfile(name = "vdi-desk-finance-01", vCpuCount = 4, ramGb = 16, readyTimeMs = 1650.0, coStopTimeMs = 210.0, samplePeriodSec = 20, assignedNodeIndex = 0, workloadType = "VDI", notes = "Virtual desktop: Excel power user"),
                VmProfile(name = "vdi-desk-finance-02", vCpuCount = 4, ramGb = 16, readyTimeMs = 1720.0, coStopTimeMs = 230.0, samplePeriodSec = 20, assignedNodeIndex = 0, workloadType = "VDI", notes = "Virtual desktop: Financial modeling"),
                VmProfile(name = "vdi-desk-cad-eng-01", vCpuCount = 8, ramGb = 32, readyTimeMs = 2850.0, coStopTimeMs = 690.0, samplePeriodSec = 20, assignedNodeIndex = 1, workloadType = "VDI", notes = "Virtual workstation: AutoCAD rendering"),
                VmProfile(name = "vdi-desk-general-01", vCpuCount = 2, ramGb = 8, readyTimeMs = 450.0, coStopTimeMs = 20.0, samplePeriodSec = 20, assignedNodeIndex = 1, workloadType = "VDI", notes = "Standard task worker desktop"),
                VmProfile(name = "vdi-desk-general-02", vCpuCount = 2, ramGb = 8, readyTimeMs = 490.0, coStopTimeMs = 22.0, samplePeriodSec = 20, assignedNodeIndex = 2, workloadType = "VDI", notes = "Standard task worker desktop"),
                VmProfile(name = "vdi-con-manager-01", vCpuCount = 4, ramGb = 16, readyTimeMs = 510.0, coStopTimeMs = 35.0, samplePeriodSec = 20, assignedNodeIndex = 2, workloadType = "App Server", notes = "Horizon Connection Server")
            )
        )
    }
}
