package com.opankcs.costa.data

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import okhttp3.ResponseBody

object DownloadSaver {
    fun save(context: Context, fileName: String, mimeType: String, body: ResponseBody): Boolean {
        val resolver = context.contentResolver

        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false

        return try {
            resolver.openOutputStream(uri)?.use { output ->
                body.byteStream().use { input ->
                    input.copyTo(output)
                }
            } ?: run {
                resolver.delete(uri, null, null)
                return false
            }

            val done = ContentValues().apply {
                put(MediaStore.Downloads.IS_PENDING, 0)
            }
            resolver.update(uri, done, null, null)
            true
        } catch (_: Exception) {
            runCatching { resolver.delete(uri, null, null) }
            false
        }
    }
}
