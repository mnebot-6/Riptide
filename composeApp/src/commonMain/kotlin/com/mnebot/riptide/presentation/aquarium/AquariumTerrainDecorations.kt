package com.mnebot.riptide.presentation.aquarium

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// ── Placement data classes ─────────────────────────────────────────────────────

data class SeaweedPlacement(
    val xFrac: Float,
    val heightFrac: Float,
    val stalkCount: Int,
    val swayPhase: Float,
    val colorIndex: Int
)

data class ShellPlacement(
    val xFrac: Float,
    val yOffsetFrac: Float,
    val sizeFrac: Float,
    val rotation: Float,
    val colorIndex: Int
)

data class StarfishPlacement(
    val xFrac: Float,
    val sizeFrac: Float,
    val colorIndex: Int,
    val rotation: Float
)

data class SmallCoralPlacement(
    val xFrac: Float,
    val heightFrac: Float,
    val type: Int,
    val colorIndex: Int
)

data class PebblePlacement(
    val xFrac: Float,
    val yOffsetFrac: Float,
    val radiusFrac: Float,
    val colorIndex: Int,
    val alpha: Float
)

data class DistantSilhouette(
    val xFrac: Float,
    val widthFrac: Float,
    val heightFrac: Float,
    val style: Int
)

data class RockPlacement(
    val cx: Float,
    val wFrac: Float,
    val hFrac: Float,
    val style: Int,
    val layer: Int
)

data class TerrainDecorations(
    val seed: Long,
    val density: Float,
    val seaweedClusters: List<SeaweedPlacement>,
    val shells: List<ShellPlacement>,
    val starfish: List<StarfishPlacement>,
    val smallCorals: List<SmallCoralPlacement>,
    val pebbles: List<PebblePlacement>,
    val distantSilhouettes: List<DistantSilhouette>,
    val bgRocks: List<RockPlacement>,
    val midRocks: List<RockPlacement>,
    val fgRocks: List<RockPlacement>
)

// ── Color palettes ────────────────────────────────────────────────────────────

private val SeaweedColors = listOf(
    Color(0xFF2B7A3E),  // Sea green
    Color(0xFF3C9D5A),  // Medium sea green
    Color(0xFF556B2F)   // Dark olive green
)

private val ShellColors = listOf(
    Color(0xFFC8A872),  // Tan
    Color(0xFFF5DEB3),  // Wheat
    Color(0xFFD2B48C)   // Light tan
)

private val CoralColors = listOf(
    Color(0xFFB8663A),  // Coral brown
    Color(0xFFF08060),  // Light coral
    Color(0xFFC87050)   // Coral red
)

private val StarfishColors = listOf(
    Color(0xFFE8A040),  // Orange
    Color(0xFFFFC080),  // Light orange
    Color(0xFFD87030)   // Dark orange
)

// ── Generator ─────────────────────────────────────────────────────────────────

internal fun generateDecorations(config: AquariumTerrainConfig): TerrainDecorations {
    val rng = kotlin.random.Random(config.seed + 1L)
    val d = config.decorationDensity.coerceIn(0.5f, 2.0f)

    // Seaweed clusters: 6 base, distributed uniformly with jitter
    val seaweedClusters = (0 until (6 * d).toInt()).map {
        val baseX = it.toFloat() / (6 * d).toInt()
        SeaweedPlacement(
            xFrac = (baseX + rng.nextFloat() * 0.08f).coerceIn(0f, 1f),
            heightFrac = 0.08f + rng.nextFloat() * 0.10f,
            stalkCount = 1 + rng.nextInt(3),
            swayPhase = rng.nextFloat() * PI.toFloat() * 2f,
            colorIndex = rng.nextInt(SeaweedColors.size)
        )
    }

    // Shells: 8 base
    val shells = (0 until (8 * d).toInt()).map {
        ShellPlacement(
            xFrac = rng.nextFloat(),
            yOffsetFrac = rng.nextFloat() * 0.004f,
            sizeFrac = 0.008f + rng.nextFloat() * 0.010f,
            rotation = rng.nextFloat() * 360f,
            colorIndex = rng.nextInt(ShellColors.size)
        )
    }

    // Starfish: 2 base
    val starfish = (0 until (2 * d).toInt()).map {
        StarfishPlacement(
            xFrac = rng.nextFloat(),
            sizeFrac = 0.012f + rng.nextFloat() * 0.008f,
            colorIndex = rng.nextInt(StarfishColors.size),
            rotation = rng.nextFloat() * 360f
        )
    }

    // Small corals: 5 base (types: 0=branching, 1=dome, 2=tube)
    val smallCorals = (0 until (5 * d).toInt()).map {
        SmallCoralPlacement(
            xFrac = rng.nextFloat(),
            heightFrac = 0.05f + rng.nextFloat() * 0.08f,
            type = rng.nextInt(3),
            colorIndex = rng.nextInt(CoralColors.size)
        )
    }

    // Pebbles: 25 base
    val pebbles = (0 until (25 * d).toInt()).map {
        PebblePlacement(
            xFrac = rng.nextFloat(),
            yOffsetFrac = rng.nextFloat() * 0.006f,
            radiusFrac = 0.002f + rng.nextFloat() * 0.005f,
            colorIndex = rng.nextInt(3),
            alpha = 0.25f + rng.nextFloat() * 0.20f
        )
    }

    // Distant silhouettes: 7 base
    val distantSilhouettes = (0 until (7 * d).toInt()).map {
        DistantSilhouette(
            xFrac = rng.nextFloat(),
            widthFrac = 0.040f + rng.nextFloat() * 0.050f,
            heightFrac = 0.015f + rng.nextFloat() * 0.025f,
            style = rng.nextInt(3)
        )
    }

    // Rocks by layer
    val bgRocks = generateRocks(rng, "bg", (5 * d).toInt(), 0.035f to 0.050f, 0.008f to 0.013f)
    val midRocks = generateRocks(rng, "mid", (6 * d).toInt(), 0.070f to 0.120f, 0.030f to 0.055f)
    val fgRocks = generateRocks(rng, "fg", (6 * d).toInt(), 0.110f to 0.210f, 0.060f to 0.110f)

    return TerrainDecorations(
        seed = config.seed,
        density = config.decorationDensity,
        seaweedClusters = seaweedClusters,
        shells = shells,
        starfish = starfish,
        smallCorals = smallCorals,
        pebbles = pebbles,
        distantSilhouettes = distantSilhouettes,
        bgRocks = bgRocks,
        midRocks = midRocks,
        fgRocks = fgRocks
    )
}

