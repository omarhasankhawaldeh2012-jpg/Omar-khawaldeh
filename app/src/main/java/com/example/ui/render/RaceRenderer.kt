package com.example.ui.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.model.Particle
import com.example.model.PowerUp
import com.example.model.PowerUpType
import com.example.model.Racer
import com.example.model.SkidMark
import com.example.physics.SceneryItem
import com.example.physics.TrackDefinition
import kotlin.math.*

object RaceRenderer {

  fun drawRaceScene(
    drawScope: DrawScope,
    track: TrackDefinition,
    racers: List<Racer>,
    particles: List<Particle>,
    skidMarks: List<SkidMark>,
    cameraX: Float,
    cameraY: Float,
    viewportWidth: Float,
    viewportHeight: Float,
    zoom: Float = 0.95f,
    showRadar: Boolean = true
  ) {
    with(drawScope) {
      // 1. Draw Background
      drawRect(color = track.backgroundColor, size = size)

      // Compute camera offset to center target
      val cx = viewportWidth / 2f
      val cy = viewportHeight / 2f

      val toScreenX = { wx: Float -> cx + (wx - cameraX) * zoom }
      val toScreenY = { wy: Float -> cy + (wy - cameraY) * zoom }

      // 2. Draw Scenery (buildings/rocks)
      for (item in track.sceneryItems) {
        val sx = toScreenX(item.x)
        val sy = toScreenY(item.y)
        if (item.isRect) {
          drawRoundRect(
            color = item.color,
            topLeft = Offset(sx - (item.width * zoom) / 2f, sy - (item.height * zoom) / 2f),
            size = Size(item.width * zoom, item.height * zoom),
            cornerRadius = CornerRadius(8f, 8f)
          )
        } else {
          drawCircle(
            color = item.color,
            radius = item.radius * zoom,
            center = Offset(sx, sy)
          )
        }
      }

      // 3. Draw Track Asphalt & Curbs
      val wps = track.waypoints
      val roadPath = Path()
      if (wps.isNotEmpty()) {
        roadPath.moveTo(toScreenX(wps[0].x), toScreenY(wps[0].y))
        for (i in 1 until wps.size) {
          roadPath.lineTo(toScreenX(wps[i].x), toScreenY(wps[i].y))
        }
        roadPath.close()
      }

      // Outer curbs
      drawPath(
        path = roadPath,
        color = track.curbColorA,
        style = Stroke(
          width = (track.width + 36f) * zoom,
          cap = StrokeCap.Round,
          join = StrokeJoin.Round
        )
      )
      // Inner asphalt road
      drawPath(
        path = roadPath,
        color = track.trackAsphaltColor,
        style = Stroke(
          width = track.width * zoom,
          cap = StrokeCap.Round,
          join = StrokeJoin.Round
        )
      )

      // Road centerline dashes
      drawPath(
        path = roadPath,
        color = Color(0x33FFFFFF),
        style = Stroke(
          width = 3.5f * zoom,
          pathEffect = PathEffect.dashPathEffect(floatArrayOf(24f * zoom, 24f * zoom), 0f),
          cap = StrokeCap.Round,
          join = StrokeJoin.Round
        )
      )

      // Start / Finish Line Checkered Strip
      if (wps.size >= 2) {
        val p0 = wps[0]
        val p1 = wps[1]
        val pAngle = atan2(p1.y - p0.y, p1.x - p0.x)
        val perp = pAngle + (Math.PI / 2.0).toFloat()
        val sX = toScreenX(p0.x)
        val sY = toScreenY(p0.y)
        val halfW = (track.width * 0.5f) * zoom

        val lineStartX = sX + cos(perp) * halfW
        val lineStartY = sY + sin(perp) * halfW
        val lineEndX = sX - cos(perp) * halfW
        val lineEndY = sY - sin(perp) * halfW

        drawLine(
          color = Color.White,
          start = Offset(lineStartX, lineStartY),
          end = Offset(lineEndX, lineEndY),
          strokeWidth = 14f * zoom,
          pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f * zoom, 14f * zoom), 0f)
        )
      }

      // 4. Draw PowerUps
      for (pu in track.powerUps) {
        if (!pu.active) continue
        val px = toScreenX(pu.x)
        val py = toScreenY(pu.y)
        drawPowerUp(this, pu, px, py, zoom)
      }

      // 5. Draw Skid Marks
      for (sm in skidMarks) {
        drawLine(
          color = Color(0x44000000),
          start = Offset(toScreenX(sm.x1), toScreenY(sm.y1)),
          end = Offset(toScreenX(sm.x2), toScreenY(sm.y2)),
          strokeWidth = 6f * zoom,
          cap = StrokeCap.Round
        )
      }

      // 6. Draw Particles (smoke, nitro, sparks)
      for (p in particles) {
        val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
        drawCircle(
          color = p.color.copy(alpha = alpha),
          radius = p.radius * zoom,
          center = Offset(toScreenX(p.x), toScreenY(p.y))
        )
      }

