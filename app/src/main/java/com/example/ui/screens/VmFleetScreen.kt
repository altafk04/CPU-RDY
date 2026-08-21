package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ContentionSeverity
import com.example.data.model.VmProfile
import com.example.domain.CalculatorEngine
import com.example.ui.MainViewModel
import com.example.ui.components.StatusPill
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusNormal
import com.example.ui.theme.StatusOptimal
import com.example.ui.theme.StatusWarning

@Composable
fun VmFleetScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val vms by viewModel.vmList.collectAsStateWithLifecycle()
    val savedReports by viewModel.savedReports.collectAsStateWithLifecycle()
    val clusterConfig by viewModel.clusterConfig.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingVm by remember { mutableStateOf<VmProfile?>(null) }

    val filteredVms = remember(vms, searchQuery, selectedFilter) {
        vms.filter { vm ->
            val matchesQuery = vm.name.contains(searchQuery, ignoreCase = true) || vm.workloadType.contains(searchQuery, ignoreCase = true)
            val matchesFilter = if (selectedFilter == "All") true else vm.workloadType.equals(selectedFilter, ignoreCase = true)
            matchesQuery && matchesFilter
        }
    }

    // Detect Right-Sizing Candidates (VMs with high vCPUs and high Ready % or CSTP %)
    val rightSizingCandidates = remember(vms) {
        vms.filter { vm ->
            val rdyPct = (vm.readyTimeMs / (vm.samplePeriodSec * 1000.0)) * 100.0 / vm.vCpuCount
            val cstpPct = (vm.coStopTimeMs / (vm.samplePeriodSec * 1000.0)) * 100.0
            vm.vCpuCount >= 6 || rdyPct > 5.0 || cstpPct > 3.0
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Screen Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "VM Workload Fleet",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${vms.size} Virtual Machines in Cluster",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.openImportDialog() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import Live", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.resetSampleFleet() },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Right-Sizing Recommendation Banner
            if (rightSizingCandidates.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = StatusWarning.copy(alpha = 0.12f)),
                        border = BorderStroke(1.dp, StatusWarning.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = StatusWarning,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Right-Sizing Opportunities Detected (${rightSizingCandidates.size} VMs)",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "${rightSizingCandidates.joinToString { it.name }} have high vCPU allocations and elevated Co-Stop or Ready queuing. Downsizing vCPUs will eliminate ESXi scheduling lockups.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Search Bar & Filter Chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search virtual machines by name or role...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("All", "Database", "App Server", "Analytics", "Web Tier", "VDI").forEach { filter ->
                            val isSelected = selectedFilter == filter
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedFilter = filter },
                                label = { Text(filter, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }
            }

            // VM List
            items(filteredVms, key = { it.id }) { vm ->
                val rdyResult = remember(vm) {
                    CalculatorEngine.calculateCpuReady(
                        readyMs = vm.readyTimeMs,
                        samplePeriodSec = vm.samplePeriodSec,
                        vCpuCount = vm.vCpuCount,
                        coStopMs = vm.coStopTimeMs
                    )
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, rdyResult.contentionSeverity.color.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = vm.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Host: AVS-Node-0${(vm.assignedNodeIndex % clusterConfig.nodeCount) + 1} | ${vm.workloadType}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StatusPill(
                                    text = "${String.format("%.1f", rdyResult.perVcpuReadyPercent)}% RDY",
                                    color = rdyResult.contentionSeverity.color,
                                    icon = Icons.Default.Speed
                                )

                                IconButton(
                                    onClick = {
                                        editingVm = vm
                                        showAddEditDialog = true
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit VM", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                }

                                IconButton(
                                    onClick = { viewModel.deleteVm(vm) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete VM", tint = StatusCritical, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Hardware Specs Pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("vCPUs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${vm.vCpuCount} vCPUs", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Memory", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${vm.ramGb} GB RAM", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Co-Stop", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${String.format("%.1f", rdyResult.coStopPercent)}% CSTP", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = if (rdyResult.coStopPercent > 3.0) StatusWarning else MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }

                        if (vm.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = vm.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Saved Reports & Snapshots
            if (savedReports.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Saved Calculation Snapshots",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                items(savedReports, key = { it.id }) { report ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Column {
                                    Text(report.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                    Text(report.summaryText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            IconButton(onClick = { viewModel.deleteReport(report.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp)) // padding for FAB
            }
        }

        // Add VM Floating Action Button
        FloatingActionButton(
            onClick = {
                editingVm = null
                showAddEditDialog = true
            },
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Virtual Machine")
        }
    }

    // Add / Edit VM Dialog
    if (showAddEditDialog) {
        var nameText by remember { mutableStateOf(editingVm?.name ?: "app-worker-${(10..99).random()}") }
        var vcpuText by remember { mutableStateOf(editingVm?.vCpuCount?.toString() ?: "4") }
        var ramText by remember { mutableStateOf(editingVm?.ramGb?.toString() ?: "16") }
        var readyMsText by remember { mutableStateOf(editingVm?.readyTimeMs?.toInt()?.toString() ?: "800") }
        var cstpMsText by remember { mutableStateOf(editingVm?.coStopTimeMs?.toInt()?.toString() ?: "80") }
        var workloadType by remember { mutableStateOf(editingVm?.workloadType ?: "App Server") }
        var notesText by remember { mutableStateOf(editingVm?.notes ?: "") }

        AlertDialog(
            onDismissRequest = { showAddEditDialog = false },
            title = {
                Text(if (editingVm != null) "Edit Virtual Machine" else "Add New Virtual Machine")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nameText,
                        onValueChange = { nameText = it },
                        label = { Text("VM Hostname / ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = vcpuText,
                            onValueChange = { vcpuText = it },
                            label = { Text("vCPUs") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = ramText,
                            onValueChange = { ramText = it },
                            label = { Text("RAM (GB)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = readyMsText,
                            onValueChange = { readyMsText = it },
                            label = { Text("Ready Time (ms)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = cstpMsText,
                            onValueChange = { cstpMsText = it },
                            label = { Text("Co-Stop (ms)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = workloadType,
                        onValueChange = { workloadType = it },
                        label = { Text("Workload Type (Database, App, Web, Analytics, VDI)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("Notes / Application Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val vcpu = vcpuText.toIntOrNull() ?: 4
                        val ram = ramText.toIntOrNull() ?: 16
                        val rdy = readyMsText.toDoubleOrNull() ?: 800.0
                        val cstp = cstpMsText.toDoubleOrNull() ?: 80.0

                        if (editingVm != null) {
                            viewModel.updateVm(
                                editingVm!!.copy(
                                    name = nameText,
                                    vCpuCount = vcpu,
                                    ramGb = ram,
                                    readyTimeMs = rdy,
                                    coStopTimeMs = cstp,
                                    workloadType = workloadType,
                                    notes = notesText
                                )
                            )
                        } else {
                            viewModel.addVm(
                                VmProfile(
                                    name = nameText,
                                    vCpuCount = vcpu,
                                    ramGb = ram,
                                    readyTimeMs = rdy,
                                    coStopTimeMs = cstp,
                                    samplePeriodSec = 20,
                                    assignedNodeIndex = (0 until clusterConfig.nodeCount).random(),
                                    workloadType = workloadType,
                                    notes = notesText
                                )
                            )
                        }
                        showAddEditDialog = false
                    }
                ) {
                    Text(if (editingVm != null) "Update" else "Add VM")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
