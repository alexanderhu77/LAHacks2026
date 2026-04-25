package com.lahacks2026.pretriage.ml.whisper

import android.content.Context
import com.lahacks2026.pretriage.BuildConfig
import com.zeticai.mlange.core.model.ZeticMLangeModel
import com.zeticai.mlange.core.tensor.Tensor
import java.nio.ByteBuffer

class WhisperEncoder(
    context: Context,
    modelKey: String,
    private val model: ZeticMLangeModel = ZeticMLangeModel(
        context,
        BuildConfig.MELANGE_TOKEN,
        modelKey
    )
) {
    fun process(audioData: FloatArray): ByteBuffer {
        val inputs = arrayOf(Tensor.of(audioData))
        return model.run(inputs)[0].data
    }

    fun close() {
        model.deinit()
    }
}
