package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ImportLiveDataDialog
import com.example.ui.screens.ClusterConfigScreen
import com.example.ui.screens.CpuReadyCalculatorScreen
import com.example.ui.screens.DrsDashboardScreen
import com.example.ui.screens.VmFleetScreen
import com.example.ui.screens.WhatIfSimulatorScreen
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusNormal
import com.example.ui.theme.StatusOptimal
import com.example.ui.theme.StatusWarning

data class NavTabItem(
    val title: String,
    val icon: ImageVector,
    val testTag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val showImportDialog by viewModel.showImportDialog.collectAsStateWithLifecycle()
    val isImporting by viewModel.isImporting.collectAsStateWithLifecycle()
    val importStatusMessage by viewModel.importStatusMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showHelpDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    val navTabs = listOf(
        NavTabItem("%RDY Calc", Icons.Default.Speed, "tab_rdy_calc"),
        NavTabItem("AVS Cluster", Icons.Default.Dns, "tab_cluster_sizer"),
        NavTabItem("DRS Impact", Icons.Default.CompareArrows, "tab_drs_dashboard"),
        NavTabItem("What-If", Icons.Default.Insights, "tab_what_if"),
        NavTabItem("VM Fleet", Icons.Default.Group, "tab_vm_fleet")
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "App Logo",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(24.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "VMware CPU RDY & DRS",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Azure VMware Solution Analyzer",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Import Live Telemetry Button
                    IconButton(
                        onClick = { viewModel.openImportDialog() },
                        modifier = Modifier.padding(end = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "Import Live Telemetry",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Normal / Dark Mode Toggle
                    IconButton(
                        onClick = { viewModel.toggleTheme() },
                        modifier = Modifier.padding(end = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isDarkTheme) "Switch to Light Mode" else "Switch to Dark Mode",
                            tint = if (isDarkTheme) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Help & Theory Guide Dialog
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Architecture Guide",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.navigationBarsPadding(),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                navTabs.forEachIndexed { index, tab ->
                    val isSelected = selectedTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.setSelectedTab(index) },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp
                                )
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tab_transition"
            ) { tabIndex ->
                when (tabIndex) {
                    0 -> CpuReadyCalculatorScreen(viewModel = viewModel)
                    1 -> ClusterConfigScreen(viewModel = viewModel)
                    2 -> DrsDashboardScreen(viewModel = viewModel)
                    3 -> WhatIfSimulatorScreen(viewModel = viewModel)
                    4 -> VmFleetScreen(viewModel = viewModel)
                }
            }
        }
    }

    // Help & Architecture Guide Dialog
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.HelpOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("VMware & AVS Guide", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "1. CPU Ready Time (%RDY)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "CPU Ready is the percentage of time a VM is ready to run instructions on physical cores, but must wait in the ESXi scheduler queue due to CPU oversubscription or co-scheduling delays.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "%RDY = (ReadyTime_ms / (Sample_sec * 1000)) * 100",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "2. Contention Thresholds",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("• < 2.5% RDY: Optimal (Instant core allocation)\n• 2.5% - 5% RDY: Normal cloud multi-tenancy\n• 5% - 10% RDY: Warning (Noticeable latency jitter)\n• > 10% RDY: Critical Bottleneck (CPU Starvation)", style = MaterialTheme.typography.bodySmall)

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "3. Co-Stop (%CSTP) & Multi-vCPU",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Wide VMs (e.g. 8 or 16 vCPUs) suffer when ESXi has to schedule multiple pCPUs simultaneously. If %CSTP > 3%, downsize vCPU count to drastically eliminate wait time.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "4. Azure VMware Solution (AVS) SKUs",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "• AV36 / AV36t: 36 Physical Cores (72 vCPUs), 576 GB RAM\n• AV52: 52 Cores (104 vCPUs), 1.5 TB RAM\n• AV64: 64 Cores (128 vCPUs), 1 TB RAM (Sapphire Rapids)",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "5. vCenter Summation (ms) Conversion",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "In vCenter performance charts, `cpu.ready.summation` aggregates wait time across all vCPUs. To convert summation ms into standard %RDY:\nPer-vCPU %RDY = (Summation_ms / (Sample_sec * 1000 * num_vCPUs)) * 100",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showHelpDialog = false }) {
                    Text("Got it")
                }
            }
        )
    }

    // Live Telemetry Import Dialog
    ImportLiveDataDialog(
        isOpen = showImportDialog,
        isImporting = isImporting,
        errorMessage = importStatusMessage,
        onDismiss = { viewModel.closeImportDialog() },
        onImportVcenter = { url, user, token, ignoreSsl ->
            viewModel.importFromVcenterRestApi(url, user, token, ignoreSsl)
        },
        onImportText = { text ->
            viewModel.importFromRawText(text)
        },
        onImportPreset = { preset ->
            viewModel.importFromPresetEnvironment(preset)
        }
    )
}
