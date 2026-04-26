package com.lahacks2026.pretriage.ml.clip

import android.content.Context
import org.json.JSONArray

data class LabelHit(val category: String, val score: Float)

/**
 * Zero-shot classifier over precomputed CLIP text embeddings.
 *
 * Bundle is produced by scripts/build_clip_labels.py and shipped in
 * assets/clip_labels.json. Each category has multiple phrasings; we average
 * their cosine scores so a scrappy phone photo that weakly matches one
 * phrasing still wins on the category overall.
 *
 * The "junk" category is a negative class — if it wins, the image is
 * unreadable / off-topic and we should suppress the visual-finding line
 * rather than feed the LLM a confidently wrong label.
 */
class ClipLabelClassifier(context: Context) {

    private data class Category(
        val name: String,
        val embeddings: Array<FloatArray>, // each row already L2-normalized
    )

    private val categories: List<Category>

    init {
        val raw = context.assets.open(ASSET_NAME)
            .bufferedReader().use { it.readText() }
        val arr = JSONArray(raw)
        categories = (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val embArr = o.getJSONArray("embeddings")
            val embs = Array(embArr.length()) { j ->
                val row = embArr.getJSONArray(j)
                FloatArray(row.length()) { k -> row.getDouble(k).toFloat() }
            }
            Category(name = o.getString("category"), embeddings = embs)
        }
    }

    /**
     * Score the image against every phrasing, then mean-aggregate per category.
     * Returns categories sorted by score, descending. Image embedding is
     * L2-normalized internally — caller does not need to pre-normalize.
     */
    fun classify(imageEmbedding: FloatArray): List<LabelHit> {
        val img = l2Normalize(imageEmbedding)
        return categories.map { cat ->
            var sum = 0f
            for (row in cat.embeddings) sum += dot(img, row)
            LabelHit(cat.name, sum / cat.embeddings.size)
        }.sortedByDescending { it.score }
    }

    private fun dot(a: FloatArray, b: FloatArray): Float {
        var s = 0f
        for (i in a.indices) s += a[i] * b[i]
        return s
    }

    private fun l2Normalize(v: FloatArray): FloatArray {
        var s = 0.0
        for (x in v) s += x * x
        val n = kotlin.math.sqrt(s).toFloat().coerceAtLeast(1e-12f)
        return FloatArray(v.size) { v[it] / n }
    }

    companion object {
        const val ASSET_NAME = "clip_labels.json"
        const val JUNK_CATEGORY = "junk"

        // Below this top-1 score on cosine-with-mean-aggregation, the match
        // is too weak to trust. Empirically, real CLIP image-text scores on
        // matching content land in 0.22-0.35; random matches are <0.18.
        const val MIN_CONFIDENCE = 0.20f
    }
}
