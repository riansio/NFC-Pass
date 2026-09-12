package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Notes
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
import com.example.data.NfcCard
import com.example.data.TagCategories
import com.example.ui.components.DigitalCardItem
import com.example.ui.theme.CardGradients
import com.example.ui.theme.CobaltSecondary
import com.example.ui.theme.CoralError
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.EmeraldSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailDialog(
    card: NfcCard,
    onDismiss: () -> Unit,
    onArmForEmulation: () -> Unit,
    onDisarmEmulation: () -> Unit,
    onCloneToBlank: () -> Unit,
    onUpdateCard: (NfcCard) -> Unit,
    onDeleteCard: (NfcCard) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isEditing by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Edit fields
    var editName by remember(card) { mutableStateOf(card.name) }
    var editCategory by remember(card) { mutableStateOf(card.category) }
    var editDescription by remember(card) { mutableStateOf(card.description) }
    var editFacility by remember(card) { mutableStateOf(card.facilityName) }
    var editType by remember(card) { mutableStateOf(card.cardType) }
    var editFacilityCode by remember(card) { mutableStateOf(card.facilityCode) }
    var editCardNumber by remember(card) { mutableStateOf(card.cardNumber) }
    var editNotes by remember(card) { mutableStateOf(card.notes) }
    var editColorIndex by remember(card) { mutableIntStateOf(card.colorGradientIndex) }
    var typeDropdownExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditing) stringResource(R.string.dialog_title_edit) else stringResource(R.string.dialog_title_details),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.btn_close),
                        tint = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Card Preview
            DigitalCardItem(
                card = if (isEditing) card.copy(
                    name = editName,
                    category = editCategory,
                    description = editDescription,
                    facilityName = editFacility,
                    cardType = editType,
                    facilityCode = editFacilityCode,
                    cardNumber = editCardNumber,
                    colorGradientIndex = editColorIndex
                ) else card,
                onClick = {}
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (!isEditing) {
                // Category and Description Box
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.section_org),
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanPrimary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )

                            // Category badge
                            val catColor = TagCategories.getCategoryColor(card.category)
                            val catIcon = TagCategories.getCategoryIcon(card.category)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = catColor.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, catColor.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(catIcon, contentDescription = null, tint = catColor, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = TagCategories.getLocalizedCategoryName(card.category, context),
                                        color = catColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (card.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.spec_desc),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = card.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFE2E8F0),
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (card.isActiveVirtualCard) {
                        Button(
                            onClick = onDisarmEmulation,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("disarm_button")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.btn_cancel), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = onArmForEmulation,
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("arm_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sensors,
                                contentDescription = null,
                                tint = Color(0xFF001E28),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.btn_swipe), color = Color(0xFF001E28), fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = onCloneToBlank,
                        colors = ButtonDefaults.buttonColors(containerColor = CobaltSecondary),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("clone_to_blank_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_duplicate_tag), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Technical Specifications Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.section_tech_specs),
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanPrimary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        TechRow(stringResource(R.string.spec_uid), card.uidHex, isMonospace = true)
                        TechRow(stringResource(R.string.spec_category), TagCategories.getLocalizedCategoryName(card.category, context))
                        TechRow(stringResource(R.string.spec_card_type), card.cardType.getLocalizedName(context))
                        TechRow(stringResource(R.string.spec_atqa), card.atqaHex.ifBlank { "00:04" }, isMonospace = true)
                        TechRow(stringResource(R.string.spec_sak), card.sakHex.ifBlank { "08" }, isMonospace = true)
                        TechRow(stringResource(R.string.spec_tech), card.techList.joinToString(", ") { it.substringAfterLast(".") })
                        if (card.historicalBytesHex.isNotBlank()) {
                            TechRow(stringResource(R.string.spec_historical), card.historicalBytesHex, isMonospace = true)
                        }
                        if (card.ndefPayload.isNotBlank()) {
                            TechRow(stringResource(R.string.spec_ndef), card.ndefPayload, isMonospace = true)
                        }
                        if (card.description.isNotBlank()) {
                            TechRow(stringResource(R.string.spec_desc), card.description)
                        }
                        if (card.notes.isNotBlank()) {
                            TechRow(stringResource(R.string.spec_notes), card.notes)
                        }
                        TechRow(stringResource(R.string.spec_emulation_count), "${card.timesEmulated} " + stringResource(R.string.taps_recorded))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Secondary Actions: Edit, Delete
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { isEditing = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("edit_card_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_edit_details), color = CyanPrimary)
                    }

                    OutlinedButton(
                        onClick = { onDeleteCard(card) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralError),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("delete_card_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = CoralError, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_delete), color = CoralError)
                    }
                }
            } else {
                // Editing View
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text(stringResource(R.string.field_name)) },
                        modifier = Modifier.fillMaxWidth().testTag("edit_tag_name_input"),
                        colors = textFieldColors()
                    )

                    // Category with suggestions
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = editCategory,
                            onValueChange = { editCategory = it },
                            label = { Text(stringResource(R.string.field_category)) },
                            placeholder = { Text(stringResource(R.string.field_category_hint)) },
                            modifier = Modifier.fillMaxWidth().testTag("edit_tag_category_input"),
                            colors = textFieldColors()
                        )

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(TagCategories.DEFAULT_CATEGORIES) { cat ->
                                val isSelected = editCategory.equals(cat, ignoreCase = true)
                                SuggestionChip(
                                    onClick = { editCategory = cat },
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

                    OutlinedTextField(
                        value = editDescription,
                        onValueChange = { editDescription = it },
                        label = { Text(stringResource(R.string.field_description)) },
                        placeholder = { Text(stringResource(R.string.field_description_hint)) },
                        modifier = Modifier.fillMaxWidth().testTag("edit_tag_description_input"),
                        maxLines = 3,
                        colors = textFieldColors()
                    )

                    OutlinedTextField(
                        value = editFacility,
                        onValueChange = { editFacility = it },
                        label = { Text(stringResource(R.string.field_facility)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors()
                    )

                    // Type Dropdown
                    ExposedDropdownMenuBox(
                        expanded = typeDropdownExpanded,
                        onExpandedChange = { typeDropdownExpanded = !typeDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = editType.getLocalizedName(context),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.spec_card_type)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
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
                                        editType = type
                                        typeDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = editFacilityCode,
                            onValueChange = { editFacilityCode = it },
                            label = { Text(stringResource(R.string.field_facility_code)) },
                            modifier = Modifier.weight(1f),
                            colors = textFieldColors()
                        )
                        OutlinedTextField(
                            value = editCardNumber,
                            onValueChange = { editCardNumber = it },
                            label = { Text(stringResource(R.string.field_card_number)) },
                            modifier = Modifier.weight(1f),
                            colors = textFieldColors()
                        )
                    }

                    OutlinedTextField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        label = { Text(stringResource(R.string.field_usage_notes)) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        colors = textFieldColors()
                    )

                    // Color theme selector
                    Text(
                        text = stringResource(R.string.field_theme),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF94A3B8)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        (0..5).forEach { index ->
                            val color = CardGradients.getAccentColor(index)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (editColorIndex == index) 3.dp else 1.dp,
                                        color = if (editColorIndex == index) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { editColorIndex = index }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { isEditing = false },
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text(stringResource(R.string.btn_cancel), color = Color.White)
                        }
                        Button(
                            onClick = {
                                onUpdateCard(
                                    card.copy(
                                        name = editName.ifBlank { card.name },
                                        category = editCategory.ifBlank { TagCategories.DEFAULT_CATEGORY },
                                        description = editDescription,
                                        facilityName = editFacility.ifBlank { card.facilityName },
                                        cardType = editType,
                                        facilityCode = editFacilityCode,
                                        cardNumber = editCardNumber,
                                        notes = editNotes,
                                        colorGradientIndex = editColorIndex
                                    )
                                )
                                isEditing = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text(stringResource(R.string.btn_save_changes), color = Color(0xFF001E28), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun TechRow(label: String, value: String, isMonospace: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF94A3B8),
            fontSize = 12.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp
        )
    }
}

@Composable
fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CyanPrimary,
    unfocusedBorderColor = Color(0xFF2A3952),
    focusedLabelColor = CyanPrimary,
    unfocusedLabelColor = Color(0xFF94A3B8),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    cursorColor = CyanPrimary
)
