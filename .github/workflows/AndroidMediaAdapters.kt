package com.movierecap.ai.media

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import com.movierecap.ai.domain.GallerySaver
import com.movierecap.ai.domain.SocialSharer
import com.movierecap.ai.domain.ShareManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreGallerySaver(private val context: Context) : GallerySaver {
    override suspend fun saveMp4(source: Uri, displayName: String): Uri? = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Movie Recap AI")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val target = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext null
        try {
            val copied = resolver.openInputStream(source)?.use { input -> resolver.openOutputStream(target)?.use { output -> input.copyTo(output); true } ?: false } ?: false
            if (!copied) { resolver.delete(target, null, null); return@withContext null }
            values.clear(); values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(target, values, null, null)
            target
        } catch (_: Exception) { resolver.delete(target, null, null); null }
    }
}

class AndroidSocialSharer(private val context: Context) : SocialSharer, ShareManager {
    override fun shareMp4(uri: Uri) {
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, "Share exported video"))
    }
}
