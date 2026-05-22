package com.instacurator.app.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/** Hard cap on photos selected per run. */
const val MAX_PHOTOS = 100

/** Default number of photos the curator should pick. */
const val DEFAULT_PICK_COUNT = 6

/** Allowed range for the output photo count. */
val PICK_RANGE = 1..10

/**
 * Holds the home-screen state: which photos the user picked and how many the
 * curator should return. Plain ViewModel — no DI framework (see app/CLAUDE.md).
 */
class MainViewModel : ViewModel() {

	private val _selectedUris = mutableStateListOf<Uri>()

	/** Photos currently selected, in pick order. Observable by Compose. */
	val selectedUris: List<Uri> get() = _selectedUris

	/** How many photos the curator should pick. */
	var pickCount by mutableIntStateOf(DEFAULT_PICK_COUNT)
		private set

	/** Replaces the selection with a fresh pick, defensively capped at MAX_PHOTOS. */
	fun setPickedUris(uris: List<Uri>) {
		_selectedUris.clear()
		_selectedUris.addAll(uris.take(MAX_PHOTOS))
	}

	/** Removes a single photo from the selection (grid tap). */
	fun removeUri(uri: Uri) {
		_selectedUris.remove(uri)
	}

	fun incrementPickCount() {
		if (pickCount < PICK_RANGE.last) pickCount++
	}

	fun decrementPickCount() {
		if (pickCount > PICK_RANGE.first) pickCount--
	}
}
