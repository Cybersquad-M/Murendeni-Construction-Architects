package com.cybersquad.murendeni.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.util.Locale

// Details about a file the user picked with the phone's file picker.
data class PickedFile(
    val uri: Uri,
    val name: String,
    val size: Long,         // In bytes. -1 when the phone does not report a size.
    val mimeType: String
)

object FileUtils {

    // Reads the name, size and type of a picked file.
    fun describe(context: Context, uri: Uri): PickedFile {
        var name = "file"
        var size = -1L

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)

                if (nameIndex >= 0 && !cursor.isNull(nameIndex)) {
                    name = cursor.getString(nameIndex)
                }
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                    size = cursor.getLong(sizeIndex)
                }
            }
        }

        val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
        return PickedFile(uri, name, size, mimeType)
    }

    // Turns a byte count into text such as "850 KB" or "2.4 MB".
    fun formatSize(bytes: Long): String {
        return if (bytes < 1024 * 1024) {
            "${maxOf(bytes / 1024, 1L)} KB"
        } else {
            String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
        }
    }
}