private fun generateRocks(
    rng: kotlin.random.Random,
    layer: String,
    count: Int,
    wRange: Pair<Float, Float>,
    hRange: Pair<Float, Float>
): List<RockPlacement> {
    return (0 until count).map {
        RockPlacement(
            cx = rng.nextFloat(),
            wFrac = wRange.first + rng.nextFloat() * (wRange.second - wRange.first),
            hFrac = hRange.first + rng.nextFloat() * (hRange.second - hRange.first),
            style = rng.nextInt(4),
            layer = when (layer) {
                "bg" -> 0
                "mid" -> 1
                else -> 2
            }
        )
    }
}

// ── Drawing functions ─────────────────────────────────────────────────────────

/**
 * Draws distant rocky silhouettes in the far background — creates depth layers.
 */
internal fun DrawScope.drawDistantSilhouettes(decorations: TerrainDecorations) {
    val w = size.width
    val h = size.height

    decorations.distantSilhouettes.forEach { s ->
        val cx = s.xFrac * w
        val topY = AquariumTerrain.terrainY(s.xFrac, h) + h * 0.005f
        val rw = s.widthFrac * w
        val rh = s.heightFrac * h

        drawRockShape(cx, topY + rh, rw, rh, s.style, Color(0xFF1A2A3A).copy(alpha = 0.55f))
    }
}

/**
 * Draws animated seaweed clusters with sinuous sway motion.
 */
internal fun DrawScope.drawSeaweedLayer(
    decorations: TerrainDecorations,
    swayAngle: Float,
    elapsedMs: Long
) {
    val w = size.width
    val h = size.height
    val t = (elapsedMs / 1000f) % 1f

    decorations.seaweedClusters.forEach { cluster ->
        val baseX = cluster.xFrac * w
        val baseY = AquariumTerrain.terrainY(cluster.xFrac, h)
        val maxHeight = cluster.heightFrac * h

        for (stalk in 0 until cluster.stalkCount) {
            val offsetX = (stalk - (cluster.stalkCount - 1) / 2f) * 6f
            val color = SeaweedColors[cluster.colorIndex]
            drawSeaweedStalk(
                baseX + offsetX, baseY, maxHeight, color,
                swayAngle, t, cluster.swayPhase
            )
        }
    }
}

private fun DrawScope.drawSeaweedStalk(
    baseX: Float,
    baseY: Float,
    maxHeight: Float,
    color: Color,
    swayAngle: Float,
    t: Float,
    phase: Float
) {
    val segmentCount = 5
    val strokeWidth = 2.dp.toPx()

    val path = Path()
    var currentX = baseX
    var currentY = baseY

    path.moveTo(currentX, currentY)

    for (seg in 1..segmentCount) {
        val progress = seg.toFloat() / segmentCount
        val nextY = baseY - maxHeight * progress

        // Horizontal sway motion
        val swayAmount = swayAngle * maxHeight * 0.12f * progress +
                         sin(t * 1.8f + phase) * 0.04f * progress
        val nextX = baseX + swayAmount

        // Curved segment
        val midX = (currentX + nextX) / 2f
        val midY = (currentY + nextY) / 2f

        path.quadraticTo(midX, midY, nextX, nextY)
        currentX = nextX
        currentY = nextY

        // Draw fronds (leaves) on upper segments
        if (seg > 3) {
            val frondLength = maxHeight * 0.06f * progress
            val angleOff = if (seg % 2 == 0) -0.4f else 0.4f
            val frondX = nextX + cos(phase + angleOff) * frondLength
            val frondY = nextY + sin(phase + angleOff) * frondLength
            path.lineTo(frondX, frondY)
            path.moveTo(nextX, nextY)
        }
    }

    drawPath(
        path,
        color = color.copy(alpha = 0.85f),
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round
        )
    )
}

