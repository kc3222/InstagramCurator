package com.instacurator.app.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Remove
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.instacurator.app.pipeline.PipelineState
import com.instacurator.app.ui.theme.InstaCuratorTheme
import com.instacurator.app.viewmodel.MAX_PHOTOS
import com.instacurator.app.viewmodel.MainViewModel
import com.instacurator.app.viewmodel.PICK_RANGE

/**
 * Home screen: pick photos, review the selection, choose how many to curate,
 * launch the pipeline. While the pipeline runs we replace the body with a
 * progress indicator. The FinalResult state is handled in MainActivity.
 */
@Composable
fun HomeScreen(viewModel: MainViewModel = hiltViewModel()) {
	val context = LocalContext.current
	val pipelineState by viewModel.pipelineState.collectAsStateWithLifecycle()

	val pickMediaLauncher = rememberLauncherForActivityResult(
		ActivityResultContracts.PickMultipleVisualMedia()
	) { uris ->
		when {
			uris.isEmpty() -> Unit // user cancelled — keep current selection
			uris.size > MAX_PHOTOS ->
				Toast.makeText(context, "Max 100 photos at a time", Toast.LENGTH_SHORT).show()
			else -> viewModel.setPickedUris(uris)
		}
	}

	when (val state = pipelineState) {
		is PipelineState.Running -> {
			PipelineProgress(stage = state.stage, progress = state.progress); return
		}
		is PipelineState.AiScoring -> {
			PipelineProgress(stage = "Scoring photos with AI", progress = state.progress); return
		}
		is PipelineState.AiSelecting -> {
			PipelineProgress(stage = "Picking your final set", progress = state.progress); return
		}
		else -> Unit
	}

	HomeScreenContent(
		selectedUris = viewModel.selectedUris,
		pickCount = viewModel.pickCount,
		useOpenAi = viewModel.useOpenAi,
		onPickPhotos = {
			pickMediaLauncher.launch(
				PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
			)
		},
		onAnalyze = viewModel::runPipeline,
		onIncrement = viewModel::incrementPickCount,
		onDecrement = viewModel::decrementPickCount,
		onPickCountChange = viewModel::updatePickCount,
		onRemovePhoto = viewModel::removeUri,
		onUseOpenAiChange = { viewModel.useOpenAi = it },
		errorMessage = (pipelineState as? PipelineState.Error)?.msg,
	)
}

@Composable
private fun PipelineProgress(stage: String, progress: Float) {
	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(32.dp),
		verticalArrangement = Arrangement.Center,
		horizontalAlignment = Alignment.CenterHorizontally,
	) {
		Text(
			text = "$stage…",
			style = MaterialTheme.typography.titleMedium,
		)
		Spacer(Modifier.height(16.dp))
		LinearProgressIndicator(
			progress = { progress.coerceIn(0f, 1f) },
			modifier = Modifier.fillMaxWidth(),
		)
	}
}

@Composable
fun HomeScreenContent(
	selectedUris: List<Uri>,
	pickCount: Int,
	useOpenAi: Boolean,
	onPickPhotos: () -> Unit,
	onAnalyze: () -> Unit,
	onIncrement: () -> Unit,
	onDecrement: () -> Unit,
	onPickCountChange: (Int) -> Unit,
	onRemovePhoto: (Uri) -> Unit,
	onUseOpenAiChange: (Boolean) -> Unit,
	errorMessage: String? = null,
) {
	var pickCountText by remember { mutableStateOf(pickCount.toString()) }
	LaunchedEffect(pickCount) {
		if (pickCountText.toIntOrNull() != pickCount) {
			pickCountText = pickCount.toString()
		}
	}
	val parsedPickCount = pickCountText.toIntOrNull()
	val pickCountValid = parsedPickCount != null &&
		parsedPickCount in PICK_RANGE.first..PICK_RANGE.last

	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(24.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
	) {
		Row(
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(8.dp),
		) {
			Icon(
				imageVector = Icons.Filled.AutoAwesome,
				contentDescription = null,
				tint = MaterialTheme.colorScheme.primary,
			)
			Text(
				text = "InstaCurator",
				style = MaterialTheme.typography.headlineMedium,
			)
		}
		Text(
			text = "Pick up to 100 photos. Get the best ones.",
			style = MaterialTheme.typography.bodyMedium,
			modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
		)

		Button(onClick = onPickPhotos) {
			Icon(
				imageVector = Icons.Filled.PhotoLibrary,
				contentDescription = null,
				modifier = Modifier.padding(end = 8.dp),
			)
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
			) {
				Icon(
					imageVector = Icons.Filled.Remove,
					contentDescription = "Decrease",
				)
			}

			Text(
				text = "Photos to pick:",
				style = MaterialTheme.typography.bodyLarge,
			)

			OutlinedTextField(
				value = pickCountText,
				onValueChange = { raw ->
					val digits = raw.filter { it.isDigit() }.take(2)
					val parsed = digits.toIntOrNull()
					pickCountText = when {
						parsed == null -> digits
						parsed > PICK_RANGE.last -> PICK_RANGE.last.toString()
						else -> digits
					}
					pickCountText.toIntOrNull()?.let { value ->
						if (value in PICK_RANGE.first..PICK_RANGE.last) {
							onPickCountChange(value)
						}
					}
				},
				singleLine = true,
				keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
				textStyle = TextStyle(textAlign = androidx.compose.ui.text.style.TextAlign.Center),
				modifier = Modifier.width(56.dp),
			)

			OutlinedButton(
				onClick = onIncrement,
				enabled = pickCount < PICK_RANGE.last,
			) {
				Icon(
					imageVector = Icons.Filled.Add,
					contentDescription = "Increase",
				)
			}
		}

		Text(
			text = "Enter a number between ${PICK_RANGE.first} and ${PICK_RANGE.last}",
			style = MaterialTheme.typography.bodySmall,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
			modifier = Modifier.padding(top = 4.dp),
		)

		Row(
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(8.dp),
			modifier = Modifier.padding(top = 12.dp),
		) {
			Text(
				text = if (useOpenAi) "Use OpenAI (local + AI)" else "Local filtering only",
				style = MaterialTheme.typography.bodyMedium,
			)
			Switch(
				checked = useOpenAi,
				onCheckedChange = onUseOpenAiChange,
			)
		}

		SelectedPhotosGrid(
			uris = selectedUris,
			onPhotoTap = onRemovePhoto,
			modifier = Modifier
				.fillMaxWidth()
				.weight(1f)
				.padding(vertical = 16.dp),
		)

		if (errorMessage != null) {
			Row(
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(6.dp),
				modifier = Modifier.padding(bottom = 8.dp),
			) {
				Icon(
					imageVector = Icons.Filled.ErrorOutline,
					contentDescription = null,
					tint = MaterialTheme.colorScheme.error,
				)
				Text(
					text = "Error: $errorMessage",
					color = MaterialTheme.colorScheme.error,
					style = MaterialTheme.typography.bodySmall,
				)
			}
		}

		Button(
			onClick = onAnalyze,
			enabled = selectedUris.isNotEmpty() && pickCountValid,
			modifier = Modifier.fillMaxWidth(),
		) {
			Icon(
				imageVector = Icons.Filled.AutoAwesome,
				contentDescription = null,
				modifier = Modifier.padding(end = 8.dp),
			)
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
			useOpenAi = true,
			onPickPhotos = {},
			onAnalyze = {},
			onIncrement = {},
			onDecrement = {},
			onPickCountChange = {},
			onRemovePhoto = {},
			onUseOpenAiChange = {},
		)
	}
}
