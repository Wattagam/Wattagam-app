package com.wattagam.app.feature.write

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.wattagam.app.feature.guestbook.DrawingColor
import com.wattagam.app.feature.guestbook.DrawingTool
import com.wattagam.app.ui.theme.WattagamColors
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

/** 06-A 편집 영역의 가로:세로 비율 (사진 꾸미기 화면이 정한 값과 같게) */
internal const val EDITOR_ASPECT = 0.96f

/** 선 하나. 좌표는 편집 영역 기준 0~1 비율이라 화면 크기와 저장 해상도가 달라도 같은 위치에 그려진다 */
internal data class DrawnStroke(val points: List<Offset>, val color: DrawingColor)

/** 사진 위 글자. 위치는 왼쪽 아래 기준 0~1 비율 */
internal data class PlacedText(val text: String, val position: Offset)

private const val STROKE_WIDTH_RATIO = 0.012f
private const val ERASER_RADIUS_RATIO = 0.04f

internal fun DrawingColor.toColor(): Color = when (this) {
    DrawingColor.Blue -> WattagamColors.Primary
    DrawingColor.Black -> Color(0xFF20252D)
    DrawingColor.Red -> Color(0xFFEF7771)
    DrawingColor.Yellow -> Color(0xFFFFD76B)
    DrawingColor.Green -> Color(0xFF7BB99E)
}

/**
 * 사진 위에 손가락으로 그리는 영역. 펜은 선을 더하고, 지우개는 손가락이 지나간 선을 통째로 지운다.
 * 사진은 [EDITOR_ASPECT] 비율로 가운데를 잘라 보여주며, 저장할 때([composeDecoratedPhoto])도 같은 영역을 쓴다.
 */
@Composable
internal fun DrawingCanvas(
    photo: ImageBitmap?,
    strokes: SnapshotStateList<DrawnStroke>,
    texts: List<PlacedText>,
    tool: DrawingTool,
    color: DrawingColor,
    photoDescription: String,
    modifier: Modifier = Modifier,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    var current by remember { mutableStateOf<List<Offset>>(emptyList()) }

    fun normalized(offset: Offset) = Offset(
        (offset.x / max(size.width, 1)).coerceIn(0f, 1f),
        (offset.y / max(size.height, 1)).coerceIn(0f, 1f),
    )

    fun eraseAt(point: Offset) {
        val aspect = size.width.toFloat() / max(size.height, 1)
        strokes.removeAll { stroke ->
            stroke.points.any { hypot((it.x - point.x) * aspect, it.y - point.y) < ERASER_RADIUS_RATIO * aspect }
        }
    }

    Box(
        modifier
            .clipToBounds()
            .background(Color.Black)
            .onSizeChanged { size = it }
            .pointerInput(tool, color) {
                detectDragGestures(
                    onDragStart = { start ->
                        val point = normalized(start)
                        if (tool == DrawingTool.Eraser) eraseAt(point) else current = listOf(point)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val point = normalized(change.position)
                        if (tool == DrawingTool.Eraser) eraseAt(point) else current = current + point
                    },
                    onDragEnd = {
                        if (tool == DrawingTool.Pen && current.isNotEmpty()) strokes.add(DrawnStroke(current, color))
                        current = emptyList()
                    },
                    onDragCancel = { current = emptyList() },
                )
            },
    ) {
        if (photo != null) Image(photo, photoDescription, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Canvas(Modifier.fillMaxSize()) {
            val width = this.size.width * STROKE_WIDTH_RATIO
            fun draw(points: List<Offset>, strokeColor: Color) {
                if (points.isEmpty()) return
                val path = Path().apply {
                    moveTo(points.first().x * this@Canvas.size.width, points.first().y * this@Canvas.size.height)
                    points.drop(1).forEach { lineTo(it.x * this@Canvas.size.width, it.y * this@Canvas.size.height) }
                    if (points.size == 1) lineTo(points.first().x * this@Canvas.size.width + 0.1f, points.first().y * this@Canvas.size.height)
                }
                drawPath(path, strokeColor, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            strokes.forEach { draw(it.points, it.color.toColor()) }
            draw(current, color.toColor())
        }
        val density = LocalDensity.current
        texts.forEach { item ->
            Text(
                item.text,
                Modifier
                    .offset {
                        IntOffset(
                            (item.position.x * size.width).roundToInt(),
                            (item.position.y * size.height).roundToInt() - with(density) { 36.dp.roundToPx() },
                        )
                    }
                    .background(Color(0xCC161616), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

/** 잘라낼 영역 (px) */
internal data class CropBox(val left: Int, val top: Int, val width: Int, val height: Int)

/** [width]×[height] 사진에서 가로:세로 [aspect] 비율로 가장 크게, 가운데를 잘라낼 영역. 화면의 ContentScale.Crop 과 같은 영역이다 */
internal fun centerCrop(width: Int, height: Int, aspect: Float): CropBox {
    val (cropW, cropH) = if (width.toFloat() / height > aspect) {
        (height * aspect).toInt() to height
    } else {
        width to (width / aspect).toInt()
    }
    return CropBox((width - cropW) / 2, (height - cropH) / 2, cropW, cropH)
}

/**
 * 원본 사진을 편집 영역 비율로 가운데를 잘라 선과 글자를 그려 넣은 한 장을 만든다 (백그라운드 스레드에서 호출).
 */
internal fun composeDecoratedPhoto(source: Bitmap, strokes: List<DrawnStroke>, texts: List<PlacedText>): Bitmap {
    val crop = centerCrop(source.width, source.height, EDITOR_ASPECT)
    val output = Bitmap.createBitmap(source, crop.left, crop.top, crop.width, crop.height).copy(Bitmap.Config.ARGB_8888, true)
    val canvas = android.graphics.Canvas(output)
    val w = output.width.toFloat()
    val h = output.height.toFloat()

    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = w * STROKE_WIDTH_RATIO
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    strokes.forEach { stroke ->
        strokePaint.color = stroke.color.toColor().toArgb()
        val path = android.graphics.Path()
        stroke.points.forEachIndexed { i, p -> if (i == 0) path.moveTo(p.x * w, p.y * h) else path.lineTo(p.x * w, p.y * h) }
        if (stroke.points.size == 1) path.lineTo(stroke.points[0].x * w + 0.1f, stroke.points[0].y * h)
        canvas.drawPath(path, strokePaint)
    }

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE; textSize = w * 0.045f }
    val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xCC161616.toInt() }
    texts.forEach { item ->
        val padX = w * 0.025f
        val padY = w * 0.013f
        val x = item.position.x * w
        val baseline = item.position.y * h - padY
        val box = RectF(x, baseline + textPaint.ascent() - padY, x + textPaint.measureText(item.text) + padX * 2, baseline + textPaint.descent() + padY)
        canvas.drawRoundRect(box, w * 0.02f, w * 0.02f, boxPaint)
        canvas.drawText(item.text, x + padX, baseline, textPaint)
    }
    return output
}
