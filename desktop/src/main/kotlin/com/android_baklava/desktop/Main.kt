/*
 * (c) MMXXV Airoku / Claude Sonnet 4.5 All rights reserved.
 */

package com.android_baklava.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.android_baklava.desktop.landroid.*
import java.awt.Cursor
import java.awt.Point
import java.awt.Toolkit
import java.awt.image.BufferedImage
import javax.swing.JOptionPane
import kotlin.math.sqrt

// Launch mode configuration
enum class LaunchMode {
    NORMAL,           // Normal game mode
    SCREENSAVER,      // Screensaver mode (/s)
    CONFIG            // Configuration dialog (/c)
}

data class AppConfig(
    val mode: LaunchMode = LaunchMode.NORMAL,
    val startFullscreen: Boolean = false,
    val forceAutopilot: Boolean = false,
    val exitOnAnyInput: Boolean = false
)

// Create an invisible cursor for screensaver mode
fun createInvisibleCursor(): Cursor {
    val cursorImg = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
    return Toolkit.getDefaultToolkit().createCustomCursor(
        cursorImg,
        Point(0, 0),
        "invisible cursor"
    )
}

fun parseLaunchArgs(args: Array<String>): AppConfig? {
    if (args.isEmpty()) {
        return AppConfig(mode = LaunchMode.NORMAL)
    }
    
    val firstArg = args[0].lowercase()
    
    return when {
        firstArg == "/s" -> {
            // Screensaver mode
            AppConfig(
                mode = LaunchMode.SCREENSAVER,
                startFullscreen = true,
                forceAutopilot = true,
                exitOnAnyInput = true
            )
        }
        firstArg == "/c" -> {
            // Configuration dialog
            AppConfig(mode = LaunchMode.CONFIG)
        }
        firstArg.startsWith("/p") -> {
            // Preview mode - not implemented, just exit
            println("Preview mode (/p) is not supported.")
            null
        }
        else -> {
            // Unknown argument, default to normal mode
            AppConfig(mode = LaunchMode.NORMAL)
        }
    }
}

fun showConfigDialog() {
    val message = """
Landroid - Android 16 Baklava Easter Egg
Space exploration screensaver and game

━【起動方法】━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

通常モード:
Landroid.exe

スクリーンセーバーモード:
Landroid.exe /s

設定ダイアログ（このウィンドウ）:
Landroid.exe /c

━【操作方法（通常モード）】━━━━━━━━━━━━━━━━━━━━━━━━

F11キー … フルスクリーン切り替え
ESCキー … 終了
マウス/タッチ … 宇宙船を操作
AUTOボタン … オートパイロット切り替え

━【スクリーンセーバーモード】━━━━━━━━━━━━━━━━━━━━━━━

• フルスクリーンで起動
• オートパイロット有効
• マウスポインタを非表示
• マウス移動/キー入力で終了

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

このスクリーンセーバーに設定可能なオプションはありません。
""".trimIndent()
    
    JOptionPane.showMessageDialog(
        null,
        message,
        "Landroid - 設定と使い方",
        JOptionPane.INFORMATION_MESSAGE
    )
}

