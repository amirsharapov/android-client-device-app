package com.example.androidclient

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import java.io.ByteArrayOutputStream

/**
 * Exposes a single method to retrieve the latest image from the media projection.
 */
class MediaProjectionImageReader(mediaProjection: MediaProjection) {
    companion object {
        const val VIRTUAL_DISPLAY_NAME = "Virtual Display"
        const val WIDTH = 1350
        const val HEIGHT = 800
        const val DPI = 240
    }

    private val mediaProjection: MediaProjection
    private val imageReader: ImageReader
    private var cachedImage: ByteArray? = null

    init {
        this.mediaProjection = mediaProjection

        this.imageReader = ImageReader.newInstance(
            WIDTH,
            HEIGHT,
            PixelFormat.RGBA_8888,
            2
        )

        this.mediaProjection.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                super.onStop()
                imageReader.close()
            }
        }, null)

        this.mediaProjection.createVirtualDisplay(
            VIRTUAL_DISPLAY_NAME,
            WIDTH,
            HEIGHT,
            DPI,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            this.imageReader.surface,
            null,
            null
        )
    }

    fun getImage(): ByteArray? {
        val image = imageReader.acquireLatestImage() ?: return cachedImage

        val planes = image.planes
        val buffer = planes[0].buffer
        val stream = ByteArrayOutputStream()

        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * imageReader.width

        val bitmapWidth = imageReader.width + rowPadding / pixelStride
        val bitmapHeight = imageReader.height

        val bitmap = Bitmap.createBitmap(
            bitmapWidth,
            bitmapHeight,
            Bitmap.Config.ARGB_8888
        )

        bitmap.copyPixelsFromBuffer(buffer)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)

        image.close()

        cachedImage = stream.toByteArray()

        return cachedImage
    }
}