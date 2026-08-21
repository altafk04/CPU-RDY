package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ContentionSeverity
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusNormal
import com.example.ui.theme.StatusOptimal
import com.example.ui.theme.StatusWarning
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun CpuReadyGauge(
    readyPercent: Double,
    perVcpuPercent: Double,
    vCpuCount: Int,
    severity: ContentionSeverity,
    modifier: Modifier = Modifier
) {
    val animatedPercent by animateFloatAsState(
        targetValue = readyPercent.toFloat(),
        animationSpec = tween(durationMillis = 600),
        label = "ready_gauge_anim"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = severity.color.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
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
                        imageVector = Icons.Default.Speed,
                        contentDescription = "CPU Ready Gauge",
                        tint = severity.color,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "VMware CPU Ready (%RDY)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = severity.color.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, severity.color.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = severity.title,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = severity.color,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Speedometer Arc
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(230.dp, 140.dp)
            ) {
                val gaugeBgColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                val primaryColor = MaterialTheme.colorScheme.primary

                Canvas(modifier = Modifier.size(220.dp, 130.dp)) {
                    val strokeWidth = 24.dp.toPx()
                    val diameter = min(size.width, size.height * 2) - strokeWidth
                    val arcSize = Size(diameter, diameter)
                    val topLeft = Offset((size.width - diameter) / 2f, strokeWidth / 2f)

                    val startAngle = 160f
                    val sweepAngle = 220f

                    // Background Track Arc
                    drawArc(
                        color = gaugeBgColor,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Color Segment: Optimal (0 - 2.5% -> maps to ~16% of 15% max display)
                    val maxScale = 15f
                    val fraction = (animatedPercent / maxScale).coerceIn(0f, 1f)
                    val activeSweep = sweepAngle * fraction

                    // Multi-stop gradient along active arc
                    val arcGradient = Brush.sweepGradient(
                        0.4f to StatusOptimal,
                        0.5f to StatusNormal,
                        0.65f to StatusWarning,
                        0.8f to StatusCritical
                    )

                    drawArc(
                        brush = arcGradient,
                        startAngle = startAngle,
                        sweepAngle = activeSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Target Needle Pin
                    val needleAngleRad = Math.toRadians((startAngle + activeSweep).toDouble())
                    val radius = diameter / 2f
                    val center = Offset(topLeft.x + radius, topLeft.y + radius)
                    val needleEnd = Offset(
                        center.x + (radius * cos(needleAngleRad)).toFloat(),
                        center.y + (radius * sin(needleAngleRad)).toFloat()
                    )

                    drawCircle(
                        color = Color.White,
                        radius = 7.dp.toPx(),
                        center = needleEnd
                    )
                    drawCircle(
                        color = severity.color,
                        radius = 4.dp.toPx(),
                        center = needleEnd
                    )
                }

                // Central Metric Readout
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 28.dp)
                ) {
                    Text(
                        text = "${String.format("%.2f", readyPercent)}%",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = severity.color
                    )
                    Text(
                        text = "Total %RDY",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sub-metrics (Per-vCPU RDY % and vCPU Count)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Per vCPU %RDY",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${String.format("%.2f", perVcpuPercent)}%",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = severity.color
                    )
                }

                Box(
                    modifier = Modifier
                        .size(1.dp, 28.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Configured vCPUs",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$vCpuCount vCPUs",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .size(1.dp, 28.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "ESXi Contention",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = severity.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = severity.color
                    )
                }
            }
        }
    }
}
