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
}
