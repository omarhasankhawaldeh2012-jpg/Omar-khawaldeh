package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CarStats
import com.example.model.Racer
import com.example.ui.render.RaceRenderer
import com.example.viewmodel.AppScreen
import com.example.viewmodel.GameUiState
import com.example.viewmodel.GameViewModel

@Composable
fun GarageScreen(
  viewModel: GameViewModel,
  uiState: GameUiState
) {
  BackHandler {
    viewModel.navigateTo(AppScreen.HOME)
  }

  var editingPlayer by remember { mutableIntStateOf(1) } // 1 for P1, 2 for P2
  val currentCarId = if (editingPlayer == 1) uiState.selectedCarId else uiState.p2CarId
  val currentCarColor = if (editingPlayer == 1) uiState.selectedCarColor else uiState.p2CarColor

  val activeCar = viewModel.availableCars.find { it.id == currentCarId } ?: viewModel.availableCars.first()
  val isUnlocked = uiState.unlockedCarIds.contains(activeCar.id)

  val paintColors = listOf(
    0xFF00E5FF, // Neon Cyan
    0xFFFF5722, // Orange Flame
    0xFF76FF03, // Acid Lime
    0xFFD500F9, // Electric Violet
    0xFFFFD600, // Solar Yellow
    0xFF37474F  // Stealth Carbon
  )

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(Color(0xFF0A0F1A), Color(0xFF111827), Color(0xFF0B0E14))
        )
      )
      .windowInsetsPadding(WindowInsets.safeDrawing)
      .verticalScroll(rememberScrollState())
      .padding(20.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Header Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(
        onClick = { viewModel.navigateTo(AppScreen.HOME) },
        modifier = Modifier
          .size(42.dp)
          .background(Color(0xFF1E2838), CircleShape)
          .testTag("garage_back_btn")
      ) {
        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
      }

      Text(
        text = "GARAGE & TUNING",
        fontSize = 20.sp,
        fontWeight = FontWeight.Black,
        color = Color.White
      )

      // Coins Pill
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF1E2838),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD600).copy(alpha = 0.6f))
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(Icons.Default.MonetizationOn, contentDescription = "Coins", tint = Color(0xFFFFD600), modifier = Modifier.size(18.dp))
          Text(text = "${uiState.coins}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
      }
    }

    Spacer(modifier = Modifier.height(18.dp))

    // Player 1 / Player 2 Customization Selector Tabs
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(Color(0xFF161F2E))
        .padding(4.dp)
    ) {
      TabButton(
        title = "PLAYER 1 CAR",
        isSelected = editingPlayer == 1,
        color = Color(0xFF00E5FF),
        testTag = "tab_p1_car",
        modifier = Modifier.weight(1f),
        onClick = { editingPlayer = 1 }
      )
      TabButton(
        title = "PLAYER 2 CAR",
        isSelected = editingPlayer == 2,
        color = Color(0xFFFF9100),
        testTag = "tab_p2_car",
        modifier = Modifier.weight(1f),
        onClick = { editingPlayer = 2 }
      )
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Interactive Turntable Car Showcase
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(180.dp)
        .clip(RoundedCornerShape(20.dp))
        .background(
          Brush.radialGradient(
            colors = listOf(Color(0xFF1E2A3E), Color(0xFF0D131F)),
            radius = 350f
          )
        )
        .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(20.dp)),
      contentAlignment = Alignment.Center
    ) {
      // Draw 2D Stylized Car on Pedestal
      Canvas(modifier = Modifier.size(240.dp, 120.dp)) {
        // Pedestal glow ring
        drawOval(
          brush = Brush.radialGradient(
            colors = listOf(Color(0x6600E5FF), Color(0x0000E5FF)),
            center = Offset(size.width / 2f, size.height * 0.75f),
            radius = size.width * 0.45f
          ),
          topLeft = Offset(size.width * 0.1f, size.height * 0.4f),
          size = androidx.compose.ui.geometry.Size(size.width * 0.8f, size.height * 0.6f)
        )

        val showcaseRacer = Racer(
          id = "showcase",
          name = activeCar.name,
          isPlayer = true,
          playerNumber = editingPlayer,
          x = size.width / 2f,
          y = size.height / 2f,
          angle = 0f,
          carColor = Color(currentCarColor),
          accentColor = Color(activeCar.accentColor)
        )

        RaceRenderer.drawRaceScene(
          drawScope = this,
          track = com.example.physics.TrackCatalog.getTrack(com.example.model.TrackType.NEON_METROPOLIS),
          racers = listOf(showcaseRacer),
          particles = emptyList(),
          skidMarks = emptyList(),
          cameraX = size.width / 2f,
          cameraY = size.height / 2f,
          viewportWidth = size.width,
          viewportHeight = size.height,
          zoom = 2.4f,
          showRadar = false
        )
      }

      Text(
        text = activeCar.name.uppercase(),
        fontSize = 18.sp,
        fontWeight = FontWeight.Black,
        color = Color.White,
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(16.dp)
      )
    }

    Spacer(modifier = Modifier.height(18.dp))

    // Car Selection Cards
    Text(
      text = "SELECT SUPERCAR",
      fontSize = 14.sp,
      fontWeight = FontWeight.Bold,
      color = Color(0xFF90A4AE),
      modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(10.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      viewModel.availableCars.forEach { car ->
        val selected = car.id == currentCarId
        val unlocked = uiState.unlockedCarIds.contains(car.id)

        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Color(0xFF1E2838) else Color(0xFF121926))
            .border(
              width = if (selected) 2.dp else 1.dp,
              color = if (selected) Color(0xFF00E5FF) else Color(0x33FFFFFF),
              shape = RoundedCornerShape(14.dp)
            )
            .clickable {
              if (editingPlayer == 1) {
                viewModel.selectCar(car.id)
              } else {
                viewModel.selectP2Car(car.id)
              }
            }
            .padding(10.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = if (unlocked) Icons.Default.DirectionsCar else Icons.Default.Lock,
              contentDescription = car.name,
              tint = if (selected) Color(0xFF00E5FF) else if (unlocked) Color.White else Color.Gray,
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = car.name,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White,
              maxLines = 1
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Specs & Performance Gauges
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(Color(0xFF141C2B))
        .padding(16.dp)
    ) {
      Text(
        text = "PERFORMANCE SPECS",
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        color = Color(0xFF90A4AE)
      )
      Spacer(modifier = Modifier.height(12.dp))

      StatBar(label = "Top Speed", value = activeCar.topSpeed / 1.25f, color = Color(0xFF00E5FF))
      Spacer(modifier = Modifier.height(8.dp))
      StatBar(label = "Acceleration", value = activeCar.acceleration / 1.35f, color = Color(0xFFFF9100))
      Spacer(modifier = Modifier.height(8.dp))
      StatBar(label = "Handling", value = activeCar.handling / 1.45f, color = Color(0xFF76FF03))
      Spacer(modifier = Modifier.height(8.dp))
      StatBar(label = "Nitro Boost", value = activeCar.nitroDuration / 1.4f, color = Color(0xFFD500F9))
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Neon Paint Customizer
    Text(
      text = "NEON PAINT FINISH",
      fontSize = 14.sp,
      fontWeight = FontWeight.Bold,
      color = Color(0xFF90A4AE),
      modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(10.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      paintColors.forEach { colorLong ->
        val isColorSelected = currentCarColor == colorLong
        Box(
          modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(Color(colorLong))
            .border(
              width = if (isColorSelected) 3.dp else 1.dp,
              color = if (isColorSelected) Color.White else Color(0x44FFFFFF),
              shape = CircleShape
            )
            .clickable {
              if (editingPlayer == 1) {
                viewModel.selectColor(colorLong)
              } else {
                viewModel.selectP2Color(colorLong)
              }
            },
          contentAlignment = Alignment.Center
        ) {
          if (isColorSelected) {
            Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color.Black, modifier = Modifier.size(20.dp))
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Buy Button if locked, or Selected indicator
    if (!isUnlocked) {
      Button(
        onClick = { viewModel.buyCar(activeCar) },
        enabled = uiState.coins >= activeCar.price,
        modifier = Modifier
          .fillMaxWidth()
          .height(54.dp)
          .testTag("buy_car_btn"),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD600))
      ) {
        Icon(Icons.Default.MonetizationOn, contentDescription = "Buy", tint = Color.Black)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "UNLOCK FOR ${activeCar.price} COINS",
          color = Color.Black,
          fontWeight = FontWeight.Black,
          fontSize = 15.sp
        )
      }
    } else {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1E2838)
      ) {
        Row(
          modifier = Modifier.padding(vertical = 14.dp),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.CheckCircle, contentDescription = "Active", tint = Color(0xFF00E676))
          Spacer(modifier = Modifier.width(8.dp))
          Text("CAR READY FOR RACE", color = Color.White, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
fun TabButton(
  title: String,
  isSelected: Boolean,
  color: Color,
  testTag: String,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(10.dp))
      .background(if (isSelected) color.copy(alpha = 0.25f) else Color.Transparent)
      .clickable(onClick = onClick)
      .testTag(testTag)
      .padding(vertical = 10.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = title,
      fontWeight = FontWeight.Black,
      fontSize = 13.sp,
      color = if (isSelected) color else Color(0xFF78909C)
    )
  }
}

@Composable
fun StatBar(label: String, value: Float, color: Color) {
  Column {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(text = label, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
      Text(text = "${(value * 100).toInt()}%", fontSize = 12.sp, color = color, fontWeight = FontWeight.Bold)
    }
    Spacer(modifier = Modifier.height(4.dp))
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(8.dp)
        .clip(RoundedCornerShape(4.dp))
        .background(Color(0xFF263238))
    ) {
      Box(
        modifier = Modifier
          .fillMaxHeight()
          .fillMaxWidth(value.coerceIn(0f, 1f))
          .background(color)
      )
    }
  }
}
