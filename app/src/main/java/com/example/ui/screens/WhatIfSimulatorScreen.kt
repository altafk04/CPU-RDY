package com.example.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.WhatIfScenarioType
import com.example.domain.CalculatorEngine
import com.example.ui.MainViewModel
import com.example.ui.components.MetricStatCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusNormal
import com.example.ui.theme.StatusOptimal
import com.example.ui.theme.StatusWarning
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WhatIfSimulatorScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val clusterConfig by viewModel.clusterConfig.collectAsStateWithLifecycle()
    val vms by viewModel.vmList.collectAsStateWithLifecycle()
    val activeScenario by viewModel.whatIfScenario.collectAsStateWithLifecycle()
    val selectedMaintenanceHost by viewModel.selectedMaintenanceHostIndex.collectAsStateWithLifecycle()

    val nodeSpec = remember(clusterConfig) {
        CalculatorEngine.resolveNodeSpec(clusterConfig)
    }

    val simulationResult = remember(activeScenario, clusterConfig, vms, nodeSpec, selectedMaintenanceHost) {
        CalculatorEngine.simulateWhatIf(
            scenarioType = activeScenario,
            config = clusterConfig,
            vms = vms,
            nodeSpec = nodeSpec,
            targetHostIndex = selectedMaintenanceHost
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "What-If Scenario Simulator",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Simulate Host Maintenance, Scaling & Right-Sizing",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusPill(text = "Predictive Engine", color = MaterialTheme.colorScheme.primary, icon = Icons.Default.Insights)
            }
        }

        // Scenario Selector Chips
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Select What-If Scenario",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = activeScenario == WhatIfScenarioType.NODE_MAINTENANCE_EVACUATION,
                            onClick = { viewModel.setWhatIfScenario(WhatIfScenarioType.NODE_MAINTENANCE_EVACUATION) },
                            label = { Text("Host Maintenance Evacuation", fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )

                        FilterChip(
                            selected = activeScenario == WhatIfScenarioType.CLUSTER_SCALE_OUT,
                            onClick = { viewModel.setWhatIfScenario(WhatIfScenarioType.CLUSTER_SCALE_OUT) },
                            label = { Text("Scale-Out (+2 AVS Nodes)", fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )

                        FilterChip(
                            selected = activeScenario == WhatIfScenarioType.VM_VCPU_RIGHT_SIZING,
                            onClick = { viewModel.setWhatIfScenario(WhatIfScenarioType.VM_VCPU_RIGHT_SIZING) },
                            label = { Text("Right-Size Multi-vCPU VMs", fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }

                    // If Maintenance Mode, allow picking host to evacuate
                    if (activeScenario == WhatIfScenarioType.NODE_MAINTENANCE_EVACUATION) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Select Host to Evacuate:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (i in 0 until clusterConfig.nodeCount) {
                                val isSelected = selectedMaintenanceHost == i
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setMaintenanceHostIndex(i) },
                                    label = { Text("Node-0${i + 1}") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = StatusCritical,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Before vs After Impact KPIs
        item {
            Text(
                text = "Simulated Impact: ${simulationResult.scenarioTitle}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricStatCard(
                    title = "Cluster Avg %RDY",
                    value = "${String.format("%.1f", simulationResult.afterAvgCpuRdy)}%",
                    subtitle = "Before: ${String.format("%.1f", simulationResult.beforeAvgCpuRdy)}%",
                    icon = Icons.Default.Speed,
                    accentColor = if (simulationResult.afterAvgCpuRdy <= simulationResult.beforeAvgCpuRdy) StatusOptimal else StatusCritical,
                    modifier = Modifier.weight(1f),
                    trailingTag = if (simulationResult.afterAvgCpuRdy <= simulationResult.beforeAvgCpuRdy) "Improved" else "Surge"
                )

                MetricStatCard(
                    title = "Peak Host CPU Load",
                    value = "${simulationResult.afterMaxHostCpu.toInt()}%",
                    subtitle = "Before: ${simulationResult.beforeMaxHostCpu.toInt()}%",
                    icon = Icons.Default.TrendingDown,
                    accentColor = if (simulationResult.afterMaxHostCpu > 85) StatusCritical else StatusOptimal,
                    modifier = Modifier.weight(1f),
                    trailingTag = if (simulationResult.afterMaxHostCpu <= 80) "Safe" else "Hot"
                )
            }
        }

        // Simulation Narrative & Summary Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Insights,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Simulation Analysis Summary",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = simulationResult.summaryDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Host Load Delta Breakdown
        item {
            Text(
                text = "Host-by-Host Load Redistribution",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        items(simulationResult.hostLoadDeltas) { (hostName, loads) ->
            val before = loads.first
            val after = loads.second

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Dns, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Text(
                                text = hostName,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${before.roundToInt()}% CPU",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${after.roundToInt()}% CPU",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (after > 85) StatusCritical else if (after > before) StatusWarning else StatusOptimal
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Progress Comparison Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction = (after.toFloat() / 100f).coerceIn(0.01f, 1f))
                                .clip(CircleShape)
                                .background(if (after > 85) StatusCritical else if (after > before) StatusWarning else StatusOptimal)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
