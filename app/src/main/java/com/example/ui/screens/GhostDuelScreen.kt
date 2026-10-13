package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameMode
import com.example.model.GhostFrame
import com.example.model.Racer
import com.example.physics.RacingPhysics
import com.example.physics.TrackCatalog
import com.example.ui.render.RaceRenderer
import com.example.viewmodel.AppScreen
import com.example.viewmodel.GameUiState
import com.example.viewmodel.GameViewModel
import com.example.viewmodel.PodiumResult
import kotlinx.coroutines.delay
import kotlin.math.atan2

@Composable
fun GhostDuelScreen(
  viewModel: GameViewModel,
  uiState: GameUiState
) {
  BackHandler {
    viewModel.navigateTo(AppScreen.HOME)
  }

  val track = remember(uiState.selectedTrack) { TrackCatalog.getTrack(uiState.selectedTrack) }
  val physics = remember(track) { RacingPhysics(track, viewModel.soundManager) }

  val p0 = track.waypoints[0]
  val p1 = track.waypoints[1]
  val startAngle = atan2(p1.y - p0.y, p1.x - p0.x)
  val carStats = viewModel.availableCars.find { it.id == uiState.selectedCarId } ?: viewModel.availableCars.first()

  val hasRecordedGhost = uiState.recordedGhost.isNotEmpty()

  val player = remember {
    Racer(
      id = if (hasRecordedGhost) "challenger" else "record_setter",
      name = if (hasRecordedGhost) "Player 2" else "Player 1",
      isPlayer = true,
      x = p0.x,
      y = p0.y,
      angle = startAngle,
      carColor = if (hasRecordedGhost) Color(uiState.p2CarColor) else Color(uiState.selectedCarColor),
      topSpeedFactor = carStats.topSpeed,
      accelFactor = carStats.acceleration,
      handlingFactor = carStats.handling
    )
  }

  // Ghost car
  val ghostRacer = remember(hasRecordedGhost) {
    if (hasRecordedGhost) {
      Racer(
        id = "ghost",
        name = "Ghost Rival",
        isPlayer = false,
        x = p0.x,
        y = p0.y,
        angle = startAngle,
        carColor = Color(0x7700E5FF),
        accentColor = Color(0x77FFFFFF)
      )
    } else null
  }

  val racers = remember(ghostRacer) {
    if (ghostRacer != null) listOf(player, ghostRacer) else listOf(player)
  }

  val recordedFrames = remember { mutableListOf<GhostFrame>() }
  var countdown by remember { mutableStateOf(3) }
  var raceStarted by remember { mutableStateOf(false) }
  var raceTimeMs by remember { mutableLongStateOf(0L) }

  // Game Loop
  LaunchedEffect(Unit) {
    while (countdown > 0) {
      viewModel.soundManager.playCountdownBeep(isFinal = false)
      delay(900)
      countdown--
    }
    viewModel.soundManager.playCountdownBeep(isFinal = true)
    raceStarted = true

    var lastNanos = System.nanoTime()
    while (!player.raceFinished) {
      withFrameNanos { now ->
        val dt = ((now - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
        lastNanos = now

        if (raceStarted) {
          raceTimeMs += (dt * 1000).toLong()

          // Record player's frame for ghost playback
          if (!hasRecordedGhost) {
            recordedFrames.add(
              GhostFrame(
                timeMs = raceTimeMs,
                x = player.x,
                y = player.y,
                angle = player.angle,
                speed = player.speed,
                isDrifting = player.isDrifting,
                isBoosting = player.isBoosting
              )
            )
          } else if (ghostRacer != null) {
            // Playback recorded ghost
            val targetFrame = uiState.recordedGhost.minByOrNull { kotlin.math.abs(it.timeMs - raceTimeMs) }
            if (targetFrame != null) {
              ghostRacer.x = targetFrame.x
              ghostRacer.y = targetFrame.y
              ghostRacer.angle = targetFrame.angle
              ghostRacer.speed = targetFrame.speed
              ghostRacer.isDrifting = targetFrame.isDrifting
              ghostRacer.isBoosting = targetFrame.isBoosting
            }
          }

          physics.updateRacer(player, dt)
          physics.updatePowerUpTimers(dt)
          physics.updateParticles(dt)

          if (player.raceFinished) {
            if (!hasRecordedGhost) {
              // Saved ghost!
              viewModel.saveGhost(recordedFrames.toList())
            }

            viewModel.finishRace(
              PodiumResult(
                mode = GameMode.GHOST_DUEL,
                player1Rank = 1,
                winnerName = if (!hasRecordedGhost) "Ghost Recorded!" else "Ghost Duel Finished!",
                bestLapTimeMs = raceTimeMs,
                coinsEarned = 200,
                isMultiplayer = true
              )
            )
          }
        }
      }
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black)
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      RaceRenderer.drawRaceScene(
        drawScope = this,
        track = track,
        racers = racers,
        particles = physics.particles,
        skidMarks = physics.skidMarks,
        cameraX = player.x,
        cameraY = player.y,
        viewportWidth = size.width,
        viewportHeight = size.height,
        zoom = 0.95f,
        showRadar = true
      )
    }

    // Top HUD
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .windowInsetsPadding(WindowInsets.statusBars)
        .padding(16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(
        onClick = { viewModel.navigateTo(AppScreen.HOME) },
        modifier = Modifier
          .size(42.dp)
          .background(Color(0xCC1E2838), CircleShape)
          .testTag("ghost_back_btn")
      ) {
        Icon(Icons.Default.ArrowBack, contentDescription = "Exit", tint = Color.White)
      }

      Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xCC0E1524)
      ) {
        Text(
          text = if (hasRecordedGhost) "VS GHOST RIVAL" else "RECORDING GHOST LAP",
          fontWeight = FontWeight.Bold,
          color = if (hasRecordedGhost) Color(0xFFE040FB) else Color(0xFF00E5FF),
          fontSize = 13.sp,
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
      }

      Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xCC0E1524)
      ) {
        val totalSec = raceTimeMs / 1000
        val ms = (raceTimeMs % 1000) / 10
        Text(
          text = String.format("%02d:%02d.%02d", totalSec / 60, totalSec % 60, ms),
          fontWeight = FontWeight.Bold,
          color = Color.White,
          fontSize = 13.sp,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
      }
    }

    // Bottom Controls
    Column(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .windowInsetsPadding(WindowInsets.navigationBars)
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
          BigControlButton(
            label = "◀",
            testTag = "ghost_steer_left",
            onPressed = { player.steeringInput = -1.0f },
            onReleased = { if (player.steeringInput < 0f) player.steeringInput = 0f }
          )
          BigControlButton(
            label = "▶",
            testTag = "ghost_steer_right",
            onPressed = { player.steeringInput = 1.0f },
            onReleased = { if (player.steeringInput > 0f) player.steeringInput = 0f }
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          BigControlButton(
            label = "DRIFT",
            testTag = "ghost_drift",
            color = Color(0xFF9C27B0),
            onPressed = { player.isDrifting = true },
            onReleased = { player.isDrifting = false }
          )

          BigControlButton(
            label = "NITRO",
            testTag = "ghost_nitro",
            color = Color(0xFF00E5FF),
            enabled = player.nitroFuel > 10f,
            onPressed = {
              if (player.nitroFuel > 10f) {
                player.isBoosting = true
                viewModel.soundManager.playNitroBoost()
              }
            },
            onReleased = { player.isBoosting = false }
          )

          BigControlButton(
            label = "GAS",
            testTag = "ghost_gas",
            color = Color(0xFF00E676),
            onPressed = { player.throttleInput = 1.0f },
            onReleased = { player.throttleInput = 0.5f }
          )
        }
      }
    }

    if (countdown > 0) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "$countdown",
          fontSize = 90.sp,
          fontWeight = FontWeight.Black,
          color = Color(0xFFFFEB3B)
        )
      }
    }
  }
}
