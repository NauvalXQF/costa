package com.opankcs.costa.ui

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList
import com.opankcs.costa.data.Prefs

fun applyAppLanguage(context: Context, language: String) {
    val prefs = Prefs(context)
    prefs.appLanguage = language
    prefs.requestRestart()

    val manager = context.getSystemService(LocaleManager::class.java) ?: return
    manager.applicationLocales = when (language) {
        "in" -> LocaleList.forLanguageTags("in")
        "en" -> LocaleList.forLanguageTags("en")
        else -> LocaleList.getEmptyLocaleList()
    }
}