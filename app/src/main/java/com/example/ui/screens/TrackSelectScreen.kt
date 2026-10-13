package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TrackType
import com.example.viewmodel.AppScreen
import com.example.viewmodel.GameUiState
import com.example.viewmodel.GameViewModel

@Composable
fun TrackSelectScreen(
  viewModel: GameViewModel,
  uiState: GameUiState
) {
  BackHandler {
    viewModel.navigateTo(AppScreen.HOME)
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(Color(0xFF090D15), Color(0xFF101726), Color(0xFF07090F))
        )
      )
      .windowInsetsPadding(WindowInsets.safeDrawing)
      .verticalScroll(rememberScrollState())
      .padding(20.dp)
  ) {
    // Top Bar
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
          .testTag("track_back_btn")
      ) {
        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
      }

      Text(
        text = "CIRCUITS & TRACKS",
        fontSize = 20.sp,
        fontWeight = FontWeight.Black,
        color = Color.White
      )

      Spacer(modifier = Modifier.size(42.dp))
    }

    Spacer(modifier = Modifier.height(24.dp))

    TrackType.values().forEach { track ->
      val isSelected = uiState.selectedTrack == track
      val bestTimeMs = uiState.bestTrackTimes[track]

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(18.dp))
          .background(if (isSelected) Color(0xFF1A2436) else Color(0xFF111724))
        .border(
          width = if (isSelected) 2.dp else 1.dp,
          color = if (isSelected) Color(0xFF00E5FF) else Color(0x33FFFFFF),
          shape = RoundedCornerShape(18.dp)
        )
        .clickable {
          viewModel.selectTrack(track)
        }
        .padding(18.dp)
      ) {
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = track.displayName,
              fontSize = 18.sp,
              fontWeight = FontWeight.Black,
              color = Color.White
            )

            if (isSelected) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF00E5FF).copy(alpha = 0.2f)
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                  Text(text = "SELECTED", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF))
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = track.description,
            fontSize = 13.sp,
            color = Color(0xFF90A4AE),
            lineHeight = 18.sp
          )

          Spacer(modifier = Modifier.height(14.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "${track.laps} LAPS",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFFF9100)
            )

            if (bestTimeMs != null && bestTimeMs > 0) {
              val sec = bestTimeMs / 1000
              val ms = (bestTimeMs % 1000) / 10
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFFFFD600), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "BEST: ${String.format("%02d:%02d.%02d", sec / 60, sec % 60, ms)}",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFFFFD600)
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
    }

    Spacer(modifier = Modifier.height(10.dp))

    Button(
      onClick = { viewModel.navigateTo(AppScreen.HOME) },
      modifier = Modifier
        .fillMaxWidth()
        .height(54.dp)
        .testTag("track_confirm_btn"),
      shape = RoundedCornerShape(14.dp),
      colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
    ) {
      Text(
        text = "CONFIRM TRACK",
        color = Color.Black,
        fontWeight = FontWeight.Black,
        fontSize = 15.sp
      )
    }
  }
}
