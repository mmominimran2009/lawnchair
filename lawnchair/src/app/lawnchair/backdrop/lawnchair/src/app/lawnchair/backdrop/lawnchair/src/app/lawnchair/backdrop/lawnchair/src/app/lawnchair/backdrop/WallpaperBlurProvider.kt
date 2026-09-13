package app.lawnchair.backdrop

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.util.Log
import com.android.launcher3.util.Executors.THREAD_POOL_EXECUTOR
import java.lang.ref.WeakReference

class WallpaperBlurProvider private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val wallpaperManager = WallpaperManager.getInstance(appContext)

    @Volatile
    private var cachedBitmap: Bitmap? = null

    @Volatile
    private var cachedBlurRadius: Float = -1f

    @Synchronized
    fun getBlurredWallpaper(blurRadius: Float = DEFAULT_BLUR_RADIUS): Bitmap? {
        cachedBitmap?.let { if (cachedBlurRadius == blurRadius) return it }

        val source = captureDownscaledWallpaper() ?: return null
        val blurred = blur(source, blurRadius)
        cachedBitmap = blurred
        cachedBlurRadius = blurRadius
        return blurred
    }

    fun prefetchAsync(blurRadius: Float = DEFAULT_BLUR_RADIUS) {
        THREAD_POOL_EXECUTOR.execute {
            try {
                getBlurredWallpaper(blurRadius)
            } catch (t: Throwable) {
                Log.w(TAG, "Wallpaper blur prefetch failed", t)
            }
        }
    }

    @Synchronized
    fun invalidate() {
        cachedBitmap?.recycle()
        cachedBitmap = null
        cachedBlurRadius = -1f
    }

    fun getBackdropWindow(
        viewBoundsOnScreen: Rect,
        screenWidth: Int,
        screenHeight: Int,
        blurRadius: Float = DEFAULT_BLUR_RADIUS,
    ): Bitmap? {
        val full = getBlurredWallpaper(blurRadius) ?: return null
        if (screenWidth <= 0 || screenHeight <= 0) return full

        val scaleX = full.width.toFloat() / screenWidth
        val scaleY = full.height.toFloat() / screenHeight

        val left = (viewBoundsOnScreen.left * scaleX).toInt().coerceIn(0, full.width - 1)
        val top = (viewBoundsOnScreen.top * scaleY).toInt().coerceIn(0, full.height - 1)
        val right = (viewBoundsOnScreen.right * scaleX).toInt().coerceIn(left + 1, full.width)
        val bottom = (viewBoundsOnScreen.bottom * scaleY).toInt().coerceIn(top + 1, full.height)

        return try {
            Bitmap.createBitmap(full, left, top, right - left, bottom - top)
        } catch (e: IllegalArgumentException) {
            full
        }
    }

    private fun captureDownscaledWallpaper(): Bitmap? {
        val drawable = try {
            wallpaperManager.drawable
        } catch (t: Throwable) {
            Log.w(TAG, "Unable to read wallpaper drawable", t)
            null
        } ?: return null

        val srcWidth = drawable.intrinsicWidth.takeIf { it > 0 } ?: return null
        val srcHeight = drawable.intrinsicHeight.takeIf { it > 0 } ?: return null

        val scale = (DOWNSCALE_TARGET_WIDTH.toFloat() / srcWidth).coerceAtMost(1f)
        val outWidth = (srcWidth * scale).toInt().coerceAtLeast(1)
        val outHeight = (srcHeight * scale).toInt().coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, outWidth, outHeight)
        drawable.draw(canvas)
        return bitmap
    }

    private fun blur(source: Bitmap, radius: Float): Bitmap =
        StackBoxBlur.blur(source, radius.toInt().coerceIn(1, 25))

    companion object {
        private const val TAG = "WallpaperBlurProvider"
        const val DEFAULT_BLUR_RADIUS = 40f
        private const val DOWNSCALE_TARGET_WIDTH = 480

        @Volatile
        private var instanceRef: WeakReference<WallpaperBlurProvider>? = null

        fun get(context: Context): WallpaperBlurProvider {
            instanceRef?.get()?.let { return it }
            synchronized(this) {
                instanceRef?.get()?.let { return it }
                val created = WallpaperBlurProvider(context)
                instanceRef = WeakReference(created)
                return created
            }
        }
    }
}
