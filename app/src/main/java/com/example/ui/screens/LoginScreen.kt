package com.example.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.auth.AuthUiState
import com.example.auth.AuthUser
import com.example.auth.CloudSyncState
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.dialogs.LanguageSelectionDialog
import com.example.util.LanguageManager
import com.example.util.SupportedLanguage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LoginScreen(
    currentUser: AuthUser?,
    authState: AuthUiState,
    syncState: CloudSyncState,
    cardCount: Int,
    onSignInWithGoogle: (activity: Activity) -> Unit,
    onSignOut: () -> Unit,
    onSyncCards: () -> Unit,
    onRestoreCards: () -> Unit,
    onDismiss: () -> Unit,
    onUpdateFullName: (String) -> Unit = {},
    onRefreshProfile: () -> Unit = {},
    requireSignIn: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var tempFullName by remember { mutableStateOf("") }

    Surface(
        color = DarkBackground,
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Action Bar / Close & Language Quick Switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (requireSignIn && currentUser == null) stringResource(R.string.hce_sign_in_required_title) else stringResource(R.string.account_profile),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Quick language selection pill in the top bar
                        Surface(
                            onClick = { showLanguageDialog = true },
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.5f)),
                            modifier = Modifier.testTag("auth_language_picker_top_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = LanguageManager.currentLanguage.flag, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (LanguageManager.currentLanguage == SupportedLanguage.SYSTEM) "Auto" else LanguageManager.currentLanguage.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Language",
                                    tint = CyanPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        if (!requireSignIn || currentUser != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.testTag("auth_screen_close_button")
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Hero Cloud Badge / Google Logo
                Surface(
                    shape = CircleShape,
                    color = if (currentUser != null) CyanPrimary.copy(alpha = 0.12f) else Color.White,
                    border = BorderStroke(1.5.dp, if (currentUser != null) CyanPrimary.copy(alpha = 0.5f) else Color(0xFFE2E8F0)),
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (currentUser != null) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(42.dp)
                            )
                        } else {
                            Image(
                                painter = painterResource(R.drawable.img_google_logo_1789115356116),
                                contentDescription = "Google Logo",
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (currentUser != null) stringResource(R.string.account_tag_storage) else if (requireSignIn) stringResource(R.string.hce_sign_in_required_title) else stringResource(R.string.login_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (currentUser != null)
                        stringResource(R.string.account_tag_storage_desc)
                    else if (requireSignIn)
                        stringResource(R.string.hce_sign_in_required_desc)
                    else
                        stringResource(R.string.login_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Authenticated Profile Card vs Sign-In Section
                if (currentUser != null) {
                    UserProfileCard(
                        user = currentUser,
                        cardCount = cardCount,
                        syncState = syncState,
                        onSyncCards = onSyncCards,
                        onRestoreCards = onRestoreCards,
                        onSignOut = onSignOut,
                        onEditNameClick = {
                            tempFullName = currentUser.displayName ?: ""
                            showEditNameDialog = true
                        },
                        onRefreshProfileClick = onRefreshProfile,
                        onOpenLanguageSelector = { showLanguageDialog = true }
                    )
                } else {
                    SignInSection(
                        authState = authState,
                        requireSignIn = requireSignIn,
                        onSignInClick = {
                            activity?.let { onSignInWithGoogle(it) }
                        },
                        onContinueOffline = onDismiss,
                        onOpenLanguageSelector = { showLanguageDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Feature Highlights
                CloudFeatureHighlights()

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (showLanguageDialog) {
            LanguageSelectionDialog(
                onDismiss = { showLanguageDialog = false }
            )
        }

        if (showEditNameDialog) {
            AlertDialog(
                onDismissRequest = { showEditNameDialog = false },
                title = {
                    Text(
                        text = stringResource(R.string.edit_full_name_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = stringResource(R.string.edit_full_name_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF94A3B8)
                        )
                        OutlinedTextField(
                            value = tempFullName,
                            onValueChange = { tempFullName = it },
                            label = { Text(stringResource(R.string.full_name_label)) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanPrimary,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("full_name_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (tempFullName.isNotBlank()) {
                                onUpdateFullName(tempFullName)
                            }
                            showEditNameDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                        modifier = Modifier.testTag("save_full_name_confirm_btn")
                    ) {
                        Text(
                            text = stringResource(R.string.save_and_sync_btn),
                            color = Color(0xFF0A0E1A),
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showEditNameDialog = false },
                        modifier = Modifier.testTag("cancel_edit_full_name_btn")
                    ) {
                        Text(
                            text = stringResource(R.string.btn_cancel),
                            color = Color(0xFF94A3B8)
                        )
                    }
                },
                containerColor = DarkSurfaceCard,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
private fun SignInSection(
    authState: AuthUiState,
    requireSignIn: Boolean,
    onSignInClick: () -> Unit,
    onContinueOffline: () -> Unit,
    onOpenLanguageSelector: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Error Message Banner if any
            if (authState is AuthUiState.Error) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF450A0A)),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFEF4444)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = authState.message,
                        color = Color(0xFFFCA5A5),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Google Sign-In Button
            GoogleSignInButton(
                isLoading = authState is AuthUiState.Loading,
                onClick = onSignInClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("sign_in_with_google_button")
            )

            if (!requireSignIn) {
                Spacer(modifier = Modifier.height(14.dp))

                // Continue Offline Secondary Option
                OutlinedButton(
                    onClick = onContinueOffline,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("continue_offline_button")
                ) {
                    Text(
                        text = stringResource(R.string.btn_continue_offline),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Supported Languages selector for Sign-In Required page
            SupportedLanguagesSection(
                onOpenFullDialog = onOpenLanguageSelector
            )
        }
    }
}

@Composable
private fun GoogleSignInButton(
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color(0xFF1F1F1F)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = CyanPrimary,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.connecting_google),
                color = Color(0xFF1F1F1F),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                GoogleGLogo(modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.btn_sign_in_google),
                    color = Color(0xFF1F1F1F),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    letterSpacing = 0.2.sp
                )
            }
        }
    }
}

/**
 * Google 'G' logo using the uploaded/generated asset
 */
@Composable
fun GoogleGLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.img_google_logo_1789115356116),
        contentDescription = "Google Logo",
        modifier = modifier.clip(CircleShape),
        contentScale = ContentScale.Fit
    )
}

@Composable
private fun UserProfileCard(
    user: AuthUser,
    cardCount: Int,
    syncState: CloudSyncState,
    onSyncCards: () -> Unit,
    onRestoreCards: () -> Unit,
    onSignOut: () -> Unit,
    onEditNameClick: () -> Unit,
    onRefreshProfileClick: () -> Unit,
    onOpenLanguageSelector: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // User Avatar & Identity
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!user.photoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = user.photoUrl,
                        contentDescription = "User avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, CyanPrimary, CircleShape)
                    )
                } else {
                    Surface(
                        shape = CircleShape,
                        color = CyanPrimary.copy(alpha = 0.2f),
                        border = BorderStroke(1.5.dp, CyanPrimary),
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = (user.displayName ?: user.email ?: "U").take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = CyanPrimary,
                                fontSize = 22.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = user.displayName ?: stringResource(R.string.google_user),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        IconButton(
                            onClick = onEditNameClick,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("edit_full_name_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.edit_full_name_title),
                                tint = CyanPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = onRefreshProfileClick,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("refresh_profile_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.sync_google_profile_tooltip),
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = user.email ?: stringResource(R.string.signed_in_with_google),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = EmeraldSuccess.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(5.dp).background(EmeraldSuccess, CircleShape))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.cloud_sync_active),
                                color = EmeraldSuccess,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Storage Status Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.stored_nfc_tags_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.passes_tags_count, cardCount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    when (syncState) {
                        is CloudSyncState.Syncing -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = CyanPrimary
                            )
                        }
                        is CloudSyncState.Synced -> {
                            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(syncState.lastSyncedTime))
                            Column(horizontalAlignment = Alignment.End) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = EmeraldSuccess,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = stringResource(R.string.synced_at, timeStr),
                                    color = EmeraldSuccess,
                                    fontSize = 10.sp
                                )
                            }
                        }
                        is CloudSyncState.Error -> {
                            Text(
                                text = stringResource(R.string.sync_pending),
                                color = Color(0xFFF59E0B),
                                fontSize = 11.sp
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = CyanPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Real-Time Account Synchronization Info Banner
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = EmeraldSuccess.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SyncAlt,
                        contentDescription = null,
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.auto_sync_account_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA7F3D0),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sync Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Upload / Backup Cards Button
                Button(
                    onClick = onSyncCards,
                    enabled = syncState !is CloudSyncState.Syncing,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("backup_cards_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = Color(0xFF001E28),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.btn_sync_now),
                        color = Color(0xFF001E28),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // Restore Cards Button
                OutlinedButton(
                    onClick = onRestoreCards,
                    enabled = syncState !is CloudSyncState.Syncing,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("restore_cards_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.btn_restore),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Supported Languages in Profile
            SupportedLanguagesSection(
                onOpenFullDialog = onOpenLanguageSelector
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Sign Out Button
            OutlinedButton(
                onClick = onSignOut,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                border = BorderStroke(1.dp, Color(0xFF7F1D1D)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .testTag("sign_out_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.btn_sign_out),
                    color = Color(0xFFEF4444),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun SupportedLanguagesSection(
    onOpenFullDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentLang = LanguageManager.currentLanguage

    // Fast-access major languages representing global coverage
    val quickLanguages = listOf(
        SupportedLanguage.SYSTEM,
        SupportedLanguage.ENGLISH,
        SupportedLanguage.SPANISH,
        SupportedLanguage.FRENCH,
        SupportedLanguage.GERMAN,
        SupportedLanguage.MANDARIN,
        SupportedLanguage.JAPANESE,
        SupportedLanguage.HINDI,
        SupportedLanguage.PORTUGUESE,
        SupportedLanguage.ARABIC,
        SupportedLanguage.INDONESIAN,
        SupportedLanguage.RUSSIAN,
        SupportedLanguage.KOREAN,
        SupportedLanguage.ITALIAN,
        SupportedLanguage.TURKISH
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.supported_languages_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CyanPrimary.copy(alpha = 0.15f),
                    border = BorderStroke(0.8.dp, CyanPrimary.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = stringResource(R.string.supported_languages_count),
                        color = CyanPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick horizontal pills for immediate 1-tap switching
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(quickLanguages) { lang ->
                    val isSelected = currentLang == lang
                    Surface(
                        onClick = { LanguageManager.setLanguage(context, lang) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) CyanPrimary.copy(alpha = 0.22f) else Color(0xFF1E293B),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) CyanPrimary else Color(0xFF334155)
                        ),
                        modifier = Modifier.testTag("quick_lang_${lang.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = lang.flag, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (lang == SupportedLanguage.SYSTEM) stringResource(R.string.language_system_default) else lang.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) CyanPrimary else Color(0xFFE2E8F0),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Button to open full searchable dialog with all 26 supported languages
            OutlinedButton(
                onClick = onOpenFullDialog,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .testTag("view_all_languages_btn")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.browse_all_languages),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFCBD5E1)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CloudFeatureHighlights() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HighlightItem(
            icon = Icons.Default.Sensors,
            title = stringResource(R.string.highlight_digital_tag_title),
            description = stringResource(R.string.highlight_digital_tag_desc)
        )
        HighlightItem(
            icon = Icons.Default.Sync,
            title = stringResource(R.string.highlight_cloud_backup_title),
            description = stringResource(R.string.highlight_cloud_backup_desc)
        )
        HighlightItem(
            icon = Icons.Default.Security,
            title = stringResource(R.string.highlight_security_title),
            description = stringResource(R.string.highlight_security_desc)
        )
    }
}

@Composable
private fun HighlightItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = CyanPrimary.copy(alpha = 0.12f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
