package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
fun SplitScreenRace(
  viewModel: GameViewModel,
  uiState: GameUiState
) {
  BackHandler {
    viewModel.navigateTo(AppScreen.HOME)
  }

  val track = remember(uiState.selectedTrack) { TrackCatalog.getTrack(uiState.selectedTrack) }
  val physics = remember(track) { RacingPhysics(track, viewModel.soundManager) }

  // Initial starting positions side-by-side at start line
  val p0 = track.waypoints[0]
  val p1 = track.waypoints[1]
  val startAngle = atan2(p1.y - p0.y, p1.x - p0.x)
  val perp = startAngle + (Math.PI / 2.0).toFloat()

  val p1CarStats = viewModel.availableCars.find { it.id == uiState.selectedCarId } ?: viewModel.availableCars.first()
  val p2CarStats = viewModel.availableCars.find { it.id == uiState.p2CarId } ?: viewModel.availableCars[1]

  val player1 = remember {
    Racer(
      id = "p1",
      name = "Player 1",
      isPlayer = true,
      playerNumber = 1,
      x = p0.x + kotlin.math.cos(perp) * 36f,
      y = p0.y + kotlin.math.sin(perp) * 36f,
      angle = startAngle,
      carColor = Color(uiState.selectedCarColor),
      topSpeedFactor = p1CarStats.topSpeed,
      accelFactor = p1CarStats.acceleration,
      handlingFactor = p1CarStats.handling
    )
  }

  val player2 = remember {
    Racer(
      id = "p2",
      name = "Player 2",
      isPlayer = true,
      playerNumber = 2,
      x = p0.x - kotlin.math.cos(perp) * 36f,
      y = p0.y - kotlin.math.sin(perp) * 36f,
      angle = startAngle,
      carColor = Color(uiState.p2CarColor),
      accentColor = Color(0xFFFFD600),
      topSpeedFactor = p2CarStats.topSpeed,
      accelFactor = p2CarStats.acceleration,
      handlingFactor = p2CarStats.handling
    )
  }

  val racers = remember { listOf(player1, player2) }

  var countdown by remember { mutableStateOf(3) }
  var raceStarted by remember { mutableStateOf(false) }
  var isPaused by remember { mutableStateOf(false) }
  var raceTimeMs by remember { mutableLongStateOf(0L) }

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

        if (raceStarted && (!player1.raceFinished || !player2.raceFinished)) {
          raceTimeMs += (dt * 1000).toLong()

          // Physics updates
          physics.updateRacer(player1, dt)
          physics.updateRacer(player2, dt)
          physics.handleCarCollisions(racers)
          physics.updatePowerUpTimers(dt)
          physics.updateParticles(dt)

          // Check Win Condition
          if (player1.raceFinished || player2.raceFinished) {
            val winner = if (player1.raceFinished && !player2.raceFinished) {
              "Player 1 (Blue)"
            } else if (player2.raceFinished && !player1.raceFinished) {
              "Player 2 (Orange)"
            } else if (player1.lapProgress >= player2.lapProgress) {
              "Player 1 (Blue)"
            } else {
              "Player 2 (Orange)"
            }

            viewModel.finishRace(
              PodiumResult(
                mode = GameMode.SPLIT_SCREEN_2P,
                player1Rank = if (winner.startsWith("Player 1")) 1 else 2,
                player2Rank = if (winner.startsWith("Player 2")) 1 else 2,
                winnerName = winner,
                bestLapTimeMs = raceTimeMs,
                coinsEarned = 250,
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
    Column(modifier = Modifier.fillMaxSize()) {
      // TOP HALF: PLAYER 2
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
      ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          RaceRenderer.drawRaceScene(
            drawScope = this,
            track = track,
            racers = racers,
            particles = physics.particles,
            skidMarks = physics.skidMarks,
            cameraX = player2.x,
            cameraY = player2.y,
            viewportWidth = size.width,
            viewportHeight = size.height,
            zoom = 0.85f,
            showRadar = true
          )
        }

        // P2 HUD (Top half)
        PlayerHudOverlay(
          racer = player2,
          totalLaps = track.type.laps,
          isTopHalf = true,
          modifier = Modifier.align(Alignment.TopCenter)
        )

        // P2 Controls
        PlayerControls(
          racer = player2,
          onNitro = { viewModel.soundManager.playNitroBoost() },
          modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .padding(10.dp)
        )
      }

      // CENTER NEON DIVIDER BAR
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(6.dp)
          .background(
            if (player1.lapProgress >= player2.lapProgress) Color(0xFF00E5FF) else Color(0xFFFF9100)
          )
      )

      // BOTTOM HALF: PLAYER 1
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
      ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          RaceRenderer.drawRaceScene(
            drawScope = this,
            track = track,
            racers = racers,
            particles = physics.particles,
            skidMarks = physics.skidMarks,
            cameraX = player1.x,
            cameraY = player1.y,
            viewportWidth = size.width,
            viewportHeight = size.height,
            zoom = 0.85f,
            showRadar = true
          )
        }

        // P1 HUD (Bottom half)
        PlayerHudOverlay(
          racer = player1,
          totalLaps = track.type.laps,
          isTopHalf = false,
          modifier = Modifier.align(Alignment.TopCenter)
        )

        // P1 Controls
        PlayerControls(
          racer = player1,
          onNitro = { viewModel.soundManager.playNitroBoost() },
          modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .padding(10.dp)
        )
      }
    }

    // Top Bar Back & Pause Controls
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .windowInsetsPadding(WindowInsets.statusBars)
        .padding(12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(
        onClick = { viewModel.navigateTo(AppScreen.HOME) },
        modifier = Modifier
          .size(40.dp)
          .background(Color(0xCC1E2838), CircleShape)
          .testTag("split_back_button")
      ) {
        Icon(Icons.Default.ArrowBack, contentDescription = "Exit race", tint = Color.White)
      }

      Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xCC1E2838)
      ) {
        Text(
          text = if (player1.lapProgress >= player2.lapProgress) "P1 LEADING" else "P2 LEADING",
          color = if (player1.lapProgress >= player2.lapProgress) Color(0xFF00E5FF) else Color(0xFFFF9100),
          fontWeight = FontWeight.Bold,
          fontSize = 12.sp,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
      }

      IconButton(
        onClick = { isPaused = !isPaused },
        modifier = Modifier
          .size(40.dp)
          .background(Color(0xCC1E2838), CircleShape)
          .testTag("split_pause_button")
      ) {
        Icon(
          imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
          contentDescription = "Pause race",
          tint = Color.White
        )
      }
    }

    // Countdown Overlay (3, 2, 1, GO!)
    if (countdown > 0) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "$countdown",
          fontSize = 88.sp,
          fontWeight = FontWeight.Black,
          color = Color(0xFFFFEB3B)
        )
      }
    }
  }
}