      // 7. Draw Racers (Cars)
      for (racer in racers) {
        val carScreenX = toScreenX(racer.x)
        val carScreenY = toScreenY(racer.y)
        drawCar(this, racer, carScreenX, carScreenY, zoom)
      }

      // 8. Draw Radar Mini-Map
      if (showRadar) {
        drawRadarMiniMap(this, track, racers, viewportWidth, viewportHeight)
      }
    }
  }

  private fun drawPowerUp(
    drawScope: DrawScope,
    pu: PowerUp,
    sx: Float,
    sy: Float,
    zoom: Float
  ) {
    with(drawScope) {
      val r = 18f * zoom
      when (pu.type) {
        PowerUpType.NITRO -> {
          drawCircle(color = Color(0x5500F2FE), radius = r * 1.5f, center = Offset(sx, sy))
          drawCircle(color = Color(0xFF00F2FE), radius = r, center = Offset(sx, sy))
          drawCircle(color = Color.White, radius = r * 0.45f, center = Offset(sx, sy))
        }
        PowerUpType.TURBO_PAD -> {
          // Glowing speed chevron
          drawCircle(color = Color(0x4400E676), radius = r * 1.6f, center = Offset(sx, sy))
          drawCircle(color = Color(0xFF00E676), radius = r, center = Offset(sx, sy))
          drawLine(
            color = Color.White,
            start = Offset(sx - 8f * zoom, sy + 6f * zoom),
            end = Offset(sx, sy - 8f * zoom),
            strokeWidth = 4f * zoom,
            cap = StrokeCap.Round
          )
          drawLine(
            color = Color.White,
            start = Offset(sx + 8f * zoom, sy + 6f * zoom),
            end = Offset(sx, sy - 8f * zoom),
            strokeWidth = 4f * zoom,
            cap = StrokeCap.Round
          )
        }
        PowerUpType.OIL_SLICK -> {
          drawOval(
            color = Color(0xDD1A1110),
            topLeft = Offset(sx - 24f * zoom, sy - 14f * zoom),
            size = Size(48f * zoom, 28f * zoom)
          )
          drawOval(
            color = Color(0x889C27B0),
            topLeft = Offset(sx - 16f * zoom, sy - 9f * zoom),
            size = Size(32f * zoom, 18f * zoom)
          )
        }
        PowerUpType.SHIELD -> {
          drawCircle(color = Color(0x55FFD700), radius = r * 1.4f, center = Offset(sx, sy))
          drawCircle(color = Color(0xFFFFD700), radius = r, center = Offset(sx, sy))
        }
      }
    }
  }

  private fun drawCar(
    drawScope: DrawScope,
    racer: Racer,
    sx: Float,
    sy: Float,
    zoom: Float
  ) {
    with(drawScope) {
      val degrees = Math.toDegrees((racer.angle + racer.driftAngle).toDouble()).toFloat()
      val carLength = 52f * zoom
      val carWidth = 28f * zoom

      rotate(degrees, pivot = Offset(sx, sy)) {
        // Headlight Cones shining onto the track
        val lightConeLength = 80f * zoom
        val lightSpread = 22f * zoom
        val headPath = Path().apply {
          moveTo(sx + carLength * 0.45f, sy - carWidth * 0.35f)
          lineTo(sx + carLength * 0.45f + lightConeLength, sy - lightSpread)
          lineTo(sx + carLength * 0.45f + lightConeLength, sy + lightSpread)
          lineTo(sx + carLength * 0.45f, sy + carWidth * 0.35f)
          close()
        }
        drawPath(
          path = headPath,
          brush = Brush.horizontalGradient(
            colors = listOf(Color(0x66FFF59D), Color(0x00FFF59D)),
            startX = sx + carLength * 0.4f,
            endX = sx + carLength * 0.4f + lightConeLength
          )
        )

        // Drop shadow
        drawRoundRect(
          color = Color(0x66000000),
          topLeft = Offset(sx - carLength / 2f + 4f, sy - carWidth / 2f + 4f),
          size = Size(carLength, carWidth),
          cornerRadius = CornerRadius(8f * zoom, 8f * zoom)
        )

        // 4 Tires
        val tireL = 14f * zoom
        val tireW = 6f * zoom
        val tireColor = Color(0xFF1E2229)
        // Front Left & Right
        drawRoundRect(
          color = tireColor,
          topLeft = Offset(sx + carLength * 0.18f, sy - carWidth * 0.58f),
          size = Size(tireL, tireW),
          cornerRadius = CornerRadius(2f, 2f)
        )
        drawRoundRect(
          color = tireColor,
          topLeft = Offset(sx + carLength * 0.18f, sy + carWidth * 0.58f - tireW),
          size = Size(tireL, tireW),
          cornerRadius = CornerRadius(2f, 2f)
        )
        // Rear Left & Right
        drawRoundRect(
          color = tireColor,
          topLeft = Offset(sx - carLength * 0.38f, sy - carWidth * 0.58f),
          size = Size(tireL, tireW),
          cornerRadius = CornerRadius(2f, 2f)
        )
        drawRoundRect(
          color = tireColor,
          topLeft = Offset(sx - carLength * 0.38f, sy + carWidth * 0.58f - tireW),
          size = Size(tireL, tireW),
          cornerRadius = CornerRadius(2f, 2f)
        )

        // Car Body (Aerodynamic wedge)
        val bodyColor = racer.carColor
        drawRoundRect(
          color = bodyColor,
          topLeft = Offset(sx - carLength / 2f, sy - carWidth / 2f),
          size = Size(carLength, carWidth),
          cornerRadius = CornerRadius(10f * zoom, 10f * zoom)
        )

        // Roof & Racing Stripe
        drawRoundRect(
          color = racer.accentColor,
          topLeft = Offset(sx - carLength * 0.15f, sy - carWidth * 0.28f),
          size = Size(carLength * 0.45f, carWidth * 0.56f),
          cornerRadius = CornerRadius(6f * zoom, 6f * zoom)
        )

        // Windshield Glass
        drawRoundRect(
          color = Color(0xFF1B2838),
          topLeft = Offset(sx + carLength * 0.05f, sy - carWidth * 0.22f),
          size = Size(carLength * 0.18f, carWidth * 0.44f),
          cornerRadius = CornerRadius(4f * zoom, 4f * zoom)
        )

        // Rear Spoiler Wing
        drawRoundRect(
          color = Color(0xFF11141C),
          topLeft = Offset(sx - carLength * 0.48f, sy - carWidth * 0.45f),
          size = Size(6f * zoom, carWidth * 0.9f),
          cornerRadius = CornerRadius(2f, 2f)
        )

        // Taillights
        drawCircle(
          color = Color(0xFFFF1744),
          radius = 3f * zoom,
          center = Offset(sx - carLength * 0.48f, sy - carWidth * 0.28f)
        )
        drawCircle(
          color = Color(0xFFFF1744),
          radius = 3f * zoom,
          center = Offset(sx - carLength * 0.48f, sy + carWidth * 0.28f)
        )

        // Player Tag Indicator (P1 Blue, P2 Orange)
        if (racer.isPlayer) {
          drawCircle(
            color = if (racer.playerNumber == 1) Color(0xFF00E5FF) else Color(0xFFFF9100),
            radius = 5.5f * zoom,
            center = Offset(sx, sy)
          )
        }
      }
    }
  }

  private fun drawRadarMiniMap(
    drawScope: DrawScope,
    track: TrackDefinition,
    racers: List<Racer>,
    viewportWidth: Float,
    viewportHeight: Float
  ) {
    with(drawScope) {
      val mapSize = 130f
      val margin = 16f
      val mapLeft = viewportWidth - mapSize - margin
      val mapTop = margin

      // Background card
      drawRoundRect(
        color = Color(0xCC0D121D),
        topLeft = Offset(mapLeft, mapTop),
        size = Size(mapSize, mapSize),
        cornerRadius = CornerRadius(12f, 12f)
      )
      drawRoundRect(
        color = Color(0x4400F2FE),
        topLeft = Offset(mapLeft, mapTop),
        size = Size(mapSize, mapSize),
        cornerRadius = CornerRadius(12f, 12f),
        style = Stroke(width = 2f)
      )

      // Find track bounding box
      var minX = Float.MAX_VALUE
      var maxX = Float.MIN_VALUE
      var minY = Float.MAX_VALUE
      var maxY = Float.MIN_VALUE
      for (wp in track.waypoints) {
        if (wp.x < minX) minX = wp.x
        if (wp.x > maxX) maxX = wp.x
        if (wp.y < minY) minY = wp.y
        if (wp.y > maxY) maxY = wp.y
      }
      val spanX = (maxX - minX).coerceAtLeast(100f)
      val spanY = (maxY - minY).coerceAtLeast(100f)
      val pad = 14f
      val drawW = mapSize - pad * 2f
      val drawH = mapSize - pad * 2f

      val toMapX = { wx: Float -> mapLeft + pad + ((wx - minX) / spanX) * drawW }
      val toMapY = { wy: Float -> mapTop + pad + ((wy - minY) / spanY) * drawH }

      // Draw mini track outline
      val miniPath = Path()
      val wps = track.waypoints
      if (wps.isNotEmpty()) {
        miniPath.moveTo(toMapX(wps[0].x), toMapY(wps[0].y))
        for (i in 1 until wps.size) {
          miniPath.lineTo(toMapX(wps[i].x), toMapY(wps[i].y))
        }
        miniPath.close()
        drawPath(
          path = miniPath,
          color = Color(0x88455A64),
          style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
      }

      // Draw Racers on Radar
      for (racer in racers) {
        val mx = toMapX(racer.x)
        val my = toMapY(racer.y)
        val dotColor = when {
          racer.isPlayer && racer.playerNumber == 1 -> Color(0xFF00E5FF)
          racer.isPlayer && racer.playerNumber == 2 -> Color(0xFFFF9100)
          else -> Color(0xFFFF5252)
        }
        val r = if (racer.isPlayer) 5f else 3.5f
        drawCircle(color = dotColor, radius = r, center = Offset(mx, my))
      }
    }
  }
}
