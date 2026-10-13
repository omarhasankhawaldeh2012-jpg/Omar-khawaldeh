package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
fun PodiumResultsScreen(
  viewModel: GameViewModel,
  uiState: GameUiState
) {
  BackHandler {
    viewModel.navigateTo(AppScreen.HOME)
  }

  val result = uiState.lastPodiumResult

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(Color(0xFF090D15), Color(0xFF131D2D), Color(0xFF070A10))
        )
      )
      .windowInsetsPadding(WindowInsets.safeDrawing)
      .padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    // Trophy Icon with Glow
    Box(
      modifier = Modifier
        .size(110.dp)
        .clip(CircleShape)
        .background(
          Brush.radialGradient(
            colors = listOf(Color(0xFFFFD600), Color(0xFFFF9100))
          )
        ),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Default.EmojiEvents,
        contentDescription = "Trophy",
        tint = Color.Black,
        modifier = Modifier.size(64.dp)
      )
    }

    Spacer(modifier = Modifier.height(20.dp))

    Text(
      text = if (result?.isMultiplayer == true) "RACE WINNER" else "RACE FINISHED",
      fontSize = 14.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = 3.sp,
      color = Color(0xFF00E5FF)
    )

    Spacer(modifier = Modifier.height(6.dp))

    Text(
      text = result?.winnerName ?: "VICTORY",
      fontSize = 32.sp,
      fontWeight = FontWeight.Black,
      color = Color.White,
      textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Results Card
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(18.dp),
      color = Color(0xFF141C2B),
      border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3300E5FF))
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        if (result?.isMultiplayer == false) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Finish Position", color = Color(0xFF90A4AE), fontSize = 14.sp)
            Text(
              "${result.player1Rank} / 6",
              fontWeight = FontWeight.Black,
              color = if (result.player1Rank == 1) Color(0xFFFFD600) else Color.White,
              fontSize = 15.sp
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("Race Time", color = Color(0xFF90A4AE), fontSize = 14.sp)
          val totalSec = (result?.bestLapTimeMs ?: 0L) / 1000
          val ms = ((result?.bestLapTimeMs ?: 0L) % 1000) / 10
          Text(
            String.format("%02d:%02d.%02d", totalSec / 60, totalSec % 60, ms),
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 15.sp
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Prize Reward", color = Color(0xFF90A4AE), fontSize = 14.sp)
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = Color(0xFFFFD600), modifier = Modifier.size(18.dp))
            Text(
              "+${result?.coinsEarned ?: 0}",
              fontWeight = FontWeight.Black,
              color = Color(0xFFFFD600),
              fontSize = 16.sp
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(30.dp))

    // Rematch Button
    Button(
      onClick = {
        when (result?.mode) {
          GameMode.SPLIT_SCREEN_2P -> viewModel.navigateTo(AppScreen.SPLIT_SCREEN_RACE)
          GameMode.GHOST_DUEL -> viewModel.navigateTo(AppScreen.GHOST_DUEL_RACE)
          else -> viewModel.navigateTo(AppScreen.GRAND_PRIX_RACE)
        }
      },
      modifier = Modifier
        .fillMaxWidth()
        .height(54.dp)
        .testTag("rematch_btn"),
      shape = RoundedCornerShape(14.dp),
      colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
    ) {
      Icon(Icons.Default.Replay, contentDescription = null, tint = Color.Black)
      Spacer(modifier = Modifier.width(8.dp))
      Text("REMATCH / RACE AGAIN", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 15.sp)
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Home Button
    OutlinedButton(
      onClick = { viewModel.navigateTo(AppScreen.HOME) },
      modifier = Modifier
        .fillMaxWidth()
        .height(54.dp)
        .testTag("results_home_btn"),
      shape = RoundedCornerShape(14.dp),
      colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
    ) {
      Icon(Icons.Default.Home, contentDescription = null, tint = Color.White)
      Spacer(modifier = Modifier.width(8.dp))
      Text("MAIN MENU", fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
  }
}
