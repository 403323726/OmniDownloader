package com.omni.downloader.engine

import com.omni.downloader.data.model.DownloadTask
import com.omni.downloader.data.model.DownloadType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormatSelectorTest {

    private fun task(
        resolution: String = "720p 高清",
        formatId: String = "",
        videoOnly: Boolean = false,
        type: DownloadType = DownloadType.VIDEO_WITH_AUDIO
    ) = DownloadTask(
        id = "t", url = "https://x.com/a/status/1", title = "t",
        downloadType = type, selectedResolution = resolution,
        selectedFormatId = formatId, selectedVideoOnly = videoOnly
    )

    @Test
    fun `uses exact format id for video-only stream and merges audio`() {
        val spec = DownloadEngine.buildVideoWithAudioSelector(
            task(formatId = "hls-368x816", videoOnly = true)
        )
        assertEquals("hls-368x816+bestaudio/bestvideo[height<=720]+bestaudio/best[height<=720]/best[height<=720]/best", spec)
    }

    @Test
    fun `muxed format is taken as is without extra audio merge`() {
        val spec = DownloadEngine.buildVideoWithAudioSelector(
            task(formatId = "http-368x816", videoOnly = false)
        )
        assertEquals("http-368x816/best[height<=720]/best", spec)
    }

    @Test
    fun `falls back to height chain when format id is pseudo`() {
        val spec = DownloadEngine.buildVideoWithAudioSelector(task(formatId = "720"))
        assertEquals("bestvideo[height<=720]+bestaudio/best[height<=720]/best[height<=720]/best", spec)
    }

    @Test
    fun `falls back when format id would break selector syntax`() {
        assertNull(DownloadEngine.exactFormatId(task(formatId = "a/b")))
        assertNull(DownloadEngine.exactFormatId(task(formatId = "best")))
        assertNull(DownloadEngine.exactFormatId(task(formatId = "")))
        assertEquals("dash-video=1", DownloadEngine.exactFormatId(task(formatId = "dash-video=1")))
    }

    @Test
    fun `video only download keeps exact format id`() {
        val spec = DownloadEngine.buildVideoOnlySelector(
            task(formatId = "137", type = DownloadType.VIDEO_ONLY)
        )
        assertEquals("137/bestvideo[height<=720]/best[height<=720]/best", spec)
    }
}
