package com.example

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import com.example.nfc.NfcController
import com.example.nfc.NfcHardwareState
import com.example.util.LanguageManager
import com.example.util.SupportedLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context default english`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("NFC Pass", appName)

        val tabWallet = context.getString(R.string.tab_wallet)
        assertEquals("My Cards", tabWallet)
    }

    @Test
    fun `verify multi-language translations exist and resolve`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val testLocales = listOf(
            Locale.forLanguageTag("es") to "Mis Tarjetas",
            Locale.forLanguageTag("fr") to "Mes Cartes",
            Locale.forLanguageTag("de") to "Meine Karten",
            Locale.forLanguageTag("ja") to "カード一覧",
            Locale.forLanguageTag("zh") to "我的卡包",
            Locale.forLanguageTag("hi") to "मेरे कार्ड",
            Locale.forLanguageTag("ar") to "بطاقاتي",
            Locale.forLanguageTag("pt") to "Meus Cartões",
            Locale.forLanguageTag("bn") to "আমার কার্ড",
            Locale.forLanguageTag("ru") to "Мои карты",
            Locale.forLanguageTag("ur") to "میرے کارڈز",
            Locale.forLanguageTag("id") to "Kartu Saya",
            Locale.forLanguageTag("ko") to "내 카드",
            Locale.forLanguageTag("it") to "Le Mie Carte",
            Locale.forLanguageTag("pl") to "Moje Karty",
            Locale.forLanguageTag("uk") to "Мої картки",
            Locale.forLanguageTag("tr") to "Kartlarım",
            Locale.forLanguageTag("vi") to "Thẻ của tôi"
        )

        for ((locale, expectedWalletTab) in testLocales) {
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            val localizedContext = context.createConfigurationContext(config)
            val translatedTab = localizedContext.getString(R.string.tab_wallet)
            assertEquals("Translation mismatch for locale $locale", expectedWalletTab, translatedTab)

            // Verify NFC status string resolution
            val nfcStatus = localizedContext.getString(R.string.nfc_status_ready)
            assertTrue("NFC ready status should not be empty for $locale", nfcStatus.isNotBlank())
        }
    }

    @Test
    fun `test LanguageManager locale wrapping for all 25 languages`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Verify total languages includes SYSTEM + 25 target languages
        assertEquals(26, SupportedLanguage.entries.size)

        for (lang in SupportedLanguage.entries) {
            if (lang == SupportedLanguage.SYSTEM) continue
            val localizedContext = LanguageManager.getLocalizedContext(context, lang)
            val title = localizedContext.getString(R.string.scan_title)
            assertTrue("Title should not be blank for ${lang.displayName}", title.isNotBlank())
        }
    }

    @Test
    fun `test sequential language switching updates context`() {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        LanguageManager.init(baseContext)

        LanguageManager.setLanguage(baseContext, SupportedLanguage.SPANISH)
        val spanContext = LanguageManager.getLocalizedContext(baseContext, SupportedLanguage.SPANISH)
        val spanTitle = spanContext.getString(R.string.tab_wallet)
        assertEquals("Mis Tarjetas", spanTitle)

        LanguageManager.setLanguage(spanContext, SupportedLanguage.FRENCH)
        val frContext = LanguageManager.getLocalizedContext(spanContext, SupportedLanguage.FRENCH)
        val frTitle = frContext.getString(R.string.tab_wallet)
        assertEquals("Mes Cartes", frTitle)

        LanguageManager.setLanguage(frContext, SupportedLanguage.GERMAN)
        val deContext = LanguageManager.getLocalizedContext(frContext, SupportedLanguage.GERMAN)
        val deTitle = deContext.getString(R.string.tab_wallet)
        assertEquals("Meine Karten", deTitle)
    }

    @Test
    fun `test NfcController hardware state initialization`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val controller = NfcController(context)

        assertNotNull(controller.hardwareState.value)
        assertTrue(
            controller.hardwareState.value == NfcHardwareState.READY ||
            controller.hardwareState.value == NfcHardwareState.DISABLED ||
            controller.hardwareState.value == NfcHardwareState.UNSUPPORTED
        )
    }

    @Test
    fun `test cloning strictly preserves original card contents and ignores user modifications`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val controller = NfcController(context)

        // Card originally scanned from a physical tag with raw UID and payload
        val cardWithEdits = com.example.data.NfcCard(
            id = 42L,
            uidHex = "04:DE:AD:BE:EF:01:02",
            name = "USER MODIFIED OFFICE NAME",
            description = "USER MODIFIED PRIVATE NOTES AND DESCRIPTION",
            category = "VIP Office",
            facilityName = "USER MODIFIED HEADQUARTERS",
            cardType = com.example.data.CardType.ACCESS_BADGE,
            facilityCode = "888",
            cardNumber = "99999",
            originalUidHex = "04:11:22:33:44:55:66",
            originalPayload = "ORIGINAL_PHYSICAL_RAW_NDEF_PAYLOAD",
            originalFacilityCode = "101",
            originalCardNumber = "54321",
            originalNdefMimeOrUri = "ORIGINAL_PHYSICAL_RAW_NDEF_PAYLOAD",
            notes = "TOP SECRET PERSONAL USER NOTES"
        )

        // Verify effective originals return original hardware scan values, not user modifications
        assertEquals("04:11:22:33:44:55:66", cardWithEdits.effectiveOriginalUidHex)
        assertEquals("ORIGINAL_PHYSICAL_RAW_NDEF_PAYLOAD", cardWithEdits.effectiveOriginalPayload)
        assertEquals("101", cardWithEdits.effectiveOriginalFacilityCode)
        assertEquals("54321", cardWithEdits.effectiveOriginalCardNumber)

        // Verify clone data summary contains original payload and excludes user edits
        val cloneSummary = controller.getOriginalCloneDataSummary(cardWithEdits)
        assertEquals("ORIGINAL_PHYSICAL_RAW_NDEF_PAYLOAD", cloneSummary)
        assertTrue("User edited name must not be in clone data", !cloneSummary.contains("USER MODIFIED OFFICE NAME"))
        assertTrue("User edited notes must not be in clone data", !cloneSummary.contains("TOP SECRET PERSONAL USER NOTES"))
        assertTrue("User edited description must not be in clone data", !cloneSummary.contains("USER MODIFIED PRIVATE NOTES"))

        // Test access card without raw NDEF payload: should clone original access credentials only
        val rawAccessCard = com.example.data.NfcCard(
            id = 43L,
            uidHex = "04:99:88:77:66:55:44",
            name = "User Custom Gym Tag",
            description = "User locker 402",
            category = "Gym",
            originalUidHex = "04:AA:BB:CC:DD:EE:FF",
            originalFacilityCode = "202",
            originalCardNumber = "12345",
            originalPayload = ""
        )

        val accessCloneSummary = controller.getOriginalCloneDataSummary(rawAccessCard)
        assertEquals("UID=04:AA:BB:CC:DD:EE:FF", accessCloneSummary)
        assertTrue("Clone summary must NOT contain facility code", !accessCloneSummary.contains("202"))
        assertTrue("Clone summary must NOT contain card number", !accessCloneSummary.contains("12345"))
        assertTrue("Clone summary must NOT contain category", !accessCloneSummary.contains("Gym"))
        assertTrue("Clone summary must NOT contain card name", !accessCloneSummary.contains("User Custom Gym Tag"))
        assertTrue("Clone summary must NOT contain user description", !accessCloneSummary.contains("User locker 402"))
    }
}
