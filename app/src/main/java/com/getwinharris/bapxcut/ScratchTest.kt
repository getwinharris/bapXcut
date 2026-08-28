package com.getwinharris.bapxcut
import com.google.android.exoplayer2.source.MergingMediaSource
import com.google.android.exoplayer2.source.MediaSource
class ScratchTest {
    fun test(s1: MediaSource, s2: MediaSource) {
        MergingMediaSource(true, s1, s2)
        MergingMediaSource(true, true, s1, s2)
    }
}
