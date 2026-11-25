package com.wattagam.app.ui.theme

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * 물결 테두리 원 · Material 3 Expressive 의 쿠키 모양.
 * 로고, 지도 핀, 순서 표시, 도장처럼 작고 눈에 띄는 표시에만 쓴다. 글이 들어가는 카드에는 [LiquidShape].
 */
class CookieShape(
    private val lobes: Int = 9,
    private val depth: Float = 0.075f,
    private val rotationDegrees: Float = -90f,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(cookiePath(size, lobes, depth, rotationDegrees))
}

/** [CookieShape] 의 테두리 선. 도장처럼 선으로 그릴 때 쓴다 */
fun cookiePath(size: Size, lobes: Int = 9, depth: Float = 0.075f, rotationDegrees: Float = -90f): Path {
    val path = Path()
    val steps = lobes * 16
    val rotation = rotationDegrees * PI / 180.0
    for (i in 0..steps) {
        val t = 2.0 * PI * i / steps
        val k = 1f - depth * (1f - cos(lobes * t).toFloat()) / 2f
        val x = size.width / 2f * (1f + k * cos(t + rotation).toFloat())
        val y = size.height / 2f * (1f + k * sin(t + rotation).toFloat())
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

/**
 * 물처럼 이어지는 모서리 · 모서리 곡선이 변까지 길게 이어지는 연속 곡률 모양.
 * 두 대각선 모서리를 더 둥글게 두면(예: 40/24/40/24) 네모 대신 흐르는 느낌이 난다.
 */
class LiquidShape(
    private val topStart: Dp,
    private val topEnd: Dp = topStart,
    private val bottomEnd: Dp = topStart,
    private val bottomStart: Dp = topEnd,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val rtl = layoutDirection == LayoutDirection.Rtl
        val px = with(density) { listOf(topStart.toPx(), topEnd.toPx(), bottomEnd.toPx(), bottomStart.toPx()) }
        // 왼쪽 위, 오른쪽 위, 오른쪽 아래, 왼쪽 아래
        val r = if (rtl) listOf(px[1], px[0], px[3], px[2]) else px
        val half = min(size.width, size.height) / 2f
        val a = r.map { min(it * CORNER_REACH, half) }
        val w = size.width
        val h = size.height
        val c = CORNER_CONTROL
        val path = Path().apply {
            moveTo(a[0], 0f)
            lineTo(w - a[1], 0f)
            cubicTo(w - a[1] * c, 0f, w, a[1] * c, w, a[1])
            lineTo(w, h - a[2])
            cubicTo(w, h - a[2] * c, w - a[2] * c, h, w - a[2], h)
            lineTo(a[3], h)
            cubicTo(a[3] * c, h, 0f, h - a[3] * c, 0f, h - a[3])
            lineTo(0f, a[0])
            cubicTo(0f, a[0] * c, a[0] * c, 0f, a[0], 0f)
            close()
        }
        return Outline.Generic(path)
    }

    private companion object {
        // 곡선이 변으로 이어지는 길이(반지름 배수)와 조절점 위치. 미리 그려 보고 고른 값
        const val CORNER_REACH = 1.35f
        const val CORNER_CONTROL = 0.22f
    }
}

/** 흐르는 카드 기본값: 왼쪽 위·오른쪽 아래를 더 둥글게 */
fun liquidCard(large: Dp = 36.dp, small: Dp = 24.dp) = LiquidShape(large, small, large, small)
