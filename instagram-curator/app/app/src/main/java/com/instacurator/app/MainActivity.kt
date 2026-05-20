package com.instacurator.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.instacurator.app.ui.theme.InstaCuratorTheme

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
						HomeScreen()
					}
				}
			}
		}
	}
}

@Composable
fun HomeScreen() {
	val context = LocalContext.current
	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(24.dp),
		verticalArrangement = Arrangement.Center,
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Text(
			text = "InstaCurator",
			style = MaterialTheme.typography.headlineMedium
		)
		Text(
			text = "Pick up to 100 photos. Get the best ones.",
			style = MaterialTheme.typography.bodyMedium,
			modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
		)
		Button(onClick = {
			// Photo Picker wiring lands in Phase 1.
			Toast.makeText(context, "Photo picker coming in Phase 1", Toast.LENGTH_SHORT).show()
		}) {
			Text("Pick Photos")
		}
	}
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
	InstaCuratorTheme {
		HomeScreen()
	}
}
