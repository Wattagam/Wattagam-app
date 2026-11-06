package com.wattagam.app.feature.write

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import com.wattagam.app.feature.guestbook.DrawingColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** 꾸민 사진은 편집 화면과 같은 비율로 잘리고, 그린 선이 같은 위치에 들어가야 한다 */
class DecorationTest {
    private fun whitePhoto(w: Int, h: Int) = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }

    @Test fun outputUsesEditorAspect() {
        val out = composeDecoratedPhoto(whitePhoto(1200, 800), emptyList(), emptyList())
        assertEquals(EDITOR_ASPECT, out.width.toFloat() / out.height, 0.01f)
    }

    @Test fun strokeIsDrawnAtNormalizedPosition() {
        val stroke = DrawnStroke(listOf(Offset(0.2f, 0.5f), Offset(0.8f, 0.5f)), DrawingColor.Red)
        val out = composeDecoratedPhoto(whitePhoto(960, 1000), listOf(stroke), emptyList())
        assertEquals(DrawingColor.Red.toColor().toArgb(), out.getPixel(out.width / 2, out.height / 2))
        assertEquals(Color.WHITE, out.getPixel(out.width / 2, out.height / 10))
    }

    @Test fun textAddsPixelsNearBottomLeft() {
        val plain = composeDecoratedPhoto(whitePhoto(960, 1000), emptyList(), emptyList())
        val withText = composeDecoratedPhoto(whitePhoto(960, 1000), emptyList(), listOf(PlacedText("안녕", Offset(0.04f, 0.95f))))
        val x = (withText.width * 0.06f).toInt()
        val y = (withText.height * 0.92f).toInt()
        assertEquals(Color.WHITE, plain.getPixel(x, y))
        assertNotEquals(Color.WHITE, withText.getPixel(x, y))
    }
}
