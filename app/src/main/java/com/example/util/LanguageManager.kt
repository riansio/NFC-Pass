package com.example.util

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

enum class SupportedLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val flag: String
) {
    SYSTEM("", "System Default", "Default Device Locale", "🌐"),
    ENGLISH("en", "English", "English (US)", "🇺🇸"),
    MANDARIN("zh", "Mandarin", "中文 (简体)", "🇨🇳"),
    HINDI("hi", "Hindi", "हिन्दी", "🇮🇳"),
    SPANISH("es", "Spanish", "Español", "🇪🇸"),
    FRENCH("fr", "French", "Français", "🇫🇷"),
    ARABIC("ar", "Arabic", "العربية", "🇸🇦"),
    PORTUGUESE("pt", "Portuguese", "Português", "🇧🇷"),
    BENGALI("bn", "Bengali", "বাংলা", "🇧🇩"),
    RUSSIAN("ru", "Russian", "Русский", "🇷🇺"),
    URDU("ur", "Urdu", "اردو", "🇵🇰"),
    INDONESIAN("in", "Indonesian", "Bahasa Indonesia", "🇮🇩"),
    GERMAN("de", "German", "Deutsch", "🇩🇪"),
    JAPANESE("ja", "Japanese", "日本語", "🇯🇵"),
    PUNJABI("pa", "Punjabi", "ਪੰਜਾਬੀ", "🇮🇳"),
    JAVANESE("jv", "Javanese", "Basa Jawa", "🇮🇩"),
    TELUGU("te", "Telugu", "తెలుగు", "🇮🇳"),
    TURKISH("tr", "Turkish", "Türkçe", "🇹🇷"),
    KOREAN("ko", "Korean", "한국어", "🇰🇷"),
    MARATHI("mr", "Marathi", "मराठी", "🇮🇳"),
    VIETNAMESE("vi", "Vietnamese", "Tiếng Việt", "🇻🇳"),
    TAMIL("ta", "Tamil", "தமிழ்", "🇮🇳"),
    ITALIAN("it", "Italian", "Italiano", "🇮🇹"),
    POLISH("pl", "Polish", "Polski", "🇵🇱"),
    UKRAINIAN("uk", "Ukrainian", "Українська", "🇺🇦"),
    TAGALOG("tl", "Tagalog", "Tagalog / Filipino", "🇵🇭")
}

fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

object LanguageManager {
    private const val TAG = "LanguageManager"
    private const val PREFS_NAME = "nfc_pass_prefs"
    private const val KEY_LANGUAGE = "selected_language"

    var currentLanguage by mutableStateOf(SupportedLanguage.SYSTEM)
        private set

    fun getDeviceSystemLocale(): Locale {
        return try {
            val systemLocales = Resources.getSystem().configuration.locales
            if (!systemLocales.isEmpty) systemLocales[0] else Locale.getDefault()
        } catch (_: Exception) {
            Locale.getDefault()
        }
    }

    fun getSavedLanguage(context: Context): SupportedLanguage {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val code = prefs.getString(KEY_LANGUAGE, "") ?: ""
        return SupportedLanguage.entries.find { it.code == code } ?: SupportedLanguage.SYSTEM
    }

    fun init(context: Context) {
        val savedLang = getSavedLanguage(context)
        currentLanguage = savedLang
        applyLocaleToContexts(context, savedLang)
    }

    fun setLanguage(context: Context, language: SupportedLanguage) {
        currentLanguage = language
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, language.code).apply()

        // 1. Android 13+ (API 33+) native per-app locale
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val localeManager = context.getSystemService(LocaleManager::class.java)
                val localeList = if (language == SupportedLanguage.SYSTEM || language.code.isEmpty()) {
                    LocaleList.getEmptyLocaleList()
                } else {
                    LocaleList.forLanguageTags(language.code)
                }
                localeManager?.applicationLocales = localeList
            } catch (e: Exception) {
                Log.w(TAG, "LocaleManager applicationLocales error: ${e.message}")
            }
        }

        // 2. Set JVM default locale
        val locale = getLocaleForLanguage(language)
        Locale.setDefault(locale)

        // 3. Update configuration directly for immediate in-memory effect
        applyLocaleToContexts(context, language)

        // 4. Force activity recreate to reload all UI and strings completely
        val activity = context.findActivity()
        if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
            activity.recreate()
        }
    }

    fun getLocaleForLanguage(language: SupportedLanguage): Locale {
        return if (language == SupportedLanguage.SYSTEM || language.code.isEmpty()) {
            getDeviceSystemLocale()
        } else {
            when (language.code) {
                "in" -> Locale("in", "ID")
                "jv" -> Locale.Builder().setLanguage("jv").build()
                "tl" -> Locale("tl", "PH")
                "en" -> Locale.ENGLISH
                else -> Locale.forLanguageTag(language.code)
            }
        }
    }

    private fun applyLocaleToContexts(context: Context, language: SupportedLanguage) {
        val locale = getLocaleForLanguage(language)
        Locale.setDefault(locale)

        val contextsToUpdate = listOfNotNull(
            context,
            context.applicationContext,
            context.findActivity()
        )

        for (ctx in contextsToUpdate) {
            try {
                val res = ctx.resources
                val config = Configuration(res.configuration)
                config.setLocale(locale)
                config.setLayoutDirection(locale)
                @Suppress("DEPRECATION")
                res.updateConfiguration(config, res.displayMetrics)
            } catch (_: Exception) {
            }
        }
    }

    fun getLocalizedContext(context: Context, language: SupportedLanguage): Context {
        val locale = getLocaleForLanguage(language)
        val baseConfig = context.resources.configuration
        val config = Configuration(baseConfig)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }
}

@Composable
fun ProvideAppLanguage(content: @Composable () -> Unit) {
    val currentLang = LanguageManager.currentLanguage
    val context = LocalContext.current
    val localizedContext = remember(currentLang, context) {
        LanguageManager.getLocalizedContext(context.applicationContext, currentLang)
    }
    CompositionLocalProvider(
        LocalContext provides localizedContext,
        androidx.compose.ui.platform.LocalConfiguration provides localizedContext.resources.configuration
    ) {
        androidx.compose.runtime.key(currentLang) {
            content()
        }
    }
}
