package com.instacurator.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.instacurator.app.pipeline.PipelineState
import com.instacurator.app.ui.CandidatesScreen
import com.instacurator.app.ui.HomeScreen
import com.instacurator.app.ui.ResultsScreen
import com.instacurator.app.ui.theme.InstaCuratorTheme
import com.instacurator.app.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		installSplashScreen()
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
						val snackbarHostState = remember { SnackbarHostState() }
						val scope = rememberCoroutineScope()

						when (val s = state) {
							is PipelineState.FinalResult -> ResultsScreen(
								photos = s.photos,
								snackbarHostState = snackbarHostState,
								onSaveAll = {
									scope.launch {
										val result = viewModel.saveAll(s.photos)
										snackbarHostState.showSnackbar(
											"Saved ${result.saved}/${result.total} to Pictures/InstagramCurator"
										)
									}
								},
								onBack = viewModel::resetPipeline,
							)
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
