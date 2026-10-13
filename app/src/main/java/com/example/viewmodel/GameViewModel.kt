package com.example.viewmodel

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundManager
import com.example.model.CarStats
import com.example.model.GameMode
import com.example.model.GhostFrame
import com.example.model.TrackType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class AppScreen {
  HOME,
  GRAND_PRIX_RACE,
  SPLIT_SCREEN_RACE,
  GHOST_DUEL_RACE,
  GARAGE,
  TRACK_SELECT,
  PODIUM_RESULTS
}

data class PodiumResult(
  val mode: GameMode,
  val player1Rank: Int,
  val player2Rank: Int = -1,
  val winnerName: String,
  val bestLapTimeMs: Long,
  val coinsEarned: Int,
  val isMultiplayer: Boolean
)

data class GameUiState(
  val currentScreen: AppScreen = AppScreen.HOME,
  val selectedMode: GameMode = GameMode.GRAND_PRIX,
  val selectedTrack: TrackType = TrackType.NEON_METROPOLIS,
  val selectedCarId: String = "apex_falcon",
  val p2CarId: String = "venom_v8",
  val selectedCarColor: Long = 0xFF00E5FF,
  val p2CarColor: Long = 0xFFFF5722,
  val coins: Int = 850,
  val unlockedCarIds: Set<String> = setOf("apex_falcon", "venom_v8"),
  val bestTrackTimes: Map<TrackType, Long> = emptyMap(),
  val soundEnabled: Boolean = true,
  val hapticsEnabled: Boolean = true,
  val lastPodiumResult: PodiumResult? = null,
  val recordedGhost: List<GhostFrame> = emptyList()
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

  val soundManager = SoundManager(application)

  private val _uiState = MutableStateFlow(GameUiState())
  val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

  val availableCars = listOf(
    CarStats(
      id = "apex_falcon",
      name = "Apex GT",
      topSpeed = 1.0f,
      acceleration = 1.0f,
      handling = 1.0f,
      nitroDuration = 1.0f,
      price = 0,
      primaryColor = 0xFF00E5FF,
      accentColor = 0xFFFF1744
    ),
    CarStats(
      id = "venom_v8",
      name = "Venom Muscle",
      topSpeed = 1.15f,
      acceleration = 0.9f,
      handling = 0.85f,
      nitroDuration = 1.1f,
      price = 400,
      primaryColor = 0xFFFF5722,
      accentColor = 0xFFFFEB3B
    ),
    CarStats(
      id = "cyber_phantom",
      name = "Cyber Phantom",
      topSpeed = 1.05f,
      acceleration = 1.25f,
      handling = 1.1f,
      nitroDuration = 1.3f,
      price = 1000,
      primaryColor = 0xFFD500F9,
      accentColor = 0xFF00E5FF
    ),
    CarStats(
      id = "drift_king_r",
      name = "Drift King RS",
      topSpeed = 0.98f,
      acceleration = 1.05f,
      handling = 1.35f,
      nitroDuration = 1.15f,
      price = 1600,
      primaryColor = 0xFF76FF03,
      accentColor = 0xFF2979FF
    )
  )

  fun navigateTo(screen: AppScreen) {
    _uiState.update { it.copy(currentScreen = screen) }
  }

  fun selectMode(mode: GameMode) {
    _uiState.update { it.copy(selectedMode = mode) }
  }

  fun selectTrack(track: TrackType) {
    _uiState.update { it.copy(selectedTrack = track) }
  }

  fun selectCar(carId: String) {
    _uiState.update { it.copy(selectedCarId = carId) }
  }

  fun selectP2Car(carId: String) {
    _uiState.update { it.copy(p2CarId = carId) }
  }

  fun selectColor(colorLong: Long) {
    _uiState.update { it.copy(selectedCarColor = colorLong) }
  }

  fun selectP2Color(colorLong: Long) {
    _uiState.update { it.copy(p2CarColor = colorLong) }
  }

  fun buyCar(car: CarStats): Boolean {
    val state = _uiState.value
    if (state.coins >= car.price && !state.unlockedCarIds.contains(car.id)) {
      _uiState.update {
        it.copy(
          coins = it.coins - car.price,
          unlockedCarIds = it.unlockedCarIds + car.id,
          selectedCarId = car.id
        )
      }
      soundManager.playLapComplete()
      return true
    }
    return false
  }

  fun toggleSound() {
    _uiState.update {
      val newVal = !it.soundEnabled
      soundManager.isAudioEnabled = newVal
      it.copy(soundEnabled = newVal)
    }
  }

  fun toggleHaptics() {
    _uiState.update {
      val newVal = !it.hapticsEnabled
      soundManager.isHapticsEnabled = newVal
      it.copy(hapticsEnabled = newVal)
    }
  }

  fun finishRace(result: PodiumResult) {
    val currentTrack = _uiState.value.selectedTrack
    val currentBest = _uiState.value.bestTrackTimes[currentTrack] ?: Long.MAX_VALUE
    val newBest = if (result.bestLapTimeMs in 1 until currentBest) result.bestLapTimeMs else currentBest

    val updatedBestTimes = _uiState.value.bestTrackTimes.toMutableMap()
    if (newBest != Long.MAX_VALUE) {
      updatedBestTimes[currentTrack] = newBest
    }

    _uiState.update {
      it.copy(
        coins = it.coins + result.coinsEarned,
        lastPodiumResult = result,
        currentScreen = AppScreen.PODIUM_RESULTS,
        bestTrackTimes = updatedBestTimes
      )
    }
    soundManager.playLapComplete()
  }

  fun saveGhost(frames: List<GhostFrame>) {
    _uiState.update { it.copy(recordedGhost = frames) }
  }
}
