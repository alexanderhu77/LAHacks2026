package com.lahacks2026.pretriage.ml.whisper

import android.content.Context
import com.zeticai.mlange.feature.automaticspeechrecognition.whisper.WhisperWrapper
import java.io.File
import java.io.FileOutputStream

class WhisperFeature(
    context: Context,
) {
    private val encoder by lazy {
        WhisperEncoder(context, ENCODER_MODEL_KEY)
    }

    private val decoder by lazy {
        WhisperDecoder(
            startToken = 50258,
            endToken = 50257,
            context = context,
            modelKey = DECODER_MODEL_KEY
        )
    }

    private val whisperWrapper by lazy {
        WhisperWrapper(copyAssetToInternalStorage(context))
    }

    private fun copyAssetToInternalStorage(
        context: Context,
        assetFileName: String = "vocab.json"
    ): String {
        val outFile = File(context.filesDir, assetFileName)
        if (!outFile.exists()) {
            context.assets.open(assetFileName).use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
        return outFile.absolutePath
    }

    fun run(audio: FloatArray): String {
        val encodedFeatures = whisperWrapper.process(audio)
        val outputs = encoder.process(encodedFeatures)
        val generatedIds = decoder.generateTokens(outputs)
        return whisperWrapper.decodeToken(generatedIds.toIntArray(), true)
    }

    fun close() {
        encoder.close()
        decoder.close()
        whisperWrapper.deinit()
    }

    companion object {
        const val ENCODER_MODEL_KEY = "OpenAI/whisper-tiny-encoder"
        const val DECODER_MODEL_KEY = "OpenAI/whisper-tiny-decoder"
    }
}