/**
 * Draws pebbles, shells, starfish, and small corals on the sea floor.
 */
internal fun DrawScope.drawFloorDecorations(decorations: TerrainDecorations) {
    val w = size.width
    val h = size.height

    // Pebbles
    decorations.pebbles.forEach { p ->
        val px = p.xFrac * w
        val py = AquariumTerrain.terrainY(p.xFrac, h) + p.yOffsetFrac * h
        val radius = p.radiusFrac * h
        val pebbleColor = when (p.colorIndex) {
            0 -> SandDark.copy(alpha = p.alpha)
            1 -> RockLight.copy(alpha = p.alpha)
            else -> SandMid.copy(alpha = p.alpha + 0.1f)
        }
        drawCircle(color = pebbleColor, radius = radius, center = Offset(px, py))
    }

    // Shells
    decorations.shells.forEach { shell ->
        val px = shell.xFrac * w
        val py = AquariumTerrain.terrainY(shell.xFrac, h) + shell.yOffsetFrac * h
        val size = shell.sizeFrac * h
        val color = ShellColors[shell.colorIndex]

        val shellPath = Path().apply {
            // Nautilo-inspired shell shape (simplified cone)
            moveTo(px, py)
            cubicTo(
                px - size * 0.4f, py - size * 0.3f,
                px - size * 0.2f, py - size * 0.6f,
                px + size * 0.1f, py - size * 0.8f
            )
            cubicTo(
                px + size * 0.3f, py - size * 0.6f,
                px + size * 0.4f, py - size * 0.3f,
                px, py
            )
            close()
        }

        drawPath(shellPath, color.copy(alpha = 0.70f))
        drawPath(
            shellPath,
            color = color.copy(alpha = 0.40f),
            style = Stroke(width = 0.5f.dp.toPx(), cap = StrokeCap.Round)
        )
    }

    // Starfish
    decorations.starfish.forEach { sf ->
        val px = sf.xFrac * w
        val py = AquariumTerrain.terrainY(sf.xFrac, h)
        val size = sf.sizeFrac * h
        val color = StarfishColors[sf.colorIndex]
        val angle = sf.rotation * PI.toFloat() / 180f

        // 5-pointed star
        val path = Path()
        for (i in 0..9) {
            val pointAngle = angle + i * (PI.toFloat() / 5f)
            val radius = if (i % 2 == 0) size else size * 0.5f
            val x = px + cos(pointAngle) * radius
            val y = py + sin(pointAngle) * radius

            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()

        drawPath(path, color.copy(alpha = 0.75f))
    }

    // Small corals
    decorations.smallCorals.forEach { coral ->
        val px = coral.xFrac * w
        val baseY = AquariumTerrain.terrainY(coral.xFrac, h)
        val height = coral.heightFrac * h
        val color = CoralColors[coral.colorIndex]

        when (coral.type) {
            0 -> {
                // Branching coral: trunk + 2 branches
                drawLine(
                    color = color,
                    start = Offset(px, baseY),
                    end = Offset(px, baseY - height),
                    strokeWidth = 1.5f.dp.toPx()
                )
                drawLine(
                    color = color.copy(alpha = 0.70f),
                    start = Offset(px, baseY - height * 0.6f),
                    end = Offset(px - height * 0.4f, baseY - height * 0.9f),
                    strokeWidth = 1.0f.dp.toPx()
                )
                drawLine(
                    color = color.copy(alpha = 0.70f),
                    start = Offset(px, baseY - height * 0.6f),
                    end = Offset(px + height * 0.4f, baseY - height * 0.9f),
                    strokeWidth = 1.0f.dp.toPx()
                )
            }
            1 -> {
                // Dome coral: 2 overlapping circles
                drawCircle(
                    color = color.copy(alpha = 0.75f),
                    radius = height * 0.5f,
                    center = Offset(px, baseY - height * 0.3f)
                )
                drawCircle(
                    color = color.copy(alpha = 0.65f),
                    radius = height * 0.35f,
                    center = Offset(px, baseY - height * 0.55f)
                )
            }
            else -> {
                // Tube coral: narrow cylinder
                drawPath(
                    Path().apply {
                        moveTo(px - height * 0.15f, baseY)
                        lineTo(px - height * 0.10f, baseY - height)
                        lineTo(px + height * 0.10f, baseY - height)
                        lineTo(px + height * 0.15f, baseY)
                        close()
                    },
                    color.copy(alpha = 0.70f)
                )
            }
        }
    }
}
