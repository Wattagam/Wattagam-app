package com.wattagam.app.data.draft

import androidx.core.net.toUri
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/** 초안은 실패·범위 밖에도 남아야 하고, 새 사진이 들어오면 예전에 꾸민 사진은 쓰지 않아야 한다 */
class DraftStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val store = DraftStore(context)
    private val place = "test-place"

    @Before fun setUp() = runBlocking { store.clear(place); Unit }
    @After fun tearDown() = runBlocking { store.clear(place); Unit }

    @Test fun messageSurvivesNewStoreInstance() = runBlocking {
        store.writeMessage(place, "오늘의 산책")
        assertEquals("오늘의 산책", DraftStore(context).readMessage(place))
    }

    @Test fun newPhotoReplacesOriginalAndDropsOldDecoration() = runBlocking {
        store.captureTempFile(place).writeText("first")
        store.commitCapture(place)
        store.decoratedFile(place).writeText("decorated")
        assertEquals(store.decoratedFile(place), store.publishPhoto(place))

        store.captureTempFile(place).writeText("second")
        store.commitCapture(place)
        assertEquals("second", store.photoFile(place).readText())
        assertFalse(store.decoratedFile(place).exists())
        assertEquals(store.photoFile(place), store.publishPhoto(place))
    }

    @Test fun importCopiesGalleryPhoto() = runBlocking {
        val source = File(context.cacheDir, "gallery.jpg").apply { writeText("gallery") }
        assertTrue(store.importPhoto(context, place, source.toUri()))
        assertEquals("gallery", store.photoFile(place).readText())
        source.delete()
        Unit
    }

    @Test fun clearRemovesEverything() = runBlocking {
        store.writeMessage(place, "메시지")
        store.captureTempFile(place).writeText("photo")
        store.commitCapture(place)
        store.clear(place)
        assertFalse(store.hasPhoto(place))
        assertNull(store.publishPhoto(place))
        assertEquals("", store.readMessage(place))
    }
}
