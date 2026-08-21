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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.CalculatorEngine
import com.example.ui.MainViewModel
import com.example.ui.components.DrsRecommendationCard
import com.example.ui.components.HostLoadBar
import com.example.ui.components.MetricStatCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusNormal
import com.example.ui.theme.StatusOptimal
import com.example.ui.theme.StatusWarning

@Composable
fun DrsDashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val clusterConfig by viewModel.clusterConfig.collectAsStateWithLifecycle()
    val vms by viewModel.vmList.collectAsStateWithLifecycle()
    val drsThreshold by viewModel.drsThreshold.collectAsStateWithLifecycle()

    val nodeSpec = remember(clusterConfig) {
        CalculatorEngine.resolveNodeSpec(clusterConfig)
    }

    val (clusterSummary, hostWorkloads) = remember(clusterConfig, vms, nodeSpec) {
        CalculatorEngine.calculateClusterSummary(clusterConfig, vms, nodeSpec)
    }

    val recommendations = remember(hostWorkloads, drsThreshold, nodeSpec) {
        CalculatorEngine.evaluateDrsRecommendations(hostWorkloads, drsThreshold, nodeSpec)
    }

    val thresholdNames = listOf(
        1 to "1 - Conservative (Host Maintenance & Affinity rules only)",
        2 to "2 - Moderately Conservative (High priority imbalances)",
        3 to "3 - Neutral (Default balanced vMotion threshold)",
        4 to "4 - Moderately Aggressive (Frequent fine-tuning)",
        5 to "5 - Aggressive (Max rebalance, highest vMotion activity)"
    )

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
                        text = "DRS Impact Analysis Dashboard",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Distributed Resource Scheduler & vMotion Simulator",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusPill(
                    text = "${recommendations.size} Actions",
                    color = if (recommendations.isEmpty()) StatusOptimal else StatusWarning,
                    icon = Icons.Default.ElectricBolt
                )
            }
        }

        // Cluster Imbalance KPI and Status Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val imbalanceColor = when {
                    clusterSummary.clusterImbalanceStdDev > 20.0 -> StatusCritical
                    clusterSummary.clusterImbalanceStdDev > 10.0 -> StatusWarning
                    else -> StatusOptimal
                }

                MetricStatCard(
                    title = "Cluster Imbalance (StdDev)",
                    value = "${String.format("%.1f", clusterSummary.clusterImbalanceStdDev)}%",
                    subtitle = if (clusterSummary.clusterImbalanceStdDev <= 10.0) "Balanced host loads" else "High CPU skew across nodes",
                    icon = Icons.Default.CompareArrows,
                    accentColor = imbalanceColor,
                    modifier = Modifier.weight(1f),
                    trailingTag = if (clusterSummary.clusterImbalanceStdDev <= 10.0) "Balanced" else "Imbalanced"
                )

                val avgRdy = hostWorkloads.map { it.averageReadyPercent }.average()
                MetricStatCard(
                    title = "Cluster Avg %RDY",
                    value = "${String.format("%.2f", avgRdy)}%",
                    subtitle = "Across ${clusterConfig.nodeCount} ESXi hosts",
                    icon = Icons.Default.Speed,
                    accentColor = if (avgRdy > 5.0) StatusWarning else StatusOptimal,
                    modifier = Modifier.weight(1f),
                    trailingTag = "${vms.size} Total VMs"
                )
            }
        }

        // DRS Migration Sensitivity Threshold Slider
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "DRS Migration Sensitivity",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Level $drsThreshold",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Slider(
                        value = drsThreshold.toFloat(),
                        onValueChange = { viewModel.setDrsThreshold(it.toInt()) },
                        valueRange = 1f..5f,
                        steps = 3,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    val currentThresholdDesc = thresholdNames.find { it.first == drsThreshold }?.second
                    Text(
                        text = currentThresholdDesc ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Active DRS Recommendations
        item {
            Text(
                text = "DRS Migration Recommendations & Impact",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (recommendations.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = StatusOptimal.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, StatusOptimal.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircleOutline,
                            contentDescription = null,
                            tint = StatusOptimal,
                            modifier = Modifier.size(32.dp)
                        )
                        Column {
                            Text(
                                text = "Cluster is Perfectly Balanced",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "No vMotion migrations are necessary at threshold Level $drsThreshold. Host CPU load variance is within optimal operational limits.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            items(recommendations, key = { it.id }) { rec ->
                DrsRecommendationCard(
                    recommendation = rec,
                    onApplyMigration = { viewModel.applyDrsMigration(rec) }
                )
            }
        }

        // Host Workload Matrix Header
        item {
            Text(
                text = "Cluster ESXi Host Load & Queue Matrix",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Host Workloads
        items(hostWorkloads, key = { it.hostIndex }) { host ->
            HostLoadBar(host = host)
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
