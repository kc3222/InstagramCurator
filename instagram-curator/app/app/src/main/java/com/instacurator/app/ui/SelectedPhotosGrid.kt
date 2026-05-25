package com.instacurator.app.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.instacurator.app.ui.theme.InstaCuratorTheme

/**
 * A 3-column grid of selected-photo thumbnails. Tapping a thumbnail removes it
 * from the selection via [onPhotoTap].
 */
@Composable
fun SelectedPhotosGrid(
	uris: List<Uri>,
	onPhotoTap: (Uri) -> Unit,
	modifier: Modifier = Modifier,
) {
	LazyVerticalGrid(
		columns = GridCells.Fixed(3),
		modifier = modifier,
		contentPadding = PaddingValues(4.dp),
		horizontalArrangement = Arrangement.spacedBy(4.dp),
		verticalArrangement = Arrangement.spacedBy(4.dp),
	) {
		items(uris, key = { it }) { uri ->
			Box(
				modifier = Modifier
					.aspectRatio(1f)
					.clickable { onPhotoTap(uri) },
			) {
				AsyncImage(
					model = uri,
					contentDescription = "Selected photo",
					contentScale = ContentScale.Crop,
					modifier = Modifier.fillMaxSize(),
				)
				Box(
					modifier = Modifier
						.align(Alignment.TopEnd)
						.padding(4.dp)
						.size(20.dp)
						.clip(CircleShape)
						.background(Color.Black.copy(alpha = 0.6f)),
					contentAlignment = Alignment.Center,
				) {
					Icon(
						imageVector = Icons.Filled.Close,
						contentDescription = "Remove photo",
						tint = Color.White,
						modifier = Modifier.size(14.dp),
					)
				}
			}
		}
	}
}

@Preview(showBackground = true)
@Composable
fun SelectedPhotosGridPreview() {
	InstaCuratorTheme {
		// Real URIs don't resolve in previews — this checks layout only.
		SelectedPhotosGrid(uris = emptyList(), onPhotoTap = {})
	}
}
