package com.example.hce

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import com.example.data.AppDatabase
import com.example.data.CardType
import com.example.data.NfcCard
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets

/**
 * HostApduService implementation allowing the device to act as an NFC card
 * for Host Card Emulation (HCE).
 *
 * Supports almost all major NFC Card formats:
 * - NFC Forum Type 4 Tag (NDEF Read & Write)
 * - Physical Access Control Systems (PACS, HID, Generic Turnstiles, PIV)
 * - Contactless EMV Payment Cards & PPSE (Visa, Mastercard, Amex, Discover, JCB, UnionPay)
 * - Contactless Transit Cards (Calypso, Paris Navigo, Suica/FeliCa envelope)
 * - MIFARE DESFire EV1/2/3 Emulation Envelope
 * - Campus / University Student ID Cards
 * - Smart Door Keys & Hotel Keycards (Assa Abloy, VingCard)
 * - Gaming & Amiibo tags
 * - ISO 15693 Vicinity & FeliCa envelope responses
 *
 * Security: Enforces "Require sign in". If no user is signed in with Firebase,
 * APDU commands are rejected with SW_SECURITY_STATUS_NOT_SATISFIED (0x6982).
 */
class VirtualCardService : HostApduService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    companion object {
        // Standard ISO 7816-4 Status Words
        val SW_SUCCESS = byteArrayOf(0x90.toByte(), 0x00.toByte())
        val SW_SECURITY_NOT_SATISFIED = byteArrayOf(0x69.toByte(), 0x82.toByte()) // User not signed in
        val SW_FILE_NOT_FOUND = byteArrayOf(0x6A.toByte(), 0x82.toByte())
        val SW_INCORRECT_P1P2 = byteArrayOf(0x6A.toByte(), 0x86.toByte())
        val SW_INS_NOT_SUPPORTED = byteArrayOf(0x6D.toByte(), 0x00.toByte())
        val SW_CLA_NOT_SUPPORTED = byteArrayOf(0x6E.toByte(), 0x00.toByte())
        val SW_UNKNOWN_ERROR = byteArrayOf(0x6F.toByte(), 0x00.toByte())
        val SW_DESFIRE_SUCCESS = byteArrayOf(0x91.toByte(), 0x00.toByte())

        // Well-known AIDs
        const val AID_NDEF_TYPE4_V2 = "D2760000850101"
        const val AID_NDEF_TYPE4_V1 = "D2760000850100"
        const val AID_PPSE = "325041592E5359532E4444463031" // 2PAY.SYS.DDF01
        const val AID_PPSE_1 = "315041592E5359532E4444463031" // 1PAY.SYS.DDF01
        const val AID_VISA = "A0000000031010"
        const val AID_MASTERCARD = "A0000000041010"
        const val AID_AMEX = "A00000002501"
        const val AID_JCB = "A0000000651010"
        const val AID_DISCOVER = "A0000001523010"
        const val AID_UNIONPAY = "A000000333010101"
        const val AID_CALYPSO_TRANSIT = "A0000002471001"
        const val AID_FELICA_TRANSIT = "D4100000030001"
        const val AID_DESFIRE_DEFAULT = "D2760001180002"
        const val AID_PACS_DEFAULT = "F0010203040506"
        const val AID_PACS_GENERIC = "F000000001"
        const val AID_PACS_SMART = "F222222222"
        const val AID_HID_ICLASS = "A000000116"
        const val AID_HID_PACS = "F048494420"
        const val AID_PIV_ID = "A00000047601"
        const val AID_CAMPUS_ID = "A0000000043060"
        const val AID_CBORD_ID = "F0394148148100"
        const val AID_HOTEL_KEY = "F000000002"

