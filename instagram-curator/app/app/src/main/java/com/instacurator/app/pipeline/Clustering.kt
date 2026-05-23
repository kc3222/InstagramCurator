package com.instacurator.app.pipeline

/**
 * Union-find clustering by pHash Hamming distance, then composite-score winner
 * per cluster. O(n^2) which is fine for n up to ~85 (the practical cap after
 * the sharpness filter on a 100-photo input).
 */
object Clustering {

	fun clusterByPHash(items: List<CandidatePhoto>): List<List<CandidatePhoto>> {
		if (items.isEmpty()) return emptyList()
		val n = items.size
		val parent = IntArray(n) { it }

		fun find(x: Int): Int {
			var cur = x
			while (parent[cur] != cur) {
				parent[cur] = parent[parent[cur]]
				cur = parent[cur]
			}
			return cur
		}

		fun union(a: Int, b: Int) {
			val ra = find(a); val rb = find(b)
			if (ra != rb) parent[ra] = rb
		}

		for (i in 0 until n) {
			for (j in i + 1 until n) {
				if (PerceptualHash.hammingDistance(items[i].phash, items[j].phash) < DEDUP_HAMMING_THRESHOLD) {
					union(i, j)
				}
			}
		}

		val groups = HashMap<Int, MutableList<CandidatePhoto>>()
		for (i in 0 until n) {
			groups.getOrPut(find(i)) { mutableListOf() }.add(items[i])
		}
		return groups.values.toList()
	}

	fun pickClusterWinner(cluster: List<CandidatePhoto>): CandidatePhoto =
		cluster.maxBy { it.compositeScore }
}

/**
 * Composite score used to pick the winner inside each pHash cluster.
 *
 * Weights from the design doc:
 *   sharpness 50% | eyes-open 25% | smile 15% | exposure 10%.
 *
 * Sharpness is normalized by capping the Laplacian variance at 1000. That cap
 * is a starting point — tune in Phase 4 once we see real-world distributions.
 */
fun composite(
	sharpness: Double,
	eyesOpen: Float,
	smile: Float,
	exposure: Float,
): Double {
	val sharpnessNorm = (sharpness / 1000.0).coerceIn(0.0, 1.0)
	return 0.50 * sharpnessNorm +
		0.25 * eyesOpen +
		0.15 * smile +
		0.10 * exposure
}
