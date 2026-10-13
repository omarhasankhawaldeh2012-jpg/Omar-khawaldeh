package com.example.physics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.audio.SoundManager
import com.example.model.Particle
import com.example.model.PowerUpType
import com.example.model.Racer
import com.example.model.SkidMark
import kotlin.math.*
import kotlin.random.Random

class RacingPhysics(
  private val track: TrackDefinition,
  private val soundManager: SoundManager? = null
) {
  val particles = mutableListOf<Particle>()
  val skidMarks = mutableListOf<SkidMark>()
  private val maxSkidMarks = 160
  private val random = Random(1337)

  fun updateRacer(racer: Racer, dt: Float) {
    if (racer.raceFinished) {
      // Decelerate gently once finished
      racer.speed *= (1f - 2f * dt).coerceAtLeast(0f)
      updatePositionAndOrientation(racer, dt)
      return
    }

    // Handle spin out from oil slick
    if (racer.isSpinning) {
      racer.spinTimer -= dt
      racer.angle += 12f * dt
      racer.speed *= (1f - 1.5f * dt).coerceAtLeast(0.1f)
      updatePositionAndOrientation(racer, dt)
      if (random.nextFloat() < 0.4f) {
        spawnSmokeParticle(racer.x, racer.y, Color.Gray)
      }
      return
    }

    // Determine track surface
    val distFromCenter = getDistanceToTrackCenter(racer.x, racer.y)
    val isOffRoad = distFromCenter > (track.width * 0.5f)
    val surfaceFriction = if (isOffRoad) 4.5f else 1.2f
    val baseMaxSpeed = 520f * racer.topSpeedFactor
    val maxSpeed = if (isOffRoad) baseMaxSpeed * 0.45f else baseMaxSpeed

    // Nitro handling
    if (racer.isBoosting && racer.nitroFuel > 0f) {
      racer.nitroFuel = (racer.nitroFuel - 32f * dt).coerceAtLeast(0f)
      val boostMaxSpeed = maxSpeed * 1.55f
      racer.speed = (racer.speed + 600f * racer.accelFactor * dt).coerceAtMost(boostMaxSpeed)

      // Nitro exhaust flames
      val tailAngle = racer.angle + Math.PI.toFloat()
      val tailX = racer.x + cos(tailAngle) * 24f
      val tailY = racer.y + sin(tailAngle) * 24f
      spawnNitroFlame(tailX, tailY, tailAngle)

      if (racer.nitroFuel <= 0f) {
        racer.isBoosting = false
      }
    } else {
      racer.isBoosting = false
      // Throttle acceleration / braking
      if (racer.throttleInput > 0f) {
        val targetSpeed = maxSpeed * racer.throttleInput
        val accelRate = 340f * racer.accelFactor
        if (racer.speed < targetSpeed) {
          racer.speed += accelRate * dt
        }
      } else if (racer.throttleInput < 0f) {
        // Braking / reverse
        racer.speed += (racer.throttleInput * 480f) * dt
        if (racer.speed < -120f) racer.speed = -120f
      } else {
        // Coasting friction
        val drag = racer.speed * surfaceFriction * dt
        racer.speed = if (racer.speed > 0f) {
          (racer.speed - drag).coerceAtLeast(0f)
        } else {
          (racer.speed + drag).coerceAtMost(0f)
        }
      }
    }

    // Steering & Drifting
    val speedRatio = (abs(racer.speed) / baseMaxSpeed).coerceIn(0.1f, 1f)
    val turnRate = 3.2f * racer.handlingFactor * speedRatio

    if (racer.steeringInput != 0f) {
      val direction = if (racer.speed >= 0f) 1f else -1f
      racer.angle += racer.steeringInput * turnRate * direction * dt

      if (racer.isDrifting && abs(racer.speed) > 180f) {
        val targetDrift = racer.steeringInput * 0.55f
        racer.driftAngle = racer.driftAngle + (targetDrift - racer.driftAngle) * 6f * dt

        // Tire skids and smoke
        spawnDriftEffects(racer)
        // Award slight nitro for drifting!
        racer.nitroFuel = (racer.nitroFuel + 12f * dt).coerceAtMost(100f)
      } else {
        racer.driftAngle *= (1f - 5f * dt).coerceAtLeast(0f)
      }
    } else {
      racer.driftAngle *= (1f - 6f * dt).coerceAtLeast(0f)
    }

    updatePositionAndOrientation(racer, dt)
    checkPowerUps(racer)
    checkTrackProgress(racer)
  }

  private fun updatePositionAndOrientation(racer: Racer, dt: Float) {
    val travelAngle = racer.angle + racer.driftAngle
    racer.x += cos(travelAngle) * racer.speed * dt
    racer.y += sin(travelAngle) * racer.speed * dt
  }

  fun updateAi(racer: Racer, otherRacers: List<Racer>, dt: Float) {
    if (racer.raceFinished) return

    val waypoints = track.waypoints
    val currentWp = waypoints[racer.currentWaypointIndex]
    // Look ahead 1 waypoint for smoother turning
    val nextWpIndex = (racer.currentWaypointIndex + 1) % waypoints.size
    val nextWp = waypoints[nextWpIndex]

    // Target a blend between current and next waypoint
    val targetX = (currentWp.x * 0.4f) + (nextWp.x * 0.6f)
    val targetY = (currentWp.y * 0.4f) + (nextWp.y * 0.6f)

    val dx = targetX - racer.x
    val dy = targetY - racer.y
    val dist = hypot(dx, dy)

    if (dist < 120f) {
      racer.currentWaypointIndex = nextWpIndex
    }

    val targetAngle = atan2(dy, dx)
    var angleDiff = targetAngle - racer.angle
    while (angleDiff > PI) angleDiff -= (2 * PI).toFloat()
    while (angleDiff < -PI) angleDiff += (2 * PI).toFloat()

    // Steer towards target
    racer.steeringInput = (angleDiff * 1.8f).coerceIn(-1f, 1f)

    // Full throttle on straights, slight brake on hairpin bends
    if (abs(angleDiff) > 0.8f) {
      racer.throttleInput = 0.6f
      racer.isDrifting = abs(angleDiff) > 1.1f
    } else {
      racer.throttleInput = 1.0f
      racer.isDrifting = false
      // AI nitro on long straights
      if (abs(angleDiff) < 0.25f && racer.nitroFuel > 60f && random.nextFloat() < 0.05f) {
        racer.isBoosting = true
      }
    }
  }

  fun handleCarCollisions(racers: List<Racer>) {
    val carRadius = 26f
    for (i in racers.indices) {
      for (j in i + 1 until racers.size) {
        val r1 = racers[i]
        val r2 = racers[j]
        val dx = r2.x - r1.x
        val dy = r2.y - r1.y
        val dist = hypot(dx, dy)
        val minDist = carRadius * 2f

        if (dist < minDist && dist > 0.001f) {
          // Push cars apart
          val overlap = (minDist - dist)
          val nx = dx / dist
          val ny = dy / dist

          r1.x -= nx * overlap * 0.5f
          r1.y -= ny * overlap * 0.5f
          r2.x += nx * overlap * 0.5f
          r2.y += ny * overlap * 0.5f

          // Bounce impulse exchange
          val tempSpeed = r1.speed
          r1.speed = (r2.speed * 0.85f).coerceAtLeast(0f)
          r2.speed = (tempSpeed * 0.85f).coerceAtLeast(0f)

          // Sound and sparks
          if (r1.isPlayer || r2.isPlayer) {
            soundManager?.playCrash()
          }
          spawnSparks((r1.x + r2.x) * 0.5f, (r1.y + r2.y) * 0.5f)
        }
      }
    }
  }

  private fun checkPowerUps(racer: Racer) {
    for (pu in track.powerUps) {
      if (!pu.active) continue
      val dist = hypot(racer.x - pu.x, racer.y - pu.y)
      if (dist < 42f) {
        when (pu.type) {
          PowerUpType.NITRO -> {
            racer.nitroFuel = (racer.nitroFuel + 45f).coerceAtMost(100f)
            pu.active = false
            pu.respawnTimer = 6f
            if (racer.isPlayer) soundManager?.playNitroBoost()
          }
          PowerUpType.TURBO_PAD -> {
            racer.speed = (racer.speed + 380f).coerceAtMost(750f)
            racer.isBoosting = true
            if (racer.isPlayer) soundManager?.playNitroBoost()
          }
          PowerUpType.OIL_SLICK -> {
            racer.spinTimer = 1.1f
            racer.isDrifting = false
            if (racer.isPlayer) soundManager?.playDriftScreech()
          }
          PowerUpType.SHIELD -> {
            racer.shieldActive = true
            pu.active = false
            pu.respawnTimer = 10f
          }
        }
      }
    }
  }

  fun updatePowerUpTimers(dt: Float) {
    for (pu in track.powerUps) {
      if (!pu.active) {
        pu.respawnTimer -= dt
        if (pu.respawnTimer <= 0f) {
          pu.active = true
        }
      }
    }
  }

  private fun checkTrackProgress(racer: Racer) {
    val waypoints = track.waypoints
    val totalWps = waypoints.size
    val currentWp = waypoints[racer.currentWaypointIndex]
    val dist = hypot(racer.x - currentWp.x, racer.y - currentWp.y)

    if (dist < 180f) {
      val nextIndex = (racer.currentWaypointIndex + 1) % totalWps
      if (racer.currentWaypointIndex == totalWps - 1 && nextIndex == 0) {
        // Completed lap!
        if (racer.isPlayer) {
          soundManager?.playLapComplete()
        }
        racer.currentLap++
        if (racer.currentLap > track.type.laps) {
          racer.raceFinished = true
        }
      }
      racer.currentWaypointIndex = nextIndex
    }

    // Compute continuous lap progress
    val baseProgress = (racer.currentLap - 1) * totalWps + racer.currentWaypointIndex
    racer.lapProgress = baseProgress.toFloat()
  }

  private fun getDistanceToTrackCenter(px: Float, py: Float): Float {
    var minDist = Float.MAX_VALUE
    val wps = track.waypoints
    for (i in wps.indices) {
      val p1 = wps[i]
      val p2 = wps[(i + 1) % wps.size]
      val d = distToSegment(px, py, p1.x, p1.y, p2.x, p2.y)
      if (d < minDist) minDist = d
    }
    return minDist
  }

  private fun distToSegment(px: Float, py: Float, x1: Float, y1: Float, x2: Float, y2: Float): Float {
    val dx = x2 - x1
    val dy = y2 - y1
    val lenSq = dx * dx + dy * dy
    if (lenSq < 0.0001f) return hypot(px - x1, py - y1)
    val t = (((px - x1) * dx + (py - y1) * dy) / lenSq).coerceIn(0f, 1f)
    val projX = x1 + t * dx
    val projY = y1 + t * dy
    return hypot(px - projX, py - projY)
  }

  fun updateParticles(dt: Float) {
    val iter = particles.iterator()
    while (iter.hasNext()) {
      val p = iter.next()
      p.x += p.vx * dt
      p.y += p.vy * dt
      p.life -= dt
      if (p.life <= 0f) {
        iter.remove()
      }
    }
  }

  private fun spawnNitroFlame(x: Float, y: Float, angle: Float) {
    for (i in 0..2) {
      val speed = random.nextFloat() * 120f + 80f
      val spread = (random.nextFloat() - 0.5f) * 0.4f
      val flameAngle = angle + spread
      val colors = listOf(Color(0xFF00E5FF), Color(0xFFFF5252), Color(0xFFFFD700))
      particles.add(
        Particle(
          x = x,
          y = y,
          vx = cos(flameAngle) * speed,
          vy = sin(flameAngle) * speed,
          life = 0.28f,
          maxLife = 0.28f,
          color = colors[random.nextInt(colors.size)],
          radius = random.nextFloat() * 7f + 4f
        )
      )
    }
  }

  private fun spawnSparks(x: Float, y: Float) {
    for (i in 0..8) {
      val angle = random.nextFloat() * (2 * PI).toFloat()
      val speed = random.nextFloat() * 220f + 60f
      particles.add(
        Particle(
          x = x,
          y = y,
          vx = cos(angle) * speed,
          vy = sin(angle) * speed,
          life = 0.35f,
          maxLife = 0.35f,
          color = Color(0xFFFFEB3B),
          radius = 3.5f
        )
      )
    }
  }

  private fun spawnDriftEffects(racer: Racer) {
    // Add skid mark
    val rearAngle = racer.angle + Math.PI.toFloat()
    val tireOffset = 14f
    val perpAngle = racer.angle + (Math.PI / 2.0).toFloat()
    val leftTireX = racer.x + cos(rearAngle) * 16f + cos(perpAngle) * tireOffset
    val leftTireY = racer.y + sin(rearAngle) * 16f + sin(perpAngle) * tireOffset
    val rightTireX = racer.x + cos(rearAngle) * 16f - cos(perpAngle) * tireOffset
    val rightTireY = racer.y + sin(rearAngle) * 16f - sin(perpAngle) * tireOffset

    if (skidMarks.size > maxSkidMarks) {
      skidMarks.removeAt(0)
    }
    skidMarks.add(SkidMark(leftTireX, leftTireY, rightTireX, rightTireY, 0.45f))

    if (random.nextFloat() < 0.35f) {
      spawnSmokeParticle(leftTireX, leftTireY, Color(0xCCECEFF1))
      spawnSmokeParticle(rightTireX, rightTireY, Color(0xCCECEFF1))
    }
  }

  private fun spawnSmokeParticle(x: Float, y: Float, color: Color) {
    particles.add(
      Particle(
        x = x,
        y = y,
        vx = (random.nextFloat() - 0.5f) * 40f,
        vy = (random.nextFloat() - 0.5f) * 40f,
        life = 0.4f,
        maxLife = 0.4f,
        color = color,
        radius = random.nextFloat() * 10f + 6f
      )
    )
  }
}
