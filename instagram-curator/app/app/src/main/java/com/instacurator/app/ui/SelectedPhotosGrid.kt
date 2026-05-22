package com.instacurator.app.ui

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
			AsyncImage(
				model = uri,
				contentDescription = "Selected photo",
				contentScale = ContentScale.Crop,
				modifier = Modifier
					.aspectRatio(1f)
					.clickable { onPhotoTap(uri) },
			)
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