@Composable
fun PlayerHudOverlay(
  racer: Racer,
  totalLaps: Int,
  isTopHalf: Boolean,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Player Badge & Lap
    Surface(
      shape = RoundedCornerShape(10.dp),
      color = Color(0xCC0E1524)
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text(
          text = racer.name,
          fontWeight = FontWeight.Black,
          fontSize = 13.sp,
          color = if (racer.playerNumber == 1) Color(0xFF00E5FF) else Color(0xFFFF9100)
        )
        Text(
          text = "LAP ${racer.currentLap.coerceAtMost(totalLaps)}/$totalLaps",
          fontWeight = FontWeight.Bold,
          fontSize = 12.sp,
          color = Color.White
        )
      }
    }

    // Speedometer & Nitro Bar
    Column(horizontalAlignment = Alignment.End) {
      Text(
        text = "${(racer.speed * 0.45f).toInt().coerceAtLeast(0)} KM/H",
        fontWeight = FontWeight.Black,
        fontSize = 14.sp,
        color = Color.White
      )
      // Mini Nitro bar
      Box(
        modifier = Modifier
          .width(70.dp)
          .height(5.dp)
          .clip(RoundedCornerShape(3.dp))
          .background(Color(0xFF263238))
      ) {
        Box(
          modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(racer.nitroFuel / 100f)
            .background(Color(0xFF00E5FF))
        )
      }
    }
  }
}

@Composable
fun PlayerControls(
  racer: Racer,
  onNitro: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.Bottom
  ) {
    // Left & Right Steering Touch Buttons
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      ControlButton(
        label = "◀",
        testTag = "p${racer.playerNumber}_steer_left",
        onPressed = { racer.steeringInput = -1.0f },
        onReleased = { if (racer.steeringInput < 0f) racer.steeringInput = 0f }
      )
      ControlButton(
        label = "▶",
        testTag = "p${racer.playerNumber}_steer_right",
        onPressed = { racer.steeringInput = 1.0f },
        onReleased = { if (racer.steeringInput > 0f) racer.steeringInput = 0f }
      )
    }

    // Drift, Nitro, and Gas/Brake Cluster
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      // Drift Button
      ControlButton(
        label = "DRIFT",
        testTag = "p${racer.playerNumber}_drift",
        color = Color(0xFF9C27B0),
        onPressed = { racer.isDrifting = true },
        onReleased = { racer.isDrifting = false }
      )

      // Nitro Button
      ControlButton(
        label = "NITRO",
        testTag = "p${racer.playerNumber}_nitro",
        color = Color(0xFF00E5FF),
        enabled = racer.nitroFuel > 10f,
        onPressed = {
          if (racer.nitroFuel > 10f) {
            racer.isBoosting = true
            onNitro()
          }
        },
        onReleased = { racer.isBoosting = false }
      )

      // Brake / Reverse Button
      ControlButton(
        label = "REV",
        testTag = "p${racer.playerNumber}_brake",
        color = Color(0xFFE53935),
        onPressed = { racer.throttleInput = -1.0f },
        onReleased = { racer.throttleInput = 1.0f }
      )

      // Throttle (Default auto-gas with manual boost tap)
      ControlButton(
        label = "GAS",
        testTag = "p${racer.playerNumber}_gas",
        color = Color(0xFF00E676),
        onPressed = { racer.throttleInput = 1.0f },
        onReleased = { racer.throttleInput = 0.5f }
      )
    }
  }
}

@Composable
fun ControlButton(
  label: String,
  testTag: String,
  color: Color = Color(0xFF37474F),
  enabled: Boolean = true,
  onPressed: () -> Unit,
  onReleased: () -> Unit
) {
  var isPressed by remember { mutableStateOf(false) }

  Box(
    modifier = Modifier
      .size(52.dp)
      .clip(RoundedCornerShape(14.dp))
      .background(
        if (isPressed) color.copy(alpha = 0.9f) else color.copy(alpha = if (enabled) 0.5f else 0.2f)
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
      fontSize = if (label.length > 2) 10.sp else 18.sp
    )
  }
}
