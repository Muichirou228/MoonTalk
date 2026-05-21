package com.example.moontalk

import android.content.Context
import android.media.MediaRecorder
import android.util.Log
import java.io.File

class AudioRecorderManager(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    fun startRecording() : Boolean {
        return try {
            val fileName = "voice_${System.currentTimeMillis()}.m4a"
            audioFile = File(context.cacheDir, fileName)

            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.AMR_WB) // Или другой формат, подходящий для PCM
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB)
                setAudioSamplingRate(16000)  // ← добавить эту строку
                setAudioChannels(1)
                setOutputFile(audioFile?.absolutePath)
                prepare()
                start()
            }
            true
        } catch (e: Exception) {
            Log.d("AUDIOOO", "Error while reccording is ${e.message}")
            false
        }
    }

    fun stopRecording(): File? {
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            audioFile
        } catch (e: Exception) {
            null
        }
    }

    fun isRecording(): Boolean = mediaRecorder != null
}