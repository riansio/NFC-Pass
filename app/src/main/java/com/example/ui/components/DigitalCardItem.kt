package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.ui.theme.CardGradients

@Composable
fun DigitalCardItem(
    card: NfcCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onArmClick: (() -> Unit)? = null
) {
    val gradient = CardGradients.getGradient(card.colorGradientIndex)
    val accentColor = CardGradients.getAccentColor(card.colorGradientIndex)

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "borderGlow"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.586f)
            .shadow(
                elevation = if (card.isActiveVirtualCard) 12.dp else 4.dp,
                shape = RoundedCornerShape(18.dp),
                spotColor = if (card.isActiveVirtualCard) accentColor else Color.Black
            )
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = if (card.isActiveVirtualCard) 2.dp else 1.dp,
                color = if (card.isActiveVirtualCard) accentColor.copy(alpha = borderAlpha) else Color(0xFF2A3952),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
            .testTag("digital_card_${card.id}"),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
        ) {
            // Decorative background curves/watermark
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                // Holographic security pattern
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accentColor.copy(alpha = 0.15f), Color.Transparent),
                        center = Offset(w * 0.85f, h * 0.2f),
                        radius = w * 0.45f
                    ),
                    radius = w * 0.45f,
                    center = Offset(w * 0.85f, h * 0.2f)
                )
                // Diagonal accent lines
                drawLine(
                    color = Color.White.copy(alpha = 0.05f),
                    start = Offset(w * 0.4f, 0f),
                    end = Offset(w * 0.7f, h),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.04f),
                    start = Offset(w * 0.45f, 0f),
                    end = Offset(w * 0.75f, h),
                    strokeWidth = 1f
                )
            }

            // Foreground Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header: Facility & Status Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = TagCategories.getCategoryColor(card.category).copy(alpha = 0.25f),
                                border = androidx.compose.foundation.BorderStroke(0.7.dp, TagCategories.getCategoryColor(card.category))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = TagCategories.getCategoryIcon(card.category),
                                        contentDescription = null,
                                        tint = TagCategories.getCategoryColor(card.category),
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    val context = LocalContext.current
                                    Text(
                                        text = TagCategories.getLocalizedCategoryName(card.category, context).uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        letterSpacing = 0.8.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = card.facilityName.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor,
                                letterSpacing = 1.2.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                fontSize = 10.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = card.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    // Status Pill (Active HCE or Type Icon)
                    if (card.isActiveVirtualCard) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = accentColor.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, accentColor),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(accentColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.status_armed_hce),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accentColor,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 9.sp,
                                    letterSpacing = 0.8.sp
                                )
                            }
                        }
                    } else {
                        val context = LocalContext.current
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.1f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = getCardTypeIcon(card.cardType),
                                    contentDescription = card.cardType.getLocalizedName(context),
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Middle: Microchip Graphic + Contactless Wave Icon
                val context = LocalContext.current
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Realistic Smart Card Chip Graphic
                    SmartChipGraphic()

                    // NFC Contactless Waves
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = stringResource(R.string.contactless_payment_title),
                            tint = if (card.isActiveVirtualCard) accentColor else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = card.cardType.getLocalizedName(context),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp
                        )
                    }
                }

                // Footer: UID & Facility credentials
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.spec_uid).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 9.sp,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = card.uidHex,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.5.sp
                            )
                        }

                        if (card.cardNumber.isNotBlank()) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "FC: ${card.facilityCode.ifBlank { "00" }} / #${card.cardNumber}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SmartChipGraphic() {
    Canvas(
        modifier = Modifier
            .size(width = 38.dp, height = 28.dp)
            .shadow(2.dp, RoundedCornerShape(4.dp))
    ) {
        val w = size.width
        val h = size.height
        // Golden metallic chip base
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFFFD54F), Color(0xFFFFB300), Color(0xFFFFC107)),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            ),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )
        // Chip contact lines
        val stroke = Stroke(width = 1.2f)
        val lineColor = Color(0xFF5D4037).copy(alpha = 0.7f)
        // Middle horizontal division
        drawLine(lineColor, Offset(0f, h * 0.5f), Offset(w, h * 0.5f), strokeWidth = 1.2f)
        // Vertical divisions
        drawLine(lineColor, Offset(w * 0.35f, 0f), Offset(w * 0.35f, h), strokeWidth = 1.2f)
        drawLine(lineColor, Offset(w * 0.65f, 0f), Offset(w * 0.65f, h), strokeWidth = 1.2f)
        // Center pad
        drawRoundRect(
            color = Color(0xFFFFE082),
            topLeft = Offset(w * 0.35f, h * 0.25f),
            size = Size(w * 0.3f, h * 0.5f),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
            style = stroke
        )
    }
}

fun getCardTypeIcon(type: CardType) = when (type) {
    CardType.ACCESS_BADGE -> Icons.Default.Badge
    CardType.MIFARE_CLASSIC -> Icons.Default.Memory
    CardType.MIFARE_DESFIRE -> Icons.Default.VpnKey
    CardType.MIFARE_ULTRALIGHT -> Icons.Default.Nfc
    CardType.DOOR_KEY -> Icons.Default.VpnKey
    CardType.HOTEL_KEY -> Icons.Default.Key
    CardType.TRANSIT_PASS -> Icons.Default.DirectionsTransit
    CardType.FELICA_TRANSIT -> Icons.Default.DirectionsTransit
    CardType.PAYMENT_CARD -> Icons.Default.Sensors
    CardType.CAMPUS_STUDENT_ID -> Icons.Default.Badge
    CardType.ISO15693_VICINITY -> Icons.Default.Memory
    CardType.AMIIBO_GAMING -> Icons.Default.Sensors
    CardType.GOV_EID_PASSPORT -> Icons.Default.Badge
    CardType.PARKING_TOLL_PASS -> Icons.Default.DirectionsTransit
    CardType.GYM_MEMBERSHIP -> Icons.Default.FitnessCenter
    CardType.SMART_POSTER -> Icons.Default.Memory
    CardType.GENERIC_TAG -> Icons.Default.Nfc
}
