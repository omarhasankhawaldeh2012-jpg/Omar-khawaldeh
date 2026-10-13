package com.example.model

import androidx.compose.ui.graphics.Color
import kotlin.math.PI

enum class GameMode {
  GRAND_PRIX,
  SPLIT_SCREEN_2P,
  GHOST_DUEL,
  TIME_TRIAL
}

enum class TrackType(val displayName: String, val laps: Int, val description: String) {
  NEON_METROPOLIS("Neon Metropolis", 3, "High-speed city circuit with glowing neon curves and turbo pads"),
  CANYON_DRIFT("Canyon Drift", 3, "Narrow mountain pass with sharp hairpins and dirt runoff"),
  CYBER_SPEEDWAY("Cyber Speedway", 4, "Futuristic oval super-circuit with warp gates and chicanes")
}

data class CarStats(
  val id: String,
  val name: String,
  val topSpeed: Float, // base max speed
  val acceleration: Float,
  val handling: Float, // turn rate
  val nitroDuration: Float,
  val price: Int,
  val primaryColor: Long,
  val accentColor: Long
)

data class Waypoint(
  val x: Float,
  val y: Float,
  val targetSpeed: Float = 1.0f
)

data class Particle(
  var x: Float,
  var y: Float,
  var vx: Float,
  var vy: Float,
  var life: Float,
  val maxLife: Float,
  val color: Color,
  val radius: Float
)

data class SkidMark(
  val x1: Float,
  val y1: Float,
  val x2: Float,
  val y2: Float,
  val alpha: Float
)

data class PowerUp(
  val id: Int,
  val x: Float,
  val y: Float,
  val type: PowerUpType,
  var active: Boolean = true,
  var respawnTimer: Float = 0f
)

enum class PowerUpType {
  NITRO,
  OIL_SLICK,
  TURBO_PAD,
  SHIELD
}

data class Racer(
  val id: String,
  val name: String,
  val isPlayer: Boolean,
  val playerNumber: Int = 1, // 1 or 2 for multiplayer
  var x: Float = 0f,
  var y: Float = 0f,
  var angle: Float = 0f, // in radians
  var speed: Float = 0f,
  var driftAngle: Float = 0f,
  var isDrifting: Boolean = false,
  var nitroFuel: Float = 100f,
  var isBoosting: Boolean = false,
  var currentWaypointIndex: Int = 0,
  var currentLap: Int = 1,
  var lapProgress: Float = 0f, // 0.0 to 1.0 for position sorting
  var raceFinished: Boolean = false,
  var finishTimeMs: Long = 0L,
  var bestLapTimeMs: Long = 0L,
  var currentLapTimeMs: Long = 0L,
  var carColor: Color = Color(0xFF00E5FF),
  var accentColor: Color = Color(0xFFFF5252),
  var spinTimer: Float = 0f, // if hit oil
  var shieldActive: Boolean = false,
  var steeringInput: Float = 0f, // -1.0 to 1.0
  var throttleInput: Float = 0f, // 1.0 = gas, -1.0 = brake
  var topSpeedFactor: Float = 1.0f,
  var accelFactor: Float = 1.0f,
  var handlingFactor: Float = 1.0f
) {
  val isSpinning: Boolean get() = spinTimer > 0f
}

data class GhostFrame(
  val timeMs: Long,
  val x: Float,
  val y: Float,
  val angle: Float,
  val speed: Float,
  val isDrifting: Boolean,
  val isBoosting: Boolean
)
