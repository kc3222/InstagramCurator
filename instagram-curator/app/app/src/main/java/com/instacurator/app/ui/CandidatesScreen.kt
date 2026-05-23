package com.instacurator.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.instacurator.app.pipeline.CandidatePhoto
import com.instacurator.app.ui.theme.InstaCuratorTheme

/**
 * Debug grid shown after the pipeline finishes. Each thumbnail carries a small
 * badge with the composite score so we can sanity-check the filtering. System
 * back returns to HomeScreen via [onBack] (which the caller wires to
 * MainViewModel.resetPipeline).
 */
@Composable
fun CandidatesScreen(
	candidates: List<CandidatePhoto>,
	onBack: () -> Unit,
) {
	BackHandler { onBack() }
	LazyVerticalGrid(
		columns = GridCells.Fixed(3),
		modifier = Modifier
			.fillMaxSize()
			.padding(8.dp),
		contentPadding = PaddingValues(4.dp),
		horizontalArrangement = Arrangement.spacedBy(4.dp),
		verticalArrangement = Arrangement.spacedBy(4.dp),
	) {
		items(candidates, key = { it.uri }) { candidate ->
			Box(modifier = Modifier.aspectRatio(1f)) {
				AsyncImage(
					model = candidate.uri,
					contentDescription = null,
					contentScale = ContentScale.Crop,
					modifier = Modifier.fillMaxSize(),
				)
				Box(
					modifier = Modifier
						.align(Alignment.BottomEnd)
						.padding(4.dp)
						.clip(RoundedCornerShape(4.dp))
						.background(Color.Black.copy(alpha = 0.6f))
						.padding(horizontal = 6.dp, vertical = 2.dp),
				) {
					Text(
						text = String.format("%.2f", candidate.compositeScore),
						color = Color.White,
						style = MaterialTheme.typography.labelSmall,
					)
				}
			}
		}
	}
}

@Preview(showBackground = true)
@Composable
fun CandidatesScreenPreview() {
	InstaCuratorTheme {
		CandidatesScreen(candidates = emptyList(), onBack = {})
	}
}
