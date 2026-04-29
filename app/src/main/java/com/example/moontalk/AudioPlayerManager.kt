package com.example.moontalk

import android.media.MediaPlayer

class AudioPlayerManager {
    private var mediaPlayer: MediaPlayer? = null
    private var currentUrl: String? = null

    fun play(url: String, onComplete: () -> Unit = {}) {
        if (currentUrl == url && mediaPlayer?.isPlaying == true) {
            stop()
            return
        }

        stop()

        currentUrl = url
        mediaPlayer = MediaPlayer().apply {
            setDataSource(url)
            prepareAsync()
            setOnPreparedListener {
                start()
            }
            setOnCompletionListener {
                stop()
                onComplete()
            }
            setOnErrorListener { _, _, _ ->
                stop()
                true
            }
        }
    }

    fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        currentUrl = null
    }

    fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true
}