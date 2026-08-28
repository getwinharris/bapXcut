package com.getwinharris.bapxcut.utils

import android.content.Context
import android.util.Log
import com.antonkarpenko.ffmpegkit.FFmpegKit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

object AudioWaveformExtractor {

    /**
     * Extracts an audio waveform from a video or audio file using FFmpeg.
     * @param videoPath The absolute path to the media file.
     * @param targetSamples The number of data points (amplitudes) to generate (e.g. 500 for a detailed wave).
     * @return A FloatArray of normalized amplitudes (0.0 to 1.0), or null if extraction failed.
     */
    suspend fun extractWaveform(context: Context, videoPath: String, targetSamples: Int = 300): FloatArray? = withContext(Dispatchers.IO) {
        val pcmFile = File(context.cacheDir, "temp_waveform_${System.currentTimeMillis()}.pcm")
        
        try {
            // Extract audio as 16-bit PCM at a low sample rate (8000Hz is plenty for visualization)
            // -vn ignores video, -ac 1 means mono, -ar 8000 means 8000Hz, -f s16le means signed 16-bit little endian
            val command = "-y -i \"$videoPath\" -vn -ac 1 -ar 8000 -f s16le \"${pcmFile.absolutePath}\""
            
            val session = FFmpegKit.execute(command)
            if (!session.returnCode.isValueSuccess) {
                Log.e("AudioWaveformExtractor", "FFmpeg failed to extract PCM: ${session.failStackTrace}")
                return@withContext null
            }
            
            if (!pcmFile.exists() || pcmFile.length() == 0L) {
                return@withContext null
            }
            
            // Read the PCM file bytes
            val bytes = pcmFile.readBytes()
            val shortBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
            val totalSamples = shortBuffer.capacity()
            
            if (totalSamples == 0) return@withContext null
            
            val result = FloatArray(targetSamples)
            val samplesPerBucket = Math.max(1, totalSamples / targetSamples)
            
            var maxGlobalAmp = 0f
            
            // Chunk the samples and find the peak amplitude for each bucket
            for (i in 0 until targetSamples) {
                var maxLocalAmp = 0f
                val startIdx = i * samplesPerBucket
                val endIdx = Math.min(startIdx + samplesPerBucket, totalSamples)
                
                for (j in startIdx until endIdx) {
                    val amp = Math.abs(shortBuffer.get(j).toFloat())
                    if (amp > maxLocalAmp) {
                        maxLocalAmp = amp
                    }
                }
                
                result[i] = maxLocalAmp
                if (maxLocalAmp > maxGlobalAmp) {
                    maxGlobalAmp = maxLocalAmp
                }
            }
            
            // Normalize all values between 0.0 and 1.0 based on the highest peak
            if (maxGlobalAmp > 0) {
                for (i in 0 until targetSamples) {
                    result[i] = result[i] / maxGlobalAmp
                }
            }
            
            return@withContext result
            
        } catch (e: Exception) {
            Log.e("AudioWaveformExtractor", "Error processing waveform", e)
            return@withContext null
        } finally {
            if (pcmFile.exists()) {
                pcmFile.delete()
            }
        }
    }
}
