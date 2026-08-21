package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ContentionSeverity
import com.example.data.model.SummationInputMode
import com.example.domain.CalculatorEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("CPU RDY & DRS", appName)
  }

  @Test
  fun `test calculateCpuReady with custom vcpu count`() {
    // 1600ms ready, 20s sample, 8 vCPUs -> 8% total, 1% per vcpu (Optimal)
    val result = CalculatorEngine.calculateCpuReady(
      readyMs = 1600.0,
      samplePeriodSec = 20,
      vCpuCount = 8,
      coStopMs = 200.0
    )
    assertEquals(1.0, result.perVcpuReadyPercent, 0.01)
    assertEquals(8.0, result.totalReadyPercent, 0.01)
    assertEquals(ContentionSeverity.OPTIMAL, result.contentionSeverity)
  }

  @Test
  fun `test calculateSummationBreakdown conversions`() {
    val breakdown = CalculatorEngine.calculateSummationBreakdown(
      inputMode = SummationInputMode.SUMMATION_MS,
      inputValue = 3200.0,
      samplePeriodSec = 20,
      vCpuCount = 8
    )
    assertEquals(3200.0, breakdown.summationMs, 0.01)
    assertEquals(400.0, breakdown.avgPerVcpuMs, 0.01)
    assertEquals(2.0, breakdown.perVcpuReadyPercent, 0.01)
    assertEquals(16.0, breakdown.totalReadyPercent, 0.01)
  }

  @Test
  fun `test parseLivePerformanceData CSV parsing`() {
    val csv = """
      app-srv-01, 8, 32, 1600, 100, App Server, Production host
      db-sql-02, 16, 64, 3200, 300, Database, SQL node
    """.trimIndent()
    val parsed = CalculatorEngine.parseLivePerformanceData(csv, 4)
    assertEquals(2, parsed.size)
    assertEquals("app-srv-01", parsed[0].name)
    assertEquals(8, parsed[0].vCpuCount)
    assertEquals(32, parsed[0].ramGb)
    assertEquals("db-sql-02", parsed[1].name)
    assertEquals(16, parsed[1].vCpuCount)
  }
}
