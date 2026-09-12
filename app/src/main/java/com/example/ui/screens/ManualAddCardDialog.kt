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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.CardType
import com.example.data.TagCategories
import com.example.ui.theme.CardGradients
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualAddCardDialog(
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        category: String,
        description: String,
        facility: String,
        type: CardType,
        uid: String,
        facilityCode: String,
        cardNumber: String,
        payload: String,
        notes: String,
        colorIndex: Int,
        armImmediately: Boolean
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(TagCategories.DEFAULT_CATEGORY) }
    var description by remember { mutableStateOf("") }
    var facility by remember { mutableStateOf("Corporate Facility") }
    var cardType by remember { mutableStateOf(CardType.ACCESS_BADGE) }
    var uidHex by remember { mutableStateOf("04:A1:B2:C3:D4:E5:80") }
    var facilityCode by remember { mutableStateOf("120") }
    var cardNumber by remember { mutableStateOf("30481") }
    var payload by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var colorIndex by remember { mutableIntStateOf(0) }
    var armImmediately by remember { mutableStateOf(true) }
    var typeDropdownExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.manual_add_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.btn_close), tint = Color.Gray)
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.field_name_req)) },
                placeholder = { Text(stringResource(R.string.field_name)) },
                modifier = Modifier.fillMaxWidth().testTag("manual_name_input"),
                colors = textFieldColors()
            )

            // Category selector & quick suggestion chips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text(stringResource(R.string.field_category)) },
                    placeholder = { Text(stringResource(R.string.field_category_hint)) },
                    modifier = Modifier.fillMaxWidth().testTag("manual_category_input"),
                    colors = textFieldColors()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
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
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.field_description)) },
                placeholder = { Text(stringResource(R.string.field_description_hint)) },
                modifier = Modifier.fillMaxWidth().testTag("manual_description_input"),
                maxLines = 3,
                colors = textFieldColors()
            )

            OutlinedTextField(
                value = facility,
                onValueChange = { facility = it },
                label = { Text(stringResource(R.string.field_facility)) },
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors()
            )

            ExposedDropdownMenuBox(
                expanded = typeDropdownExpanded,
                onExpandedChange = { typeDropdownExpanded = !typeDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = cardType.getLocalizedName(context),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.spec_card_type)) },
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

            OutlinedTextField(
                value = uidHex,
                onValueChange = { uidHex = it },
                label = { Text(stringResource(R.string.field_uid_req)) },
                placeholder = { Text(stringResource(R.string.label_uid)) },
                modifier = Modifier.fillMaxWidth().testTag("manual_uid_input"),
                colors = textFieldColors()
            )

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

            OutlinedTextField(
                value = payload,
                onValueChange = { payload = it },
                label = { Text(stringResource(R.string.field_token_payload)) },
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors()
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.field_notes)) },
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors()
            )

            // Color Picker
            Text(stringResource(R.string.field_theme), color = Color(0xFF94A3B8), fontSize = 12.sp)
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
                                width = if (colorIndex == index) 3.dp else 1.dp,
                                color = if (colorIndex == index) Color.White else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { colorIndex = index }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { armImmediately = !armImmediately },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = armImmediately,
                    onCheckedChange = { armImmediately = it },
                    colors = CheckboxDefaults.colors(checkedColor = CyanPrimary, checkmarkColor = Color(0xFF001E28))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.field_arm_immediately), color = Color.White, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            val defaultCardName = stringResource(R.string.default_card_name)
            val defaultFacility = stringResource(R.string.default_facility)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text(stringResource(R.string.btn_cancel), color = Color.White)
                }
                Button(
                    onClick = {
                        onSave(
                            name.ifBlank { defaultCardName },
                            category.ifBlank { TagCategories.DEFAULT_CATEGORY },
                            description,
                            facility.ifBlank { defaultFacility },
                            cardType,
                            uidHex.ifBlank { "04:A1:B2:C3:D4:E5:80" },
                            facilityCode,
                            cardNumber,
                            payload,
                            notes,
                            colorIndex,
                            armImmediately
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                    modifier = Modifier.weight(1f).height(48.dp).testTag("save_manual_button")
                ) {
                    Text(stringResource(R.string.btn_add_card), color = Color(0xFF001E28), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
