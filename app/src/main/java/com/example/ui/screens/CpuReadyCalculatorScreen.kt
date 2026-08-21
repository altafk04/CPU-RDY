package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.SampleIntervalPreset
import com.example.ui.MainViewModel
import com.example.ui.components.CpuReadyGauge
import com.example.ui.components.MetricStatCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusNormal
import com.example.ui.theme.StatusOptimal
import com.example.ui.theme.StatusWarning

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CpuReadyCalculatorScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val readyMs by viewModel.calcReadyMs.collectAsStateWithLifecycle()
    val samplePeriodSec by viewModel.calcSamplePeriodSec.collectAsStateWithLifecycle()
    val vCpuCount by viewModel.calcVcpuCount.collectAsStateWithLifecycle()
    val coStopMs by viewModel.calcCoStopMs.collectAsStateWithLifecycle()
    val result by viewModel.cpuReadyResult.collectAsStateWithLifecycle()

    var readyInputText by remember(readyMs) { mutableStateOf(readyMs.toInt().toString()) }
    var coStopInputText by remember(coStopMs) { mutableStateOf(coStopMs.toInt().toString()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Screen Title Banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "VMware CPU Ready Calculator",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Real-time %RDY & %CSTP Contention Analyzer",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { viewModel.saveCurrentCalculationReport() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = "Save Report",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Live Speedometer Gauge
        item {
            CpuReadyGauge(
                readyPercent = result.totalReadyPercent,
                perVcpuPercent = result.perVcpuReadyPercent,
                vCpuCount = result.vCpuCount,
                severity = result.contentionSeverity
            )
        }

        // Sampling Interval Presets Selector
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Sample Interval Window",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SampleIntervalPreset.PRESETS.forEach { preset ->
                            val isSelected = samplePeriodSec == preset.seconds
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    viewModel.updateCalcInputs(samplePeriodSec = preset.seconds)
                                },
                                label = {
                                    Text(
                                        text = preset.title,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    val currentPreset = SampleIntervalPreset.PRESETS.find { it.seconds == samplePeriodSec }
                    Text(
                        text = currentPreset?.description ?: "Custom interval: $samplePeriodSec seconds (${samplePeriodSec * 1000} ms)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Input Controls: Ready Time (ms), vCPUs, and Co-Stop Time (ms)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "VMware Performance Counter Values",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Ready Time (ms)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CPU Ready Time (Milliseconds):",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${readyMs.toInt()} ms",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        val maxSliderMs = (samplePeriodSec * 1000.0 * 0.4).toFloat().coerceAtLeast(2000f)
                        Slider(
                            value = readyMs.toFloat().coerceIn(0f, maxSliderMs),
                            onValueChange = {
                                viewModel.updateCalcInputs(readyMs = it.toDouble())
                            },
                            valueRange = 0f..maxSliderMs,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = readyInputText,
                            onValueChange = { input ->
                                readyInputText = input
                                val parsed = input.toDoubleOrNull()
                                if (parsed != null && parsed >= 0) {
                                    viewModel.updateCalcInputs(readyMs = parsed)
                                }
                            },
                            label = { Text("Exact Ready Time (ms)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // vCPU Count Selector
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Assigned VM vCPUs:",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        if (vCpuCount > 1) viewModel.updateCalcInputs(vCpuCount = vCpuCount / 2)
                                    },
                                    enabled = vCpuCount > 1
                                ) {
                                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease vCPUs")
                                }
                                Text(
                                    text = "$vCpuCount vCPUs",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                IconButton(
                                    onClick = {
                                        if (vCpuCount < 64) viewModel.updateCalcInputs(vCpuCount = (vCpuCount * 2).coerceAtMost(64))
                                    },
                                    enabled = vCpuCount < 64
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = "Increase vCPUs")
                                }
                            }
                        }

                        // Quick Select Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf(1, 2, 4, 8, 16, 32).forEach { count ->
                                val isSelected = vCpuCount == count
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 2.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.updateCalcInputs(vCpuCount = count) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.Transparent,
                                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        ),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                                    ) {
                                        Text(text = "${count}c", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Co-Stop Time (%CSTP)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Co-Stop Time (%CSTP ms):",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Multi-vCPU co-scheduling latency",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${String.format("%.2f", result.coStopPercent)}% CSTP",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = if (result.coStopPercent > 3.0) StatusWarning else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        OutlinedTextField(
                            value = coStopInputText,
                            onValueChange = { input ->
                                coStopInputText = input
                                val parsed = input.toDoubleOrNull()
                                if (parsed != null && parsed >= 0) {
                                    viewModel.updateCalcInputs(coStopMs = parsed)
                                }
                            },
                            label = { Text("Co-Stop Time (ms)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }

        // Performance & Contention Impact Metrics
        item {
            Text(
                text = "Performance Impact Breakdown",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricStatCard(
                    title = "Latency Penalty",
                    value = "${result.latencyDelayMsPerSec.toInt()} ms/s",
                    subtitle = "Time waiting per clock sec",
                    icon = Icons.Default.HourglassTop,
                    accentColor = result.contentionSeverity.color,
                    modifier = Modifier.weight(1f)
                )

                MetricStatCard(
                    title = "Co-Scheduling Skew",
                    value = "${String.format("%.2f", result.coSchedulingSkewFactor)}x",
                    subtitle = "${result.vCpuCount} vCPU lock penalty",
                    icon = Icons.Default.Speed,
                    accentColor = if (result.vCpuCount >= 8) StatusWarning else StatusOptimal,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Actionable Insights & Remediation
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = result.contentionSeverity.color.copy(alpha = 0.08f)
                ),
                border = BorderStroke(1.dp, result.contentionSeverity.color.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = result.contentionSeverity.color,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Virtualization Remediation Guidance",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    result.actionableInsights.forEach { insight ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .size(6.dp)
                                    .background(result.contentionSeverity.color, CircleShape)
                            )
                            Text(
                                text = insight,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Reference Formula Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "VMware CPU Ready Formula & Reference",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "%RDY = (ReadyTime_ms / (Sample_sec * 1000)) * 100\nPer-vCPU %RDY = Total %RDY / num_vCPUs",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatusPill(text = "< 2.5% Optimal", color = StatusOptimal)
                        StatusPill(text = "2.5-5% Normal", color = StatusNormal)
                        StatusPill(text = "5-10% Warning", color = StatusWarning)
                        StatusPill(text = "> 10% Critical", color = StatusCritical)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