fun main(args: Array<String>) {
    val config = parseLaunchArgs(args) ?: return // Exit if null (help or preview mode)
    
    if (config.mode == LaunchMode.CONFIG) {
        showConfigDialog()
        return
    }
    
    application {
        var isFullscreen by remember { mutableStateOf(config.startFullscreen) }
        
        // Remember window state before going fullscreen
        val savedWindowSize = remember { mutableStateOf(DpSize(1200.dp, 800.dp)) }
        val savedWindowPosition = remember { mutableStateOf<WindowPosition>(WindowPosition.PlatformDefault) }
        
        val windowState = rememberWindowState(
            placement = if (isFullscreen) WindowPlacement.Fullscreen else WindowPlacement.Floating,
            width = savedWindowSize.value.width,
            height = savedWindowSize.value.height,
            position = savedWindowPosition.value
        )
        
        // Track initial mouse position for screensaver mode
        val initialMousePos = remember { mutableStateOf<Offset?>(null) }
        val isStabilized = remember { mutableStateOf(false) }
        
        // Use LaunchedEffect to set stabilization flag after delay
        if (config.exitOnAnyInput && !isStabilized.value) {
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(1500) // Wait 1.5 seconds
                isStabilized.value = true
            }
        }
        
        // Create Universe once and preserve it across fullscreen toggles
        val universe = remember { 
            Universe(namer = Namer(), randomSeed = randomSeed()).apply {
                if (TEST_UNIVERSE) {
                    initTest()
                } else {
                    initRandom()
                }
                
                // set up the autopilot
                val autopilot = Autopilot(ship, this)
                ship.autopilot = autopilot
                add(autopilot)
                autopilot.enabled = config.forceAutopilot
            }
        }
        
        // In screensaver mode, create black windows for secondary monitors
        if (config.mode == LaunchMode.SCREENSAVER) {
            val graphicsEnvironment = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
            val screens = graphicsEnvironment.screenDevices
            val invisibleCursor = remember { createInvisibleCursor() }
            
            // For each secondary screen, create a black window
            screens.forEachIndexed { index, screen ->
                if (index > 0) { // Skip the primary screen
                    val bounds = screen.defaultConfiguration.bounds
                    Window(
                        onCloseRequest = ::exitApplication,
                        title = "",
                        state = rememberWindowState(
                            placement = WindowPlacement.Floating,
                            position = WindowPosition(bounds.x.dp, bounds.y.dp),
                            width = bounds.width.dp,
                            height = bounds.height.dp
                        ),
                        undecorated = true,
                        alwaysOnTop = true,
                        onKeyEvent = { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyDown) {
                                exitApplication()
                                true
                            } else false
                        }
                    ) {
                        // Set invisible cursor on the secondary window
                        DisposableEffect(Unit) {
                            window.cursor = invisibleCursor
                            onDispose { }
                        }
                        
                        Box(
                            modifier = Modifier.fillMaxSize()
                                .background(androidx.compose.ui.graphics.Color.Black)
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            awaitPointerEvent()
                                            exitApplication()
                                        }
                                    }
                                }
                        )
                    }
                }
            }
        }
        
        // Use key to force Window recreation when fullscreen state changes
        key(isFullscreen) {
            Window(
                onCloseRequest = ::exitApplication,
                title = "Landroid - Android 16 Space Explorer",
                state = windowState,
                undecorated = isFullscreen,
                alwaysOnTop = config.mode == LaunchMode.SCREENSAVER,
                onKeyEvent = { keyEvent ->
                    when {
                        // F11 to toggle fullscreen (not in screensaver mode)
                        keyEvent.key == Key.F11 && keyEvent.type == KeyEventType.KeyDown && config.mode != LaunchMode.SCREENSAVER -> {
                            if (isFullscreen) {
                                // Exiting fullscreen: restore saved state
                                isFullscreen = false
                                windowState.placement = WindowPlacement.Floating
                                windowState.size = savedWindowSize.value
                                windowState.position = savedWindowPosition.value
                            } else {
                                // Entering fullscreen: save current state
                                savedWindowSize.value = windowState.size
                                savedWindowPosition.value = windowState.position
                                isFullscreen = true
                                windowState.placement = WindowPlacement.Fullscreen
                            }
                            true
                        }
                        // ESC to exit
                        keyEvent.key == Key.Escape && keyEvent.type == KeyEventType.KeyDown -> {
                            exitApplication()
                            true
                        }
                        // Any key exits in screensaver mode
                        config.exitOnAnyInput && keyEvent.type == KeyEventType.KeyDown -> {
                            exitApplication()
                            true
                        }
                        else -> false
                    }
                }
            ) {
            // Set invisible cursor for screensaver mode
            if (config.mode == LaunchMode.SCREENSAVER) {
                DisposableEffect(Unit) {
                    val invisibleCursor = createInvisibleCursor()
                    window.cursor = invisibleCursor
                    onDispose { }
                }
            }
            
            LandroidApp(
                universe = universe,
                config = config,
                onMouseEvent = { mousePos ->
                    if (config.exitOnAnyInput) {
                        // Ignore mouse events until stabilization period is over
                        if (!isStabilized.value) {
                            // Keep updating the baseline position during stabilization
                            initialMousePos.value = mousePos
                            return@LandroidApp
                        }
                        
                        // After stabilization period, check for mouse movement
                        val initial = initialMousePos.value
                        if (initial != null) {
                            val dx = mousePos.x - initial.x
                            val dy = mousePos.y - initial.y
                            val distance = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                            // Exit if mouse moved significantly (more than 15 pixels)
                            if (distance > 15f) {
                                exitApplication()
                            }
                        }
                    }
                }
            )
            }
        }
    }
}

@androidx.compose.runtime.Composable
fun LandroidApp(
    universe: Universe,
    config: AppConfig = AppConfig(),
    onMouseEvent: ((Offset) -> Unit)? = null
) {
    // Root box to capture all mouse events
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize()
            .let { mod ->
                if (config.exitOnAnyInput && onMouseEvent != null) {
                    mod.pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                event.changes.forEach { change ->
                                    // Detect any pointer event: move, press, release
                                    onMouseEvent(change.position)
                                    // Consume the event if it's a press or release
                                    if (change.pressed || change.previousPressed != change.pressed) {
                                        change.consume()
                                    }
                                }
                            }
                        }
                    }
                } else {
                    mod
                }
            }
    ) {
        Spaaaace(modifier = Modifier.fillMaxSize(), u = universe, foldState = mutableStateOf(null))
        DebugText(DEBUG_TEXT)
        
        val minRadius = 50.dp.toLocalPx()
        val maxRadius = 100.dp.toLocalPx()
        
        // Disable manual controls in screensaver mode
        if (!config.forceAutopilot) {
            FlightStick(
                modifier = Modifier.fillMaxSize(),
                minRadius = minRadius,
                maxRadius = maxRadius,
                color = androidx.compose.ui.graphics.Color.Green,
            ) { vec ->
                (universe.follow as? Spacecraft)?.let { ship ->
                    if (vec == Vec2.Zero) {
                        ship.thrust = Vec2.Zero
                    } else {
                        val a = vec.angle()
                        ship.angle = a
                        
                        val m = vec.mag()
                        if (m < minRadius) {
                            ship.thrust = Vec2.Zero
                        } else {
                            ship.thrust = Vec2.makeWithAngleMag(
                                a,
                                lexp(minRadius, maxRadius, m).coerceIn(0f, 1f),
                            )
                        }
                    }
                }
            }
        }
        
        // Show telemetry with AUTO button only in non-screensaver mode
        Telemetry(universe, showAutoButton = !config.forceAutopilot)
    }
}
