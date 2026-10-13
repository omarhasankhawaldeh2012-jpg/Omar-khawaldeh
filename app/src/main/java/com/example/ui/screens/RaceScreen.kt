package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameMode
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
fun RaceScreen(
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
  val perp = startAngle + (Math.PI / 2.0).toFloat()
  val carStats = viewModel.availableCars.find { it.id == uiState.selectedCarId } ?: viewModel.availableCars.first()

  // 6 Racers: Player + 5 AI
  val racers = remember {
    val list = mutableListOf<Racer>()
    // Player starts at pole position
    list.add(
      Racer(
        id = "player",
        name = "Player",
        isPlayer = true,
        x = p0.x,
        y = p0.y,
        angle = startAngle,
        carColor = Color(uiState.selectedCarColor),
        topSpeedFactor = carStats.topSpeed,
        accelFactor = carStats.acceleration,
        handlingFactor = carStats.handling
      )
    )

    val aiNames = listOf("Blaze", "Viper", "Phantom", "Thunder", "Comet")
    val aiColors = listOf(
      Color(0xFFFF5252),
      Color(0xFF7C4DFF),
      Color(0xFFFFD600),
      Color(0xFF00E676),
      Color(0xFFFF4081)
    )

    for (i in 0 until 5) {
      val row = (i + 1) / 2
      val side = if (i % 2 == 0) 1f else -1f
      val backDist = row * 65f
      val lateralDist = side * 35f

      val startX = p0.x - kotlin.math.cos(startAngle) * backDist + kotlin.math.cos(perp) * lateralDist
      val startY = p0.y - kotlin.math.sin(startAngle) * backDist + kotlin.math.sin(perp) * lateralDist

      list.add(
        Racer(
          id = "ai_$i",
          name = aiNames[i],
          isPlayer = false,
          x = startX,
          y = startY,
          angle = startAngle,
          carColor = aiColors[i],
          topSpeedFactor = 0.96f + (i * 0.02f),
          accelFactor = 0.95f + (i * 0.02f),
          handlingFactor = 0.94f + (i * 0.02f)
        )
      )
    }
    list
  }

  val player = racers.first()
  var countdown by remember { mutableStateOf(3) }
  var raceStarted by remember { mutableStateOf(false) }
  var isPaused by remember { mutableStateOf(false) }
  var raceTimeMs by remember { mutableLongStateOf(0L) }
  var playerRank by remember { mutableIntStateOf(1) }

  // Game Loop
  LaunchedEffect(isPaused) {
    if (countdown > 0) {
      while (countdown > 0) {
        viewModel.soundManager.playCountdownBeep(isFinal = false)
        delay(900)
        countdown--
      }
      viewModel.soundManager.playCountdownBeep(isFinal = true)
      raceStarted = true
    }

    var lastNanos = System.nanoTime()
    while (!isPaused) {
      withFrameNanos { now ->
        val dt = ((now - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
        lastNanos = now

        if (raceStarted && !player.raceFinished) {
          raceTimeMs += (dt * 1000).toLong()

          // Update AI racers
          for (r in racers) {
            if (!r.isPlayer) {
              physics.updateAi(r, racers, dt)
            }
            physics.updateRacer(r, dt)
          }

          // Handle car bumping collisions
          physics.handleCarCollisions(racers)
          physics.updatePowerUpTimers(dt)
          physics.updateParticles(dt)

          // Calculate player rank among the 6 cars
          val sorted = racers.sortedByDescending { it.lapProgress }
          playerRank = sorted.indexOfFirst { it.isPlayer } + 1

          // Finish check
          if (player.raceFinished) {
            val coins = when (playerRank) {
              1 -> 350
              2 -> 200
              3 -> 120
              else -> 50
            }
            viewModel.finishRace(
              PodiumResult(
                mode = GameMode.GRAND_PRIX,
                player1Rank = playerRank,
                winnerName = if (playerRank == 1) "Player 1" else sorted.first().name,
                bestLapTimeMs = raceTimeMs,
                coinsEarned = coins,
                isMultiplayer = false
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
    // Canvas Game Viewport
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
        zoom = if (player.isBoosting) 0.88f else 0.98f,
        showRadar = true
      )
    }

    // Top HUD: Rank, Lap, Pause, Time
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .windowInsetsPadding(WindowInsets.statusBars)
        .padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = { viewModel.navigateTo(AppScreen.HOME) },
          modifier = Modifier
            .size(42.dp)
            .background(Color(0xCC1E2838), CircleShape)
            .testTag("race_back_button")
        ) {
          Icon(Icons.Default.ArrowBack, contentDescription = "Exit race", tint = Color.White)
        }

        // Rank Badge
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = when (playerRank) {
            1 -> Color(0xFFFFD600)
            2 -> Color(0xFFB0BEC5)
            3 -> Color(0xFFCD7F32)
            else -> Color(0xFF263238)
          }
        ) {
          Text(
            text = "$playerRank/6",
            fontWeight = FontWeight.Black,
            fontSize = 16.sp,
            color = if (playerRank <= 3) Color.Black else Color.White,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
          )
        }
      }

      // Lap and Time Indicator
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xCC0E1524)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Text(
            text = "LAP ${player.currentLap.coerceAtMost(track.type.laps)}/${track.type.laps}",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
          )
          val totalSec = raceTimeMs / 1000
          val ms = (raceTimeMs % 1000) / 10
          Text(
            text = String.format("%02d:%02d.%02d", totalSec / 60, totalSec % 60, ms),
            color = Color(0xFF00E5FF),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
          )
        }
      }

      IconButton(
        onClick = { isPaused = !isPaused },
        modifier = Modifier
          .size(42.dp)
          .background(Color(0xCC1E2838), CircleShape)
          .testTag("race_pause_button")
      ) {
        Icon(
          imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
          contentDescription = "Pause",
          tint = Color.White
        )
      }
    }

    // Bottom HUD: Speedometer & Nitro Tank
    Column(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .windowInsetsPadding(WindowInsets.navigationBars)
        .padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Speed and Nitro Bars
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Speed Display
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = Color(0xCC141D2D)
        ) {
          Text(
            text = "${(player.speed * 0.45f).toInt().coerceAtLeast(0)} KM/H",
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
          )
        }

        // Nitro Fuel Gauge
        Column(horizontalAlignment = Alignment.End) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.FlashOn,
              contentDescription = "Nitro",
              tint = Color(0xFF00E5FF),
              modifier = Modifier.size(16.dp)
            )
            Text(
              text = "NITRO ${player.nitroFuel.toInt()}%",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF00E5FF)
            )
          }
          Spacer(modifier = Modifier.height(3.dp))
          Box(
            modifier = Modifier
              .width(130.dp)
              .height(8.dp)
              .clip(RoundedCornerShape(4.dp))
              .background(Color(0xFF263238))
          ) {
            Box(
              modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(player.nitroFuel / 100f)
                .background(
                  Brush.horizontalGradient(
                    colors = listOf(Color(0xFF00B0FF), Color(0xFF00E5FF))
                  )
                )
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Single Player Controls: Steering + Drift + Nitro + Gas + Brake
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
      ) {
        // Left & Right Steering
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
          BigControlButton(
            label = "◀",
            testTag = "steer_left_btn",
            onPressed = { player.steeringInput = -1.0f },
            onReleased = { if (player.steeringInput < 0f) player.steeringInput = 0f }
          )
          BigControlButton(
            label = "▶",
            testTag = "steer_right_btn",
            onPressed = { player.steeringInput = 1.0f },
            onReleased = { if (player.steeringInput > 0f) player.steeringInput = 0f }
          )
        }

        // Action Buttons: Drift, Nitro, Brake, Gas
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          BigControlButton(
            label = "DRIFT",
            testTag = "drift_btn",
            color = Color(0xFF9C27B0),
            onPressed = { player.isDrifting = true },
            onReleased = { player.isDrifting = false }
          )

          BigControlButton(
            label = "NITRO",
            testTag = "nitro_btn",
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
            label = "REV",
            testTag = "brake_btn",
            color = Color(0xFFE53935),
            onPressed = { player.throttleInput = -1.0f },
            onReleased = { player.throttleInput = 1.0f }
          )

          BigControlButton(
            label = "GAS",
            testTag = "gas_btn",
            color = Color(0xFF00E676),
            onPressed = { player.throttleInput = 1.0f },
            onReleased = { player.throttleInput = 0.5f }
          )
        }
      }
    }

    // Countdown Overlay
    if (countdown > 0) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = "$countdown",
            fontSize = 96.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFFFFEB3B)
          )
          Text(
            text = "GET READY!",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
        }
      }
    }
  }
}

@Composable
fun BigControlButton(
  label: String,
  testTag: String,
  color: Color = Color(0xFF2E3B4E),
  enabled: Boolean = true,
  onPressed: () -> Unit,
  onReleased: () -> Unit
) {
  var isPressed by remember { mutableStateOf(false) }

  Box(
    modifier = Modifier
      .size(58.dp)
      .clip(RoundedCornerShape(16.dp))
      .background(
        if (isPressed) color.copy(alpha = 0.95f) else color.copy(alpha = if (enabled) 0.65f else 0.25f)
      )
      .pointerInput(enabled) {
        detectTapGestures(
          onPress = {
            if (!enabled) return@detectTapGestures
            isPressed = true
            onPressed()
            tryAwaitRelease()
            isPressed = false
            onReleased()
          }
        )
      }
      .testTag(testTag),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = label,
      color = if (enabled) Color.White else Color.Gray,
      fontWeight = FontWeight.Black,
      fontSize = if (label.length > 2) 11.sp else 22.sp
    )
  }
}
