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
import androidx.compose.ui.Modifier
import com.instacurator.app.ui.HomeScreen
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
