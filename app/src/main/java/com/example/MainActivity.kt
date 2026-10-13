package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme(darkTheme = true, dynamicColor = false) {
        val gameViewModel: GameViewModel = viewModel()
        val uiState by gameViewModel.uiState.collectAsState()

        Surface(
          modifier = Modifier.fillMaxSize(),
          color = Color(0xFF090D15)
        ) {
          when (uiState.currentScreen) {
            AppScreen.HOME -> HomeScreen(viewModel = gameViewModel, uiState = uiState)
            AppScreen.SPLIT_SCREEN_RACE -> SplitScreenRace(viewModel = gameViewModel, uiState = uiState)
            AppScreen.GRAND_PRIX_RACE -> RaceScreen(viewModel = gameViewModel, uiState = uiState)
            AppScreen.GHOST_DUEL_RACE -> GhostDuelScreen(viewModel = gameViewModel, uiState = uiState)
            AppScreen.GARAGE -> GarageScreen(viewModel = gameViewModel, uiState = uiState)
            AppScreen.TRACK_SELECT -> TrackSelectScreen(viewModel = gameViewModel, uiState = uiState)
            AppScreen.PODIUM_RESULTS -> PodiumResultsScreen(viewModel = gameViewModel, uiState = uiState)
          }
        }
      }
    }
  }
}
