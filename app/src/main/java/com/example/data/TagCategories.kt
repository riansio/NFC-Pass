package com.example.data

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.R

object TagCategories {
    val DEFAULT_CATEGORIES = listOf(
        "Office & Work",
        "Home & Smart Lock",
        "Transit & Travel",
        "Gym & Fitness",
        "Smart Tags & IoT",
        "Personal Keys"
    )

    const val DEFAULT_CATEGORY = "Office & Work"

    fun getCategoryIcon(category: String): ImageVector {
        val lower = category.lowercase()
        return when {
            lower.contains("office") || lower.contains("work") || lower.contains("badge") -> Icons.Default.Business
            lower.contains("home") || lower.contains("house") || lower.contains("garage") -> Icons.Default.Home
            lower.contains("transit") || lower.contains("metro") || lower.contains("bus") -> Icons.Default.DirectionsTransit
            lower.contains("gym") || lower.contains("fitness") -> Icons.Default.FitnessCenter
            lower.contains("lock") || lower.contains("key") -> Icons.Default.VpnKey
            lower.contains("smart") || lower.contains("iot") || lower.contains("ndef") -> Icons.Default.Nfc
            else -> Icons.Default.Tag
        }
    }

    fun getCategoryColor(category: String): Color {
        val lower = category.lowercase()
        return when {
            lower.contains("office") || lower.contains("work") -> Color(0xFF00E5FF) // Cyan
            lower.contains("home") || lower.contains("house") -> Color(0xFF00E676) // Emerald Green
            lower.contains("transit") || lower.contains("metro") -> Color(0xFFFFB300) // Amber
            lower.contains("gym") || lower.contains("fitness") -> Color(0xFFFF5252) // Coral Red
            lower.contains("smart") || lower.contains("iot") -> Color(0xFFE040FB) // Purple
            lower.contains("key") -> Color(0xFF448AFF) // Blue
            else -> Color(0xFF38BDF8) // Sky Blue
        }
    }

    fun getLocalizedCategoryName(category: String, context: Context): String {
        val lower = category.lowercase()
        return when {
            lower.contains("office") || lower.contains("work") || lower.contains("badge") -> context.getString(R.string.cat_work)
            lower.contains("home") || lower.contains("house") || lower.contains("garage") -> context.getString(R.string.cat_home)
            lower.contains("transit") || lower.contains("metro") || lower.contains("bus") -> context.getString(R.string.cat_transit)
            lower.contains("gym") || lower.contains("fitness") -> context.getString(R.string.cat_gym)
            lower.contains("smart") || lower.contains("iot") || lower.contains("ndef") -> context.getString(R.string.cat_smart_tag)
            lower.contains("key") || lower.contains("lock") -> context.getString(R.string.cat_keys)
            else -> category
        }
    }
}
