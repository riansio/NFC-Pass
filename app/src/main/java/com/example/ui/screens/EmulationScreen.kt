package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.NfcCard
import com.example.hce.ApduLogEntry
import com.example.ui.components.ApduLogView
import com.example.ui.components.DigitalCardItem
import com.example.ui.components.NfcRadarPulse
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.EmeraldSuccess

@Composable
fun EmulationScreen(
    activeCard: NfcCard?,
    apduLogs: List<ApduLogEntry>,
    onDisarm: () -> Unit,
    onSimulateReaderTap: () -> Unit,
    onClearLogs: () -> Unit,
    onNavigateWallet: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isLogsExpanded by remember { mutableStateOf(apduLogs.isNotEmpty()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // HCE Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (activeCard != null) {
                    NfcRadarPulse(
                        title = stringResource(R.string.emulate_armed_title),
                        subtitle = stringResource(R.string.emulate_armed_subtitle),
                        activeColor = EmeraldSuccess,
                        centerIcon = Icons.Default.Sensors
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    DigitalCardItem(
                        card = activeCard,
                        onClick = {}
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onSimulateReaderTap,
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("simulate_reader_tap_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF001E28))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.btn_test_reader_tap), color = Color(0xFF001E28), fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onDisarm,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("disarm_emulation_button")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.btn_cancel), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    NfcRadarPulse(
                        title = stringResource(R.string.emulate_disarmed_title),
                        subtitle = stringResource(R.string.emulate_disarmed_subtitle),
                        activeColor = Color(0xFF64748B),
                        centerIcon = Icons.Default.Security,
                        isPulsing = false
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onNavigateWallet,
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("choose_card_to_arm_button")
                    ) {
                        Icon(Icons.Default.Sensors, contentDescription = null, tint = Color(0xFF001E28))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_swipe), color = Color(0xFF001E28), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Simplified APDU Traffic Log (collapsible)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isLogsExpanded = !isLogsExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.emulate_log_title),
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanPrimary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = if (apduLogs.isEmpty()) "No traffic recorded yet" else "${apduLogs.size} APDU events recorded",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B),
                            fontSize = 11.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (apduLogs.isNotEmpty()) {
                            IconButton(onClick = onClearLogs) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.btn_clear_logs), tint = Color.Gray)
                            }
                        }
                        IconButton(onClick = { isLogsExpanded = !isLogsExpanded }) {
                            Icon(
                                imageVector = if (isLogsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle logs",
                                tint = Color.Gray
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = isLogsExpanded) {
                    Column {
                        Spacer(modifier = Modifier.height(10.dp))
                        ApduLogView(
                            logs = apduLogs,
                            modifier = Modifier.height(200.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // System Info & How HCE Works (clean & simplified)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.emulate_arch_title),
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanPrimary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.emulate_arch_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
