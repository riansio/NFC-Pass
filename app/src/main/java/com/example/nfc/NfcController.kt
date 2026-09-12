package com.example.nfc

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.nfc.tech.NfcA
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import com.example.data.NfcCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.charset.StandardCharsets

enum class ScanMode {
    IDLE,
    SCANNING_NEW,
    WRITING_BLANK
}

enum class NfcHardwareState {
    READY,
    DISABLED,
    UNSUPPORTED
}

sealed class WriteStatus {
    object Idle : WriteStatus()
    object WaitingForTag : WriteStatus()
    object Writing : WriteStatus()
    data class Success(val tagUid: String, val message: String) : WriteStatus()
    data class Error(val errorMessage: String) : WriteStatus()
}

class NfcController(private val context: Context) : NfcAdapter.ReaderCallback {

    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(context)

    val isSupported: Boolean get() = nfcAdapter != null
    val isEnabled: Boolean get() = nfcAdapter?.isEnabled == true

    private val _hardwareState = MutableStateFlow(computeHardwareState())
    val hardwareState: StateFlow<NfcHardwareState> = _hardwareState.asStateFlow()

    private val _scanMode = MutableStateFlow(ScanMode.IDLE)
    val scanMode: StateFlow<ScanMode> = _scanMode.asStateFlow()

    private val _lastScannedTag = MutableStateFlow<NfcTagData?>(null)
    val lastScannedTag: StateFlow<NfcTagData?> = _lastScannedTag.asStateFlow()

    private val _writeStatus = MutableStateFlow<WriteStatus>(WriteStatus.Idle)
    val writeStatus: StateFlow<WriteStatus> = _writeStatus.asStateFlow()

    private var targetCardToClone: NfcCard? = null

    fun refreshHardwareState() {
        _hardwareState.value = computeHardwareState()
    }

    private fun computeHardwareState(): NfcHardwareState {
        return when {
            nfcAdapter == null -> NfcHardwareState.UNSUPPORTED
            !nfcAdapter.isEnabled -> NfcHardwareState.DISABLED
            else -> NfcHardwareState.READY
        }
    }

    fun openNfcSettings(ctx: Context = context) {
        try {
            val intent = Intent(Settings.ACTION_NFC_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallback = Intent(Settings.ACTION_WIRELESS_SETTINGS)
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(fallback)
            } catch (_: Exception) {
                try {
                    val appSettings = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    appSettings.data = Uri.fromParts("package", ctx.packageName, null)
                    appSettings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    ctx.startActivity(appSettings)
                } catch (_: Exception) {}
            }
        }
    }

