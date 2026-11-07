package com.dailysystems.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Line icons from the prototype, drawn on a 24×24 grid. Tinted at draw time by Icon's tint,
 * so the black here never shows.
 */
object AppIcons {
    private fun icon(name: String, stroke: Float, vararg paths: String, filled: List<String> = emptyList()) =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach {
                addPath(
                    pathData = PathParser().parsePathString(it).toNodes(),
                    stroke = SolidColor(Color.Black), strokeLineWidth = stroke,
                    strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
                )
            }
            filled.forEach { addPath(pathData = PathParser().parsePathString(it).toNodes(), fill = SolidColor(Color.Black)) }
        }.build()

    private fun circle(cx: Float, cy: Float, r: Float) =
        "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0z"

    val Today = icon(
        "today", 1.8f,
        "M6.5 5h11a3 3 0 0 1 3 3v9.5a3 3 0 0 1-3 3h-11a3 3 0 0 1-3-3V8a3 3 0 0 1 3-3z",
        "M3.5 10h17M8 3v4M16 3v4",
        filled = listOf(circle(12f, 15f, 1.6f)),
    )
    val Tasks = icon("tasks", 1.8f, "M4 6.5l1.6 1.6L8.5 5M4 12.5l1.6 1.6 2.9-3.1M4 18.5l1.6 1.6 2.9-3.1M11.5 7h9M11.5 13h9M11.5 19h9")
    val Systems = icon(
        "systems", 1.8f,
        circle(6f, 6f, 2.5f), circle(18f, 6f, 2.5f), circle(6f, 18f, 2.5f), circle(18f, 18f, 2.5f),
        "M8.5 6h7M6 8.5v7M18 8.5v7",
    )
    val Notes = icon("notes", 1.8f, "M6 3.5h8.5L19 8v12.5H6z", "M14 3.5V8h5M9 12.5h7M9 16h5")
    val Check = icon("check", 3f, "M5 12.5l4.5 4.5L19 7.5")
    val Close = icon("close", 2.2f, "M6 6l12 12M18 6L6 18")
    val Left = icon("left", 2f, "M15 5l-7 7 7 7")
    val Right = icon("right", 2f, "M9 5l7 7-7 7")
    val Up = icon("up", 2f, "M6 14l6-6 6 6")
    val Down = icon("down", 2f, "M6 10l6 6 6-6")
    val Tag = icon(
        "tag", 1.8f,
        "M3.5 12.2V4.5a1 1 0 0 1 1-1h7.7l8.3 8.3a1.5 1.5 0 0 1 0 2.1l-6.4 6.4a1.5 1.5 0 0 1-2.1 0z",
        circle(8f, 8f, 1.5f),
    )
    val Edit = icon("edit", 1.8f, "M4 20h4L19 9l-4-4L4 16z", "M13.5 6.5l4 4")
    val Note = icon("note", 2f, "M6 3.5h8.5L19 8v12.5H6z", "M9 12.5h7M9 16h5")
    val Plug = icon("plug", 2f, circle(6f, 12f, 2.5f), circle(18f, 12f, 2.5f), "M8.5 12h7")
    val Plus = icon("plus", 2.2f, "M12 5v14M5 12h14")
    val Calendar = icon(
        "calendar", 1.8f,
        "M6.5 5h11a3 3 0 0 1 3 3v9.5a3 3 0 0 1-3 3h-11a3 3 0 0 1-3-3V8a3 3 0 0 1 3-3z",
        "M3.5 10h17M8 3v4M16 3v4",
    )
    val Bell = icon("bell", 1.8f, "M6 16.5V11a6 6 0 0 1 12 0v5.5l1.5 1.5h-15z", "M10 20.5a2 2 0 0 0 4 0")
    val Repeat = icon("repeat", 2f, "M17 3.5l3 3-3 3", "M4 11.5v-1a4 4 0 0 1 4-4h12", "M7 20.5l-3-3 3-3", "M20 12.5v1a4 4 0 0 1-4 4H4")
    val Grip = icon(
        "grip", 1f,
        filled = listOf(circle(9f, 6f, 1.5f), circle(15f, 6f, 1.5f), circle(9f, 12f, 1.5f), circle(15f, 12f, 1.5f), circle(9f, 18f, 1.5f), circle(15f, 18f, 1.5f)),
    )
    val Search = icon("search", 2f, circle(11f, 11f, 6.5f), "M16 16l4.5 4.5")
}
