package com.example

import com.example.data.model.AvsNodeSpec
import com.example.data.model.ClusterConfig
import com.example.data.model.ContentionSeverity
import com.example.data.model.WhatIfScenarioType
import com.example.domain.CalculatorEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorEngineTest {

    @Test
    fun testCpuReadyCalculation_twentySecondSample() {
        // 2000 ms in 20 seconds for 1 vCPU = 10%
        val result = CalculatorEngine.calculateCpuReady(
            readyMs = 2000.0,
            samplePeriodSec = 20,
            vCpuCount = 1,
            coStopMs = 0.0
        )

        assertEquals(10.0, result.totalReadyPercent, 0.01)
        assertEquals(10.0, result.perVcpuReadyPercent, 0.01)
        assertEquals(ContentionSeverity.CRITICAL, result.contentionSeverity)
    }

    @Test
    fun testCpuReadyCalculation_multiVcpu() {
        // 2000 ms in 20 seconds for 4 vCPUs = 10% total, 2.5% per vCPU
        val result = CalculatorEngine.calculateCpuReady(
            readyMs = 2000.0,
            samplePeriodSec = 20,
            vCpuCount = 4,
            coStopMs = 200.0
        )

        assertEquals(10.0, result.totalReadyPercent, 0.01)
        assertEquals(2.5, result.perVcpuReadyPercent, 0.01)
        assertEquals(ContentionSeverity.NORMAL, result.contentionSeverity)
    }

    @Test
    fun testAvsNodeSpecResolution() {
        val config = ClusterConfig(skuCode = "AV36")
        val spec = CalculatorEngine.resolveNodeSpec(config)

        assertEquals(36, spec.physicalCores)
        assertEquals(72, spec.logicalCores)
        assertEquals(576, spec.totalRamGb)
    }

    @Test
    fun testWhatIfScaleOut() {
        val config = ClusterConfig(nodeCount = 3, skuCode = "AV36")
        val vms = CalculatorEngine.generateSampleVmFleet()
        val spec = CalculatorEngine.resolveNodeSpec(config)

        val result = CalculatorEngine.simulateWhatIf(
            scenarioType = WhatIfScenarioType.CLUSTER_SCALE_OUT,
            config = config,
            vms = vms,
            nodeSpec = spec
        )

        assertTrue(result.afterAvgCpuRdy < result.beforeAvgCpuRdy)
        assertTrue(result.afterMaxHostCpu < result.beforeMaxHostCpu)
    }
}
