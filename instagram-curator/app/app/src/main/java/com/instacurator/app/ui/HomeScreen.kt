package com.instacurator.app.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.instacurator.app.ui.theme.InstaCuratorTheme
import com.instacurator.app.viewmodel.MAX_PHOTOS
import com.instacurator.app.viewmodel.MainViewModel
import com.instacurator.app.viewmodel.PICK_RANGE

/**
 * Home screen: pick photos, review the selection, choose how many to curate.
 * Stateful entry point — owns the picker launcher and delegates rendering to
 * the stateless [HomeScreenContent] so the screen stays preview-able.
 */
@Composable
fun HomeScreen(viewModel: MainViewModel = viewModel()) {
	val context = LocalContext.current

	val pickMediaLauncher = rememberLauncherForActivityResult(
		ActivityResultContracts.PickMultipleVisualMedia()
	) { uris ->
		if (uris.size > MAX_PHOTOS) {
			Toast.makeText(context, "Max 100 photos at a time", Toast.LENGTH_SHORT).show()
		} else {
			viewModel.setPickedUris(uris)
		}
	}

	HomeScreenContent(
		selectedUris = viewModel.selectedUris,
		pickCount = viewModel.pickCount,
		onPickPhotos = {
			pickMediaLauncher.launch(
				PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
			)
		},
		onAnalyze = { /* Pipeline + backend call land in Phase 3. */ },
		onIncrement = viewModel::incrementPickCount,
		onDecrement = viewModel::decrementPickCount,
		onRemovePhoto = viewModel::removeUri,
	)
}

@Composable
fun HomeScreenContent(
	selectedUris: List<Uri>,
	pickCount: Int,
	onPickPhotos: () -> Unit,
	onAnalyze: () -> Unit,
	onIncrement: () -> Unit,
	onDecrement: () -> Unit,
	onRemovePhoto: (Uri) -> Unit,
) {
	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(24.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
	) {
		Text(
			text = "InstaCurator",
			style = MaterialTheme.typography.headlineMedium,
		)
		Text(
			text = "Pick up to 100 photos. Get the best ones.",
			style = MaterialTheme.typography.bodyMedium,
			modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
		)

		Button(onClick = onPickPhotos) {
			Text("Pick Photos")
		}

		Text(
			text = "${selectedUris.size} of $MAX_PHOTOS photos selected",
			style = MaterialTheme.typography.bodyMedium,
			modifier = Modifier.padding(top = 16.dp),
		)

		Row(
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(12.dp),
			modifier = Modifier.padding(top = 12.dp),
		) {
			OutlinedButton(
				onClick = onDecrement,
				enabled = pickCount > PICK_RANGE.first,
			) { Text("–") }

			Text(
				text = "Photos to pick: $pickCount",
				style = MaterialTheme.typography.bodyLarge,
			)

			OutlinedButton(
				onClick = onIncrement,
				enabled = pickCount < PICK_RANGE.last,
			) { Text("+") }
		}

		SelectedPhotosGrid(
			uris = selectedUris,
			onPhotoTap = onRemovePhoto,
			modifier = Modifier
				.fillMaxWidth()
				.weight(1f)
				.padding(vertical = 16.dp),
		)

		Button(
			onClick = onAnalyze,
			enabled = selectedUris.isNotEmpty(),
			modifier = Modifier.fillMaxWidth(),
		) {
			Text("Analyze")
		}
	}
}

@Preview(showBackground = true)
@Composable
fun HomeScreenContentPreview() {
	InstaCuratorTheme {
		HomeScreenContent(
			selectedUris = emptyList(),
			pickCount = 6,
			onPickPhotos = {},
			onAnalyze = {},
			onIncrement = {},
			onDecrement = {},
			onRemovePhoto = {},
		)
	}
}
