package com.wattagam.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.dp
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
 * 모서리는 크기에 따라: 작은 표시 8, 버튼·입력 12~14, 카드 16. 칩·토글처럼 정말 알약 모양인 것만 [Pill].
 * 안쪽 모서리는 바깥 모서리에서 사이 여백을 뺀 값으로 맞춘다.
 */
object WattagamShapes {
    val Small = RoundedCornerShape(8.dp)
    val Field = RoundedCornerShape(12.dp)
    val Button = RoundedCornerShape(14.dp)
    val Card = RoundedCornerShape(16.dp)
    val Pill = RoundedCornerShape(percent = 50)
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
)

private fun textStyle(size: Int, height: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = NotoSansKr,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = height.sp,
)

private val WattagamTypography = Typography(
    // 위계는 크기와 색으로. 가장 큰 제목도 600 까지만
    headlineLarge = textStyle(30, 42, FontWeight.SemiBold),
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
        shapes = Shapes(medium = WattagamShapes.Field, large = WattagamShapes.Card),
        content = content,
    )
}
