package com.codeninjavik.myra.ui.forwarding

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codeninjavik.myra.ui.components.PermissionGate
import com.codeninjavik.myra.ui.components.PrimaryButton
import com.codeninjavik.myra.ui.components.SectionCard
import com.codeninjavik.myra.ui.theme.AmberThinking
import com.codeninjavik.myra.ui.theme.GreenConnected
import com.codeninjavik.myra.ui.theme.Hairline
import com.codeninjavik.myra.ui.theme.Ink
import com.codeninjavik.myra.ui.theme.Lifted
import com.codeninjavik.myra.ui.theme.RedAccent
import com.codeninjavik.myra.ui.theme.TextPrimary
import com.codeninjavik.myra.ui.theme.TextSecondary
import com.codeninjavik.myra.ui.theme.TextTertiary
import com.codeninjavik.myra.ui.theme.Surface

@Composable
fun ForwardingScreen(
    viewModel: ForwardingViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    PermissionGate(onPermissionsGranted = { }) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Ink
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Text(
                    text = "MYRA",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = RedAccent
                )
                Text(
                    text = "AI Receptionist Call Forwarding",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Error / Info Toast Replacement
                AnimatedVisibility(visible = state.errorMsg != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = RedAccent.copy(alpha = 0.1f)),
                        border = BorderStroke(1.dp, RedAccent)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = RedAccent)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = state.errorMsg ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.clearNotifications() }) {
                                Icon(Icons.Default.Call, contentDescription = "Clear", tint = TextSecondary)
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = state.infoMsg != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = GreenConnected.copy(alpha = 0.1f)),
                        border = BorderStroke(1.dp, GreenConnected)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = GreenConnected)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = state.infoMsg ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.clearNotifications() }) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Clear", tint = TextSecondary)
                            }
                        }
                    }
                }

                // SIM Selection Card
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SimCard, contentDescription = null, tint = TextSecondary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "1. Select SIM (Active Account)",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    if (state.availableSims.isEmpty()) {
                        Text(
                            text = "No SIM Cards detected or permission not granted. Using Default SIM.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextTertiary
                        )
                    } else {
                        state.availableSims.forEach { sim ->
                            val isSelected = state.selectedSim?.subscriptionId == sim.subscriptionId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.selectSim(sim) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.selectSim(sim) },
                                    colors = RadioButtonDefaults.colors(selectedColor = RedAccent)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "${sim.displayName} (${sim.carrierName})",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = TextPrimary
                                    )
                                    if (sim.number.isNotEmpty()) {
                                        Text(
                                            text = sim.number,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Forwarding Settings Mode Card
                SectionCard {
                    Text(
                        text = "2. Select Forwarding Type",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setForwardingMode(true) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (state.isUnconditional) Lifted else Surface
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (state.isUnconditional) RedAccent else Hairline
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "All Calls",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Unconditional (MYRA answers everything)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setForwardingMode(false) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (!state.isUnconditional) Lifted else Surface
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (!state.isUnconditional) RedAccent else Hairline
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Conditional",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "On busy, unanswered, or unreachable",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // User Number Input (For test-call verification)
                SectionCard {
                    Text(
                        text = "3. Enter Your Phone Number",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Used by our server to verify forwarding setup via a test call.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = state.userNumber,
                        onValueChange = { viewModel.setUserNumber(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Your Indian mobile (+91)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = RedAccent,
                            unfocusedBorderColor = Hairline
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Progressive Flow Stages Tracker
                Text(
                    text = "Enabling MYRA Setup Flow",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                StageRow(
                    index = 1,
                    title = "Dial Carrier Code",
                    description = "Places MMI command (**21* / **67*) to register the rules directly with carrier.",
                    isCurrent = state.stage == ForwardingSetupStage.IDLE,
                    isCompleted = state.stage.ordinal > ForwardingSetupStage.IDLE.ordinal,
                    actionText = "Dial Code",
                    onAction = { viewModel.startStage1Dial() },
                    enabled = !state.isProcessing
                )

                StageRow(
                    index = 2,
                    title = "Carrier Interrogation",
                    description = "Queries your SIM's radio status using USSD to retrieve routing registers.",
                    isCurrent = state.stage == ForwardingSetupStage.DIAL_MMI,
                    isCompleted = state.stage.ordinal > ForwardingSetupStage.DIAL_MMI.ordinal,
                    actionText = "Verify Radio",
                    onAction = { viewModel.startStage2AskCarrier() },
                    enabled = state.stage.ordinal >= ForwardingSetupStage.DIAL_MMI.ordinal && !state.isProcessing
                )

                StageRow(
                    index = 3,
                    title = "Verification Test Call",
                    description = "Our cloud backend makes a live test call to ensure routing lands on MYRA server.",
                    isCurrent = state.stage == ForwardingSetupStage.ASK_CARRIER || state.stage == ForwardingSetupStage.TEST_CALL,
                    isCompleted = state.stage == ForwardingSetupStage.ACTIVE,
                    actionText = "Place Test Call",
                    onAction = { viewModel.startStage3TestCall() },
                    enabled = state.stage.ordinal >= ForwardingSetupStage.ASK_CARRIER.ordinal && !state.isProcessing
                )

                StageRow(
                    index = 4,
                    title = "Active & Online",
                    description = "Rigorous verification completed. MYRA is fully active.",
                    isCurrent = state.stage == ForwardingSetupStage.ACTIVE,
                    isCompleted = state.stage == ForwardingSetupStage.ACTIVE,
                    actionText = if (state.isOn) "MYRA ACTIVE" else "INACTIVE",
                    onAction = {},
                    enabled = false
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Off Switch Safe Zone Safeguard
                SectionCard {
                    Text(
                        text = "Safety Off-Switch Safeguard",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = RedAccent
                    )
                    Text(
                        text = "If you ever choose to sign out or uninstall MYRA, make sure to erase carrier forwarding settings so calls route back to you normally.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Carrier Erase Code",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Dial ##21# manually anytime",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextTertiary
                            )
                        }

                        PrimaryButton(
                            text = "Turn OFF",
                            onClick = { viewModel.turnOffForwarding() },
                            modifier = Modifier.width(120.dp),
                            isSecondary = true,
                            enabled = !state.isProcessing
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StageRow(
    index: Int,
    title: String,
    description: String,
    isCurrent: Boolean,
    isCompleted: Boolean,
    actionText: String,
    onAction: () -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Status Ring indicator
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    color = if (isCompleted) GreenConnected else if (isCurrent) AmberThinking else Lifted,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Ink,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Text(
                    text = index.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrent) Ink else TextTertiary
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (isCurrent || isCompleted) TextPrimary else TextTertiary
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isCurrent || isCompleted) TextSecondary else TextTertiary
            )
            
            if (isCurrent && enabled) {
                Spacer(modifier = Modifier.height(12.dp))
                PrimaryButton(
                    text = actionText,
                    onClick = onAction,
                    modifier = Modifier.width(160.dp),
                    isSecondary = index > 1 // Stage 1 is major, others are confirmations
                )
            }
        }
    }
}
