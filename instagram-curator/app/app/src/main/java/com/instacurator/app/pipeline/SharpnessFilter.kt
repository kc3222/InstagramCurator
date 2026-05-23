package com.instacurator.app.pipeline

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.imgproc.Imgproc
import kotlin.math.pow

/** Photos with Laplacian variance below this are considered too blurry to keep. */
const val SHARPNESS_THRESHOLD = 100.0

/**
 * Laplacian variance — standard "is the image in focus?" proxy. Higher = sharper.
 */
object SharpnessFilter {
	fun sharpnessVariance(bitmap: Bitmap): Double {
		val src = Mat()
		val gray = Mat()
		val lap = Mat()
		val mean = MatOfDouble()
		val stddev = MatOfDouble()
		try {
			Utils.bitmapToMat(bitmap, src)
			Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGBA2GRAY)
			Imgproc.Laplacian(gray, lap, CvType.CV_64F)
			Core.meanStdDev(lap, mean, stddev)
			val sd = stddev.get(0, 0)[0]
			return sd.pow(2)
		} finally {
			src.release(); gray.release(); lap.release()
			mean.release(); stddev.release()
		}
	}
}
