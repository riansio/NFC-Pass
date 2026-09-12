package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.CardRepository
import com.example.data.CardType
import com.example.data.NfcCard
import com.example.hce.ApduLogEntry
import com.example.hce.HceManager
import com.example.nfc.NfcController
import com.example.nfc.NfcTagData
import com.example.nfc.ScanMode
import com.example.nfc.WriteStatus
import com.example.data.TagCategories
import com.example.auth.AuthUiState
import com.example.auth.AuthUser
import com.example.auth.CloudCardSyncManager
import com.example.auth.CloudSyncState
import com.example.auth.FirebaseAuthManager
import com.example.auth.UserCancelledException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MainTab(val title: String) {
    WALLET("My Cards"),
    SCAN("Scan & Add"),
    DUPLICATE("Clone Tag"),
    EMULATE("HCE Virtual")
}

class NfcViewModel(
    application: Application,
    private val repository: CardRepository,
    val nfcController: NfcController
) : AndroidViewModel(application) {

    private val _currentTab = MutableStateFlow(MainTab.WALLET)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    val allCards: StateFlow<List<NfcCard>> = repository.allCards
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeCardFromDb: StateFlow<NfcCard?> = repository.activeCard
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeVirtualCard: StateFlow<NfcCard?> = HceManager.activeCard

    val apduLogs: StateFlow<List<ApduLogEntry>> = HceManager.apduLogs

    val lastScannedTag: StateFlow<NfcTagData?> = nfcController.lastScannedTag
    val writeStatus: StateFlow<WriteStatus> = nfcController.writeStatus

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _categoryFilter = MutableStateFlow<String?>(null)
    val categoryFilter: StateFlow<String?> = _categoryFilter.asStateFlow()

    private val _typeFilter = MutableStateFlow<CardType?>(null)
    val typeFilter: StateFlow<CardType?> = _typeFilter.asStateFlow()

    val availableCategories: StateFlow<List<String>> = allCards.map { cards ->
        val customCats = cards.map { it.category.trim() }.filter { it.isNotBlank() }
        (TagCategories.DEFAULT_CATEGORIES + customCats).distinct()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TagCategories.DEFAULT_CATEGORIES)

    private val _selectedCardForDetail = MutableStateFlow<NfcCard?>(null)
    val selectedCardForDetail: StateFlow<NfcCard?> = _selectedCardForDetail.asStateFlow()

    private val _selectedCardForClone = MutableStateFlow<NfcCard?>(null)
    val selectedCardForClone: StateFlow<NfcCard?> = _selectedCardForClone.asStateFlow()

    private val _showManualAddDialog = MutableStateFlow(false)
    val showManualAddDialog: StateFlow<Boolean> = _showManualAddDialog.asStateFlow()

    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    val hardwareState = nfcController.hardwareState

    private val _showHardwareDialog = MutableStateFlow(false)
    val showHardwareDialog: StateFlow<Boolean> = _showHardwareDialog.asStateFlow()

    private val _showLanguageDialog = MutableStateFlow(false)
    val showLanguageDialog: StateFlow<Boolean> = _showLanguageDialog.asStateFlow()

    // Firebase Auth & Credential Manager
    val authManager = FirebaseAuthManager(application)
    val cloudSyncManager = CloudCardSyncManager(application, repository)

    val currentUser: StateFlow<AuthUser?> = authManager.currentUser
    private val _authUiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()
    val cloudSyncState: StateFlow<CloudSyncState> = cloudSyncManager.syncState

    private val _showLoginScreen = MutableStateFlow(false)
    val showLoginScreen: StateFlow<Boolean> = _showLoginScreen.asStateFlow()

    fun setShowLoginScreen(show: Boolean) {
        _showLoginScreen.value = show
    }

    fun signInWithGoogle(activityContext: Context) {
        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            val result = authManager.signInWithGoogle(activityContext)
            result.fold(
                onSuccess = { user ->
                    _authUiState.value = AuthUiState.Success(user)
                    _bannerMessage.value = "Welcome, ${user.displayName ?: "User"}! Storing tags to your account."
                    // Auto-sync local cards to the user's Google account
                    syncCardsToAccount()
                },
                onFailure = { error ->
                    if (error is UserCancelledException) {
                        _authUiState.value = AuthUiState.Idle
                    } else {
                        _authUiState.value = AuthUiState.Error(error.localizedMessage ?: "Sign-in failed")
                    }
                }
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authManager.signOut()
            _authUiState.value = AuthUiState.Idle
            _bannerMessage.value = "Signed out of Google account."
        }
    }

    fun syncCardsToAccount() {
        val user = authManager.currentUser.value ?: return
        viewModelScope.launch {
            val currentCards = allCards.value
            val result = cloudSyncManager.fullSyncWithAccount(user.uid, currentCards)
            result.fold(
                onSuccess = { (uploaded, restored) ->
                    _bannerMessage.value = if (restored > 0) {
                        "Account synced: $uploaded tags saved, $restored restored!"
                    } else {
                        "Synced $uploaded tags to your Google account!"
                    }
                },
                onFailure = { error ->
                    _bannerMessage.value = "Cloud sync warning: ${error.localizedMessage}"
                }
            )
        }
    }

    fun restoreCardsFromAccount() {
        val user = authManager.currentUser.value ?: return
        viewModelScope.launch {
            val result = cloudSyncManager.restoreCardsFromAccount(user.uid)
            result.fold(
                onSuccess = { count ->
                    _bannerMessage.value = "Restored $count tags from your Google account!"
                },
                onFailure = { error ->
                    _bannerMessage.value = "Restore failed: ${error.localizedMessage}"
                }
            )
        }
    }

    fun refreshUserProfile() {
        viewModelScope.launch {
            authManager.refreshUserProfileRealtime().fold(
                onSuccess = { user ->
                    _bannerMessage.value = "Google profile synced: ${user.displayName ?: "User"}"
                },
                onFailure = { /* silent */ }
            )
        }
    }

    fun updateFullName(newName: String) {
        viewModelScope.launch {
            authManager.updateFullName(newName).fold(
                onSuccess = { user ->
                    _bannerMessage.value = "Name updated: ${user.displayName ?: "User"}"
                    syncCardsToAccount()
                },
                onFailure = { error ->
                    _bannerMessage.value = "Could not update name: ${error.localizedMessage}"
                }
            )
        }
    }

    fun setShowHardwareDialog(show: Boolean) {
        _showHardwareDialog.value = show
        if (show) {
            nfcController.refreshHardwareState()
        }
    }

    fun setShowLanguageDialog(show: Boolean) {
        _showLanguageDialog.value = show
    }

    fun refreshHardwareState() {
        nfcController.refreshHardwareState()
    }

    fun openNfcSettings() {
        nfcController.openNfcSettings()
    }

    val filteredCards: StateFlow<List<NfcCard>> = combine(allCards, _searchQuery, _categoryFilter, _typeFilter) { cards, query, catFilter, typeFilter ->
        cards.filter { card ->
            val matchesQuery = query.isBlank() ||
                    card.name.contains(query, ignoreCase = true) ||
                    card.description.contains(query, ignoreCase = true) ||
                    card.category.contains(query, ignoreCase = true) ||
                    card.facilityName.contains(query, ignoreCase = true) ||
                    card.uidHex.contains(query, ignoreCase = true) ||
                    card.cardNumber.contains(query, ignoreCase = true) ||
                    card.ndefPayload.contains(query, ignoreCase = true)
            val matchesCategory = catFilter == null || card.category.equals(catFilter, ignoreCase = true)
            val matchesType = typeFilter == null || card.cardType == typeFilter
            matchesQuery && matchesCategory && matchesType
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            // Delete all previous sample cards so the app starts with 0 cards as requested
            val prefs = getApplication<Application>().getSharedPreferences("nfc_pass_prefs", Context.MODE_PRIVATE)
            if (!prefs.getBoolean("has_cleared_initial_cards_v3", false)) {
                repository.clearAllCards()
                prefs.edit().putBoolean("has_cleared_initial_cards_v3", true).apply()
            }
        }
        // Sync active card to HceManager
        viewModelScope.launch {
            repository.activeCard.collect { card ->
                HceManager.setActiveCard(card)
            }
        }
        // Sync authentication state to HceManager for HCE security enforcement
        viewModelScope.launch {
            currentUser.collect { user ->
                HceManager.setIsAuthenticated(user != null)
            }
        }
    }

    fun selectTab(tab: MainTab) {
        _currentTab.value = tab
        when (tab) {
            MainTab.SCAN -> {
                nfcController.setScanMode(ScanMode.SCANNING_NEW)
            }
            MainTab.DUPLICATE -> {
                val cardToClone = _selectedCardForClone.value ?: allCards.value.firstOrNull()
                _selectedCardForClone.value = cardToClone
                nfcController.setScanMode(ScanMode.WRITING_BLANK, cardToClone)
            }
            else -> {
                nfcController.setScanMode(ScanMode.IDLE)
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(category: String?) {
        _categoryFilter.value = category
    }

    fun setTypeFilter(type: CardType?) {
        _typeFilter.value = type
    }

    fun selectCardForDetail(card: NfcCard?) {
        _selectedCardForDetail.value = card
    }

    fun selectCardForClone(card: NfcCard) {
        _selectedCardForClone.value = card
        nfcController.setScanMode(ScanMode.WRITING_BLANK, card)
        _currentTab.value = MainTab.DUPLICATE
        _bannerMessage.value = "Selected \"${card.name}\" to write to blank tag"
    }

    fun setCardForCloneSelection(card: NfcCard) {
        _selectedCardForClone.value = card
        nfcController.setScanMode(ScanMode.WRITING_BLANK, card)
    }

    fun setShowManualAdd(show: Boolean) {
        _showManualAddDialog.value = show
    }

    fun clearBannerMessage() {
        _bannerMessage.value = null
    }

    fun armCardForEmulation(card: NfcCard) {
        viewModelScope.launch {
            repository.setActiveCard(card.id)
            HceManager.setActiveCard(card)
            _bannerMessage.value = "Card \"${card.name}\" ready to swipe!"
        }
    }

    fun disarmEmulation() {
        viewModelScope.launch {
            repository.deactivateAll()
            HceManager.setActiveCard(null)
            _bannerMessage.value = "Card swipe cancelled."
        }
    }

    fun saveScannedTagAsCard(
        name: String,
        category: String,
        description: String,
        facilityName: String,
        cardType: CardType,
        facilityCode: String,
        cardNumber: String,
        notes: String,
        colorIndex: Int,
        armImmediately: Boolean
    ) {
        val tag = nfcController.lastScannedTag.value ?: return
        viewModelScope.launch {
            val card = NfcCard(
                name = name.ifBlank { "NFC Access Tag" },
                category = category.ifBlank { TagCategories.DEFAULT_CATEGORY },
                description = description,
                facilityName = facilityName.ifBlank { "Virtual Facility" },
                cardType = cardType,
                uidHex = tag.uidHex,
                atqaHex = tag.atqaHex,
                sakHex = tag.sakHex,
                techList = tag.techList,
                historicalBytesHex = tag.historicalBytesHex,
                ndefPayload = tag.ndefPayload,
                ndefMimeOrUri = tag.ndefPayload,
                facilityCode = facilityCode,
                cardNumber = cardNumber,
                colorGradientIndex = colorIndex,
                isActiveVirtualCard = armImmediately,
                notes = notes
            )
            val newId = repository.insert(card)
            val savedCard = card.copy(id = newId)
            if (armImmediately) {
                repository.setActiveCard(newId)
                HceManager.setActiveCard(savedCard.copy(isActiveVirtualCard = true))
            }
            nfcController.clearScannedTag()
            _bannerMessage.value = "Tag \"${card.name}\" saved to ${card.category}!"
            // Synchronize newly added tag to user account
            currentUser.value?.let { user ->
                launch {
                    cloudSyncManager.syncCardAdded(user.uid, savedCard)
                }
            }
            selectTab(MainTab.WALLET)
        }
    }

    fun saveManualCard(
        name: String,
        category: String,
        description: String,
        facilityName: String,
        cardType: CardType,
        uidHex: String,
        facilityCode: String,
        cardNumber: String,
        ndefPayload: String,
        notes: String,
        colorIndex: Int,
        armImmediately: Boolean
    ) {
        viewModelScope.launch {
            val formattedUid = if (uidHex.contains(":")) uidHex.uppercase() else {
                uidHex.chunked(2).joinToString(":").uppercase()
            }
            val card = NfcCard(
                name = name.ifBlank { "Custom Access Tag" },
                category = category.ifBlank { TagCategories.DEFAULT_CATEGORY },
                description = description,
                facilityName = facilityName.ifBlank { "Virtual Facility" },
                cardType = cardType,
                uidHex = formattedUid.ifBlank { "04:A1:B2:C3:D4:E5:80" },
                atqaHex = "00:04",
                sakHex = "08",
                techList = listOf("android.nfc.tech.NfcA", "android.nfc.tech.IsoDep"),
                ndefPayload = ndefPayload,
                facilityCode = facilityCode,
                cardNumber = cardNumber,
                colorGradientIndex = colorIndex,
                isActiveVirtualCard = armImmediately,
                notes = notes
            )
            val newId = repository.insert(card)
            val savedCard = card.copy(id = newId)
            if (armImmediately) {
                repository.setActiveCard(newId)
                HceManager.setActiveCard(savedCard.copy(isActiveVirtualCard = true))
            }
            _showManualAddDialog.value = false
            _bannerMessage.value = "New tag \"${card.name}\" saved to ${card.category}!"
            // Synchronize newly added tag to user account
            currentUser.value?.let { user ->
                launch {
                    cloudSyncManager.syncCardAdded(user.uid, savedCard)
                }
            }
        }
    }

    fun updateCard(card: NfcCard) {
        viewModelScope.launch {
            repository.update(card)
            if (card.isActiveVirtualCard) {
                HceManager.setActiveCard(card)
            }
            _selectedCardForDetail.value = card
            _bannerMessage.value = "Card updated."
            // Synchronize updated tag to user account
            currentUser.value?.let { user ->
                launch {
                    cloudSyncManager.syncCardUpdated(user.uid, card)
                }
            }
        }
    }

    fun deleteCard(card: NfcCard) {
        viewModelScope.launch {
            if (card.isActiveVirtualCard) {
                HceManager.setActiveCard(null)
            }
            repository.delete(card)
            _selectedCardForDetail.value = null
            _bannerMessage.value = "Card deleted."
            // Synchronize deleted tag from user account
            currentUser.value?.let { user ->
                launch {
                    cloudSyncManager.syncCardDeleted(user.uid, card)
                }
            }
        }
    }

    fun clearScannedTag() {
        nfcController.clearScannedTag()
    }

    fun resetWriteStatus() {
        nfcController.resetWriteStatus()
    }

    fun simulateTestReaderTap() {
        val active = HceManager.activeCard.value
        val isAuthed = currentUser.value != null
        if (!isAuthed) {
            HceManager.simulateReaderTap(active, false)
            _bannerMessage.value = "Security Error: Sign in required to emulate cards!"
            return
        }

        if (active == null) {
            HceManager.simulateReaderTap(null, true)
            _bannerMessage.value = "Please select a card to swipe first!"
            return
        }

        HceManager.simulateReaderTap(active, true)
        viewModelScope.launch {
            repository.recordEmulation(active.id)
        }
        _bannerMessage.value = "Reader tapped! Emulation successful (90 00 OK)"
    }

    fun simulateTagScan(type: Int) {
        nfcController.simulateScan(type)
    }

    fun simulateBlankTagWrite() {
        nfcController.simulateDuplicateWrite()
    }

    fun clearApduLogs() {
        HceManager.clearLogs()
    }

    class Factory(
        private val application: Application,
        private val repository: CardRepository,
        private val nfcController: NfcController
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NfcViewModel(application, repository, nfcController) as T
        }
    }
}
