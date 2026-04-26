package com.lahacks2026.pretriage.ml.clip

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.lahacks2026.pretriage.BuildConfig
import com.zeticai.mlange.core.model.ZeticMLangeModel
import com.zeticai.mlange.core.tensor.Tensor
import java.nio.ByteOrder

/**
 * Wraps `OpenAI/clip-vit-base-patch32` in Zetic's catalog as an image encoder.
 * For the probe push, we only run the image side: bitmap → 512-dim embedding.
 *
 * Whether this model also accepts text tokens (full CLIP) or is image-only
 * is the question this push exists to answer. The first on-device run will
 * log the output tensor shape so we know which path the next push takes:
 * full Path A (Zetic ships both encoders) vs Path B (pre-compute text
 * embeddings offline and bundle them).
 */
class ClipImageEncoder(context: Context) {

    private val model = ZeticMLangeModel(
        context.applicationContext,
        BuildConfig.MELANGE_TOKEN,
        MODEL_KEY
    )

    /** Returns a flat FloatArray (typically 512-dim for CLIP-ViT-Base-Patch32). */
    fun encode(bitmap: Bitmap): FloatArray {
        val pixelTensor = ClipImagePreprocessor.preprocess(bitmap)
        val outputs = model.run(arrayOf(Tensor.of(pixelTensor)))
        val buffer = outputs[0].data.order(ByteOrder.LITTLE_ENDIAN)
        val floats = FloatArray(buffer.capacity() / Float.SIZE_BYTES)
        buffer.asFloatBuffer().get(floats)
        Log.i(
            TAG,
            "image embedding generated: dim=${floats.size}, " +
                "norm=${"%.3f".format(l2Norm(floats))}, " +
                "head=${floats.take(6).joinToString(prefix = "[", postfix = "…]") { "%.3f".format(it) }}"
        )
        return floats
    }

    fun close() {
        runCatching { model.close() }
    }

    private fun l2Norm(v: FloatArray): Float {
        var s = 0.0
        for (x in v) s += x * x
        return kotlin.math.sqrt(s).toFloat()
    }

    companion object {
        const val TAG = "ClipEncoder"
        const val MODEL_KEY = "OpenAI/clip-vit-base-patch32"
    }
}
