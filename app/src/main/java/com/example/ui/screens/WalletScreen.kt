package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.CardType
import com.example.data.NfcCard
import com.example.data.TagCategories
import com.example.ui.components.DigitalCardItem
import com.example.ui.theme.CobaltSecondary
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.EmeraldSuccess

@Composable
fun WalletScreen(
    cards: List<NfcCard>,
    activeCard: NfcCard?,
    searchQuery: String,
    selectedFilter: CardType?,
    selectedCategory: String?,
    availableCategories: List<String>,
    onSearchChange: (String) -> Unit,
    onFilterChange: (CardType?) -> Unit,
    onCategoryChange: (String?) -> Unit,
    onCardClick: (NfcCard) -> Unit,
    onArmCard: (NfcCard) -> Unit,
    onDisarm: () -> Unit,
    onSelectForClone: (NfcCard) -> Unit,
    onNavigateScan: () -> Unit,
    onShowManualAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSearchExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().background(DarkBackground)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Status & Active Virtual Card Banner
            item {
                ActiveCardBanner(
                    activeCard = activeCard,
                    onDisarm = onDisarm,
                    onArmDefault = {
                        cards.firstOrNull()?.let { onArmCard(it) }
                    }
                )
            }

            // Search and Category Filter Bar
            item {
                val context = LocalContext.current
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${stringResource(R.string.wallet_title_stored)} (${cards.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        IconButton(
                            onClick = { isSearchExpanded = !isSearchExpanded },
                            modifier = Modifier.testTag("toggle_search_button")
                        ) {
                            Icon(
                                imageVector = if (isSearchExpanded) Icons.Default.Clear else Icons.Default.Search,
                                contentDescription = "Search cards",
                                tint = CyanPrimary
                            )
                        }
                    }

                    AnimatedVisibility(visible = isSearchExpanded) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchChange,
                            placeholder = { Text(stringResource(R.string.wallet_search_placeholder)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CyanPrimary) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchChange("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("card_search_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors()
                        )
                    }

                    // Category Filter Chips
                    Text(
                        text = stringResource(R.string.label_categories),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedCategory == null,
                                onClick = { onCategoryChange(null) },
                                label = { Text(stringResource(R.string.filter_all_categories)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanPrimary.copy(alpha = 0.2f),
                                    selectedLabelColor = CyanPrimary,
                                    labelColor = Color(0xFF94A3B8)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = Color(0xFF233552),
                                    selectedBorderColor = CyanPrimary,
                                    enabled = true,
                                    selected = selectedCategory == null
                                ),
                                modifier = Modifier.testTag("filter_category_all")
                            )
                        }

                        items(availableCategories) { cat ->
                            val isSelected = selectedCategory == cat
                            val catColor = TagCategories.getCategoryColor(cat)
                            val catIcon = TagCategories.getCategoryIcon(cat)
                            FilterChip(
                                selected = isSelected,
                                onClick = { onCategoryChange(if (isSelected) null else cat) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = catIcon,
                                        contentDescription = null,
                                        tint = if (isSelected) catColor else Color(0xFF94A3B8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text(TagCategories.getLocalizedCategoryName(cat, context)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = catColor.copy(alpha = 0.2f),
                                    selectedLabelColor = catColor,
                                    labelColor = Color(0xFF94A3B8)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = Color(0xFF233552),
                                    selectedBorderColor = catColor,
                                    enabled = true,
                                    selected = isSelected
                                ),
                                modifier = Modifier.testTag("filter_category_${cat.replace(" ", "_").lowercase()}")
                            )
                        }
                    }

                    // Card Type Filter chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedFilter == null,
                                onClick = { onFilterChange(null) },
                                label = { Text(stringResource(R.string.filter_all_types)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CobaltSecondary.copy(alpha = 0.25f),
                                    selectedLabelColor = CobaltSecondary,
                                    labelColor = Color(0xFF64748B)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = Color(0xFF1E293B),
                                    selectedBorderColor = CobaltSecondary,
                                    enabled = true,
                                    selected = selectedFilter == null
                                )
                            )
                        }
                        items(CardType.entries.toTypedArray()) { type ->
                            FilterChip(
                                selected = selectedFilter == type,
                                onClick = { onFilterChange(if (selectedFilter == type) null else type) },
                                label = { Text(type.getLocalizedName(context)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CobaltSecondary.copy(alpha = 0.25f),
                                    selectedLabelColor = CobaltSecondary,
                                    labelColor = Color(0xFF64748B)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = Color(0xFF1E293B),
                                    selectedBorderColor = CobaltSecondary,
                                    enabled = true,
                                    selected = selectedFilter == type
                                )
                            )
                        }
                    }
                }
            }

            // Cards List
            if (cards.isEmpty()) {
                item {
                    EmptyCardsView(
                        onScanClick = onNavigateScan,
                        onManualClick = onShowManualAdd
                    )
                }
            } else {
                items(cards, key = { it.id }) { card ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurfaceCard, RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        // Digital Card Visual Graphic
                        DigitalCardItem(
                            card = card,
                            onClick = { onCardClick(card) },
                            onArmClick = { onArmCard(card) }
                        )

                        // Tag Description & Direct Quick Action Row
                        if (card.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = card.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8),
                                maxLines = 2,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (card.isActiveVirtualCard) {
                                Button(
                                    onClick = onDisarm,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1.1f)
                                        .height(38.dp)
                                        .testTag("card_cancel_button_${card.id}")
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.btn_cancel), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = { onArmCard(card) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1.1f)
                                        .height(38.dp)
                                        .testTag("card_swipe_button_${card.id}")
                                ) {
                                    Icon(Icons.Default.Sensors, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF001E28))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.btn_swipe), color = Color(0xFF001E28), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Quick Duplicate to Blank Button
                            Button(
                                onClick = { onSelectForClone(card) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CobaltSecondary
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("duplicate_tag_button_${card.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.btn_duplicate_tag), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // View / Edit Details Button
                            OutlinedButton(
                                onClick = { onCardClick(card) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanPrimary),
                                modifier = Modifier
                                    .height(38.dp)
                                    .testTag("card_details_button_${card.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = CyanPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = onNavigateScan,
            containerColor = CyanPrimary,
            contentColor = Color(0xFF001E28),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("scan_fab")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.Sensors, contentDescription = stringResource(R.string.btn_scan_tag))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.btn_scan_tag), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ActiveCardBanner(
    activeCard: NfcCard?,
    onDisarm: () -> Unit,
    onArmDefault: () -> Unit
) {
    if (activeCard != null) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, EmeraldSuccess.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF06281B)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(EmeraldSuccess.copy(alpha = 0.2f), CircleShape)
                            .border(1.dp, EmeraldSuccess, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = null,
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(EmeraldSuccess, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.hce_transmitter_armed),
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldSuccess,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = activeCard.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "UID: ${activeCard.uidHex}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }

                Button(
                    onClick = onDisarm,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("banner_disarm_button")
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.btn_cancel), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    } else {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color.White.copy(alpha = 0.06f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Nfc,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.hce_transmitter_inactive),
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFFCBD5E1),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.hce_transmitter_inactive_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B),
                            fontSize = 11.sp
                        )
                    }
                }

                Button(
                    onClick = onArmDefault,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("banner_arm_button")
                ) {
                    Icon(Icons.Default.Sensors, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF001E28))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.btn_swipe), color = Color(0xFF001E28), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun EmptyCardsView(
    onScanClick: () -> Unit,
    onManualClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(CyanPrimary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Nfc,
                    contentDescription = null,
                    tint = CyanPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.wallet_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.wallet_empty_desc),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onScanClick,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                ) {
                    Text(stringResource(R.string.btn_scan_tag), color = Color(0xFF001E28), fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onManualClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                ) {
                    Text(stringResource(R.string.btn_manual_entry), color = Color.White)
                }
            }
        }
    }
}
