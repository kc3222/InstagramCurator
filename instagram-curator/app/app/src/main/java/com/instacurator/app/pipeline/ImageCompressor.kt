package com.instacurator.app.pipeline

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * JPEG compression for the AI pipeline. Two preset sizes:
 *   - 600px / quality 75 → /score-batch (cheap pass, gpt-4o-mini)
 *   - 1200px / quality 85 → /cohesive-select (single pass, gpt-4.1)
 *
 * Uses inSampleSize so we never allocate a full-resolution bitmap.
 */
object ImageCompressor {
	private const val TAG = "ImageCompressor"

	suspend fun compressTo600px(context: Context, uri: Uri): ByteArray =
		compress(context, uri, maxDim = 600, quality = 75)

	suspend fun compressTo1200px(context: Context, uri: Uri): ByteArray =
		compress(context, uri, maxDim = 1200, quality = 85)

	private suspend fun compress(
		context: Context,
		uri: Uri,
		maxDim: Int,
		quality: Int,
	): ByteArray = withContext(Dispatchers.IO) {
		val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
		context.contentResolver.openInputStream(uri)?.use { stream ->
			BitmapFactory.decodeStream(stream, null, bounds)
		}
		val width = bounds.outWidth
		val height = bounds.outHeight
		if (width <= 0 || height <= 0) {
			Log.w(TAG, "Could not read bounds for $uri")
			return@withContext ByteArray(0)
		}

		val opts = BitmapFactory.Options().apply {
			inSampleSize = computeInSampleSize(width, height, maxDim)
			inPreferredConfig = Bitmap.Config.ARGB_8888
		}
		val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
			BitmapFactory.decodeStream(stream, null, opts)
		} ?: return@withContext ByteArray(0)

		try {
			val output = ByteArrayOutputStream()
			bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
			output.toByteArray()
		} finally {
			if (!bitmap.isRecycled) bitmap.recycle()
		}
	}

	private fun computeInSampleSize(width: Int, height: Int, maxDim: Int): Int {
		var sample = 1
		var w = width
		var h = height
		while ((w / 2) >= maxDim && (h / 2) >= maxDim) {
			w /= 2
			h /= 2
			sample *= 2
		}
		return sample
	}
}
