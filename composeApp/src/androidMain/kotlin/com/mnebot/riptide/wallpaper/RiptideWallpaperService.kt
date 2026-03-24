package com.mnebot.riptide.wallpaper

import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.Choreographer
import android.view.SurfaceHolder
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.mnebot.riptide.presentation.aquarium.drawAquariumBackground
import com.mnebot.riptide.presentation.aquarium.drawAquariumCreatures
import com.mnebot.riptide.presentation.aquarium.getCurrentHourFraction
import com.mnebot.riptide.presentation.aquarium.interpolateSky
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos

private const val TAG = "RiptideWallpaper"
private const val DATA_REFRESH_INTERVAL_MS = 5 * 60 * 1000L // 5 minutes
private const val PI = 3.14159265

class RiptideWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = RiptideEngine()

    inner class RiptideEngine : Engine() {
        private val dataProvider by lazy { WallpaperDataProvider(applicationContext) }
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        private val canvasDrawScope = CanvasDrawScope()
        private lateinit var density: Density

        private var creatureData: WallpaperCreatureData? = null
        private var startNs = System.nanoTime()
        private var visible = false

        // Target: 30fps. We re-register at every vsync (60fps) but only draw
        // when ≥1 target interval has elapsed since the last rendered frame.
        // This keeps frames always vsync-aligned (smooth) while halving GPU work.
        private val targetIntervalNs = 33_333_333L  // 1/30s in nanoseconds
        private var lastDrawnFrameNs = 0L

        private val frameCallback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNs: Long) {
                if (!visible) return
                // Always re-register at the next vsync for smooth timing
                Choreographer.getInstance().postFrameCallback(this)
                // Only draw if enough time has passed since last draw
                if (frameTimeNs - lastDrawnFrameNs >= targetIntervalNs) {
                    drawFrame(frameTimeNs)
                    lastDrawnFrameNs = frameTimeNs
                }
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            val dm = resources.displayMetrics
            @Suppress("DEPRECATION")
            density = Density(dm.density, dm.scaledDensity)

            // Load creature data from DB
            scope.launch {
                try {
                    creatureData = dataProvider.loadCreatureData()
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading creature data", e)
                }
            }

            // Periodic refresh to pick up newly unlocked creatures
            scope.launch {
                while (true) {
                    delay(DATA_REFRESH_INTERVAL_MS)
                    try {
                        creatureData = dataProvider.loadCreatureData()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error refreshing creature data", e)
                    }
                }
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) {
                startNs = System.nanoTime()
                Choreographer.getInstance().postFrameCallback(frameCallback)
            } else {
                Choreographer.getInstance().removeFrameCallback(frameCallback)
            }
        }

        private fun drawFrame(frameTimeNs: Long) {
            val holder = surfaceHolder ?: return
            val canvas = try {
                holder.lockCanvas()
            } catch (e: Exception) {
                Log.e(TAG, "Error locking canvas", e)
                return
            } ?: return

            try {
                val elapsedMs = (frameTimeNs - startNs) / 1_000_000L
                val composeCanvas = Canvas(canvas)
                val size = Size(canvas.width.toFloat(), canvas.height.toFloat())

                canvasDrawScope.draw(density, LayoutDirection.Ltr, composeCanvas, size) {
                    // Background: sky, ocean, seabed, waves, bubbles
                    val hourFraction = getCurrentHourFraction()
                    val sky = interpolateSky(hourFraction)
                    val swayAngle = cos(elapsedMs * PI / 3000.0).toFloat()
                    val bubbleProgress = (elapsedMs % 8000L) / 8000f
                    drawAquariumBackground(swayAngle, bubbleProgress, sky)

                    // Creatures
                    val data = creatureData ?: return@draw
                    if (data.unlockedCreatures.isNotEmpty()) {
                        drawAquariumCreatures(
                            data.unlockedCreatures,
                            data.fixedCreatures,
                            data.creatureLevelBySpecies,
                            elapsedMs
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error drawing frame", e)
            } finally {
                try {
                    holder.unlockCanvasAndPost(canvas)
                } catch (e: Exception) {
                    Log.e(TAG, "Error unlocking canvas", e)
                }
            }
        }

        override fun onDestroy() {
            scope.cancel()
            Choreographer.getInstance().removeFrameCallback(frameCallback)
            super.onDestroy()
        }
    }
}
