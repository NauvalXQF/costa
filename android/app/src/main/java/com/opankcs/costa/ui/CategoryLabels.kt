package com.opankcs.costa.ui

import android.content.Context
import com.opankcs.costa.R
import com.opankcs.costa.data.Category
import com.opankcs.costa.data.CategoryTotal

fun localizedCategoryName(context: Context, slug: String?, fallback: String?): String {
    val res = when (slug) {
        "makan" -> R.string.category_makan
        "bensin" -> R.string.category_bensin
        "gojek-grab" -> R.string.category_gojek_grab
        "listrik" -> R.string.category_listrik
        "kopi" -> R.string.category_kopi
        "gym" -> R.string.category_gym
        "parkir" -> R.string.category_parkir
        "kos" -> R.string.category_kos
        "laundry" -> R.string.category_laundry
        "kesehatan" -> R.string.category_kesehatan
        "hiburan" -> R.string.category_hiburan
        "internet" -> R.string.category_internet
        "lainnya" -> R.string.category_lainnya
        "jajan" -> R.string.category_jajan
        else -> 0
    }
    return if (res != 0) context.getString(res) else fallback ?: "-"
}

fun localizedCategoryName(context: Context, category: Category?): String =
    if (category == null) {
        "-"
    } else if (category.userId != null) {
        category.name
    } else {
        localizedCategoryName(context, category.slug, category.name)
    }

fun localizedCategoryName(context: Context, item: CategoryTotal): String =
    localizedCategoryName(context, item.slug, item.name)
