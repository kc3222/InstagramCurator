package com.instacurator.app.pipeline

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

/** Two photos cluster as duplicates if their pHash Hamming distance is below this. */
const val DEDUP_HAMMING_THRESHOLD = 10

/**
 * 64-bit perceptual hash based on the discrete cosine transform. Robust to
 * resizing and small edits; designed for the duplicate-cluster stage.
 *
 * Algorithm: downscale to 32x32 grayscale, run DCT, take the top-left 8x8
 * low-frequency block, threshold each coefficient against the median of the
 * other 63 (DC term excluded), pack the 64 bits row-major into a Long.
 */
object PerceptualHash {
	fun pHash(bitmap: Bitmap): Long {
		val src = Mat()
		val gray = Mat()
		val resized = Mat()
		val gray32f = Mat()
		val dctOut = Mat()
		try {
			Utils.bitmapToMat(bitmap, src)
			Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGBA2GRAY)
			Imgproc.resize(gray, resized, Size(32.0, 32.0))
			resized.convertTo(gray32f, CvType.CV_32F)
			Core.dct(gray32f, dctOut)

			// Read the top-left 8x8 block of dctOut row by row.
			val coeffs = FloatArray(64)
			val rowBuf = FloatArray(8)
			for (r in 0 until 8) {
				dctOut.get(r, 0, rowBuf)
				System.arraycopy(rowBuf, 0, coeffs, r * 8, 8)
			}

			// Median of 63 coefficients (skip the DC term at index 0).
			val withoutDc = FloatArray(63)
			System.arraycopy(coeffs, 1, withoutDc, 0, 63)
			withoutDc.sort()
			val median = withoutDc[withoutDc.size / 2]

			var hash = 0L
			for (i in 0 until 64) {
				if (coeffs[i] > median) {
					hash = hash or (1L shl (63 - i))
				}
			}
			return hash
		} finally {
			src.release(); gray.release(); resized.release()
			gray32f.release(); dctOut.release()
		}
	}

	fun hammingDistance(a: Long, b: Long): Int = java.lang.Long.bitCount(a xor b)
}