    fun triggerHapticFeedback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(80)
                }
            }
        } catch (_: Exception) {}
    }

    fun setScanMode(mode: ScanMode, targetCard: NfcCard? = null) {
        _scanMode.value = mode
        targetCardToClone = targetCard
        if (mode == ScanMode.WRITING_BLANK) {
            _writeStatus.value = WriteStatus.WaitingForTag
        } else if (mode == ScanMode.IDLE) {
            _writeStatus.value = WriteStatus.Idle
        }
    }

    fun clearScannedTag() {
        _lastScannedTag.value = null
    }

    fun resetWriteStatus() {
        _writeStatus.value = WriteStatus.Idle
    }

    fun enableReader(activity: Activity) {
        if (nfcAdapter == null || !nfcAdapter.isEnabled) return
        val flags = NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS
        val options = Bundle()
        options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250)
        nfcAdapter.enableReaderMode(activity, this, flags, options)
    }

    fun disableReader(activity: Activity) {
        nfcAdapter?.disableReaderMode(activity)
    }

    override fun onTagDiscovered(tag: Tag?) {
        if (tag == null) return

        when (_scanMode.value) {
            ScanMode.WRITING_BLANK -> {
                handleTagWrite(tag)
            }
            ScanMode.SCANNING_NEW, ScanMode.IDLE -> {
                handleTagRead(tag)
            }
        }
    }

    fun handleTagRead(tag: Tag) {
        val uid = tag.id?.joinToString(":") { "%02X".format(it) } ?: "UNKNOWN"
        val techList = tag.techList.map { it.substringAfterLast(".") }

        var atqaHex = "00:04"
        var sakHex = "08"
        var historicalBytesHex = ""
        var ndefPayload = ""
        var ndefType = ""
        var isWritable = true
        var maxSize = 0

        // Parse NfcA
        val nfcA = NfcA.get(tag)
        if (nfcA != null) {
            try {
                val atqaBytes = nfcA.atqa
                atqaHex = atqaBytes.joinToString(":") { "%02X".format(it) }
                sakHex = "%02X".format(nfcA.sak.toInt() and 0xFF)
            } catch (_: Exception) {}
        }

        // Parse IsoDep
        val isoDep = IsoDep.get(tag)
        if (isoDep != null) {
            try {
                val hist = isoDep.historicalBytes
                if (hist != null && hist.isNotEmpty()) {
                    historicalBytesHex = hist.joinToString(":") { "%02X".format(it) }
                }
            } catch (_: Exception) {}
        }

        // Parse NDEF
        val ndef = Ndef.get(tag)
        if (ndef != null) {
            try {
                ndef.connect()
                isWritable = ndef.isWritable
                maxSize = ndef.maxSize
                val message = ndef.cachedNdefMessage ?: ndef.ndefMessage
                if (message != null && message.records.isNotEmpty()) {
                    val record = message.records[0]
                    ndefType = String(record.type, StandardCharsets.UTF_8)
                    ndefPayload = parseRecordPayload(record)
                }
                ndef.close()
            } catch (_: Exception) {}
        }

        val chipGuess = guessChipType(techList, sakHex, atqaHex, maxSize)

        val scannedData = NfcTagData(
            uidHex = uid,
            techList = techList,
            atqaHex = atqaHex,
            sakHex = sakHex,
            historicalBytesHex = historicalBytesHex,
            ndefPayload = ndefPayload,
            ndefType = ndefType,
            isWritable = isWritable,
            maxSize = maxSize,
            chipTypeGuess = chipGuess
        )

        _lastScannedTag.value = scannedData
        triggerHapticFeedback()
    }

    private fun handleTagWrite(tag: Tag) {
        val card = targetCardToClone
        if (card == null) {
            _writeStatus.value = WriteStatus.Error("No source card selected to clone")
            return
        }

        _writeStatus.value = WriteStatus.Writing
        val tagUid = tag.id?.joinToString(":") { "%02X".format(it) } ?: "UNKNOWN"

        try {
            val ndefMessage = buildNdefMessageForCard(card)
            val ndef = Ndef.get(tag)

            if (ndef != null) {
                ndef.connect()
                if (!ndef.isWritable) {
                    _writeStatus.value = WriteStatus.Error("Blank tag is write-protected or locked.")
                    ndef.close()
                    return
                }
                if (ndef.maxSize < ndefMessage.byteArrayLength) {
                    _writeStatus.value = WriteStatus.Error(
                        "Tag capacity too small (${ndef.maxSize} bytes vs needed ${ndefMessage.byteArrayLength} bytes)."
                    )
                    ndef.close()
                    return
                }
                ndef.writeNdefMessage(ndefMessage)
                ndef.close()
                triggerHapticFeedback()
                _writeStatus.value = WriteStatus.Success(
                    tagUid = tagUid,
                    message = "Successfully cloned card \"${card.name}\" to tag $tagUid (${ndefMessage.byteArrayLength} bytes written)."
                )
            } else {
                val formatable = NdefFormatable.get(tag)
                if (formatable != null) {
                    formatable.connect()
                    formatable.format(ndefMessage)
                    formatable.close()
                    triggerHapticFeedback()
                    _writeStatus.value = WriteStatus.Success(
                        tagUid = tagUid,
                        message = "Formatted and cloned card \"${card.name}\" to tag $tagUid!"
                    )
                } else {
                    _writeStatus.value = WriteStatus.Error(
                        "Tag does not support NDEF formatting. Ensure you are using a standard NFC Forum Type 2/4 blank tag (NTAG213/215/216)."
                    )
                }
            }
        } catch (e: Exception) {
            _writeStatus.value = WriteStatus.Error("Write failed: ${e.localizedMessage ?: "Communication lost during write."}")
        }
    }

    private fun buildNdefMessageForCard(card: NfcCard): NdefMessage {
        val records = mutableListOf<NdefRecord>()

        // Record 1: Text record with access badge credentials
        val accessData = "NFC-PASS:UID=${card.uidHex}|NAME=${card.name}|CAT=${card.category}|DESC=${card.description}|FACILITY=${card.facilityCode}|CARD=${card.cardNumber}|PAYLOAD=${card.ndefPayload}"
        val textRecord = NdefRecord.createTextRecord("en", accessData)
        records.add(textRecord)

        // Record 2: Optional URI record if card has URI payload
        if (card.ndefMimeOrUri.startsWith("http://", ignoreCase = true) ||
            card.ndefMimeOrUri.startsWith("https://", ignoreCase = true)) {
            try {
                records.add(NdefRecord.createUri(card.ndefMimeOrUri))
            } catch (_: Exception) {}
        }

        // Record 3: Custom MIME record for direct app identification
        val mimeRecord = NdefRecord.createMime(
            "application/vnd.nfcpass.card",
            accessData.toByteArray(StandardCharsets.UTF_8)
        )
        records.add(mimeRecord)

        return NdefMessage(records.toTypedArray())
    }

    private fun parseRecordPayload(record: NdefRecord): String {
        return try {
            val payload = record.payload
            if (record.tnf == NdefRecord.TNF_WELL_KNOWN && record.type.contentEquals(NdefRecord.RTD_TEXT)) {
                val statusByte = payload[0].toInt()
                val languageLength = statusByte and 0x3F
                val isUtf8 = (statusByte and 0x80) == 0
                val charset = if (isUtf8) StandardCharsets.UTF_8 else StandardCharsets.UTF_16
                String(payload, 1 + languageLength, payload.size - 1 - languageLength, charset)
            } else if (record.tnf == NdefRecord.TNF_WELL_KNOWN && record.type.contentEquals(NdefRecord.RTD_URI)) {
                val prefix = when (payload[0].toInt()) {
                    0x01 -> "http://www."
                    0x02 -> "https://www."
                    0x03 -> "http://"
                    0x04 -> "https://"
                    0x05 -> "tel:"
                    0x06 -> "mailto:"
                    else -> ""
                }
                prefix + String(payload, 1, payload.size - 1, StandardCharsets.UTF_8)
            } else {
                String(payload, StandardCharsets.UTF_8)
            }
        } catch (_: Exception) {
            record.payload.joinToString(" ") { "%02X".format(it) }
        }
    }

    private fun guessChipType(techList: List<String>, sak: String, atqa: String, maxSize: Int): String {
        return when {
            techList.contains("MifareClassic") -> "NXP MIFARE Classic 1K (S50)"
            techList.contains("MifareUltralight") -> {
                when {
                    maxSize > 500 -> "NXP NTAG216 (888 bytes)"
                    maxSize > 400 -> "NXP NTAG215 (504 bytes)"
                    maxSize > 100 -> "NXP NTAG213 (144 bytes)"
                    else -> "NXP MIFARE Ultralight (64 bytes)"
                }
            }
            techList.contains("IsoDep") -> "NXP MIFARE DESFire / ISO 14443-4 Smart Card"
            techList.contains("NfcA") -> "ISO 14443-3A Standard Tag"
            techList.contains("NfcV") -> "ISO 15693 Vicinity Tag (SLIX)"
            else -> "Standard Contactless Tag"
        }
    }

    // Simulation helpers for testing without physical NFC hardware or in emulator:
    fun simulateScan(sampleType: Int = 0) {
        val samples = listOf(
            // 0: Corporate DESFire Access Badge
            NfcTagData(
                uidHex = "04:6A:B2:1C:89:4F:80",
                techList = listOf("NfcA", "IsoDep", "Ndef"),
                atqaHex = "00:04",
                sakHex = "20",
                historicalBytesHex = "80:4F:0C:A0:00:00:03:06",
                ndefPayload = "SEC-PASS-99014:DOOR-LAB-3",
                ndefType = "T",
                isWritable = true,
                maxSize = 1024,
                chipTypeGuess = "NXP MIFARE DESFire EV2 (8KB)"
            ),
            // 1: MIFARE Classic 1K Turnstile Key
            NfcTagData(
                uidHex = "3D:A4:91:0F",
                techList = listOf("NfcA", "MifareClassic"),
                atqaHex = "00:04",
                sakHex = "08",
                historicalBytesHex = "",
                ndefPayload = "FACILITY-KEY-7712",
                ndefType = "MIME",
                isWritable = true,
                maxSize = 752,
                chipTypeGuess = "NXP MIFARE Classic 1K (S50)"
            ),
            // 2: NTAG215 Smart Tag
            NfcTagData(
                uidHex = "04:E8:22:91:FA:50:80",
                techList = listOf("NfcA", "MifareUltralight", "Ndef"),
                atqaHex = "00:44",
                sakHex = "00",
                historicalBytesHex = "",
                ndefPayload = "https://nfc.security.corp/badge?id=83910",
                ndefType = "U",
                isWritable = true,
                maxSize = 504,
                chipTypeGuess = "NXP NTAG215 (504 bytes)"
            ),
            // 3: Sony FeliCa Suica Transit Card
            NfcTagData(
                uidHex = "01:2E:4B:8C:10:20:30:40",
                techList = listOf("NfcF", "IsoDep"),
                atqaHex = "00:00",
                sakHex = "00",
                historicalBytesHex = "",
                ndefPayload = "FELICA_SUICA_TRANSIT:BALANCE:¥4850",
                ndefType = "T",
                isWritable = false,
                maxSize = 256,
                chipTypeGuess = "Sony FeliCa Lite-S (RC-S966)"
            ),
            // 4: Calypso Paris Navigo Metro Pass
            NfcTagData(
                uidHex = "14:B2:99:3A:C1:88",
                techList = listOf("IsoDep", "NfcB"),
                atqaHex = "00:00",
                sakHex = "20",
                historicalBytesHex = "10:82:47:10:01",
                ndefPayload = "CALYPSO_NAVIGO_PASS:METRO_ZONE_1_5",
                ndefType = "T",
                isWritable = false,
                maxSize = 2048,
                chipTypeGuess = "Calypso / Intercode Contactless Pass"
            ),
            // 5: Contactless EMV Payment Card
            NfcTagData(
                uidHex = "41:11:92:80:31:00:55",
                techList = listOf("NfcA", "IsoDep"),
                atqaHex = "00:04",
                sakHex = "20",
                historicalBytesHex = "2PAY.SYS.DDF01",
                ndefPayload = "EMV_CONTACTLESS:VISA_PAYWAVE",
                ndefType = "T",
                isWritable = false,
                maxSize = 4096,
                chipTypeGuess = "EMV Contactless Smart Card (Visa/MC)"
            ),
            // 6: NTAG215 Amiibo Gaming Tag
            NfcTagData(
                uidHex = "04:3A:C1:F2:70:4E:80",
                techList = listOf("NfcA", "MifareUltralight", "Ndef"),
                atqaHex = "00:44",
                sakHex = "00",
                historicalBytesHex = "",
                ndefPayload = "AMIIBO:CHARACTER_ID:00000000-00140002",
                ndefType = "MIME",
                isWritable = false,
                maxSize = 504,
                chipTypeGuess = "NTAG215 Nintendo Amiibo Tag"
            ),
            // 7: HID Corporate 1000 37-bit Badge
            NfcTagData(
                uidHex = "04:DF:31:4A:12:89:80",
                techList = listOf("NfcA", "IsoDep"),
                atqaHex = "00:04",
                sakHex = "20",
                historicalBytesHex = "A0:00:00:01:16",
                ndefPayload = "HID_ICLASS_SE:WIEGAND_37:FC_120:CN_30481",
                ndefType = "T",
                isWritable = true,
                maxSize = 1024,
                chipTypeGuess = "HID Global iCLASS SE / Seos Badge"
            ),
            // 8: University Campus & Student ID
            NfcTagData(
                uidHex = "04:55:C8:19:62:3B:80",
                techList = listOf("NfcA", "IsoDep"),
                atqaHex = "00:04",
                sakHex = "20",
                historicalBytesHex = "",
                ndefPayload = "CBORD_STUDENT_ID:88041234:DEPT_ENG",
                ndefType = "T",
                isWritable = true,
                maxSize = 1024,
                chipTypeGuess = "Campus / University Contactless ID"
            ),
            // 9: VingCard RFID Hotel Keycard
            NfcTagData(
                uidHex = "7A:91:02:4B",
                techList = listOf("NfcA", "MifareClassic"),
                atqaHex = "00:04",
                sakHex = "08",
                historicalBytesHex = "",
                ndefPayload = "VINGCARD_HOTEL:ROOM_1408:CHECKOUT_VALID",
                ndefType = "T",
                isWritable = true,
                maxSize = 752,
                chipTypeGuess = "Assa Abloy / VingCard Hotel Key"
            ),
            // 10: ISO 15693 Vicinity Tag
            NfcTagData(
                uidHex = "E0:04:01:00:1A:2B:3C:4D",
                techList = listOf("NfcV"),
                atqaHex = "",
                sakHex = "",
                historicalBytesHex = "",
                ndefPayload = "VICINITY_SLIX_TAG:INVENTORY_ITEM_9021",
                ndefType = "T",
                isWritable = true,
                maxSize = 112,
                chipTypeGuess = "NXP ICODE SLIX (ISO 15693 Vicinity)"
            ),
            // 11: Electronic Passport / Digital ID
            NfcTagData(
                uidHex = "04:77:88:99:AA:BB:CC",
                techList = listOf("IsoDep", "NfcB"),
                atqaHex = "",
                sakHex = "20",
                historicalBytesHex = "ICAO_DOC_9303",
                ndefPayload = "ICAO_EID:MRTD_MACHINE_READABLE_PASS",
                ndefType = "T",
                isWritable = false,
                maxSize = 8192,
                chipTypeGuess = "ICAO 9303 Digital ePassport / eID"
            )
        )
        val sample = samples[sampleType % samples.size]
        _lastScannedTag.value = sample
        triggerHapticFeedback()
    }

    fun simulateDuplicateWrite() {
        val card = targetCardToClone
        if (card == null) {
            _writeStatus.value = WriteStatus.Error("No source card selected to clone")
            return
        }
        _writeStatus.value = WriteStatus.Writing
        // Simulate blank tag detection and successful write
        val simulatedBlankUid = "04:D1:88:2E:39:6A:80"
        triggerHapticFeedback()
        _writeStatus.value = WriteStatus.Success(
            tagUid = simulatedBlankUid,
            message = "Cloned \"${card.name}\" to NTAG215 Blank Tag (UID: $simulatedBlankUid). Verified 124 bytes written!"
        )
    }
}
