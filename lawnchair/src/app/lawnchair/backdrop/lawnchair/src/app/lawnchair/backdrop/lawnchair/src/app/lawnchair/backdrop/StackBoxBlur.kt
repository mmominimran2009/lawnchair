package app.lawnchair.backdrop

import android.graphics.Bitmap

internal object StackBoxBlur {

    fun blur(bitmap: Bitmap, radius: Int): Bitmap {
        if (radius < 1) return bitmap
        val working = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val w = working.width
        val h = working.height
        val pixels = IntArray(w * h)
        working.getPixels(pixels, 0, w, 0, 0, w, h)

        repeat(3) {
            boxBlurHorizontal(pixels, w, h, radius)
            boxBlurVertical(pixels, w, h, radius)
        }

        working.setPixels(pixels, 0, w, 0, 0, w, h)
        return working
    }

    private fun boxBlurHorizontal(pixels: IntArray, w: Int, h: Int, radius: Int) {
        val temp = IntArray(w)
        for (y in 0 until h) {
            val rowStart = y * w
            for (x in 0 until w) {
                var a = 0
                var r = 0
                var g = 0
                var b = 0
                var count = 0
                for (dx in -radius..radius) {
                    val sx = x + dx
                    if (sx < 0 || sx >= w) continue
                    val p = pixels[rowStart + sx]
                    a += (p ushr 24) and 0xFF
                    r += (p ushr 16) and 0xFF
                    g += (p ushr 8) and 0xFF
                    b += p and 0xFF
                    count++
                }
                temp[x] = ((a / count) shl 24) or ((r / count) shl 16) or ((g / count) shl 8) or (b / count)
            }
            System.arraycopy(temp, 0, pixels, rowStart, w)
        }
    }

    private fun boxBlurVertical(pixels: IntArray, w: Int, h: Int, radius: Int) {
        val temp = IntArray(h)
        for (x in 0 until w) {
            for (y in 0 until h) {
                var a = 0
                var r = 0
                var g = 0
                var b = 0
                var count = 0
                for (dy in -radius..radius) {
                    val sy = y + dy
                    if (sy < 0 || sy >= h) continue
                    val p = pixels[sy * w + x]
                    a += (p ushr 24) and 0xFF
                    r += (p ushr 16) and 0xFF
                    g += (p ushr 8) and 0xFF
                    b += p and 0xFF
                    count++
                }
                temp[y] = ((a / count) shl 24) or ((r / count) shl 16) or ((g / count) shl 8) or (b / count)
            }
            for (y in 0 until h) {
                pixels[y * w + x] = temp[y]
            }
        }
    }
}
