package com.wattagam.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.wattagam.app.R

object WattagamColors {
    val Primary = Color(0xFF245CF5)
    val Ink = Color(0xFF14213A)
    val Muted = Color(0xFF62728A)
    val Surface = Color(0xFFFFFFFF)
    val Soft = Color(0xFFF4F7FC)
    val PrimarySoft = Color(0xFFEAF0FF)
    val Border = Color(0xFFDCE4F1)
    val Danger = Color(0xFFB93838)
    val DangerSoft = Color(0xFFFFF0ED)

    /** 장소 도장 인주색. 도장 자국에만 쓰고 오류(Danger)와 섞어 쓰지 않는다 */
    val StampInk = Color(0xFFB8472F)
}

object WattagamSpacing {
    val Small = 8.dp
    val Medium = 12.dp
    val Content = 16.dp
    val Screen = 24.dp
}

/**
 * 리퀴드 모양 (Material 3 Expressive). 네모 대신 물처럼 흐르는 모서리:
 * 작은 표시 12, 입력 20, 버튼·칩은 알약 [Pill], 카드는 대각선 모서리가 더 둥근 [Card] (36/24).
 * 로고·지도 핀·순서·도장처럼 작게 눈에 띄는 표시는 [Cookie]. 안쪽 모서리는 바깥 모서리에서 사이 여백을 뺀 값.
 */
object WattagamShapes {
    val Small = RoundedCornerShape(12.dp)
    val Field = RoundedCornerShape(20.dp)
    val Button = RoundedCornerShape(percent = 50)
    val Card: Shape = liquidCard()
    val Pill = RoundedCornerShape(percent = 50)
    val Cookie: Shape = CookieShape()
}

object WattagamSizes {
    val TouchTarget = 48.dp
    val Button = 52.dp
    val TopBar = 60.dp
}

@OptIn(ExperimentalTextApi::class)
private val NotoSansKr = FontFamily(
    Font(R.font.noto_sans_kr, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.noto_sans_kr, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.noto_sans_kr, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.noto_sans_kr, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

// 피그마 글자 간격: 굵은 제목 -2%, 나머지 -0.5%
private fun textStyle(size: Int, height: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = NotoSansKr,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = height.sp,
    letterSpacing = if (weight == FontWeight.Bold) (-0.02).em else (-0.005).em,
)

private val WattagamTypography = Typography(
    // 피그마 공통 디자인: 화면 제목 30/Bold/42, 브랜드 24/Bold/32
    headlineLarge = textStyle(30, 42, FontWeight.Bold),
    headlineSmall = textStyle(24, 32, FontWeight.Bold),
    titleLarge = textStyle(18, 27, FontWeight.Medium),
    titleMedium = textStyle(16, 24, FontWeight.Medium),
    bodyLarge = textStyle(16, 24),
    bodyMedium = textStyle(14, 22),
    bodySmall = textStyle(12, 18),
    labelLarge = textStyle(15, 22, FontWeight.Medium),
    labelMedium = textStyle(14, 21, FontWeight.Medium),
    labelSmall = textStyle(12, 18),
)

@Composable
fun WattagamTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = WattagamColors.Primary,
            onPrimary = WattagamColors.Surface,
            primaryContainer = WattagamColors.PrimarySoft,
            onPrimaryContainer = WattagamColors.Primary,
            background = WattagamColors.Surface,
            onBackground = WattagamColors.Ink,
            surface = WattagamColors.Surface,
            onSurface = WattagamColors.Ink,
            surfaceVariant = WattagamColors.Soft,
            onSurfaceVariant = WattagamColors.Muted,
            outline = WattagamColors.Border,
            error = WattagamColors.Danger,
            errorContainer = WattagamColors.DangerSoft,
        ),
        typography = WattagamTypography,
        shapes = Shapes(small = WattagamShapes.Small, medium = WattagamShapes.Field, large = RoundedCornerShape(28.dp), extraLarge = RoundedCornerShape(36.dp)),
        content = content,
    )
}
