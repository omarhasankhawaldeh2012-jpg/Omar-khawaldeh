package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameMode
import com.example.viewmodel.AppScreen
import com.example.viewmodel.GameUiState
import com.example.viewmodel.GameViewModel

@Composable
fun HomeScreen(
  viewModel: GameViewModel,
  uiState: GameUiState
) {
  val infiniteTransition = rememberInfiniteTransition(label = "neon_glow")
  val glowAlpha by infiniteTransition.animateFloat(
    initialValue = 0.4f,
    targetValue = 0.95f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "glow"
  )

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(Color(0xFF090D16), Color(0xFF0F1726), Color(0xFF070A10))
        )
      )
      .windowInsetsPadding(WindowInsets.safeDrawing)
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 20.dp, vertical = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Top Bar with Coin Bank & Settings
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Coins Pill
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF1E2838),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD600).copy(alpha = 0.6f))
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(
            imageVector = Icons.Default.MonetizationOn,
            contentDescription = "Coins",
            tint = Color(0xFFFFD600),
            modifier = Modifier.size(20.dp)
          )
          Text(
            text = "${uiState.coins}",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
          )
        }
      }

      // Audio & Haptic Toggles
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IconButton(
          onClick = { viewModel.toggleSound() },
          modifier = Modifier
            .size(42.dp)
            .background(Color(0xFF1E2838), CircleShape)
            .testTag("sound_toggle")
        ) {
          Icon(
            imageVector = if (uiState.soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
            contentDescription = "Sound toggle",
            tint = if (uiState.soundEnabled) Color(0xFF00E5FF) else Color.Gray,
            modifier = Modifier.size(20.dp)
          )
        }
        IconButton(
          onClick = { viewModel.toggleHaptics() },
          modifier = Modifier
            .size(42.dp)
            .background(Color(0xFF1E2838), CircleShape)
            .testTag("haptics_toggle")
        ) {
          Icon(
            imageVector = if (uiState.hapticsEnabled) Icons.Default.Vibration else Icons.Default.PhoneAndroid,
            contentDescription = "Haptics toggle",
            tint = if (uiState.hapticsEnabled) Color(0xFFFF5252) else Color.Gray,
            modifier = Modifier.size(20.dp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Hero Title
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = "NITRO RIVALS",
        fontSize = 38.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 2.sp,
        color = Color.White,
        textAlign = TextAlign.Center
      )
      Text(
        text = "ARCADE MULTIPLAYER RACING",
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 4.sp,
        color = Color(0xFF00E5FF).copy(alpha = glowAlpha)
      )
    }

    Spacer(modifier = Modifier.height(26.dp))

    // Featured Primary Mode: 2-PLAYER SPLIT SCREEN
    ModeSelectionCard(
      title = "2-PLAYER SPLIT SCREEN",
      subtitle = "Race head-to-head on the same device! Simultaneous real-time controls for both drivers.",
      badge = "MULTIPLAYER",
      gradient = listOf(Color(0xFFFF3D00), Color(0xFFFF9100)),
      icon = Icons.Default.PeopleAlt,
      testTag = "mode_split_screen",
      onClick = {
        viewModel.selectMode(GameMode.SPLIT_SCREEN_2P)
        viewModel.navigateTo(AppScreen.SPLIT_SCREEN_RACE)
      }
    )

    Spacer(modifier = Modifier.height(14.dp))

    // Mode 2: GRAND PRIX CHAMPIONSHIP
    ModeSelectionCard(
      title = "GRAND PRIX CHAMPIONSHIP",
      subtitle = "Compete against a grid of 5 intense AI racers. Use nitro, drift curves, and dodge oil slicks.",
      badge = "6 CARS GRID",
      gradient = listOf(Color(0xFF00B0FF), Color(0xFF00E5FF)),
      icon = Icons.Default.SportsScore,
      testTag = "mode_grand_prix",
      onClick = {
        viewModel.selectMode(GameMode.GRAND_PRIX)
        viewModel.navigateTo(AppScreen.GRAND_PRIX_RACE)
      }
    )

    Spacer(modifier = Modifier.height(14.dp))

    // Mode 3: GHOST RIVAL DUEL
    ModeSelectionCard(
      title = "GHOST RIVAL DUEL",
      subtitle = "Record your fastest lap & challenge a friend to beat your translucent ghost car!",
      badge = "TIME DUEL",
      gradient = listOf(Color(0xFF9C27B0), Color(0xFFE040FB)),
      icon = Icons.Default.Timer,
      testTag = "mode_ghost_duel",
      onClick = {
        viewModel.selectMode(GameMode.GHOST_DUEL)
        viewModel.navigateTo(AppScreen.GHOST_DUEL_RACE)
      }
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Quick Action Bar: Garage & Track Select
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Button(
        onClick = { viewModel.navigateTo(AppScreen.GARAGE) },
        modifier = Modifier
          .weight(1f)
          .height(54.dp)
          .testTag("garage_button"),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2838)),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f))
      ) {
        Icon(Icons.Default.DirectionsCar, contentDescription = "Garage", tint = Color(0xFF00E5FF))
        Spacer(modifier = Modifier.width(8.dp))
        Text("GARAGE & CARS", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
      }

      Button(
        onClick = { viewModel.navigateTo(AppScreen.TRACK_SELECT) },
        modifier = Modifier
          .weight(1f)
          .height(54.dp)
          .testTag("track_select_button"),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2838)),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9100).copy(alpha = 0.5f))
      ) {
        Icon(Icons.Default.Map, contentDescription = "Tracks", tint = Color(0xFFFF9100))
        Spacer(modifier = Modifier.width(8.dp))
        Text(uiState.selectedTrack.displayName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp, maxLines = 1)
      }
    }

    Spacer(modifier = Modifier.height(20.dp))
  }
}

@Composable
private fun ModeSelectionCard(
  title: String,
  subtitle: String,
  badge: String,
  gradient: List<Color>,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  testTag: String,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(
        Brush.horizontalGradient(
          colors = listOf(Color(0xFF161E2E), Color(0xFF121926))
        )
      )
      .border(
        width = 1.5.dp,
        brush = Brush.horizontalGradient(gradient),
        shape = RoundedCornerShape(18.dp)
      )
      .clickable(onClick = onClick)
      .testTag(testTag)
      .padding(18.dp)
  ) {
    Column {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(44.dp)
              .background(Brush.linearGradient(gradient), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = icon,
              contentDescription = title,
              tint = Color.White,
              modifier = Modifier.size(24.dp)
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
          )
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = gradient.first().copy(alpha = 0.25f)
        ) {
          Text(
            text = badge,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = gradient.first(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = subtitle,
        fontSize = 13.sp,
        color = Color(0xFF90A4AE),
        lineHeight = 18.sp
      )
    }
  }
}
