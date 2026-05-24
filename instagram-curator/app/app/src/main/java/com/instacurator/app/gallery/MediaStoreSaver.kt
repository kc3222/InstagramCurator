package com.instacurator.app.gallery

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class SaveResult(val saved: Int, val failed: Int) {
	val total: Int get() = saved + failed
}

/**
 * Copies the selected source URIs into the public Pictures/InstagramCurator/
 * directory via MediaStore. Uses the IS_PENDING pattern so partial writes never
 * surface in the gallery.
 *
 * Scoped-storage IS_PENDING + RELATIVE_PATH is Android 10+ only. On older
 * Android we surface a failure via [SaveResult] and let the UI show a clear
 * message; we don't bother with the legacy WRITE_EXTERNAL_STORAGE path.
 */
@Singleton
class MediaStoreSaver @Inject constructor(
	@ApplicationContext private val context: Context,
) {

	suspend fun saveAll(photos: List<Uri>): SaveResult = withContext(Dispatchers.IO) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
			return@withContext SaveResult(saved = 0, failed = photos.size)
		}
		var saved = 0
		var failed = 0
		val ts = System.currentTimeMillis()
		photos.forEachIndexed { index, source ->
			if (saveOne(source, index, ts)) saved++ else failed++
		}
		SaveResult(saved, failed)
	}

	private fun saveOne(source: Uri, index: Int, timestamp: Long): Boolean {
		val resolver = context.contentResolver
		val displayName = "InstagramCurator_${timestamp}_$index.jpg"
		val values = ContentValues().apply {
			put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
			put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
			put(
				MediaStore.MediaColumns.RELATIVE_PATH,
				"${Environment.DIRECTORY_PICTURES}/InstagramCurator",
			)
			put(MediaStore.MediaColumns.IS_PENDING, 1)
		}
		val destUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
			?: return false.also { Log.w(TAG, "MediaStore.insert returned null for $displayName") }

		return try {
			resolver.openInputStream(source)?.use { input ->
				resolver.openOutputStream(destUri)?.use { output ->
					input.copyTo(output)
				} ?: error("openOutputStream returned null")
			} ?: error("openInputStream returned null for $source")
			values.clear()
			values.put(MediaStore.MediaColumns.IS_PENDING, 0)
			resolver.update(destUri, values, null, null)
			true
		} catch (t: Throwable) {
			Log.w(TAG, "Failed saving $source", t)
			runCatching { resolver.delete(destUri, null, null) }
			false
		}
	}

	private companion object {
		const val TAG = "MediaStoreSaver"
	}
}
