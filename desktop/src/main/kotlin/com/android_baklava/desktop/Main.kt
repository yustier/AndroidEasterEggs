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

fun parseLaunchArgs(args: Array<String>): AppConfig? {
    if (args.isEmpty()) {
        return AppConfig(mode = LaunchMode.NORMAL)
    }
    
    val firstArg = args[0].lowercase()
    
    return when {
        firstArg == "--help" || firstArg == "-h" -> {
            showHelpMessage()
            null // Exit after showing help
        }
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

fun showHelpMessage() {
    val helpText = """
        Landroid - Android 16 Baklava Easter Egg
        Space exploration screensaver and game
        
        Usage:
          Landroid.exe          - Launch in normal game mode
          Landroid.exe /s       - Launch as screensaver (fullscreen)
          Landroid.exe /c       - Show configuration dialog
          Landroid.exe --help   - Show this help message
        
        Controls (Game Mode):
          F11                   - Toggle fullscreen
          ESC                   - Exit application
          Mouse/Touch           - Control spacecraft
          AUTO button           - Toggle autopilot
        
        Screensaver Mode:
          - Starts in fullscreen with autopilot enabled
          - Any mouse/keyboard input will exit
    """.trimIndent()
    
    println(helpText)
}

fun showConfigDialog() {
    JOptionPane.showMessageDialog(
        null,
        "Landroid Screensaver\n\n" +
        "This screensaver has no configurable options.\n\n" +
        "For command-line options, run:\n" +
        "  Landroid.exe --help",
        "Landroid Configuration",
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
            LandroidApp(
                universe = universe,
                config = config,
                onMouseEvent = { mousePos ->
                    if (config.exitOnAnyInput) {
                        // Initialize mouse position on first detection
                        if (initialMousePos.value == null) {
                            initialMousePos.value = mousePos
                        } else {
                            // Exit if mouse moved significantly (more than 5 pixels)
                            val initial = initialMousePos.value!!
                            val dx = mousePos.x - initial.x
                            val dy = mousePos.y - initial.y
                            val distance = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                            if (distance > 5f) {
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
                                event.changes.firstOrNull()?.let { change ->
                                    onMouseEvent(change.position)
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
