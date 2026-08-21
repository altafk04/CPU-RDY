package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.CalculatorEngine
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusNormal
import com.example.ui.theme.StatusOptimal
import com.example.ui.theme.StatusWarning

@Composable
fun ImportLiveDataDialog(
    isOpen: Boolean,
    isImporting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onImportVcenter: (url: String, username: String, token: String, ignoreSsl: Boolean) -> Unit,
    onImportText: (text: String) -> Unit,
    onImportPreset: (presetName: String) -> Unit
) {
    if (!isOpen) return

    var selectedTab by remember { mutableIntStateOf(0) }

    // REST API state
    var vcenterUrl by remember { mutableStateOf("https://vcenter.corp.local") }
    var vcenterUser by remember { mutableStateOf("administrator@vsphere.local") }
    var vcenterToken by remember { mutableStateOf("") }
    var ignoreSsl by remember { mutableStateOf(true) }

    // Text import state
    var rawText by remember {
        mutableStateOf(
            """# Name, vCPUs, RAM_GB, Ready_ms, CSTP_ms, Workload, Notes
sql-server-prod-01, 16, 128, 3600, 1100, Database, Tier-1 OLTP DB
sql-server-prod-02, 16, 128, 3400, 950, Database, High-availability secondary
app-worker-node-01, 8, 32, 1800, 320, App Server, Java Spring Boot cluster
app-worker-node-02, 8, 32, 1750, 290, App Server, Java Spring Boot cluster
web-edge-nginx-01, 4, 16, 520, 30, Web Tier, SSL termination reverse proxy
web-edge-nginx-02, 4, 16, 490, 25, Web Tier, SSL termination reverse proxy
elastic-search-node-01, 12, 64, 2800, 850, Analytics, Telemetry indexing pipeline
redis-cache-cluster-01, 4, 32, 420, 15, Database, Distributed session store"""
        )
    }

    val tabTitles = listOf("vCenter REST API", "Paste CSV / esxtop", "Live Presets")

    AlertDialog(
        onDismissRequest = { if (!isImporting) onDismiss() },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Import Live VMware / AVS Data",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontSize = 12.sp, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }

                if (errorMessage != null) {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = StatusCritical.copy(alpha = 0.12f)),
                        border = BorderStroke(1.dp, StatusCritical.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = StatusCritical, modifier = Modifier.size(20.dp))
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                if (isImporting) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(36.dp))
                            Text(
                                text = "Connecting & Ingesting live telemetry...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else {
                    when (selectedTab) {
                        0 -> {
                            // vCenter REST API tab
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "Connect directly to your VMware vCenter 7.0/8.0 or ESXi Appliance REST API (`/api/vcenter/vm`):",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                OutlinedTextField(
                                    value = vcenterUrl,
                                    onValueChange = { vcenterUrl = it },
                                    label = { Text("vCenter Host URL / IP") },
                                    placeholder = { Text("https://vcenter.corp.local") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                OutlinedTextField(
                                    value = vcenterUser,
                                    onValueChange = { vcenterUser = it },
                                    label = { Text("vSphere Username / SSO Account") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                OutlinedTextField(
                                    value = vcenterToken,
                                    onValueChange = { vcenterToken = it },
                                    label = { Text("API Session Token or Password") },
                                    placeholder = { Text("vmware-api-session-id or password") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { ignoreSsl = !ignoreSsl }
                                ) {
                                    Checkbox(
                                        checked = ignoreSsl,
                                        onCheckedChange = { ignoreSsl = it }
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Trust Self-Signed SSL Certificate (Lab/Internal)",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                Button(
                                    onClick = {
                                        onImportVcenter(vcenterUrl, vcenterUser, vcenterToken, ignoreSsl)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Http, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Connect & Ingest Live VMs", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        1 -> {
                            // Paste text / CSV tab
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "Paste output from `esxtop`, RVTools (vCPU tab), PowerCLI `Get-Stat`, or standard CSV:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                OutlinedTextField(
                                    value = rawText,
                                    onValueChange = { rawText = it },
                                    label = { Text("CSV / Performance Table Output") },
                                    placeholder = { Text("Name, vCPUs, RAM_GB, Ready_ms, CSTP_ms...") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val detectedCount = CalculatorEngine.parseLivePerformanceData(rawText).size
                                    Text(
                                        text = "Detected: $detectedCount VMs",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    Button(
                                        onClick = {
                                            onImportText(rawText)
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Parse & Load Fleet", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        2 -> {
                            // Live Environment Presets tab
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "Select a live simulated cloud production environment to ingest immediate telemetry data:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                val presets = CalculatorEngine.getEnvironmentPresets()
                                presets.forEach { preset ->
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onImportPreset(preset.first) }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = preset.first,
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "${preset.second.size} VMs • Total ${preset.second.sumOf { it.vCpuCount }} vCPUs • ${preset.second.sumOf { it.ramGb }} GB RAM",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Button(
                                                onClick = { onImportPreset(preset.first) },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text("Load", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isImporting
            ) {
                Text("Close")
            }
        }
    )
}
