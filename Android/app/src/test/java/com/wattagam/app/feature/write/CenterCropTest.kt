package com.wattagam.app.feature.write

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CenterCropTest {
    @Test fun wideLandscapePhotoKeepsFullHeightAndCropsSidesEvenly() {
        val crop = centerCrop(4000, 3000, EDITOR_ASPECT)
        assertEquals(3000, crop.height)
        assertEquals((3000 * EDITOR_ASPECT).toInt(), crop.width)
        assertEquals((4000 - crop.width) / 2, crop.left)
        assertEquals(0, crop.top)
    }

    @Test fun tallPortraitPhotoKeepsFullWidthAndCropsTopAndBottomEvenly() {
        val crop = centerCrop(3000, 4000, EDITOR_ASPECT)
        assertEquals(3000, crop.width)
        assertEquals(0, crop.left)
        assertEquals((4000 - crop.height) / 2, crop.top)
    }

    @Test fun cropStaysInsidePhotoAndMatchesEditorAspect() {
        listOf(1080 to 1920, 1920 to 1080, 1000 to 1000, 961 to 1001).forEach { (w, h) ->
            val crop = centerCrop(w, h, EDITOR_ASPECT)
            assertTrue(crop.left >= 0 && crop.top >= 0)
            assertTrue(crop.left + crop.width <= w && crop.top + crop.height <= h)
            assertEquals(EDITOR_ASPECT, crop.width.toFloat() / crop.height, 0.01f)
        }
    }
}
