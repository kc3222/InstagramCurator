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

	/**
	 * Pick [n] photos that are both high-scoring and visually distinct.
	 *
	 * Goes through candidates in score order and only accepts one whose pHash
	 * is at least [diversityHamming] bits away from every already-picked photo.
	 * If that diversity rule leaves fewer than [n] picks (because the user
	 * asked for more photos than there are distinct subjects), the remaining
	 * slots are filled by score, ignoring diversity. This means a request for
	 * 4 photos from 4 different subjects always returns 4 different subjects,
	 * but a request for 6 from 3 subjects still returns 6 photos.
	 *
	 * [diversityHamming] is intentionally looser than [DEDUP_HAMMING_THRESHOLD]:
	 * the cluster step merges near-identical shots, this step also rejects
	 * "different framing, same subject" pairs that cluster_dedup missed.
	 */
	fun selectDiverse(
		candidates: List<CandidatePhoto>,
		n: Int,
		diversityHamming: Int = 12,
	): List<CandidatePhoto> {
		if (n <= 0 || candidates.isEmpty()) return emptyList()
		val sorted = candidates.sortedByDescending { it.compositeScore }
		if (sorted.size <= n) return sorted

		val picked = mutableListOf<CandidatePhoto>()
		for (c in sorted) {
			if (picked.size >= n) break
			val tooClose = picked.any {
				PerceptualHash.hammingDistance(c.phash, it.phash) < diversityHamming
			}
			if (!tooClose) picked.add(c)
		}
		if (picked.size < n) {
			val pickedSet = picked.toHashSet()
			for (c in sorted) {
				if (picked.size >= n) break
				if (c !in pickedSet) picked.add(c)
			}
		}
		return picked
	}
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
