package com.opankcs.costa.ui

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.ContextWrapper
import android.os.LocaleList
import com.opankcs.costa.data.Prefs

fun applyAppLanguage(context: Context, language: String) {
    val manager = context.getSystemService(LocaleManager::class.java) ?: return
    manager.applicationLocales = when (language) {
        "in" -> LocaleList.forLanguageTags("in")
        "en" -> LocaleList.forLanguageTags("en")
        else -> LocaleList.getEmptyLocaleList()
    }
    Prefs(context).appLanguage = language
    context.findActivity()?.recreate()
}

private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}