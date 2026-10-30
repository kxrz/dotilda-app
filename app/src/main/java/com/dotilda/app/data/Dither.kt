package com.dotilda.app.data

/**
 * Floyd-Steinberg dithering to [levels] evenly spaced greys (4 for Dotilda's photos, which is
 * what e-ink shows cleanly). [luma] holds one 0..255 value per pixel, row by row; the result
 * holds the chosen grey (0..255) per pixel.
 */
fun ditherFloydSteinberg(luma: IntArray, width: Int, height: Int, levels: Int = 4): IntArray {
    val step = 255f / (levels - 1)
    val work = FloatArray(luma.size) { luma[it].toFloat() }
    val out = IntArray(luma.size)
    for (y in 0 until height) {
        for (x in 0 until width) {
            val i = y * width + x
            val old = work[i].coerceIn(0f, 255f)
            val new = (Math.round(old / step) * step).coerceIn(0f, 255f)
            out[i] = new.toInt()
            val error = old - new
            if (x + 1 < width) work[i + 1] += error * 7 / 16
            if (y + 1 < height) {
                if (x > 0) work[i + width - 1] += error * 3 / 16
                work[i + width] += error * 5 / 16
                if (x + 1 < width) work[i + width + 1] += error * 1 / 16
            }
        }
    }
    return out
}
