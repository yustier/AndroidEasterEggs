/*
 * Copyright (C) 2023 The Android Open Source Project
 * Desktop adaptation
 */

package com.android_baklava.desktop.landroid

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.lang.Float.max
import java.lang.Float.min
import java.util.Calendar
import java.util.GregorianCalendar
import kotlin.math.absoluteValue
import kotlin.math.floor
import kotlin.math.sqrt
import kotlin.random.Random

enum class RandomSeedType {
    Fixed,
    Daily,
    Evergreen,
}

const val TEST_UNIVERSE = false

val RANDOM_SEED_TYPE = RandomSeedType.Daily

const val FIXED_RANDOM_SEED = 5038L
const val DEFAULT_CAMERA_ZOOM = 1f
const val MIN_CAMERA_ZOOM = 250f / UNIVERSE_RANGE // 0.0025f
const val MAX_CAMERA_ZOOM = 5f
var TOUCH_CAMERA_PAN = false
var TOUCH_CAMERA_ZOOM = false
var DYNAMIC_ZOOM = false

fun dailySeed(): Long {
    val today = GregorianCalendar()
    return today.get(Calendar.YEAR) * 10_000L +
        today.get(Calendar.MONTH) * 100L +
        today.get(Calendar.DAY_OF_MONTH)
}

fun randomSeed(): Long {
    return when (RANDOM_SEED_TYPE) {
        RandomSeedType.Fixed -> FIXED_RANDOM_SEED
        RandomSeedType.Daily -> dailySeed()
        else -> Random.Default.nextLong().mod(10_000_000).toLong()
    }.absoluteValue
}

fun getDessertCode(): String = "BAK"  // Baklava

fun getSystemDesignation(universe: Universe): String {
    return "${getDessertCode()}-${universe.randomSeed % 100_000}"
}

val DEBUG_TEXT = mutableStateOf("Hello Universe")
const val SHOW_DEBUG_TEXT = false

@Composable
fun DebugText(text: MutableState<String>) {
    if (SHOW_DEBUG_TEXT) {
        Text(
            modifier = Modifier.fillMaxWidth().border(0.5.dp, color = Color.Yellow).padding(2.dp),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            fontSize = 9.sp,
            color = Color.Yellow,
            text = text.value,
        )
    }
}

