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
import com.mnebot.riptide.data.local.db.DatabaseProvider
import com.mnebot.riptide.data.repository.DaySummaryRepositoryImpl
import com.mnebot.riptide.data.repository.DayTaskRepositoryImpl
import com.mnebot.riptide.data.repository.UserPreferencesRepositoryImpl
import com.mnebot.riptide.domain.DecorationUnlockChecker
import com.mnebot.riptide.domain.EcosystemProcessor
import com.mnebot.riptide.data.repository.EcosystemStateRepositoryImpl
import com.mnebot.riptide.data.repository.MarineCreatureRepositoryImpl
import com.mnebot.riptide.presentation.aquarium.drawAquariumBackground
import com.mnebot.riptide.presentation.aquarium.drawAquariumCreatures
import com.mnebot.riptide.presentation.aquarium.getCurrentHourFraction
import com.mnebot.riptide.presentation.aquarium.interpolateSky
import com.mnebot.riptide.presentation.aquarium.weather.RandomWeatherProvider
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
        private val weatherProvider = RandomWeatherProvider()
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        // Repos + checker created lazily on first use (avoids DB open at service start)
        private val wallpaperPrefs by lazy { UserPreferencesRepositoryImpl(applicationContext) }
        private val decorationChecker by lazy {
            val db             = DatabaseProvider.getDatabase(applicationContext)
            val ecoStateRepo   = EcosystemStateRepositoryImpl(db.ecosystemStateDao())
            val creatureRepo   = MarineCreatureRepositoryImpl(db.marineCreatureDao())
            val daySummaryRepo = DaySummaryRepositoryImpl(db.daySummaryDao())
            val dayTaskRepo    = DayTaskRepositoryImpl(db.dayTaskDao())
            val ecoProcessor   = EcosystemProcessor(ecoStateRepo, creatureRepo)
            DecorationUnlockChecker(daySummaryRepo, dayTaskRepo, creatureRepo, ecoProcessor, wallpaperPrefs)
        }
        private val canvasDrawScope = CanvasDrawScope()
        private lateinit var density: Density

        @Volatile
        private var creatureData: WallpaperCreatureData? = null
        private var startNs = System.nanoTime()
        private var visible = false

        // FPS-based frame pacing: re-register at every vsync (60fps) but only draw
        // when ≥1 target interval has elapsed since the last rendered frame.
        // This keeps frames always vsync-aligned (smooth) while reducing GPU work.
        private var targetIntervalNs = 33_333_333L  // 1/30s default, updated from prefs
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
            scope.launch(Dispatchers.IO) {
                try {
                    creatureData = dataProvider.loadCreatureData()
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading creature data", e)
                }
            }

            // Observe FPS preference changes
            scope.launch {
                wallpaperPrefs.getWallpaperFps().collect { fps ->
                    targetIntervalNs = 1_000_000_000L / fps.toLong().coerceIn(15, 60)
                }
            }

            // Periodic refresh to pick up newly unlocked creatures
            scope.launch(Dispatchers.IO) {
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
                // Marcar wallpaper como activado y comprobar desbloqueo de SUNKEN_SHIP
                scope.launch {
                    try {
                        wallpaperPrefs.setWallpaperActivated()   // persiste el flag
                        decorationChecker.checkSunkenShip()      // lee el flag y desbloquea si aplica
                    } catch (e: Exception) {
                        Log.e(TAG, "Error checking decoration unlock", e)
                    }
                }
            } else {
                Choreographer.getInstance().removeFrameCallback(frameCallback)
            }
        }

        private fun drawFrame(frameTimeNs: Long) {
            val holder = surfaceHolder ?: return
            val canvas = try {
                if (android.os.Build.VERSION.SDK_INT >= 26) {
                    holder.lockHardwareCanvas()
                } else {
                    holder.lockCanvas()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error locking canvas", e)
                return
            } ?: return

            try {
                val elapsedMs = (frameTimeNs - startNs) / 1_000_000L
                val composeCanvas = Canvas(canvas)
                val size = Size(canvas.width.toFloat(), canvas.height.toFloat())

                canvasDrawScope.draw(density, LayoutDirection.Ltr, composeCanvas, size) {
                    // Background: sky, ocean, seabed, waves, bubbles, weather, lighting, particles
                    val hourFraction = getCurrentHourFraction()
                    val sky = interpolateSky(hourFraction)
                    val swayAngle = cos(elapsedMs * PI / 3000.0).toFloat()
                    val bubbleProgress = (elapsedMs % 8000L) / 8000f
                    val weather = weatherProvider.currentWeather(elapsedMs)
                    drawAquariumBackground(swayAngle, bubbleProgress, sky, weather, elapsedMs, hourFraction)

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
