package com.instacurator.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.instacurator.app.pipeline.PipelineState
import com.instacurator.app.ui.CandidatesScreen
import com.instacurator.app.ui.HomeScreen
import com.instacurator.app.ui.theme.InstaCuratorTheme
import com.instacurator.app.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContent {
			InstaCuratorTheme {
				Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
					Surface(
						modifier = Modifier
							.fillMaxSize()
							.padding(innerPadding),
						color = MaterialTheme.colorScheme.background
					) {
						val viewModel: MainViewModel = hiltViewModel()
						val state by viewModel.pipelineState.collectAsStateWithLifecycle()
						when (val s = state) {
							is PipelineState.Done -> CandidatesScreen(
								candidates = s.candidates,
								onBack = viewModel::resetPipeline,
							)
							else -> HomeScreen(viewModel = viewModel)
						}
					}
				}
			}
		}
	}
}