@Composable
fun Telemetry(universe: Universe, showAutoButton: Boolean = true) {
    var topVisible by remember { mutableStateOf(false) }
    var bottomVisible by remember { mutableStateOf(false) }

    var catalogFontSize by remember { mutableStateOf(9.sp) }

    val textStyle =
        TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            letterSpacing = 1.sp,
            lineHeight = 12.sp,
        )

    LaunchedEffect("blah") {
        delay(1000)
        bottomVisible = true
        topVisible = true
    }

    val explored = universe.planets.filter { it.explored }

    val recomposeScope = currentRecomposeScope
    Telescope(universe) { recomposeScope.invalidate() }

    BoxWithConstraints(
        modifier =
            Modifier.fillMaxSize().padding(6.dp)
    ) {
        val wide = maxWidth > maxHeight
        Column(
            modifier =
                Modifier.align(if (wide) Alignment.BottomEnd else Alignment.BottomStart)
                    .fillMaxWidth(if (wide) 0.45f else 1.0f)
        ) {
            val autopilotEnabled = universe.ship.autopilot?.enabled == true
            if (autopilotEnabled) {
                universe.ship.autopilot?.let { autopilot ->
                    AnimatedVisibility(
                        modifier = Modifier,
                        visible = bottomVisible,
                    ) {
                        Text(
                            style = textStyle,
                            color = Colors.Autopilot,
                            modifier = Modifier.align(Alignment.Start),
                            text = autopilot.telemetry,
                        )
                    }
                }
            }

            Row(modifier = Modifier.padding(top = 6.dp)) {
                AnimatedVisibility(
                    modifier = Modifier.weight(1f),
                    visible = bottomVisible,
                ) {
                    Text(
                        style = textStyle,
                        color = Colors.Console,
                        text =
                            with(universe.ship) {
                                val closest = universe.closestPlanet()
                                val distToClosest =
                                    ((closest.pos - pos).mag() - closest.radius).toInt()
                                listOfNotNull(
                                        landing?.let {
                                            "LND: ${it.planet.name.uppercase()}\n" +
                                                "JOB: ${it.text.uppercase()}"
                                        }
                                            ?: if (distToClosest < 10_000) {
                                                "ALT: $distToClosest"
                                            } else null,
                                        "THR: %.0f%%".format(thrust.mag() * 100f),
                                        "POS: %s".format(pos.str("%+7.0f")),
                                        "VEL: %.0f".format(velocity.mag()),
                                    )
                                    .joinToString("\n")
                            },
                    )
                }

                if (showAutoButton) {
                    AnimatedVisibility(
                        visible = bottomVisible,
                    ) {
                        ConsoleButton(
                            textStyle = textStyle,
                            color = Colors.Console,
                            bgColor = if (autopilotEnabled) Colors.Autopilot else Color.Transparent,
                            borderColor = Colors.Console,
                            text = "AUTO",
                        ) {
                            universe.ship.autopilot?.let {
                                it.enabled = !it.enabled
                                DYNAMIC_ZOOM = it.enabled
                                if (!it.enabled) universe.ship.thrust = Vec2.Zero
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            modifier = Modifier.align(Alignment.TopStart),
            visible = topVisible,
        ) {
            Text(
                style = textStyle,
                fontSize = catalogFontSize,
                lineHeight = catalogFontSize,
                letterSpacing = 1.sp,
                color = Colors.Console,
                onTextLayout = { textLayoutResult ->
                    if (textLayoutResult.didOverflowHeight) {
                        catalogFontSize = 8.sp
                    }
                },
                text =
                    (with(universe.star) {
                            listOf(
                                "  STAR: $name (${getSystemDesignation(universe)})",
                                " CLASS: ${cls.name}",
                                "RADIUS: ${radius.toInt()}",
                                "  MASS: %.3g".format(mass),
                                "BODIES: ${explored.size} / ${universe.planets.size}",
                                "",
                            )
                        } +
                            explored
                                .map {
                                    listOf(
                                        "  BODY: ${it.name}",
                                        "  TYPE: ${it.description.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase() else c.toString() }}",
                                        "  ATMO: ${it.atmosphere.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase() else c.toString() }}",
                                        " FAUNA: ${it.fauna.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase() else c.toString() }}",
                                        " FLORA: ${it.flora.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase() else c.toString() }}",
                                        "",
                                    )
                                }
                                .flatten())
                        .joinToString("\n"),
            )
        }
    }
}

@Composable
fun FlightStick(
    modifier: Modifier,
    minRadius: Float = 0f,
    maxRadius: Float = 1000f,
    color: Color = Color.Green,
    onStickChanged: (vector: Vec2) -> Unit,
) {
    val origin = remember { mutableStateOf(Vec2.Zero) }
    val target = remember { mutableStateOf(Vec2.Zero) }

    Box(
        modifier =
            modifier
                .pointerInput(Unit) {
                    forEachGesture {
                        awaitPointerEventScope {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            origin.value = down.position
                            target.value = down.position

                            do {
                                val event: PointerEvent = awaitPointerEvent()
                                target.value = event.changes[0].position

                                onStickChanged(target.value - origin.value)
                            } while (
                                !event.changes.any { it.isConsumed } &&
                                    event.changes.count { it.pressed } == 1
                            )

                            target.value = Vec2.Zero
                            origin.value = Vec2.Zero

                            onStickChanged(Vec2.Zero)
                        }
                    }
                }
                .drawBehind {
                    if (origin.value != Vec2.Zero) {
                        val delta = target.value - origin.value
                        val mag = min(maxRadius, delta.mag())
                        val r = max(minRadius, mag)
                        val a = delta.angle()
                        drawCircle(
                            color = color,
                            center = origin.value,
                            radius = r,
                            style =
                                Stroke(
                                    width = 2f,
                                    pathEffect =
                                        if (mag < minRadius)
                                            PathEffect.dashPathEffect(
                                                floatArrayOf(this.density * 1f, this.density * 2f)
                                            )
                                        else null,
                                ),
                        )
                        drawLine(
                            color = color,
                            start = origin.value,
                            end = origin.value + Vec2.makeWithAngleMag(a, mag),
                            strokeWidth = 2f,
                        )
                    }
                }
    )
}

@Composable
fun Spaaaace(
    modifier: Modifier,
    u: Universe,
    foldState: MutableState<Any?> = mutableStateOf(null),
) {
    LaunchedEffect(u) {
        while (true) withInfiniteAnimationFrameNanos { frameTimeNanos -> u.step(frameTimeNanos) }
    }

    var cameraZoom by remember { mutableFloatStateOf(DEFAULT_CAMERA_ZOOM) }
    var cameraOffset by remember { mutableStateOf(Offset.Zero) }

    val transformableState =
        rememberTransformableState { zoomChange, offsetChange, rotationChange ->
            if (TOUCH_CAMERA_PAN) cameraOffset += offsetChange / cameraZoom
            if (TOUCH_CAMERA_ZOOM)
                cameraZoom = clamp(cameraZoom * zoomChange, MIN_CAMERA_ZOOM, MAX_CAMERA_ZOOM)
        }

    var canvasModifier = modifier

    if (TOUCH_CAMERA_PAN || TOUCH_CAMERA_ZOOM) {
        canvasModifier = canvasModifier.transformable(transformableState)
    }

    val centerFracX: Float by animateFloatAsState(0.5f, label = "centerX")
    val centerFracY: Float by animateFloatAsState(0.5f, label = "centerY")

    UniverseCanvas(u, canvasModifier) { u ->
        drawRect(Colors.Eigengrau, Offset.Zero, size)

        val closest = u.closestPlanet()
        val distToNearestSurf = max(0f, (u.ship.pos - closest.pos).mag() - closest.radius * 1.2f)
        val targetZoom =
            if (DYNAMIC_ZOOM) {
                clamp(500f / distToNearestSurf, MIN_CAMERA_ZOOM, MAX_CAMERA_ZOOM)
            } else {
                DEFAULT_CAMERA_ZOOM
            }
        if (!TOUCH_CAMERA_ZOOM) {
            cameraZoom = expSmooth(cameraZoom, targetZoom, dt = u.dt, speed = 1.5f)
        }

        if (!TOUCH_CAMERA_PAN) cameraOffset = (u.follow?.pos ?: Vec2.Zero) * -1f

        val visibleSpaceSizeMeters = size / cameraZoom
        val visibleSpaceRectMeters =
            Rect(
                -cameraOffset -
                    Offset(
                        visibleSpaceSizeMeters.width * centerFracX,
                        visibleSpaceSizeMeters.height * centerFracY,
                    ),
                visibleSpaceSizeMeters,
            )

        var gridStep = 1000f
        while (gridStep * cameraZoom < 32.dp.toPx()) gridStep *= 10

        DEBUG_TEXT.value =
            ("SIMULATION //\n" +
                "entities: ${u.entities.size} // " +
                "zoom: ${"%.4f".format(cameraZoom)}x // " +
                "fps: ${"%3.0f".format(1f / u.dt)} " +
                "dt: ${u.dt}\n" +
                ((u.follow as? Spacecraft)?.let {
                    "ship: p=%s v=%7.2f a=%6.3f t=%s\n"
                        .format(
                            it.pos.str("%+7.1f"),
                            it.velocity.mag(),
                            it.angle,
                            it.thrust.str("%+5.2f"),
                        )
                } ?: "") +
                "star: '${u.star.name}' designation=${getSystemDesignation(u)} " +
                "class=${u.star.cls.name} r=${u.star.radius.toInt()} m=${u.star.mass}\n" +
                "planets: ${u.planets.size}\n" +
                u.planets.joinToString("\n") {
                    val range = (u.ship.pos - it.pos).mag()
                    val vorbit = sqrt(GRAVITATION * it.mass / range)
                    val vescape = sqrt(2 * GRAVITATION * it.mass / it.radius)
                    " * ${it.name}:\n" +
                        if (it.explored) {
                            "   TYPE:  ${it.description.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase() else c.toString() }}\n" +
                                "   ATMO:  ${it.atmosphere.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase() else c.toString() }}\n" +
                                "   FAUNA: ${it.fauna.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase() else c.toString() }}\n" +
                                "   FLORA: ${it.flora.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase() else c.toString() }}\n"
                        } else {
                            "   (Unexplored)\n"
                        } +
                        "   orbit=${(it.pos - it.orbitCenter).mag().toInt()}" +
                        " radius=${it.radius.toInt()}" +
                        " mass=${"%g".format(it.mass)}" +
                        " vel=${(it.speed).toInt()}" +
                        " // range=${"%.0f".format(range)}" +
                        " vorbit=${vorbit.toInt()} vescape=${vescape.toInt()}"
                })

        zoom(cameraZoom) {
            translate(
                -visibleSpaceRectMeters.center.x + size.width * 0.5f,
                -visibleSpaceRectMeters.center.y + size.height * 0.5f,
            ) {
                var x = floor(visibleSpaceRectMeters.left / gridStep) * gridStep
                while (x < visibleSpaceRectMeters.right) {
                    drawLine(
                        color = Colors.Eigengrau2,
                        start = Offset(x, visibleSpaceRectMeters.top),
                        end = Offset(x, visibleSpaceRectMeters.bottom),
                        strokeWidth = (if ((x % (gridStep * 10) == 0f)) 3f else 1.5f) / cameraZoom,
                    )
                    x += gridStep
                }

                var y = floor(visibleSpaceRectMeters.top / gridStep) * gridStep
                while (y < visibleSpaceRectMeters.bottom) {
                    drawLine(
                        color = Colors.Eigengrau2,
                        start = Offset(visibleSpaceRectMeters.left, y),
                        end = Offset(visibleSpaceRectMeters.right, y),
                        strokeWidth = (if ((y % (gridStep * 10) == 0f)) 3f else 1.5f) / cameraZoom,
                    )
                    y += gridStep
                }

                this@zoom.drawUniverse(u)
            }
        }
    }
}
