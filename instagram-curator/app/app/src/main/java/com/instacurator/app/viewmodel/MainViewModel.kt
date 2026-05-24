package com.instacurator.app.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.instacurator.app.gallery.MediaStoreSaver
import com.instacurator.app.gallery.SaveResult
import com.instacurator.app.pipeline.PipelineProcessor
import com.instacurator.app.pipeline.PipelineState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Hard cap on photos selected per run. */
const val MAX_PHOTOS = 100

/** Default number of photos the curator should pick. */
const val DEFAULT_PICK_COUNT = 6

/** Allowed range for the output photo count. */
val PICK_RANGE = 1..10

/**
 * Holds home-screen state (selected photos, pickCount), drives the on-device
 * pipeline, then auto-chains the AI pipeline. Owns the snackbar-bound saveAll
 * path so MainActivity stays free of injected dependencies.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
	private val processor: PipelineProcessor,
	private val saver: MediaStoreSaver,
) : ViewModel() {

	private val _selectedUris = mutableStateListOf<Uri>()
	val selectedUris: List<Uri> get() = _selectedUris

	var pickCount by mutableIntStateOf(DEFAULT_PICK_COUNT)
		private set

	private val _pipelineState = MutableStateFlow<PipelineState>(PipelineState.Idle)
	val pipelineState: StateFlow<PipelineState> = _pipelineState.asStateFlow()

	fun setPickedUris(uris: List<Uri>) {
		_selectedUris.clear()
		_selectedUris.addAll(uris.take(MAX_PHOTOS))
	}

	fun removeUri(uri: Uri) {
		_selectedUris.remove(uri)
	}

	fun incrementPickCount() {
		if (pickCount < PICK_RANGE.last) pickCount++
	}

	fun decrementPickCount() {
		if (pickCount > PICK_RANGE.first) pickCount--
	}

	fun runPipeline() {
		if (isRunning(_pipelineState.value)) return
		val uris = _selectedUris.toList()
		if (uris.isEmpty()) return

		viewModelScope.launch {
			_pipelineState.value = PipelineState.Running("Starting", 0f)
			try {
				val candidates = processor.process(uris) { stage, p ->
					_pipelineState.value = PipelineState.Running(stage, p)
				}
				val finalUris = processor.runAiPipeline(candidates, pickCount) { state ->
					_pipelineState.value = state
				}
				_pipelineState.value = PipelineState.FinalResult(finalUris)
			} catch (t: Throwable) {
				_pipelineState.value = PipelineState.Error(t.message ?: "Pipeline failed")
			}
		}
	}

	fun resetPipeline() {
		_pipelineState.value = PipelineState.Idle
	}

	/** Save all photos to Pictures/InstagramCurator/. Called from the FAB. */
	suspend fun saveAll(photos: List<Uri>): SaveResult = saver.saveAll(photos)

	private fun isRunning(state: PipelineState): Boolean = state is PipelineState.Running ||
		state is PipelineState.AiScoring ||
		state is PipelineState.AiSelecting
}
