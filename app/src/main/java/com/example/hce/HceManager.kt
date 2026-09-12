package com.example.hce

import com.example.data.NfcCard
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ApduLogEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val direction: Direction,
    val hexData: String,
    val description: String,
    val statusWord: String = ""
) {
    enum class Direction { INCOMING, OUTGOING, SYSTEM }

    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))
}

object HceManager {
    private val _activeCard = MutableStateFlow<NfcCard?>(null)
    val activeCard: StateFlow<NfcCard?> = _activeCard.asStateFlow()

    private val _isAuthenticated = MutableStateFlow<Boolean>(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _apduLogs = MutableStateFlow<List<ApduLogEntry>>(emptyList())
    val apduLogs: StateFlow<List<ApduLogEntry>> = _apduLogs.asStateFlow()

    private val _emulationEvent = MutableSharedFlow<NfcCard>(extraBufferCapacity = 5)
    val emulationEvent: SharedFlow<NfcCard> = _emulationEvent.asSharedFlow()

    fun setIsAuthenticated(authenticated: Boolean) {
        _isAuthenticated.value = authenticated
        if (!authenticated) {
            log(
                ApduLogEntry.Direction.SYSTEM,
                "",
                "HCE Security Status: Signed out. Emulation is locked."
            )
        } else {
            log(
                ApduLogEntry.Direction.SYSTEM,
                "",
                "HCE Security Status: User authenticated. Emulation unlocked."
            )
        }
    }

    fun setActiveCard(card: NfcCard?) {
        _activeCard.value = card
        if (card != null) {
            log(
                ApduLogEntry.Direction.SYSTEM,
                card.uidHex,
                "Card Ready to Swipe: \"${card.name}\" (UID: ${card.uidHex})"
            )
        } else {
            log(
                ApduLogEntry.Direction.SYSTEM,
                "",
                "Card swipe cancelled. No active card."
            )
        }
    }

    fun log(direction: ApduLogEntry.Direction, hexData: String, description: String, statusWord: String = "") {
        val entry = ApduLogEntry(
            direction = direction,
            hexData = hexData,
            description = description,
            statusWord = statusWord
        )
        val current = _apduLogs.value.toMutableList()
        if (current.size >= 100) {
            current.removeAt(current.size - 1)
        }
        current.add(0, entry)
        _apduLogs.value = current
    }

    fun notifyEmulated(card: NfcCard) {
        _emulationEvent.tryEmit(card)
    }

    fun clearLogs() {
        _apduLogs.value = emptyList()
    }

    /**
     * Simulates an authentic contactless reader query matching the active card type.
     * Enforces the "Require sign in" rule: if currentUser == null, returns security error.
     */
    fun simulateReaderTap(card: NfcCard?, isSignedIn: Boolean) {
        if (!isSignedIn) {
            log(
                ApduLogEntry.Direction.INCOMING,
                "00:A4:04:00:07:F0:01:02:03:04:05:06",
                "Reader Presentation Query"
            )
            log(
                ApduLogEntry.Direction.OUTGOING,
                "69:82",
                "SECURITY ERROR (6982): User must be signed in to emulate NFC cards.",
                "6982"
            )
            return
        }

        if (card == null) {
            log(
                ApduLogEntry.Direction.INCOMING,
                "00:A4:04:00:07:D2:76:00:00:85:01:01",
                "Reader Poll: No Card Armed"
            )
            log(
                ApduLogEntry.Direction.OUTGOING,
                "6A:82",
                "SW_FILE_NOT_FOUND (6A82): No active virtual card armed",
                "6A82"
            )
            return
        }

        when (card.cardType) {
            com.example.data.CardType.PAYMENT_CARD -> {
                log(ApduLogEntry.Direction.INCOMING, "00:A4:04:00:0E:32:50:41:59:2E:53:59:53:2E:44:44:46:30:31", "SELECT PPSE (2PAY.SYS.DDF01)")
                log(ApduLogEntry.Direction.OUTGOING, "6F:2A:84:0E:32:50:41:59:2E:53:59:53...:90:00", "Returned Payment Directory Template", "9000")
                log(ApduLogEntry.Direction.INCOMING, "80:A8:00:00:02:83:00", "GPO (Get Processing Options)")
                log(ApduLogEntry.Direction.OUTGOING, "77:0A:82:02:08:00:94:04:08:01:01:00:90:00", "Returned AIP (0800) & AFL Locator", "9000")
                log(ApduLogEntry.Direction.INCOMING, "00:B2:01:0C:00", "READ RECORD #1 (Payment Track)")
                log(ApduLogEntry.Direction.OUTGOING, "70:14:57:11:...:90:00", "Payment Card Validated", "9000")
            }
            com.example.data.CardType.TRANSIT_PASS, com.example.data.CardType.FELICA_TRANSIT -> {
                log(ApduLogEntry.Direction.INCOMING, "00:A4:04:00:07:A0:00:00:02:47:10:01", "SELECT Calypso Transit Pass")
                log(ApduLogEntry.Direction.OUTGOING, "6F:14:84:07:A0:00:00:02:47:10:01:...:90:00", "Transit Application Selected", "9000")
                log(ApduLogEntry.Direction.INCOMING, "00:A4:00:00:02:20:01", "SELECT Transit Contract File")
                log(ApduLogEntry.Direction.OUTGOING, "90:00", "Transit Contract Selected", "9000")
                log(ApduLogEntry.Direction.INCOMING, "00:B2:01:04:1D", "READ RECORD Transit Pass Info")
                log(ApduLogEntry.Direction.OUTGOING, "01:02:20:26:12:31:00:63:01:90:00", "Fare Active (Valid pass, 99 trips)", "9000")
            }
            com.example.data.CardType.MIFARE_DESFIRE -> {
                log(ApduLogEntry.Direction.INCOMING, "90:60:00:00:00", "DESFire GET VERSION")
                log(ApduLogEntry.Direction.OUTGOING, "04:01:01:02:00:18:05:91:00", "DESFire EV2 8KB Hardware Header", "9100")
                log(ApduLogEntry.Direction.INCOMING, "90:51:00:00:00", "DESFire GET CARD UID")
                log(ApduLogEntry.Direction.OUTGOING, "${card.uidHex.replace(":", "")}:91:00", "DESFire 7-Byte UID Read", "9100")
            }
            else -> {
                // Access badges, keys, student ID, generic
                log(ApduLogEntry.Direction.INCOMING, "00:A4:04:00:07:F0:01:02:03:04:05:06", "SELECT Physical Access Control AID")
                log(ApduLogEntry.Direction.OUTGOING, "50:41:43:53:...:90:00", "Access Granted: \"${card.name}\"", "9000")
                log(ApduLogEntry.Direction.INCOMING, "00:CA:01:00:00", "GET DATA: Card Serial Number (UID)")
                log(ApduLogEntry.Direction.OUTGOING, "${card.uidHex.replace(":", "")}:90:00", "UID Transmitted: ${card.uidHex}", "9000")
            }
        }
        notifyEmulated(card)
    }
}
