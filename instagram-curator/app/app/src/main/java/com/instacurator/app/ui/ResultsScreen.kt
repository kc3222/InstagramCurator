package com.instacurator.app.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
 * Final results: vertical snap-scrolling 9:16 cards, one per selected photo,
 * with an index badge and a Save All FAB. System back returns home.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
	photos: List<Uri>,
	snackbarHostState: SnackbarHostState,
	onSaveAll: () -> Unit,
	onBack: () -> Unit,
) {
	BackHandler { onBack() }

	Scaffold(
		modifier = Modifier.fillMaxSize(),
		snackbarHost = { SnackbarHost(snackbarHostState) },
		topBar = {
			TopAppBar(
				title = { Text("Your Picks") },
				navigationIcon = {
					IconButton(onClick = onBack) {
						Icon(
							imageVector = Icons.AutoMirrored.Filled.ArrowBack,
							contentDescription = "Back",
						)
					}
				},
				actions = {
					TextButton(onClick = onBack) {
						Icon(
							imageVector = Icons.Filled.Refresh,
							contentDescription = null,
							modifier = Modifier.padding(end = 4.dp),
						)
						Text("Start again")
					}
				},
			)
		},
		floatingActionButton = {
			ExtendedFloatingActionButton(onClick = onSaveAll) {
				Icon(Icons.Default.Save, contentDescription = null)
				Text(text = "Save All", modifier = Modifier.padding(start = 8.dp))
			}
		},
	) { innerPadding ->
		val listState = rememberLazyListState()
		val flingBehavior = rememberSnapFlingBehavior(listState)
		LazyColumn(
			state = listState,
			flingBehavior = flingBehavior,
			modifier = Modifier
				.fillMaxSize()
				.padding(innerPadding),
			contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
			verticalArrangement = Arrangement.spacedBy(16.dp),
		) {
			itemsIndexed(photos, key = { _, uri -> uri }) { index, uri ->
				ResultCard(index = index, uri = uri)
			}
		}
	}
}

@Composable
private fun ResultCard(index: Int, uri: Uri) {
	Box(
		modifier = Modifier
			.fillMaxWidth()
			.aspectRatio(9f / 16f)
			.clip(RoundedCornerShape(16.dp)),
	) {
		AsyncImage(
			model = uri,
			contentDescription = "Result ${index + 1}",
			contentScale = ContentScale.Crop,
			modifier = Modifier.fillMaxSize(),
		)
		Row(
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(4.dp),
			modifier = Modifier
				.align(Alignment.TopStart)
				.padding(12.dp)
				.clip(RoundedCornerShape(12.dp))
				.background(Color.Black.copy(alpha = 0.6f))
				.padding(horizontal = 10.dp, vertical = 4.dp),
		) {
			Icon(
				imageVector = Icons.Filled.AutoAwesome,
				contentDescription = null,
				tint = Color(0xFFFFD54F),
				modifier = Modifier.size(14.dp),
			)
			Text(
				text = "#${index + 1}",
				color = Color.White,
				style = MaterialTheme.typography.labelLarge,
			)
		}
	}
}

@Preview(showBackground = true)
@Composable
fun ResultsScreenPreview() {
	InstaCuratorTheme {
		ResultsScreen(
			photos = emptyList(),
			snackbarHostState = remember { SnackbarHostState() },
			onSaveAll = {},
			onBack = {},
		)
	}
}
