package com.omni.downloader.engine

import com.omni.downloader.data.model.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TransferStageTest {

    @Test
    fun `merging beats every other label`() {
        assertEquals(
            "正在合并音视频轨…",
            DownloadEngine.processingStageLabel(isAudioStage = true, sawMerge = true, status = TaskStatus.PROCESSING)
        )
    }

    @Test
    fun `audio stage is labelled even before status flips`() {
        assertEquals(
            "正在下载音轨",
            DownloadEngine.processingStageLabel(isAudioStage = true, sawMerge = false, status = TaskStatus.DOWNLOADING)
        )
    }

    @Test
    fun `post processing without merger falls back to saving`() {
        assertEquals(
            "正在保存文件…",
            DownloadEngine.processingStageLabel(isAudioStage = false, sawMerge = false, status = TaskStatus.PROCESSING)
        )
    }

    @Test
    fun `plain downloading has no stage label`() {
        assertNull(
            DownloadEngine.processingStageLabel(isAudioStage = false, sawMerge = false, status = TaskStatus.DOWNLOADING)
        )
    }

    @Test
    fun `single stream keeps raw percentage`() {
        assertEquals(42f, DownloadEngine.stagedProgress(42f, isAudioStage = false, expectsSeparateAudio = false), 0.001f)
    }

    @Test
    fun `split streams map video to ninety and audio to the tail`() {
        assertEquals(45f, DownloadEngine.stagedProgress(50f, isAudioStage = false, expectsSeparateAudio = true), 0.001f)
        assertEquals(94.5f, DownloadEngine.stagedProgress(50f, isAudioStage = true, expectsSeparateAudio = true), 0.001f)
    }
}
