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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.CardType
import com.example.data.TagCategories
import com.example.nfc.NfcTagData
import com.example.ui.components.NfcRadarPulse
import com.example.ui.theme.CardGradients
import com.example.ui.theme.CobaltSecondary
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.EmeraldSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    scannedTag: NfcTagData?,
    isNfcSupported: Boolean,
    isNfcEnabled: Boolean,
    onSaveCard: (
        name: String,
        category: String,
        description: String,
        facility: String,
        type: CardType,
        facilityCode: String,
        cardNumber: String,
        notes: String,
        colorIndex: Int,
        armImmediately: Boolean
    ) -> Unit,
    onClearScannedTag: () -> Unit,
    onSimulateScan: (Int) -> Unit,
    onManualAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Form state when tag is scanned
    var cardName by remember(scannedTag) {
        mutableStateOf(scannedTag?.let { "Access Pass (${it.uidHex.takeLast(5)})" } ?: "")
    }
    var category by remember(scannedTag) { mutableStateOf(TagCategories.DEFAULT_CATEGORY) }
    var description by remember(scannedTag) { mutableStateOf("") }
    var facilityName by remember(scannedTag) { mutableStateOf("Main Headquarters") }
    var cardType by remember(scannedTag) { mutableStateOf(scannedTag?.guessCardType() ?: CardType.ACCESS_BADGE) }
    var facilityCode by remember(scannedTag) { mutableStateOf("101") }
    var cardNumber by remember(scannedTag) { mutableStateOf("54201") }
    var notes by remember(scannedTag) { mutableStateOf("") }
    var colorIndex by remember(scannedTag) { mutableIntStateOf(0) }
    var armImmediately by remember(scannedTag) { mutableStateOf(true) }
    var typeDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hardware Status Indicator
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                if (isNfcEnabled) EmeraldSuccess else CyanPrimary,
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isNfcEnabled) stringResource(R.string.nfc_status_ready) else stringResource(R.string.nfc_status_simulated),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }

                OutlinedButton(
                    onClick = onManualAddClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanPrimary),
                    modifier = Modifier.testTag("manual_entry_top_button")
                ) {
                    Text(stringResource(R.string.btn_manual_entry), fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (scannedTag == null) {
            // Radar Animation & Scanning Instructions
            NfcRadarPulse(
                title = stringResource(R.string.scan_ready_title),
                subtitle = stringResource(R.string.scan_ready_subtitle),
                activeColor = CyanPrimary
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Simulation Helper Box (for testing in emulator or testing without tags)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.scan_sim_title),
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanPrimary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.scan_sim_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onSimulateScan(0) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth().testTag("simulate_desfire_button")
                        ) {
                            Text(stringResource(R.string.scan_sim_desfire), color = Color.White, fontSize = 13.sp)
                        }
                        Button(
                            onClick = { onSimulateScan(1) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth().testTag("simulate_mifare_button")
                        ) {
                            Text(stringResource(R.string.scan_sim_mifare), color = Color.White, fontSize = 13.sp)
                        }
                        Button(
                            onClick = { onSimulateScan(2) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth().testTag("simulate_ntag_button")
                        ) {
                            Text(stringResource(R.string.scan_sim_ntag), color = Color.White, fontSize = 13.sp)
                        }
                        Button(
                            onClick = { onSimulateScan(3) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth().testTag("simulate_felica_button")
                        ) {
                            Text(stringResource(R.string.scan_sim_felica), color = Color.White, fontSize = 13.sp)
                        }
                        Button(
                            onClick = { onSimulateScan(4) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth().testTag("simulate_calypso_button")
                        ) {
                            Text(stringResource(R.string.scan_sim_calypso), color = Color.White, fontSize = 13.sp)
                        }
                        Button(
                            onClick = { onSimulateScan(5) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth().testTag("simulate_emv_button")
                        ) {
                            Text(stringResource(R.string.scan_sim_emv), color = Color.White, fontSize = 13.sp)
                        }
                        Button(
                            onClick = { onSimulateScan(6) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth().testTag("simulate_amiibo_button")
                        ) {
                            Text(stringResource(R.string.scan_sim_amiibo), color = Color.White, fontSize = 13.sp)
                        }
                        Button(
                            onClick = { onSimulateScan(7) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth().testTag("simulate_hid_button")
                        ) {
                            Text(stringResource(R.string.scan_sim_hid), color = Color.White, fontSize = 13.sp)
                        }
                        Button(
                            onClick = { onSimulateScan(8) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth().testTag("simulate_student_button")
                        ) {
                            Text(stringResource(R.string.scan_sim_student), color = Color.White, fontSize = 13.sp)
                        }
                        Button(
                            onClick = { onSimulateScan(9) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth().testTag("simulate_hotel_button")
                        ) {
                            Text(stringResource(R.string.scan_sim_hotel), color = Color.White, fontSize = 13.sp)
                        }
                        Button(
                            onClick = { onSimulateScan(10) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth().testTag("simulate_vicinity_button")
                        ) {
                            Text(stringResource(R.string.scan_sim_vicinity), color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            }
        } else {
            // Tag Scanned View & Save Form
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, EmeraldSuccess.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(EmeraldSuccess.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = EmeraldSuccess,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.scan_tag_success),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldSuccess,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = scannedTag.chipTypeGuess,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        IconButton(onClick = onClearScannedTag) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.scan_btn_clear), tint = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Extracted Hardware Info Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0A0F1D), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(stringResource(R.string.label_uid), color = Color(0xFF94A3B8), fontSize = 12.sp)
                                Text(scannedTag.uidHex, color = CyanPrimary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(stringResource(R.string.label_tech_stack), color = Color(0xFF94A3B8), fontSize = 12.sp)
                                Text(scannedTag.techList.joinToString(", "), color = Color.White, fontSize = 11.sp)
                            }
                            if (scannedTag.ndefPayload.isNotBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(stringResource(R.string.label_payload), color = Color(0xFF94A3B8), fontSize = 12.sp)
                                    Text(scannedTag.ndefPayload, color = Color(0xFFE2E8F0), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.scan_configure_card),
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanPrimary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Input Fields
                    val context = LocalContext.current
                    OutlinedTextField(
                        value = cardName,
                        onValueChange = { cardName = it },
                        label = { Text(stringResource(R.string.field_name)) },
                        modifier = Modifier.fillMaxWidth().testTag("scan_save_name_input"),
                        colors = textFieldColors()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Category selection & suggestion chips
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text(stringResource(R.string.field_category)) },
                            placeholder = { Text(stringResource(R.string.field_category_hint)) },
                            modifier = Modifier.fillMaxWidth().testTag("scan_save_category_input"),
                            colors = textFieldColors()
                        )

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(TagCategories.DEFAULT_CATEGORIES) { cat ->
                                val isSelected = category.equals(cat, ignoreCase = true)
                                SuggestionChip(
                                    onClick = { category = cat },
                                    label = { Text(TagCategories.getLocalizedCategoryName(cat, context), fontSize = 11.sp) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = if (isSelected) CyanPrimary.copy(alpha = 0.2f) else Color(0xFF1E293B),
                                        labelColor = if (isSelected) CyanPrimary else Color(0xFF94A3B8)
                                    ),
                                    border = SuggestionChipDefaults.suggestionChipBorder(
                                        borderColor = if (isSelected) CyanPrimary else Color(0xFF334155),
                                        enabled = true
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(stringResource(R.string.field_description)) },
                        placeholder = { Text(stringResource(R.string.field_description_hint)) },
                        modifier = Modifier.fillMaxWidth().testTag("scan_save_description_input"),
                        maxLines = 3,
                        colors = textFieldColors()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = facilityName,
                        onValueChange = { facilityName = it },
                        label = { Text(stringResource(R.string.field_facility)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Category Dropdown
                    ExposedDropdownMenuBox(
                        expanded = typeDropdownExpanded,
                        onExpandedChange = { typeDropdownExpanded = !typeDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = cardType.getLocalizedName(context),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.field_category)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            colors = textFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = typeDropdownExpanded,
                            onDismissRequest = { typeDropdownExpanded = false },
                            modifier = Modifier.background(DarkSurfaceCard)
                        ) {
                            CardType.entries.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.getLocalizedName(context), color = Color.White) },
                                    onClick = {
                                        cardType = type
                                        typeDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = facilityCode,
                            onValueChange = { facilityCode = it },
                            label = { Text(stringResource(R.string.field_facility_code)) },
                            modifier = Modifier.weight(1f),
                            colors = textFieldColors()
                        )
                        OutlinedTextField(
                            value = cardNumber,
                            onValueChange = { cardNumber = it },
                            label = { Text(stringResource(R.string.field_card_number)) },
                            modifier = Modifier.weight(1f),
                            colors = textFieldColors()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Color theme selector
                    Text(
                        text = stringResource(R.string.field_theme),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        (0..5).forEach { index ->
                            val color = CardGradients.getAccentColor(index)
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (colorIndex == index) 3.dp else 1.dp,
                                        color = if (colorIndex == index) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { colorIndex = index }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Arm immediately toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { armImmediately = !armImmediately },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = armImmediately,
                            onCheckedChange = { armImmediately = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = CyanPrimary,
                                checkmarkColor = Color(0xFF001E28)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.field_arm_immediately),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Save Button
                    Button(
                        onClick = {
                            onSaveCard(
                                cardName,
                                category.ifBlank { TagCategories.DEFAULT_CATEGORY },
                                description,
                                facilityName,
                                cardType,
                                facilityCode,
                                cardNumber,
                                notes,
                                colorIndex,
                                armImmediately
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_scanned_card_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            tint = Color(0xFF001E28),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.scan_btn_save),
                            color = Color(0xFF001E28),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}
