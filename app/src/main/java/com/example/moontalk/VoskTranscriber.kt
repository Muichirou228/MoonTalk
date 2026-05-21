package com.example.moontalk

import android.content.Context
import android.util.Log
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.io.FileInputStream

class VoskTranscriber(private val context: Context) {
    private var model: Model? = null
    private var recognizer: Recognizer? = null

    init {
        try {
            val modelPath = "models/vosk-model-small-en-us-0.15"

            // Копируем модель из assets во внешнее хранилище
            val modelDir = File(context.filesDir, "vosk-model")
            if (!modelDir.exists()) {
                Log.d("VOSK", "Копируем модель из assets...")
                copyAssetsToDirectory(modelPath, modelDir)
            }

            // ПРОВЕРКА: что внутри папки?
            Log.d("VOSK", "Содержимое папки модели: ${modelDir.listFiles()?.map { it.name }}")

            // Проверяем наличие папки 'am' (обязательно должна быть)
            val amDir = File(modelDir, "am")
            Log.d("VOSK", "Папка am существует: ${amDir.exists()}")

            model = Model(modelDir.absolutePath)
            recognizer = Recognizer(model, 16000.0f)
            Log.d("VOSK", "Модель успешно загружена!")
        } catch (e: Exception) {
            Log.e("VOSK", "Ошибка загрузки модели: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun copyAssetsToDirectory(assetPath: String, destinationDir: File) {
        try {
            val assets = context.assets
            val files = assets.list(assetPath) ?: return

            destinationDir.mkdirs()

            for (file in files) {
                val assetFilePath = "$assetPath/$file"
                val destFile = File(destinationDir, file)

                // Проверяем, является ли элемент папкой
                val subFiles = assets.list(assetFilePath)
                val isDirectory = subFiles != null && subFiles.isNotEmpty()

                if (isDirectory) {
                    copyAssetsToDirectory(assetFilePath, destFile)
                } else {
                    assets.open(assetFilePath).use { inputStream ->
                        destFile.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("VOSK", "Ошибка копирования: ${e.message}")
        }
    }

    fun transcribe(audioFile: File): String? {
        return try {
            Log.d("VOSK", "Starting transcription for: ${audioFile.absolutePath}")
            Log.d("VOSK", "File size: ${audioFile.length()} bytes")
            val fis = FileInputStream(audioFile)
            val buffer = ByteArray(4096)
            var bytesRead: Int

            while (fis.read(buffer).also { bytesRead = it } != -1) {
                recognizer?.acceptWaveForm(buffer, bytesRead)
            }

            val result = recognizer?.finalResult ?: ""
            Log.d("VOSK", "Raw result: $result")

            // Парсим JSON: {"text": "hello world"}
            val regex = "\"text\"\\s*:\\s*\"([^\"]*)\"".toRegex()
            val text = regex.find(result)?.groupValues?.get(1) ?: ""
            Log.d("VOSK", "Parsed text: '$text'")
            fis.close()
            recognizer?.reset()
            text
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("VOSK", "Error: ${e.message}")
            null
        }
    }

    fun close() {
        recognizer?.close()
        model?.close()
    }
}