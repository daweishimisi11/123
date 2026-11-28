package com.av123.video.data.playback

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@androidx.annotation.OptIn(UnstableApi::class)
@RunWith(AndroidJUnit4::class)
class MediaCacheTest {

    @Test
    fun tryClear_deletesCachedFilesAndDatabaseIndex() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        MediaCache.tryClear(context)
        val cache = MediaCache.get(context)
        val span = cache.startReadWrite("test-key", 0L, 4L)
        try {
            val file = cache.startFile("test-key", 0L, 4L)
            file.writeBytes(byteArrayOf(1, 2, 3, 4))
            cache.commitFile(file, 4L)
        } finally {
            cache.releaseHoleSpan(span)
        }
        assertTrue(cache.keys.contains("test-key"))

        assertTrue(MediaCache.tryClear(context))

        val reopened = MediaCache.get(context)
        assertTrue(reopened.keys.isEmpty())
        assertEquals(0L, reopened.cacheSpace)
        MediaCache.tryClear(context)
    }
}
