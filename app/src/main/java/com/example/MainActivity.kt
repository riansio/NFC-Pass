package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.AppDatabase
import com.example.data.CardRepository
import com.example.nfc.NfcController
import com.example.nfc.NfcHardwareState
import com.example.ui.MainTab
import com.example.ui.NfcViewModel
import com.example.ui.components.NfcHardwareBanner
import com.example.ui.dialogs.LanguageSelectionDialog
import com.example.ui.dialogs.NfcHardwarePermissionDialog
import com.example.ui.screens.CardDetailDialog
import com.example.ui.screens.CloneScreen
import com.example.ui.screens.EmulationScreen
import com.example.ui.screens.GoogleGLogo
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.ManualAddCardDialog
import com.example.ui.screens.ScanScreen
import com.example.ui.screens.WalletScreen
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CobaltSecondary
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MyApplicationTheme
import com.example.util.LanguageManager
import com.example.util.ProvideAppLanguage
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var nfcController: NfcController

    private val nfcStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == NfcAdapter.ACTION_ADAPTER_STATE_CHANGED) {
                nfcController.refreshHardwareState()
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        val savedLang = LanguageManager.getSavedLanguage(newBase)
        val localized = LanguageManager.getLocalizedContext(newBase, savedLang)
        super.attachBaseContext(localized)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LanguageManager.init(this)
        enableEdgeToEdge()

        val database = AppDatabase.getInstance(this)
        val repository = CardRepository(database.nfcCardDao())
        nfcController = NfcController(this)

        setContent {
            ProvideAppLanguage {
                MyApplicationTheme {
                    val viewModel: NfcViewModel = viewModel(
                        factory = NfcViewModel.Factory(application, repository, nfcController)
                    )
                    NfcPassApp(viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        nfcController.enableReader(this)
        nfcController.refreshHardwareState()
        try {
            registerReceiver(nfcStateReceiver, IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED))
        } catch (_: Exception) {}
    }

    override fun onPause() {
        super.onPause()
        nfcController.disableReader(this)
        try {
            unregisterReceiver(nfcStateReceiver)
        } catch (_: Exception) {}
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_TECH_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action
        ) {
            @Suppress("DEPRECATION")
            val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            tag?.let { nfcController.onTagDiscovered(it) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NfcPassApp(viewModel: NfcViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val allCards by viewModel.allCards.collectAsStateWithLifecycle()
    val filteredCards by viewModel.filteredCards.collectAsStateWithLifecycle()
    val activeVirtualCard by viewModel.activeVirtualCard.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val typeFilter by viewModel.typeFilter.collectAsStateWithLifecycle()
    val categoryFilter by viewModel.categoryFilter.collectAsStateWithLifecycle()
    val availableCategories by viewModel.availableCategories.collectAsStateWithLifecycle()
    val selectedCardForDetail by viewModel.selectedCardForDetail.collectAsStateWithLifecycle()
    val selectedCardForClone by viewModel.selectedCardForClone.collectAsStateWithLifecycle()
    val showManualAddDialog by viewModel.showManualAddDialog.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.bannerMessage.collectAsStateWithLifecycle()

    val scannedTag by viewModel.lastScannedTag.collectAsStateWithLifecycle()
    val writeStatus by viewModel.writeStatus.collectAsStateWithLifecycle()
    val apduLogs by viewModel.apduLogs.collectAsStateWithLifecycle()

    val hardwareState by viewModel.hardwareState.collectAsStateWithLifecycle()
    val showHardwareDialog by viewModel.showHardwareDialog.collectAsStateWithLifecycle()
    val showLanguageDialog by viewModel.showLanguageDialog.collectAsStateWithLifecycle()

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val authUiState by viewModel.authUiState.collectAsStateWithLifecycle()
    val cloudSyncState by viewModel.cloudSyncState.collectAsStateWithLifecycle()
    val showLoginScreen by viewModel.showLoginScreen.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(bannerMessage) {
        bannerMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.clearBannerMessage()
        }
    }

    // STRICT "REQUIRE SIGN IN" GATE
    if (currentUser == null) {
        LoginScreen(
            currentUser = null,
            authState = authUiState,
            syncState = cloudSyncState,
            cardCount = allCards.size,
            onSignInWithGoogle = { activity ->
                viewModel.signInWithGoogle(activity)
            },
            onSignInWithEmail = { email, password ->
                viewModel.signInWithEmail(email, password)
            },
            onSignUpWithEmail = { email, password, name ->
                viewModel.signUpWithEmail(email, password, name)
            },
            onPasswordReset = { email ->
                viewModel.sendPasswordReset(email)
            },
            onSignOut = { viewModel.signOut() },
            onSyncCards = { viewModel.syncCardsToAccount() },
            onRestoreCards = { viewModel.restoreCardsFromAccount() },
            onUpdateFullName = { newName -> viewModel.updateFullName(newName) },
            onRefreshProfile = { viewModel.refreshUserProfile() },
            onDismiss = { /* Non-dismissable: Sign in is strictly required */ },
            requireSignIn = true
        )
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = CyanPrimary.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Sensors,
                                    contentDescription = null,
                                    tint = CyanPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.app_name).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 2.sp
                        )
                    }
                },
                navigationIcon = {
                    // NFC Hardware Status Pill in Top Bar
                    val (pillBg, pillDot, pillText) = when (hardwareState) {
                        NfcHardwareState.READY -> Triple(
                            EmeraldSuccess.copy(alpha = 0.15f),
                            EmeraldSuccess,
                            stringResource(R.string.nfc_ready_badge)
                        )
                        NfcHardwareState.DISABLED -> Triple(
                            AmberWarning.copy(alpha = 0.2f),
                            AmberWarning,
                            stringResource(R.string.nfc_off_badge)
                        )
                        NfcHardwareState.UNSUPPORTED -> Triple(
                            Color(0xFF38BDF8).copy(alpha = 0.15f),
                            Color(0xFF38BDF8),
                            stringResource(R.string.status_simulated)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = pillBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, pillDot.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .clickable { viewModel.setShowHardwareDialog(true) }
                            .testTag("topbar_nfc_status_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(pillDot, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = pillText,
                                color = pillDot,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                },
                actions = {
                    // Google Account / Cloud Sync Avatar Button
                    IconButton(
                        onClick = { viewModel.setShowLoginScreen(true) },
                        modifier = Modifier.testTag("topbar_account_btn")
                    ) {
                        if (currentUser?.photoUrl != null) {
                            AsyncImage(
                                model = currentUser?.photoUrl,
                                contentDescription = "User account",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                            )
                        } else if (currentUser != null) {
                            Surface(
                                shape = CircleShape,
                                color = CyanPrimary.copy(alpha = 0.25f),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = (currentUser?.displayName ?: "U").take(1).uppercase(),
                                        color = CyanPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        } else {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF1E293B),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    GoogleGLogo(modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    // Language Switcher Icon
                    IconButton(
                        onClick = { viewModel.setShowLanguageDialog(true) },
                        modifier = Modifier.testTag("topbar_language_btn")
                    ) {
                        Text(
                            text = LanguageManager.currentLanguage.flag,
                            fontSize = 18.sp
                        )
                    }

                    // Active HCE beacon pill in top bar
                    if (activeVirtualCard != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = EmeraldSuccess.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(EmeraldSuccess, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.hce_live),
                                    color = EmeraldSuccess,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = DarkSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("main_navigation_bar")
            ) {
                NavigationBarItem(
                    selected = currentTab == MainTab.WALLET,
                    onClick = { viewModel.selectTab(MainTab.WALLET) },
                    icon = { Icon(Icons.Default.CreditCard, contentDescription = stringResource(R.string.tab_wallet)) },
                    label = { Text(stringResource(R.string.tab_wallet)) },
                    colors = navigationItemColors(),
                    modifier = Modifier.testTag("nav_wallet")
                )

                NavigationBarItem(
                    selected = currentTab == MainTab.SCAN,
                    onClick = { viewModel.selectTab(MainTab.SCAN) },
                    icon = { Icon(Icons.Default.Sensors, contentDescription = stringResource(R.string.tab_scan)) },
                    label = { Text(stringResource(R.string.tab_scan)) },
                    colors = navigationItemColors(),
                    modifier = Modifier.testTag("nav_scan")
                )

                NavigationBarItem(
                    selected = currentTab == MainTab.DUPLICATE,
                    onClick = { viewModel.selectTab(MainTab.DUPLICATE) },
                    icon = { Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.tab_duplicate)) },
                    label = { Text(stringResource(R.string.tab_duplicate)) },
                    colors = navigationItemColors(),
                    modifier = Modifier.testTag("nav_duplicate")
                )

                NavigationBarItem(
                    selected = currentTab == MainTab.EMULATE,
                    onClick = { viewModel.selectTab(MainTab.EMULATE) },
                    icon = { Icon(Icons.Default.Security, contentDescription = stringResource(R.string.tab_emulate)) },
                    label = { Text(stringResource(R.string.tab_emulate)) },
                    colors = navigationItemColors(),
                    modifier = Modifier.testTag("nav_emulate")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Hardware Status Banner (shown when NFC is disabled or unsupported)
            NfcHardwareBanner(
                hardwareState = hardwareState,
                onOpenSettings = { viewModel.openNfcSettings() },
                onRefresh = { viewModel.refreshHardwareState() }
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "tabTransition"
                ) { tab ->
                    when (tab) {
                        MainTab.WALLET -> {
                            WalletScreen(
                                cards = filteredCards,
                                activeCard = activeVirtualCard,
                                searchQuery = searchQuery,
                                selectedFilter = typeFilter,
                                selectedCategory = categoryFilter,
                                availableCategories = availableCategories,
                                onSearchChange = { viewModel.setSearchQuery(it) },
                                onFilterChange = { viewModel.setTypeFilter(it) },
                                onCategoryChange = { viewModel.setCategoryFilter(it) },
                                onCardClick = { viewModel.selectCardForDetail(it) },
                                onArmCard = { viewModel.armCardForEmulation(it) },
                                onDisarm = { viewModel.disarmEmulation() },
                                onSelectForClone = {
                                    viewModel.setCardForCloneSelection(it)
                                    viewModel.selectTab(MainTab.DUPLICATE)
                                },
                                onNavigateScan = { viewModel.selectTab(MainTab.SCAN) },
                                onShowManualAdd = { viewModel.setShowManualAdd(true) }
                            )
                        }

                        MainTab.SCAN -> {
                            ScanScreen(
                                scannedTag = scannedTag,
                                isNfcSupported = viewModel.nfcController.isSupported,
                                isNfcEnabled = viewModel.nfcController.isEnabled,
                                onSaveCard = { name, category, description, facility, type, fc, num, notes, color, arm ->
                                    viewModel.saveScannedTagAsCard(name, category, description, facility, type, fc, num, notes, color, arm)
                                },
                                onClearScannedTag = { viewModel.clearScannedTag() },
                                onSimulateScan = { viewModel.simulateTagScan(it) },
                                onManualAddClick = { viewModel.setShowManualAdd(true) }
                            )
                        }

                        MainTab.DUPLICATE -> {
                            CloneScreen(
                                sourceCard = selectedCardForClone ?: allCards.firstOrNull(),
                                allCards = allCards,
                                writeStatus = writeStatus,
                                onSelectCardToClone = { viewModel.setCardForCloneSelection(it) },
                                onResetWriteStatus = { viewModel.resetWriteStatus() },
                                onSimulateBlankTap = { viewModel.simulateBlankTagWrite() }
                            )
                        }

                        MainTab.EMULATE -> {
                            EmulationScreen(
                                activeCard = activeVirtualCard,
                                apduLogs = apduLogs,
                                onDisarm = { viewModel.disarmEmulation() },
                                onSimulateReaderTap = { viewModel.simulateTestReaderTap() },
                                onClearLogs = { viewModel.clearApduLogs() },
                                onNavigateWallet = { viewModel.selectTab(MainTab.WALLET) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Detail Dialog
    selectedCardForDetail?.let { card ->
        CardDetailDialog(
            card = card,
            onDismiss = { viewModel.selectCardForDetail(null) },
            onArmForEmulation = {
                viewModel.armCardForEmulation(card)
                viewModel.selectCardForDetail(null)
            },
            onDisarmEmulation = {
                viewModel.disarmEmulation()
                viewModel.selectCardForDetail(null)
            },
            onCloneToBlank = {
                viewModel.selectCardForClone(card)
                viewModel.selectCardForDetail(null)
            },
            onUpdateCard = { updated ->
                viewModel.updateCard(updated)
            },
            onDeleteCard = { toDelete ->
                viewModel.deleteCard(toDelete)
            }
        )
    }

    // Manual Add Dialog
    if (showManualAddDialog) {
        ManualAddCardDialog(
            onDismiss = { viewModel.setShowManualAdd(false) },
            onSave = { name, category, description, facility, type, uid, fc, num, payload, notes, color, arm ->
                viewModel.saveManualCard(name, category, description, facility, type, uid, fc, num, payload, notes, color, arm)
            }
        )
    }

    // Hardware & Runtime Permissions Dialog
    if (showHardwareDialog) {
        NfcHardwarePermissionDialog(
            hardwareState = hardwareState,
            onOpenSettings = {
                viewModel.openNfcSettings()
            },
            onRefresh = {
                viewModel.refreshHardwareState()
            },
            onOpenLanguageSelector = {
                viewModel.setShowLanguageDialog(true)
            },
            onTestHaptic = {
                viewModel.nfcController.triggerHapticFeedback()
            },
            onDismiss = {
                viewModel.setShowHardwareDialog(false)
            }
        )
    }

    // Language Selector Dialog
    if (showLanguageDialog) {
        LanguageSelectionDialog(
            onDismiss = { viewModel.setShowLanguageDialog(false) }
        )
    }

    // Google Sign-In & Cloud Tag Storage Screen
    if (showLoginScreen) {
        Dialog(
            onDismissRequest = { viewModel.setShowLoginScreen(false) },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            LoginScreen(
                currentUser = currentUser,
                authState = authUiState,
                syncState = cloudSyncState,
                cardCount = allCards.size,
                onSignInWithGoogle = { activity ->
                    viewModel.signInWithGoogle(activity)
                },
                onSignInWithEmail = { email, password ->
                    viewModel.signInWithEmail(email, password)
                },
                onSignUpWithEmail = { email, password, name ->
                    viewModel.signUpWithEmail(email, password, name)
                },
                onPasswordReset = { email ->
                    viewModel.sendPasswordReset(email)
                },
                onSignOut = {
                    viewModel.setShowLoginScreen(false)
                    viewModel.signOut()
                },
                onSyncCards = { viewModel.syncCardsToAccount() },
                onRestoreCards = { viewModel.restoreCardsFromAccount() },
                onUpdateFullName = { newName -> viewModel.updateFullName(newName) },
                onRefreshProfile = { viewModel.refreshUserProfile() },
                onDismiss = { viewModel.setShowLoginScreen(false) }
            )
        }
    }
}

@Composable
fun navigationItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = CyanPrimary,
    selectedTextColor = CyanPrimary,
    indicatorColor = CyanPrimary.copy(alpha = 0.15f),
    unselectedIconColor = Color(0xFF64748B),
    unselectedTextColor = Color(0xFF64748B)
)

