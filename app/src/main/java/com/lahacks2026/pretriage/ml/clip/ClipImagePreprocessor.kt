package com.lahacks2026.pretriage.ml.clip

import android.graphics.Bitmap

/**
 * Bitmap → [1, 3, 224, 224] CLIP-normalized FloatArray.
 *
 * OpenAI's CLIP-ViT-Base-Patch32 was trained with a specific preprocessing
 * pipeline that we MUST match exactly or the embeddings will be garbage:
 *   1. Resize so shortest side = 224, center crop to 224×224.
 *   2. Convert to RGB float32 in [0, 1].
 *   3. Normalize with CLIP's image-net-derived mean/std.
 *   4. Layout as NCHW (channels-first), batch=1.
 *
 * Constants from the official CLIP repository
 * (https://github.com/openai/CLIP/blob/main/clip/clip.py).
 */
object ClipImagePreprocessor {

    const val INPUT_SIZE = 224

    // OpenAI CLIP normalization values. Do not change.
    private val MEAN = floatArrayOf(0.48145466f, 0.4578275f, 0.40821073f)
    private val STD = floatArrayOf(0.26862954f, 0.26130258f, 0.27577711f)

    /**
     * Returns a flat FloatArray of length 3 * 224 * 224 = 150528 in NCHW order:
     * [R-plane (224×224), G-plane (224×224), B-plane (224×224)].
     */
    fun preprocess(source: Bitmap): FloatArray {
        val cropped = centerSquareCrop(source)
        val resized = Bitmap.createScaledBitmap(cropped, INPUT_SIZE, INPUT_SIZE, true)

        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        resized.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        val planeSize = INPUT_SIZE * INPUT_SIZE
        val out = FloatArray(3 * planeSize)
        for (i in 0 until planeSize) {
            val pixel = pixels[i]
            val r = ((pixel shr 16) and 0xFF) / 255f
            val g = ((pixel shr 8) and 0xFF) / 255f
            val b = (pixel and 0xFF) / 255f
            out[i] = (r - MEAN[0]) / STD[0]
            out[planeSize + i] = (g - MEAN[1]) / STD[1]
            out[2 * planeSize + i] = (b - MEAN[2]) / STD[2]
        }

        if (resized != cropped) resized.recycle()
        if (cropped != source) cropped.recycle()
        return out
    }

    private fun centerSquareCrop(source: Bitmap): Bitmap {
        val side = minOf(source.width, source.height)
        if (source.width == source.height) return source
        val x = (source.width - side) / 2
        val y = (source.height - side) / 2
        return Bitmap.createBitmap(source, x, y, side, side)
    }
}
