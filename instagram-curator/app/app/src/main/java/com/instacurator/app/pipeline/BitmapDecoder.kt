package com.instacurator.app.pipeline

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log

/**
 * Two-pass bitmap decode that downscales via inSampleSize so we never hold a
 * full-resolution bitmap in memory. Photo Picker URIs grant transient read
 * access — no permission needed.
 */
object BitmapDecoder {
	private const val TAG = "BitmapDecoder"

	fun decodeDownscaled(context: Context, uri: Uri, maxDim: Int = 600): Bitmap? {
		return try {
			val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
			context.contentResolver.openInputStream(uri)?.use { stream ->
				BitmapFactory.decodeStream(stream, null, bounds)
			}
			val width = bounds.outWidth
			val height = bounds.outHeight
			if (width <= 0 || height <= 0) {
				Log.w(TAG, "Could not read bounds for $uri")
				return null
			}

			val opts = BitmapFactory.Options().apply {
				inSampleSize = computeInSampleSize(width, height, maxDim)
				inPreferredConfig = Bitmap.Config.ARGB_8888
			}
			context.contentResolver.openInputStream(uri)?.use { stream ->
				BitmapFactory.decodeStream(stream, null, opts)
			}
		} catch (t: Throwable) {
			Log.w(TAG, "Failed to decode $uri", t)
			null
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
