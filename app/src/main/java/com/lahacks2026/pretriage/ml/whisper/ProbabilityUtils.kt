package com.lahacks2026.pretriage.ml.whisper

object ProbabilityUtils {
    fun argmax(array: FloatArray): Int {
        var maxIndex = 0
        var maxValue = array[0]
        for (i in 1 until array.size) {
            if (array[i] > maxValue) {
                maxIndex = i
                maxValue = array[i]
            }
        }
        return maxIndex
    }
}
