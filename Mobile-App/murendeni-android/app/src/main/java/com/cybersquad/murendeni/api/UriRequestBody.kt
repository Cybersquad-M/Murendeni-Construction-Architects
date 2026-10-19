package com.cybersquad.murendeni.api

import android.content.Context
import android.net.Uri
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import java.io.IOException

// Sends a file picked with the phone's file picker as part of an API request.
// The file is streamed in small pieces, so a large file is never held in memory all at once.
class UriRequestBody(
    private val context: Context,
    private val uri: Uri,
    private val mimeType: String,
    private val size: Long
) : RequestBody() {

    override fun contentType(): MediaType? = mimeType.toMediaTypeOrNull()

    // -1 means the size is unknown, and the file is then sent in chunks.
    override fun contentLength(): Long = size

    override fun writeTo(sink: BufferedSink) {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IOException("The file could not be opened.")

        input.use { sink.writeAll(it.source()) }
    }
}