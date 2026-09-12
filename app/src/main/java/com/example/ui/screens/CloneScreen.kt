package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.NfcCard
import com.example.data.TagCategories
import com.example.nfc.WriteStatus
import com.example.ui.components.DigitalCardItem
import com.example.ui.components.NfcRadarPulse
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CobaltSecondary
import com.example.ui.theme.CoralError
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.EmeraldSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloneScreen(
    sourceCard: NfcCard?,
    allCards: List<NfcCard>,
    writeStatus: WriteStatus,
    onSelectCardToClone: (NfcCard) -> Unit,
    onResetWriteStatus: () -> Unit,
    onSimulateBlankTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showTagPickerSheet by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Banner: Strict Original Cloning Guarantee
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF071F17)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
                .testTag("strict_original_clone_banner")
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = EmeraldSuccess,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(top = 1.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = stringResource(R.string.clone_original_only_banner_title),
                        fontWeight = FontWeight.Bold,
                        color = EmeraldSuccess,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = stringResource(R.string.clone_original_only_banner_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB8E6D3),
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Step 1: Select Source Card to Duplicate
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.clone_source_title),
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanPrimary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )

                    if (allCards.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { showTagPickerSheet = true },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("switch_source_card_button")
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp), tint = CyanPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.clone_select_tag, allCards.size), fontSize = 12.sp, color = CyanPrimary)
                        }
                    }
                }

                // Quick Selector Chip Carousel for All Saved Tags
                if (allCards.size > 1) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.clone_quick_select),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allCards) { card ->
                            val isSelected = sourceCard?.id == card.id
                            val catColor = TagCategories.getCategoryColor(card.category)
                            val catIcon = TagCategories.getCategoryIcon(card.category)
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectCardToClone(card) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = catIcon,
                                        contentDescription = null,
                                        tint = if (isSelected) catColor else Color(0xFF94A3B8),
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                label = { Text(card.name, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = catColor.copy(alpha = 0.25f),
                                    selectedLabelColor = Color.White,
                                    labelColor = Color(0xFF94A3B8)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = Color(0xFF1E293B),
                                    selectedBorderColor = catColor,
                                    enabled = true,
                                    selected = isSelected
                                ),
                                modifier = Modifier.testTag("quick_select_card_${card.id}")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (sourceCard != null) {
                    OriginalPhysicalTagCard(
                        card = sourceCard,
                        onSwitchCard = { showTagPickerSheet = true }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.clone_no_card), color = Color.Gray)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Step 2: Blank Tag Writer Status
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
                Text(
                    text = stringResource(R.string.clone_target_title),
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanPrimary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                when (writeStatus) {
                    is WriteStatus.Idle, is WriteStatus.WaitingForTag -> {
                        NfcRadarPulse(
                            title = stringResource(R.string.clone_ready_blank_title),
                            subtitle = stringResource(R.string.clone_ready_blank_desc),
                            activeColor = CobaltSecondary,
                            centerIcon = Icons.Default.ContentCopy
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Supported blank tag types info
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0A0F1D), RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            TagTypePill("NTAG213 (144B)")
                            TagTypePill("NTAG215 (504B)")
                            TagTypePill("NTAG216 (888B)")
                            TagTypePill("Mifare Blank")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Test Simulator button for testing on emulator
                        Button(
                            onClick = onSimulateBlankTap,
                            colors = ButtonDefaults.buttonColors(containerColor = CobaltSecondary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("simulate_blank_write_button")
                        ) {
                            Icon(Icons.Default.Nfc, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.btn_simulate_blank), fontWeight = FontWeight.Bold)
                        }
                    }

                    is WriteStatus.Writing -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color = CyanPrimary,
                                    modifier = Modifier.size(54.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = stringResource(R.string.clone_writing_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.clone_writing_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }

                    is WriteStatus.Success -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF06291C), RoundedCornerShape(12.dp))
                                .border(1.dp, EmeraldSuccess, RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = stringResource(R.string.clone_success_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = writeStatus.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFB8E6D3),
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.clone_new_uid, writeStatus.tagUid),
                                style = MaterialTheme.typography.bodySmall,
                                color = CyanPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = onResetWriteStatus,
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                modifier = Modifier.fillMaxWidth().testTag("clone_another_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF002213))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.btn_clone_another), color = Color(0xFF002213), fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    is WriteStatus.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF2E0C10), RoundedCornerShape(12.dp))
                                .border(1.dp, CoralError, RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Error",
                                tint = CoralError,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = stringResource(R.string.clone_failed_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = writeStatus.errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFFB4B8),
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = onResetWriteStatus,
                                colors = ButtonDefaults.buttonColors(containerColor = CoralError),
                                modifier = Modifier.fillMaxWidth().testTag("retry_write_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.btn_try_again), color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Technical Guide Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = null,
                        tint = AmberWarning,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.clone_guide_title),
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberWarning,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.clone_guide_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showTagPickerSheet) {
        SavedTagPickerSheet(
            allCards = allCards,
            selectedCard = sourceCard,
            onSelectCard = { card ->
                onSelectCardToClone(card)
                showTagPickerSheet = false
            },
            onDismiss = { showTagPickerSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedTagPickerSheet(
    allCards: List<NfcCard>,
    selectedCard: NfcCard?,
    onSelectCard: (NfcCard) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val categories = remember(allCards) {
        allCards.map { it.category }.distinct().sorted()
    }

    val filteredCards = remember(allCards, searchQuery, selectedCategory) {
        allCards.filter { card ->
            val matchesCategory = selectedCategory == null || card.category.equals(selectedCategory, ignoreCase = true)
            val matchesQuery = searchQuery.isBlank() ||
                card.name.contains(searchQuery, ignoreCase = true) ||
                card.description.contains(searchQuery, ignoreCase = true) ||
                card.category.contains(searchQuery, ignoreCase = true) ||
                card.uidHex.contains(searchQuery, ignoreCase = true) ||
                card.facilityName.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

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
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.tag_picker_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.tag_picker_count, filteredCards.size, allCards.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.btn_close), tint = Color(0xFF94A3B8))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(stringResource(R.string.tag_picker_search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CyanPrimary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tag_picker_search_input"),
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Category filter chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text(stringResource(R.string.filter_all_count, allCards.size)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = CyanPrimary,
                            labelColor = Color(0xFF94A3B8)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = Color(0xFF1E293B),
                            selectedBorderColor = CyanPrimary,
                            enabled = true,
                            selected = selectedCategory == null
                        )
                    )
                }

                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    val catColor = TagCategories.getCategoryColor(cat)
                    val catIcon = TagCategories.getCategoryIcon(cat)
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = if (isSelected) null else cat },
                        leadingIcon = {
                            Icon(catIcon, contentDescription = null, tint = if (isSelected) catColor else Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                        },
                        label = { Text(TagCategories.getLocalizedCategoryName(cat, context)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = catColor.copy(alpha = 0.2f),
                            selectedLabelColor = catColor,
                            labelColor = Color(0xFF94A3B8)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = Color(0xFF1E293B),
                            selectedBorderColor = catColor,
                            enabled = true,
                            selected = isSelected
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tag List
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (filteredCards.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(stringResource(R.string.no_matching_tags), color = Color(0xFF64748B))
                        }
                    }
                } else {
                    items(filteredCards, key = { it.id }) { card ->
                        val isSelected = selectedCard?.id == card.id
                        val catColor = TagCategories.getCategoryColor(card.category)
                        val catIcon = TagCategories.getCategoryIcon(card.category)

                        Card(
                            onClick = { onSelectCard(card) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFF132F3D) else DarkSurfaceCard
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) CyanPrimary else Color(0xFF1E293B)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("tag_picker_item_${card.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = card.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = catColor.copy(alpha = 0.15f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(catIcon, contentDescription = null, tint = catColor, modifier = Modifier.size(11.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(TagCategories.getLocalizedCategoryName(card.category, context), color = catColor, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }

                                    if (card.description.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = card.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFCBD5E1),
                                            maxLines = 1
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Original UID: ${card.effectiveOriginalUidHex}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }

                                if (isSelected) {
                                    Surface(
                                        shape = CircleShape,
                                        color = CyanPrimary,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = stringResource(R.string.selected_desc),
                                                tint = Color(0xFF001E28),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OriginalPhysicalTagCard(
    card: NfcCard,
    onSwitchCard: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSwitchCard() }
            .testTag("original_physical_tag_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF091220)),
        border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Hardware Tag Status & Unmodified Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = CyanPrimary.copy(alpha = 0.15f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Sensors,
                                contentDescription = null,
                                tint = CyanPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.clone_source_original_title),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "Hardware Tag ID: ${card.effectiveOriginalUidHex}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = EmeraldSuccess.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "RAW HARDWARE ONLY",
                        color = EmeraldSuccess,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)

            // Primary Original Hardware UID
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF040A14), RoundedCornerShape(10.dp))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.clone_raw_uid_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Hardware Block 0",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = card.effectiveOriginalUidHex,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Original Payload if present (pure scanned raw payload only)
            if (card.effectiveOriginalPayload.isNotBlank() && !card.effectiveOriginalPayload.startsWith("NFC-PASS:", ignoreCase = true)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF040A14), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.clone_raw_payload_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = card.effectiveOriginalPayload,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color(0xFFE2E8F0),
                        lineHeight = 16.sp
                    )
                }
            }

            // Radio Technologies & Protocols
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tech: ${card.techList.joinToString(" • ") { it.substringAfterLast(".") }}",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
                Text(
                    text = "ATQA: ${card.atqaHex} | SAK: ${card.sakHex}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = Color(0xFF64748B)
                )
            }

            // Explicit Excluded Details Notice
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF161C26),
                border = BorderStroke(1.dp, Color(0xFF27354A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pure Hardware Clone: Columns for Category, Facility, Facility Code, and Card number are removed. Card name is not duplicated to the cloned card.",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.5.sp,
                        color = Color(0xFF94A3B8),
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TagTypePill(name: String) {
    Box(
        modifier = Modifier
            .background(Color(0xFF1E293B), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(name, color = CyanPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}
