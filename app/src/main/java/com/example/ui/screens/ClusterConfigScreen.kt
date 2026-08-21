package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AvsNodeSpec
import com.example.data.model.ClusterConfig
import com.example.domain.CalculatorEngine
import com.example.ui.MainViewModel
import com.example.ui.components.AvsSkuCard
import com.example.ui.components.MetricStatCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusNormal
import com.example.ui.theme.StatusOptimal
import com.example.ui.theme.StatusWarning

@Composable
fun ClusterConfigScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val clusterConfig by viewModel.clusterConfig.collectAsStateWithLifecycle()
    val vms by viewModel.vmList.collectAsStateWithLifecycle()

    val nodeSpec = remember(clusterConfig) {
        CalculatorEngine.resolveNodeSpec(clusterConfig)
    }

    val (clusterSummary, _) = remember(clusterConfig, vms, nodeSpec) {
        CalculatorEngine.calculateClusterSummary(clusterConfig, vms, nodeSpec)
    }

    var showCustomDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Screen Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AVS Node & Cluster Sizing",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Azure VMware Solution SKU & Overcommit Spec",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (clusterSummary.clusterHealthScore >= 80) StatusOptimal.copy(alpha = 0.15f)
                    else if (clusterSummary.clusterHealthScore >= 60) StatusWarning.copy(alpha = 0.15f)
                    else StatusCritical.copy(alpha = 0.15f),
                    border = BorderStroke(
                        1.dp,
                        if (clusterSummary.clusterHealthScore >= 80) StatusOptimal.copy(alpha = 0.5f)
                        else if (clusterSummary.clusterHealthScore >= 60) StatusWarning.copy(alpha = 0.5f)
                        else StatusCritical.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = if (clusterSummary.clusterHealthScore >= 80) StatusOptimal else StatusWarning,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Health: ${clusterSummary.clusterHealthScore}/100",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (clusterSummary.clusterHealthScore >= 80) StatusOptimal else StatusWarning
                        )
                    }
                }
            }
        }

        // High-level KPI Summary Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val ratioColor = when {
                    clusterSummary.vCpuToPhysicalCoreRatio > 3.5 -> StatusCritical
                    clusterSummary.vCpuToPhysicalCoreRatio > 2.5 -> StatusWarning
                    else -> StatusOptimal
                }

                MetricStatCard(
                    title = "vCPU : pCore Ratio",
                    value = "${String.format("%.2f", clusterSummary.vCpuToPhysicalCoreRatio)} : 1",
                    subtitle = "Safe threshold: < 3.0:1",
                    icon = Icons.Default.Memory,
                    accentColor = ratioColor,
                    modifier = Modifier.weight(1f),
                    trailingTag = if (clusterSummary.vCpuToPhysicalCoreRatio <= 3.0) "Optimal" else "Overcommit"
                )

                MetricStatCard(
                    title = "Memory Allocation",
                    value = "${clusterSummary.memoryAllocationPercent.toInt()}%",
                    subtitle = "${clusterSummary.totalAllocatedRamGb} of ${clusterSummary.totalUsableRamGb} GB",
                    icon = Icons.Default.Storage,
                    accentColor = if (clusterSummary.memoryAllocationPercent > 85) StatusWarning else StatusOptimal,
                    modifier = Modifier.weight(1f),
                    trailingTag = "${clusterSummary.totalUsableRamGb - clusterSummary.totalAllocatedRamGb}GB Free"
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricStatCard(
                    title = "Usable Cluster Cores",
                    value = "${clusterSummary.totalUsablePhysicalCores} Cores",
                    subtitle = "${clusterSummary.totalLogicalVcpus} HT Threads across ${clusterSummary.totalNodes} nodes",
                    icon = Icons.Default.Dns,
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                MetricStatCard(
                    title = "vSphere HA N+1",
                    value = if (clusterSummary.isHaCompliant) "Compliant" else "At Risk",
                    subtitle = "Failover load: ${clusterSummary.nPlusOneFailoverLoadPercent.toInt()}% CPU",
                    icon = Icons.Default.Security,
                    accentColor = if (clusterSummary.isHaCompliant) StatusOptimal else StatusCritical,
                    modifier = Modifier.weight(1f),
                    trailingTag = "${clusterConfig.haFailoverNodesReserved} Node Res."
                )
            }
        }

        // Cluster Sizing Controls (Nodes Count & HA Reservation)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Cluster Node Scale & HA Reservation",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Node Count Stepper
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Cluster Node Count (AVS Hosts):",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "AVS Standard: 3 to 16 nodes per cluster",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    if (clusterConfig.nodeCount > 3) {
                                        viewModel.updateClusterConfig(clusterConfig.copy(nodeCount = clusterConfig.nodeCount - 1))
                                    }
                                },
                                enabled = clusterConfig.nodeCount > 3
                            ) {
                                Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease Nodes")
                            }

                            Text(
                                text = "${clusterConfig.nodeCount} Nodes",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )

                            IconButton(
                                onClick = {
                                    if (clusterConfig.nodeCount < 16) {
                                        viewModel.updateClusterConfig(clusterConfig.copy(nodeCount = clusterConfig.nodeCount + 1))
                                    }
                                },
                                enabled = clusterConfig.nodeCount < 16
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Increase Nodes")
                            }
                        }
                    }

                    // HA Admission Control Policy
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "vSphere HA Failover Capacity:",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1 to "N+1 (1 Node)", 2 to "N+2 (2 Nodes)").forEach { (res, label) ->
                                val isSelected = clusterConfig.haFailoverNodesReserved == res
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.updateClusterConfig(clusterConfig.copy(haFailoverNodesReserved = res))
                                    },
                                    label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // AVS Node SKU Preset Selection
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Azure VMware Solution Node SKUs",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedButton(
                    onClick = { showCustomDialog = true },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Custom Host")
                }
            }
        }

        // Standard AVS SKUs (AV36, AV36t, AV52, AV64)
        AvsNodeSpec.ALL_PRESETS.forEach { spec ->
            item {
                AvsSkuCard(
                    spec = spec,
                    isSelected = clusterConfig.skuCode.equals(spec.skuCode, ignoreCase = true),
                    onSelect = {
                        viewModel.updateClusterConfig(clusterConfig.copy(skuCode = spec.skuCode))
                    }
                )
            }
        }

        // Custom Host Card if selected
        if (clusterConfig.skuCode.equals("CUSTOM", ignoreCase = true)) {
            item {
                AvsSkuCard(
                    spec = nodeSpec,
                    isSelected = true,
                    onSelect = { showCustomDialog = true }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Custom Host Spec Builder Dialog
    if (showCustomDialog) {
        var socketsText by remember { mutableStateOf(clusterConfig.customSockets.toString()) }
        var coresText by remember { mutableStateOf(clusterConfig.customCoresPerSocket.toString()) }
        var ramText by remember { mutableStateOf(clusterConfig.customTotalRamGb.toString()) }
        var clockText by remember { mutableStateOf(clusterConfig.customClockGhz.toString()) }
        var resCoresText by remember { mutableStateOf(clusterConfig.customReservedCores.toString()) }
        var resRamText by remember { mutableStateOf(clusterConfig.customReservedRamGb.toString()) }

        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Configure Custom ESXi Node")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = socketsText,
                            onValueChange = { socketsText = it },
                            label = { Text("Sockets") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = coresText,
                            onValueChange = { coresText = it },
                            label = { Text("Cores/Socket") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = ramText,
                            onValueChange = { ramText = it },
                            label = { Text("Total RAM (GB)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = clockText,
                            onValueChange = { clockText = it },
                            label = { Text("Base Clock (GHz)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = resCoresText,
                            onValueChange = { resCoresText = it },
                            label = { Text("ESXi Res. Cores") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = resRamText,
                            onValueChange = { resRamText = it },
                            label = { Text("ESXi Res. RAM (GB)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val s = socketsText.toIntOrNull() ?: 2
                        val c = coresText.toIntOrNull() ?: 18
                        val r = ramText.toIntOrNull() ?: 576
                        val clk = clockText.toDoubleOrNull() ?: 2.4
                        val rc = resCoresText.toIntOrNull() ?: 2
                        val rr = resRamText.toIntOrNull() ?: 36

                        viewModel.updateClusterConfig(
                            clusterConfig.copy(
                                skuCode = "CUSTOM",
                                customSockets = s,
                                customCoresPerSocket = c,
                                customTotalRamGb = r,
                                customClockGhz = clk,
                                customReservedCores = rc,
                                customReservedRamGb = rr
                            )
                        )
                        showCustomDialog = false
                    }
                ) {
                    Text("Apply Specs")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
