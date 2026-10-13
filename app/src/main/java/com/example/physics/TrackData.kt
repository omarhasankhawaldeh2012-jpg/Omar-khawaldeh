package com.example.physics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.model.PowerUp
import com.example.model.PowerUpType
import com.example.model.TrackType
import com.example.model.Waypoint
import kotlin.math.*

data class TrackDefinition(
  val type: TrackType,
  val name: String,
  val width: Float,
  val waypoints: List<Waypoint>,
  val powerUps: List<PowerUp>,
  val backgroundColor: Color,
  val trackAsphaltColor: Color,
  val curbColorA: Color,
  val curbColorB: Color,
  val accentColor: Color,
  val sceneryItems: List<SceneryItem>
)

data class SceneryItem(
  val x: Float,
  val y: Float,
  val radius: Float,
  val color: Color,
  val isRect: Boolean = false,
  val width: Float = 0f,
  val height: Float = 0f
)

object TrackCatalog {

  fun getTrack(type: TrackType): TrackDefinition {
    return when (type) {
      TrackType.NEON_METROPOLIS -> createNeonMetropolis()
      TrackType.CANYON_DRIFT -> createCanyonDrift()
      TrackType.CYBER_SPEEDWAY -> createCyberSpeedway()
    }
  }

  private fun createNeonMetropolis(): TrackDefinition {
    // A sophisticated urban racing circuit: 14 waypoints forming an interesting, fun GP circuit
    val pts = listOf(
      Waypoint(400f, 900f),
      Waypoint(1000f, 900f),
      Waypoint(1500f, 850f),
      Waypoint(1900f, 650f),
      Waypoint(2000f, 300f),
      Waypoint(1700f, 150f),
      Waypoint(1200f, 250f),
      Waypoint(900f, 400f),
      Waypoint(600f, 250f),
      Waypoint(300f, 150f),
      Waypoint(100f, 400f),
      Waypoint(150f, 700f)
    )

    val powerUps = listOf(
      PowerUp(1, 700f, 900f, PowerUpType.TURBO_PAD),
      PowerUp(2, 1750f, 750f, PowerUpType.NITRO),
      PowerUp(3, 1850f, 200f, PowerUpType.OIL_SLICK),
      PowerUp(4, 1050f, 320f, PowerUpType.TURBO_PAD),
      PowerUp(5, 450f, 200f, PowerUpType.NITRO),
      PowerUp(6, 200f, 550f, PowerUpType.OIL_SLICK)
    )

    // Neon skyscraper silhouettes and street light accents
    val scenery = mutableListOf<SceneryItem>()
    // Inner city buildings
    scenery.add(SceneryItem(1400f, 550f, 0f, Color(0xFF162035), true, 260f, 180f))
    scenery.add(SceneryItem(1100f, 650f, 0f, Color(0xFF0F172A), true, 220f, 140f))
    scenery.add(SceneryItem(500f, 600f, 0f, Color(0xFF131D31), true, 280f, 200f))
    // Outer glow rings
    scenery.add(SceneryItem(2000f, 100f, 80f, Color(0x3300F2FE)))
    scenery.add(SceneryItem(100f, 100f, 90f, Color(0x33FF007F)))

    return TrackDefinition(
      type = TrackType.NEON_METROPOLIS,
      name = "Neon Metropolis",
      width = 220f,
      waypoints = pts,
      powerUps = powerUps,
      backgroundColor = Color(0xFF0A0D14),
      trackAsphaltColor = Color(0xFF1E2330),
      curbColorA = Color(0xFF00F2FE),
      curbColorB = Color(0xFFFF007F),
      accentColor = Color(0xFF00F2FE),
      sceneryItems = scenery
    )
  }

  private fun createCanyonDrift(): TrackDefinition {
    // Sharp mountain curves, technical drift course
    val pts = listOf(
      Waypoint(300f, 800f),
      Waypoint(900f, 850f),
      Waypoint(1400f, 950f),
      Waypoint(1800f, 750f),
      Waypoint(1600f, 500f),
      Waypoint(1200f, 550f),
      Waypoint(900f, 400f),
      Waypoint(1300f, 250f),
      Waypoint(1700f, 180f),
      Waypoint(1200f, 100f),
      Waypoint(600f, 150f),
      Waypoint(200f, 350f),
      Waypoint(200f, 650f)
    )

    val powerUps = listOf(
      PowerUp(1, 600f, 825f, PowerUpType.TURBO_PAD),
      PowerUp(2, 1700f, 625f, PowerUpType.NITRO),
      PowerUp(3, 1050f, 475f, PowerUpType.OIL_SLICK),
      PowerUp(4, 1500f, 215f, PowerUpType.TURBO_PAD),
      PowerUp(5, 400f, 250f, PowerUpType.NITRO)
    )

    val scenery = mutableListOf<SceneryItem>()
    // Canyon rocks / boulders
    scenery.add(SceneryItem(800f, 650f, 110f, Color(0xFF3E2723)))
    scenery.add(SceneryItem(1450f, 380f, 90f, Color(0xFF4E342E)))
    scenery.add(SceneryItem(900f, 220f, 120f, Color(0xFF3E2723)))
    scenery.add(SceneryItem(400f, 450f, 100f, Color(0xFF2E1C14)))

    return TrackDefinition(
      type = TrackType.CANYON_DRIFT,
      name = "Canyon Drift",
      width = 200f,
      waypoints = pts,
      powerUps = powerUps,
      backgroundColor = Color(0xFF1A120B),
      trackAsphaltColor = Color(0xFF2C241D),
      curbColorA = Color(0xFFFF9800),
      curbColorB = Color(0xFFD84315),
      accentColor = Color(0xFFFF7043),
      sceneryItems = scenery
    )
  }

  private fun createCyberSpeedway(): TrackDefinition {
    // Ultra high speed stadium track
    val pts = listOf(
      Waypoint(500f, 900f),
      Waypoint(1200f, 900f),
      Waypoint(1700f, 800f),
      Waypoint(1900f, 500f),
      Waypoint(1700f, 200f),
      Waypoint(1200f, 150f),
      Waypoint(500f, 150f),
      Waypoint(200f, 350f),
      Waypoint(200f, 650f)
    )

    val powerUps = listOf(
      PowerUp(1, 850f, 900f, PowerUpType.TURBO_PAD),
      PowerUp(2, 1850f, 650f, PowerUpType.TURBO_PAD),
      PowerUp(3, 1450f, 175f, PowerUpType.NITRO),
      PowerUp(4, 350f, 250f, PowerUpType.NITRO)
    )

    val scenery = mutableListOf<SceneryItem>()
    scenery.add(SceneryItem(1050f, 525f, 180f, Color(0xFF101C30)))
    scenery.add(SceneryItem(1050f, 525f, 120f, Color(0xFF00E5FF)))

    return TrackDefinition(
      type = TrackType.CYBER_SPEEDWAY,
      name = "Cyber Speedway",
      width = 250f,
      waypoints = pts,
      powerUps = powerUps,
      backgroundColor = Color(0xFF070B14),
      trackAsphaltColor = Color(0xFF1A2234),
      curbColorA = Color(0xFF00E676),
      curbColorB = Color(0xFF00B0FF),
      accentColor = Color(0xFF00E676),
      sceneryItems = scenery
    )
  }
}
