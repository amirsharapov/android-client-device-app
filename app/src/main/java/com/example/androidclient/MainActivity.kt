package com.example.androidclient

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.example.androidclient.Constants.TAG

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupNotificationChannel()

        val intent = Intent(this, Service::class.java)

        val manager = getSystemService(
            Context.MEDIA_PROJECTION_SERVICE
        ) as android.media.projection.MediaProjectionManager

        val launcher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                intent.putExtra(Intent.EXTRA_INTENT, result.data)
                Log.i(TAG, "Starting media projection.")
                val debug = startForegroundService(intent)
                startService(intent)
            }
        }

        launcher.launch(manager.createScreenCaptureIntent())
    }

    fun setupNotificationChannel() {
        Log.i(TAG, "createNotificationChannel: Method called")
        val channel = NotificationChannel(
            Constants.NOTIFICATION_CHANNEL_ID,
            "Genesis Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
