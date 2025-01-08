package com.example.androidclient

import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.androidclient.Constants.NOTIFICATION_CHANNEL_ID
import com.example.androidclient.Constants.TAG
import fi.iki.elonen.NanoHTTPD
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.io.ByteArrayInputStream
import java.net.InetSocketAddress

class Service : Service() {
    private var mediaProjectionImageReader: MediaProjectionImageReader? = null
    private var mediaProjection: MediaProjection? = null
    private var httpServer: HTTPServer? = null
    private var serviceThread: Thread? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Service started.")

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setOngoing(true)
            .setContentTitle("Android Automation Client")
            .setContentText("Android Automation Client is running in the background.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        startForeground(
            1,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        )

        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        if (intent != null) {
            val data = intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)

            if (data != null && mediaProjection == null) {
                mediaProjection = manager.getMediaProjection(Activity.RESULT_OK, data)
                mediaProjectionImageReader = MediaProjectionImageReader(mediaProjection!!)

                serviceThread = Thread(AsyncDisplayCapture());
                serviceThread!!.start()
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    inner class AsyncDisplayCapture : Runnable {
        override fun run() {
            if (mediaProjection == null) {
                throw IllegalStateException("MediaProjection is null")
            }

            httpServer = HTTPServer().apply {
                try {
                    start()
                } catch (e: Exception) {
                    Log.e(TAG, "run: " + e.message)
                    e.printStackTrace()
                }
            }
        }
    }

    inner class HTTPServer() : NanoHTTPD(8080) {
        override fun serve(session: IHTTPSession): Response {
            val qualityParam = session.parameters["quality"]?.get(0)
            val quality = qualityParam?.toIntOrNull() ?: 100

            val image = mediaProjectionImageReader!!.getImage(quality)
            val imageMimeType = "image/jpeg"

            return newFixedLengthResponse(
                Response.Status.OK,
                imageMimeType,
                ByteArrayInputStream(image),
                image!!.size.toLong()
            )
        }
    }
}