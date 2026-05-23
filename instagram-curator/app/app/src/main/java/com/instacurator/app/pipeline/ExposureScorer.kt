package com.instacurator.app.pipeline

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfFloat
import org.opencv.core.MatOfInt
import org.opencv.imgproc.Imgproc

/**
 * Histogram-clipping based exposure quality: fraction of pixels stuck at pure
 * black (0) or pure white (255). Returns 1 - clipped, clamped to [0, 1].
 */
object ExposureScorer {
	fun exposureQuality(bitmap: Bitmap): Float {
		val src = Mat()
		val gray = Mat()
		val hist = Mat()
		try {
			Utils.bitmapToMat(bitmap, src)
			Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGBA2GRAY)
			Imgproc.calcHist(
				listOf(gray),
				MatOfInt(0),
				Mat(),
				hist,
				MatOfInt(256),
				MatOfFloat(0f, 256f),
			)

			val histArr = FloatArray(256)
			hist.get(0, 0, histArr)
			val clipped = histArr[0] + histArr[255]
			val totalPixels = gray.rows() * gray.cols()
			val clippedFraction = if (totalPixels > 0) clipped / totalPixels else 0f
			return (1f - clippedFraction).coerceIn(0f, 1f)
		} finally {
			src.release(); gray.release(); hist.release()
		}
	}
}
