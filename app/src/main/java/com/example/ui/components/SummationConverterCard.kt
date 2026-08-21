package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import com.example.data.model.SummationBreakdown
import com.example.data.model.SummationInputMode
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusNormal
import com.example.ui.theme.StatusOptimal
import com.example.ui.theme.StatusWarning

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SummationConverterCard(
    breakdown: SummationBreakdown,
    onModeChanged: (SummationInputMode) -> Unit,
    onValueChanged: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }
    var inputText by remember(breakdown.inputValue, breakdown.inputMode) {
        mutableStateOf(
            if (breakdown.inputMode == SummationInputMode.PERCENT_RDY_TARGET) {
                String.format("%.2f", breakdown.inputValue)
            } else {
                breakdown.inputValue.toInt().toString()
            }
        )
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with toggle collapse
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "CPU Ready Summation Converter",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "vCenter Summation (ms) ⇄ Per-vCPU %RDY Conversion Engine",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Mode Selector Chips
                    Text(
                        text = "Calculation / Input Mode:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SummationInputMode.entries.forEach { mode ->
                            val isSelected = breakdown.inputMode == mode
                            FilterChip(
                                selected = isSelected,
                                onClick = { onModeChanged(mode) },
                                label = {
                                    Text(
                                        text = mode.displayName,
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

                    Text(
                        text = breakdown.inputMode.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Input Field & Slider
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (breakdown.inputMode) {
                                    SummationInputMode.SUMMATION_MS -> "Summation Value (ms):"
                                    SummationInputMode.AVG_PER_VCPU_MS -> "Average Wait per vCPU (ms):"
                                    SummationInputMode.PERCENT_RDY_TARGET -> "Desired %RDY Target:"
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = when (breakdown.inputMode) {
                                    SummationInputMode.SUMMATION_MS -> "${breakdown.summationMs.toInt()} ms"
                                    SummationInputMode.AVG_PER_VCPU_MS -> "${breakdown.avgPerVcpuMs.toInt()} ms"
                                    SummationInputMode.PERCENT_RDY_TARGET -> "${String.format("%.2f", breakdown.perVcpuReadyPercent)}%"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        val maxRange = when (breakdown.inputMode) {
                            SummationInputMode.SUMMATION_MS -> (breakdown.samplePeriodMs * breakdown.vCpuCount * 0.4).toFloat().coerceAtLeast(3000f)
                            SummationInputMode.AVG_PER_VCPU_MS -> (breakdown.samplePeriodMs * 0.4).toFloat().coerceAtLeast(2000f)
                            SummationInputMode.PERCENT_RDY_TARGET -> 40f
                        }

                        Slider(
                            value = breakdown.inputValue.toFloat().coerceIn(0f, maxRange),
                            onValueChange = { onValueChanged(it.toDouble()) },
                            valueRange = 0f..maxRange,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { input ->
                                inputText = input
                                val parsed = input.toDoubleOrNull()
                                if (parsed != null && parsed >= 0) {
                                    onValueChanged(parsed)
                                }
                            },
                            label = {
                                Text(
                                    when (breakdown.inputMode) {
                                        SummationInputMode.SUMMATION_MS -> "Enter vCenter Summation (ms)"
                                        SummationInputMode.AVG_PER_VCPU_MS -> "Enter Avg ms per vCPU"
                                        SummationInputMode.PERCENT_RDY_TARGET -> "Enter Target %RDY (e.g., 5.0)"
                                    }
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Conversion Results Matrix
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Instant Conversion Equivalents",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                ConversionMetricItem(
                                    label = "vCenter Summation",
                                    value = "${breakdown.summationMs.toInt()} ms",
                                    helper = "Across all ${breakdown.vCpuCount} vCPUs"
                                )
                                ConversionMetricItem(
                                    label = "Avg Wait / vCPU",
                                    value = "${String.format("%.1f", breakdown.avgPerVcpuMs)} ms",
                                    helper = "Per single core"
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                ConversionMetricItem(
                                    label = "Per-vCPU %RDY",
                                    value = "${String.format("%.2f", breakdown.perVcpuReadyPercent)}%",
                                    helper = "Standard VMware KPI",
                                    valueColor = when {
                                        breakdown.perVcpuReadyPercent < 2.5 -> StatusOptimal
                                        breakdown.perVcpuReadyPercent < 5.0 -> StatusNormal
                                        breakdown.perVcpuReadyPercent < 10.0 -> StatusWarning
                                        else -> StatusCritical
                                    }
                                )
                                ConversionMetricItem(
                                    label = "Total VM %RDY",
                                    value = "${String.format("%.2f", breakdown.totalReadyPercent)}%",
                                    helper = "Summed across ${breakdown.vCpuCount} vCPUs"
                                )
                            }
                        }
                    }

                    // Mathematical Derivation Steps
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Functions,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Mathematical Proof & Derivation",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Text(
                                text = breakdown.formulaExplanation,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            breakdown.mathSteps.forEach { step ->
                                Text(
                                    text = step,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversionMetricItem(
    label: String,
    value: String,
    helper: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(modifier = Modifier.width(140.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            ),
            color = valueColor
        )
        Text(
            text = helper,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
}