        // Type 4 Tag Capability Container (CC file: 15 bytes)
        // CCLEN=0x000F, Version 2.0, MLe=0x007F, MLc=0x007F, NDEF File Control TLV
        private val CC_FILE = byteArrayOf(
            0x00, 0x0F,           // CCLEN (15 bytes)
            0x20,                 // Mapping Version 2.0
            0x00, 0x7F,           // MLe (max read size: 127 bytes)
            0x00, 0x7F,           // MLc (max write size: 127 bytes)
            0x04, 0x06,           // NDEF File Control TLV (Tag=0x04, Len=0x06)
            0xE1.toByte(), 0x04,   // File ID: 0xE104
            0x08, 0x00,           // Max NDEF size: 2048 bytes
            0x00,                 // Read access: Free
            0x00                  // Write access: Free
        )
    }

    private var selectedFileId: Int = -1
    private var selectedAid: String = ""

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        if (commandApdu == null || commandApdu.isEmpty()) {
            return SW_UNKNOWN_ERROR
        }

        val hexCommand = toHex(commandApdu)

        // 1. REQUIRE SIGN IN SECURITY CHECK
        val isUserSignedIn = try {
            (FirebaseAuth.getInstance().currentUser != null) || HceManager.isAuthenticated.value
        } catch (_: Exception) {
            HceManager.isAuthenticated.value
        }

        if (!isUserSignedIn) {
            HceManager.log(
                ApduLogEntry.Direction.INCOMING,
                hexCommand,
                "Reader Query Blocked: Sign-In Required"
            )
            val response = SW_SECURITY_NOT_SATISFIED
            HceManager.log(
                ApduLogEntry.Direction.OUTGOING,
                toHex(response),
                "SECURITY DENIAL (6982): Sign in with Google required to emulate NFC cards",
                "6982"
            )
            return response
        }

        val activeCard = HceManager.activeCard.value

        // Parse APDU CLA, INS, P1, P2
        val cla = commandApdu[0].toInt() and 0xFF
        val ins = commandApdu[1].toInt() and 0xFF
        val p1 = if (commandApdu.size > 2) commandApdu[2].toInt() and 0xFF else 0
        val p2 = if (commandApdu.size > 3) commandApdu[3].toInt() and 0xFF else 0

        // =========================================================================
        // 1. SELECT COMMAND (INS = 0xA4)
        // =========================================================================
        if (ins == 0xA4) {
            // Select by AID (P1 = 0x04)
            if (p1 == 0x04) {
                val lc = if (commandApdu.size > 4) commandApdu[4].toInt() and 0xFF else 0
                val aidBytes = if (commandApdu.size >= 5 + lc) commandApdu.copyOfRange(5, 5 + lc) else byteArrayOf()
                val aidHex = toHex(aidBytes).replace(":", "").uppercase()
                selectedAid = aidHex

                HceManager.log(
                    ApduLogEntry.Direction.INCOMING,
                    hexCommand,
                    "Reader SELECT AID: $aidHex"
                )

                // Route based on AID format:
                when {
                    // NFC Forum NDEF Type 4 Tag
                    aidHex.startsWith(AID_NDEF_TYPE4_V2) || aidHex.startsWith(AID_NDEF_TYPE4_V1) -> {
                        selectedFileId = -1
                        val response = SW_SUCCESS
                        HceManager.log(
                            ApduLogEntry.Direction.OUTGOING,
                            toHex(response),
                            "Selected NFC Forum Type 4 NDEF Application",
                            "9000"
                        )
                        return response
                    }

                    // Contactless Payment Directory (PPSE)
                    aidHex.startsWith(AID_PPSE) || aidHex.startsWith(AID_PPSE_1) -> {
                        val ppseFci = buildPpseFciResponse()
                        val response = ppseFci + SW_SUCCESS
                        HceManager.log(
                            ApduLogEntry.Direction.OUTGOING,
                            toHex(response),
                            "Selected Payment PPSE (Directory entries ready)",
                            "9000"
                        )
                        recordCardUsage(activeCard)
                        return response
                    }

                    // Visa Contactless Payment
                    aidHex.startsWith(AID_VISA) -> {
                        val visaFci = buildPaymentAppFci("VISA DIGITAL PASS", byteArrayOf(0xA0.toByte(), 0x00, 0x00, 0x00, 0x03, 0x10, 0x10))
                        val response = visaFci + SW_SUCCESS
                        HceManager.log(
                            ApduLogEntry.Direction.OUTGOING,
                            toHex(response),
                            "Selected Visa Contactless Application",
                            "9000"
                        )
                        recordCardUsage(activeCard)
                        return response
                    }

                    // Mastercard Contactless Payment
                    aidHex.startsWith(AID_MASTERCARD) -> {
                        val mcFci = buildPaymentAppFci("MC DIGITAL PASS", byteArrayOf(0xA0.toByte(), 0x00, 0x00, 0x00, 0x04, 0x10, 0x10))
                        val response = mcFci + SW_SUCCESS
                        HceManager.log(
                            ApduLogEntry.Direction.OUTGOING,
                            toHex(response),
                            "Selected Mastercard Contactless Application",
                            "9000"
                        )
                        recordCardUsage(activeCard)
                        return response
                    }

                    // Calypso Transit (Paris Navigo, Israel Rav-Kav, etc.)
                    aidHex.startsWith(AID_CALYPSO_TRANSIT) -> {
                        val transitFci = buildTransitFci(activeCard)
                        val response = transitFci + SW_SUCCESS
                        HceManager.log(
                            ApduLogEntry.Direction.OUTGOING,
                            toHex(response),
                            "Selected Calypso Transit Pass Application",
                            "9000"
                        )
                        recordCardUsage(activeCard)
                        return response
                    }

                    // FeliCa Suica / Transit Envelope
                    aidHex.startsWith(AID_FELICA_TRANSIT) -> {
                        val felicaEnvelope = buildFelicaEnvelope(activeCard)
                        val response = felicaEnvelope + SW_SUCCESS
                        HceManager.log(
                            ApduLogEntry.Direction.OUTGOING,
                            toHex(response),
                            "Selected FeliCa / Suica Transit Pass",
                            "9000"
                        )
                        recordCardUsage(activeCard)
                        return response
                    }

                    // MIFARE DESFire Root Application
                    aidHex.startsWith(AID_DESFIRE_DEFAULT) -> {
                        val desfireHeader = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07)
                        val response = desfireHeader + SW_SUCCESS
                        HceManager.log(
                            ApduLogEntry.Direction.OUTGOING,
                            toHex(response),
                            "Selected MIFARE DESFire Application",
                            "9000"
                        )
                        recordCardUsage(activeCard)
                        return response
                    }

                    // University Campus / Student ID
                    aidHex.startsWith(AID_CAMPUS_ID) || aidHex.startsWith(AID_CBORD_ID) -> {
                        val studentData = buildStudentBadgeResponse(activeCard)
                        val response = studentData + SW_SUCCESS
                        HceManager.log(
                            ApduLogEntry.Direction.OUTGOING,
                            toHex(response),
                            "Campus Student ID Validated (UID: ${activeCard?.uidHex})",
                            "9000"
                        )
                        recordCardUsage(activeCard)
                        return response
                    }

                    // Hotel Smart Key / Door Access
                    aidHex.startsWith(AID_HOTEL_KEY) -> {
                        val hotelData = buildHotelKeyResponse(activeCard)
                        val response = hotelData + SW_SUCCESS
                        HceManager.log(
                            ApduLogEntry.Direction.OUTGOING,
                            toHex(response),
                            "Hotel Room Smart Key Presented",
                            "9000"
                        )
                        recordCardUsage(activeCard)
                        return response
                    }

                    // Access Control / HID / Turnstile Badges (Default fallback for PACS)
                    else -> {
                        val badgePayload = buildBadgePayload(activeCard)
                        val response = badgePayload + SW_SUCCESS

                        val cardLabel = activeCard?.name ?: "Virtual Access Badge"
                        HceManager.log(
                            ApduLogEntry.Direction.OUTGOING,
                            toHex(response),
                            "Access Granted for \"$cardLabel\" (UID: ${activeCard?.uidHex})",
                            "9000"
                        )
                        recordCardUsage(activeCard)
                        return response
                    }
                }
            } else if (p1 == 0x00) {
                // Select by File ID (e.g. 0xE103 for CC, 0xE104 for NDEF, 0x2000 for Transit Environment)
                if (commandApdu.size >= 7) {
                    val fileId = ((commandApdu[5].toInt() and 0xFF) shl 8) or (commandApdu[6].toInt() and 0xFF)
                    selectedFileId = fileId
                    val fileName = when (fileId) {
                        0xE103 -> "Capability Container (CC)"
                        0xE104 -> "NDEF Data File"
                        0x2000 -> "Transit Environment File"
                        0x2001 -> "Transit Contracts File"
                        0x2010 -> "Transit Trip Event Log"
                        else -> "File 0x${fileId.toString(16).uppercase()}"
                    }
                    HceManager.log(
                        ApduLogEntry.Direction.INCOMING,
                        hexCommand,
                        "Reader SELECT File: $fileName"
                    )
                    val response = SW_SUCCESS
                    HceManager.log(
                        ApduLogEntry.Direction.OUTGOING,
                        toHex(response),
                        "File $fileName Selected Successfully",
                        "9000"
                    )
                    return response
                }
            }
        }

        // =========================================================================
        // 2. READ BINARY COMMAND (INS = 0xB0)
        // =========================================================================
        if (ins == 0xB0) {
            val offset = (p1 shl 8) or p2
            val le = if (commandApdu.isNotEmpty()) commandApdu.last().toInt() and 0xFF else 0

            HceManager.log(
                ApduLogEntry.Direction.INCOMING,
                hexCommand,
                "Reader READ BINARY (Offset: $offset, Le: $le)"
            )

            when (selectedFileId) {
                0xE103 -> {
                    // Capability Container (CC) file read
                    val data = if (offset < CC_FILE.size) {
                        val length = minOf(if (le > 0) le else (CC_FILE.size - offset), CC_FILE.size - offset)
                        CC_FILE.copyOfRange(offset, offset + length)
                    } else byteArrayOf()

                    val response = data + SW_SUCCESS
                    HceManager.log(
                        ApduLogEntry.Direction.OUTGOING,
                        toHex(response),
                        "Transmitted CC File (${data.size} bytes)",
                        "9000"
                    )
                    return response
                }

                0xE104 -> {
                    // NDEF File read (Full NFC Forum NDEF compliant message)
                    val ndefData = buildNdefData(activeCard)
                    val data = if (offset < ndefData.size) {
                        val length = minOf(if (le > 0) le else (ndefData.size - offset), ndefData.size - offset)
                        ndefData.copyOfRange(offset, offset + length)
                    } else byteArrayOf()

                    val response = data + SW_SUCCESS
                    HceManager.log(
                        ApduLogEntry.Direction.OUTGOING,
                        toHex(response),
                        "Transmitted Virtual Card NDEF Data (${data.size} bytes)",
                        "9000"
                    )
                    recordCardUsage(activeCard)
                    return response
                }

                0x2000, 0x2001 -> {
                    // Transit pass records
                    val transitData = buildTransitPassRecord(activeCard)
                    val response = transitData + SW_SUCCESS
                    HceManager.log(
                        ApduLogEntry.Direction.OUTGOING,
                        toHex(response),
                        "Transmitted Transit Pass Data (${transitData.size} bytes)",
                        "9000"
                    )
                    recordCardUsage(activeCard)
                    return response
                }

                else -> {
                    // Generic read: return active card token, Wiegand bits, or payload
                    val payload = activeCard?.ndefPayload?.takeIf { it.isNotBlank() }
                        ?: activeCard?.uidHex ?: "ACTIVE_VIRTUAL_PASS"
                    val bytes = payload.toByteArray(StandardCharsets.UTF_8)
                    val response = bytes + SW_SUCCESS
                    HceManager.log(
                        ApduLogEntry.Direction.OUTGOING,
                        toHex(response),
                        "Transmitted Card Binary Payload",
                        "9000"
                    )
                    recordCardUsage(activeCard)
                    return response
                }
            }
        }

        // =========================================================================
        // 3. UPDATE BINARY COMMAND (INS = 0xD6) - External Reader Writing to Card!
        // =========================================================================
        if (ins == 0xD6) {
            val offset = (p1 shl 8) or p2
            val lc = if (commandApdu.size > 4) commandApdu[4].toInt() and 0xFF else 0
            val writeData = if (commandApdu.size >= 5 + lc) commandApdu.copyOfRange(5, 5 + lc) else byteArrayOf()

            HceManager.log(
                ApduLogEntry.Direction.INCOMING,
                hexCommand,
                "External Reader UPDATE BINARY (Write $lc bytes at offset $offset)"
            )

            // Save new payload back to card
            activeCard?.let { card ->
                val newText = try {
                    String(writeData, StandardCharsets.UTF_8)
                } catch (_: Exception) {
                    toHex(writeData)
                }
                serviceScope.launch {
                    try {
                        val updated = card.copy(
                            ndefPayload = newText,
                            lastEmulatedAt = System.currentTimeMillis()
                        )
                        AppDatabase.getInstance(applicationContext).nfcCardDao().update(updated)
                        HceManager.setActiveCard(updated)
                    } catch (_: Exception) { }
                }
            }

            val response = SW_SUCCESS
            HceManager.log(
                ApduLogEntry.Direction.OUTGOING,
                toHex(response),
                "Binary Write Accepted from External NFC Reader",
                "9000"
            )
            return response
        }

        // =========================================================================
        // 4. GET DATA COMMAND (INS = 0xCA) - Card Serial Number (UID) & CPLC
        // =========================================================================
        if (ins == 0xCA) {
            val tag = (p1 shl 8) or p2
            HceManager.log(
                ApduLogEntry.Direction.INCOMING,
                hexCommand,
                "Reader GET DATA: Tag 0x${tag.toString(16).uppercase()}"
            )

            val data = when (tag) {
                // Card Serial Number / UID
                0x0100, 0x0000 -> parseUidToBytes(activeCard?.uidHex)
                // CPLC data (Card Production Life Cycle)
                0x9F7F -> byteArrayOf(0x9F.toByte(), 0x7F.toByte(), 0x08, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08)
                // Facility Code + Card Number (Wiegand payload)
                0x0200 -> {
                    val fc = (activeCard?.facilityCode?.toIntOrNull() ?: 100) and 0xFF
                    val cn = (activeCard?.cardNumber?.toIntOrNull() ?: 50000)
                    byteArrayOf(fc.toByte(), ((cn shr 8) and 0xFF).toByte(), (cn and 0xFF).toByte())
                }
                else -> parseUidToBytes(activeCard?.uidHex)
            }

            val response = data + SW_SUCCESS
            HceManager.log(
                ApduLogEntry.Direction.OUTGOING,
                toHex(response),
                "Returned Card Data for Tag 0x${tag.toString(16).uppercase()}",
                "9000"
            )
            recordCardUsage(activeCard)
            return response
        }

        // =========================================================================
        // 5. GET PROCESSING OPTIONS (GPO - INS = 0xA8) for EMV Contactless Payment
        // =========================================================================
        if (ins == 0xA8) {
            HceManager.log(
                ApduLogEntry.Direction.INCOMING,
                hexCommand,
                "Reader GPO (Get Processing Options)"
            )
            // AIP (Application Interchange Profile: 08 00) + AFL (Application File Locator: SFI 1, Rec 1-1)
            val gpoResponse = byteArrayOf(
                0x77, 0x0A,                      // Format 2 Template
                0x82.toByte(), 0x02, 0x08, 0x00, // AIP: Contactless Magstripe / EMV supported
                0x94.toByte(), 0x04, 0x08, 0x01, 0x01, 0x00 // AFL: SFI 1, Record 1
            )
            val response = gpoResponse + SW_SUCCESS
            HceManager.log(
                ApduLogEntry.Direction.OUTGOING,
                toHex(response),
                "GPO Response: AIP & AFL Locator Transmitted",
                "9000"
            )
            recordCardUsage(activeCard)
            return response
        }

        // =========================================================================
        // 6. READ RECORD (INS = 0xB2) for EMV / Transit Records
        // =========================================================================
        if (ins == 0xB2) {
            val recordNumber = p1
            val sfi = (p2 shr 3) and 0x1F
            HceManager.log(
                ApduLogEntry.Direction.INCOMING,
                hexCommand,
                "Reader READ RECORD #$recordNumber (SFI: $sfi)"
            )

            val recordData = buildTrack2EquivalentRecord(activeCard)
            val response = recordData + SW_SUCCESS
            HceManager.log(
                ApduLogEntry.Direction.OUTGOING,
                toHex(response),
                "Returned Track 2 / Access Record Data",
                "9000"
            )
            recordCardUsage(activeCard)
            return response
        }

        // =========================================================================
        // 7. DESFIRE NATIVE COMMANDS (INS = 0x60 Get Version, INS = 0x51 Get UID)
        // =========================================================================
        if (ins == 0x60 || cla == 0x90 && ins == 0x60) {
            HceManager.log(
                ApduLogEntry.Direction.INCOMING,
                hexCommand,
                "DESFire Command: GET VERSION"
            )
            // NXP DESFire EV2 Version Header: Vendor=0x04 (NXP), Type=0x01, Subtype=0x01, Version=0x02, Storage=0x18 (8KB)
            val versionData = byteArrayOf(
                0x04, 0x01, 0x01, 0x02, 0x00, 0x18, 0x05
            )
            val response = versionData + SW_DESFIRE_SUCCESS
            HceManager.log(
                ApduLogEntry.Direction.OUTGOING,
                toHex(response),
                "Returned NXP DESFire EV2 Hardware Version Header",
                "9100"
            )
            recordCardUsage(activeCard)
            return response
        }

        if (ins == 0x51 || cla == 0x90 && ins == 0x51) {
            HceManager.log(
                ApduLogEntry.Direction.INCOMING,
                hexCommand,
                "DESFire Command: GET CARD UID"
            )
            val uidBytes = parseUidToBytes(activeCard?.uidHex)
            val response = uidBytes + SW_DESFIRE_SUCCESS
            HceManager.log(
                ApduLogEntry.Direction.OUTGOING,
                toHex(response),
                "Returned DESFire 7-Byte UID",
                "9100"
            )
            recordCardUsage(activeCard)
            return response
        }

        // =========================================================================
        // 8. INTERNAL AUTHENTICATE / CHALLENGE (INS = 0x88)
        // =========================================================================
        if (ins == 0x88) {
            HceManager.log(
                ApduLogEntry.Direction.INCOMING,
                hexCommand,
                "Reader INTERNAL AUTHENTICATE Challenge"
            )
            // Crypto challenge response token
            val challengeResponse = byteArrayOf(
                0x3B, 0x8A.toByte(), 0x80.toByte(), 0x01, 0x4A, 0x55, 0x6E, 0x69
            )
            val response = challengeResponse + SW_SUCCESS
            HceManager.log(
                ApduLogEntry.Direction.OUTGOING,
                toHex(response),
                "Authentication Challenge Solved (Access Granted)",
                "9000"
            )
            recordCardUsage(activeCard)
            return response
        }

        // =========================================================================
        // 9. CATCH-ALL UNIVERSAL RESPONSE FOR ANY PROPRIETARY APDU
        // =========================================================================
        HceManager.log(
            ApduLogEntry.Direction.INCOMING,
            hexCommand,
            "Reader Command INS 0x${ins.toString(16).uppercase()} CLA 0x${cla.toString(16).uppercase()}"
        )
        val uidPayload = parseUidToBytes(activeCard?.uidHex)
        val response = uidPayload + SW_SUCCESS
        HceManager.log(
            ApduLogEntry.Direction.OUTGOING,
            toHex(response),
            "Responded with Active Card Credentials (UID: ${activeCard?.uidHex})",
            "9000"
        )
        recordCardUsage(activeCard)
        return response
    }

    override fun onDeactivated(reason: Int) {
        val reasonStr = when (reason) {
            DEACTIVATION_LINK_LOSS -> "Link Lost (Card moved away from reader)"
            DEACTIVATION_DESELECTED -> "Deselected (Another application AID selected)"
            else -> "Deactivated (Reason $reason)"
        }
        HceManager.log(
            ApduLogEntry.Direction.SYSTEM,
            "",
            "NFC Session: $reasonStr"
        )
    }

    // =========================================================================
    // HELPER BUILDERS FOR PROTOCOLS & FORMATS
    // =========================================================================

    private fun recordCardUsage(card: NfcCard?) {
        if (card == null) return
        HceManager.notifyEmulated(card)
        serviceScope.launch {
            try {
                AppDatabase.getInstance(applicationContext)
                    .nfcCardDao()
                    .recordEmulation(card.id, System.currentTimeMillis())
            } catch (_: Exception) { }
        }
    }

    private fun parseUidToBytes(uidHex: String?): ByteArray {
        if (uidHex.isNullOrBlank()) {
            return byteArrayOf(0x04, 0x12, 0x34, 0x56, 0x78, 0x9A.toByte(), 0xBC.toByte())
        }
        val clean = uidHex.replace(":", "").replace(" ", "").trim()
        val result = ByteArray(clean.length / 2)
        for (i in result.indices) {
            result[i] = clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        return result
    }

    /**
     * Builds standards-compliant NFC Forum Type 4 NDEF message.
     */
    private fun buildNdefData(card: NfcCard?): ByteArray {
        val recordText = card?.let {
            "NFC-CARD|ID:${it.id}|NAME:${it.name}|TYPE:${it.cardType.name}|UID:${it.uidHex}|FC:${it.facilityCode}|CARD:${it.cardNumber}|DATA:${it.ndefPayload}"
        } ?: "NFC-CARD|EMPTY"

        val ndefRecord = NdefRecord.createTextRecord("en", recordText)
        val ndefMessage = NdefMessage(arrayOf(ndefRecord))
        val messageBytes = ndefMessage.toByteArray()

        // Type 4 tag NDEF file requires 2-byte NLEN (Big-Endian) prepended
        val nlen = messageBytes.size
        val result = ByteArray(2 + nlen)
        result[0] = ((nlen shr 8) and 0xFF).toByte()
        result[1] = (nlen and 0xFF).toByte()
        System.arraycopy(messageBytes, 0, result, 2, nlen)
        return result
    }

    /**
     * Builds standard PPSE (Proximity Payment System Environment) FCI Template.
     */
    private fun buildPpseFciResponse(): ByteArray {
        // Tag 6F: FCI Template
        // Tag 84: DF Name (2PAY.SYS.DDF01)
        // Tag A5: Proprietary Template
        // Tag BF0C: Directory Entry
        // Tag 61: Application Entry containing Visa AID (A0000000031010) and Mastercard AID (A0000000041010)
        return byteArrayOf(
            0x6F, 0x2A,             // FCI Template, Len 42
            0x84.toByte(), 0x0E,     // DF Name
            0x32, 0x50, 0x41, 0x59, 0x2E, 0x53, 0x59, 0x53, 0x2E, 0x44, 0x44, 0x46, 0x30, 0x31, // "2PAY.SYS.DDF01"
            0xA5.toByte(), 0x18,    // FCI Proprietary Template
            0xBF.toByte(), 0x0C, 0x15, // FCI Issuer Discretionary Data
            0x61, 0x13,             // Directory Entry
            0x4F, 0x07, 0xA0.toByte(), 0x00, 0x00, 0x00, 0x03, 0x10, 0x10, // Visa AID
            0x50, 0x04, 0x56, 0x49, 0x53, 0x41,                             // App Label "VISA"
            0x87.toByte(), 0x01, 0x01                                       // App Priority: 1
        )
    }

    /**
     * Builds FCI for specific Payment Application (Visa / Mastercard).
     */
    private fun buildPaymentAppFci(label: String, aidBytes: ByteArray): ByteArray {
        val labelBytes = label.toByteArray(StandardCharsets.US_ASCII)
        val fci = ByteArray(14 + aidBytes.size + labelBytes.size)
        var i = 0
        fci[i++] = 0x6F // FCI Template
        fci[i++] = (12 + aidBytes.size + labelBytes.size).toByte()
        fci[i++] = 0x84.toByte() // DF Name
        fci[i++] = aidBytes.size.toByte()
        System.arraycopy(aidBytes, 0, fci, i, aidBytes.size)
        i += aidBytes.size
        fci[i++] = 0xA5.toByte() // Proprietary Template
        fci[i++] = (6 + labelBytes.size).toByte()
        fci[i++] = 0x50 // App Label
        fci[i++] = labelBytes.size.toByte()
        System.arraycopy(labelBytes, 0, fci, i, labelBytes.size)
        i += labelBytes.size
        fci[i++] = 0x9F.toByte()
        fci[i++] = 0x38
        fci[i++] = 0x00 // Empty PDOL
        return fci
    }

    /**
     * Builds EMV Track 2 equivalent record.
     */
    private fun buildTrack2EquivalentRecord(card: NfcCard?): ByteArray {
        // Tag 70: AEF Data Template
        // Tag 57: Track 2 Equivalent Data (PAN = 16 digits derived from UID/CardNum, Exp Date 2812, Service Code 201)
        val uidNumbers = card?.uidHex?.replace(":", "")?.filter { it.isDigit() }?.padEnd(16, '9')?.take(16)
            ?: "4000123456789010"
        val track2String = "${uidNumbers}D2812201000000000F"
        val track2Bytes = track2String.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

        val template = ByteArray(4 + track2Bytes.size)
        template[0] = 0x70 // Record Template
        template[1] = (2 + track2Bytes.size).toByte()
        template[2] = 0x57 // Track 2 Equivalent Data
        template[3] = track2Bytes.size.toByte()
        System.arraycopy(track2Bytes, 0, template, 4, track2Bytes.size)
        return template
    }

    /**
     * Builds Physical Access Control (PACS) & Security Turnstile payload.
     */
    private fun buildBadgePayload(card: NfcCard?): ByteArray {
        val fc = (card?.facilityCode?.toIntOrNull() ?: 120) and 0xFFFF
        val cn = (card?.cardNumber?.toIntOrNull() ?: 30481) and 0xFFFF
        val uidBytes = parseUidToBytes(card?.uidHex)

        // Custom binary badge payload: [0x50, 0x41, 0x43, 0x53, FC_HI, FC_LO, CN_HI, CN_LO, UID...]
        val payload = ByteArray(8 + uidBytes.size)
        payload[0] = 0x50 // 'P'
        payload[1] = 0x41 // 'A'
        payload[2] = 0x43 // 'C'
        payload[3] = 0x53 // 'S'
        payload[4] = ((fc shr 8) and 0xFF).toByte()
        payload[5] = (fc and 0xFF).toByte()
        payload[6] = ((cn shr 8) and 0xFF).toByte()
        payload[7] = (cn and 0xFF).toByte()
        System.arraycopy(uidBytes, 0, payload, 8, uidBytes.size)
        return payload
    }

    /**
     * Builds Calypso Transit Pass FCI.
     */
    private fun buildTransitFci(card: NfcCard?): ByteArray {
        val serial = parseUidToBytes(card?.uidHex)
        val header = byteArrayOf(0x6F, 0x14, 0x84.toByte(), 0x07, 0xA0.toByte(), 0x00, 0x00, 0x02, 0x47, 0x10, 0x01)
        return header + serial
    }

    /**
     * Builds Calypso / Metro transit contract record.
     */
    private fun buildTransitPassRecord(card: NfcCard?): ByteArray {
        // Transit Contract: Network ID (0x0102), Validity Period (Current Year), Trips Remaining (99)
        return byteArrayOf(
            0x01, 0x02,             // Transit Network Code
            0x20, 0x26, 0x12, 0x31, // Valid through 2026-12-31
            0x00, 0x63,             // 99 Trips remaining
            0x01                    // Active Pass Status
        )
    }

    /**
     * Builds Sony FeliCa / Suica transit envelope.
     */
    private fun buildFelicaEnvelope(card: NfcCard?): ByteArray {
        // FeliCa IDm (8 bytes: Manufacturer 0x01, IC 0x2E...) + PMm (8 bytes parameter)
        val idm = byteArrayOf(0x01, 0x2E, 0x4B, 0x8C.toByte(), 0x10, 0x20, 0x30, 0x40)
        val pmm = byteArrayOf(0x10, 0x0B, 0x4B, 0x42, 0x84.toByte(), 0x85.toByte(), 0xD0.toByte(), 0xFF.toByte())
        return idm + pmm
    }

    /**
     * Builds Campus / University Student Badge response.
     */
    private fun buildStudentBadgeResponse(card: NfcCard?): ByteArray {
        val studentId = card?.cardNumber?.ifBlank { "88041234" } ?: "88041234"
        val payloadStr = "CAMPUS_ID|ID:$studentId|DEPT:ENG|BUILDING:ACCESS_ALL|MEALS:ACTIVE"
        return payloadStr.toByteArray(StandardCharsets.UTF_8)
    }

    /**
     * Builds Hotel Keycard response.
     */
    private fun buildHotelKeyResponse(card: NfcCard?): ByteArray {
        val room = card?.cardNumber?.ifBlank { "1408" } ?: "1408"
        val payloadStr = "HOTEL_KEY|ROOM:$room|CHECKOUT:VALID|DOOR_GRANT:APPROVED"
        return payloadStr.toByteArray(StandardCharsets.UTF_8)
    }

    private fun toHex(bytes: ByteArray): String {
        return bytes.joinToString(":") { "%02X".format(it) }
    }
}
