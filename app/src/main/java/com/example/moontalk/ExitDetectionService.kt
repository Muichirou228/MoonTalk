package com.example.moontalk
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

//import java.security.Provider

class ExitDetectionService : Service() {

    private val authRepo = SupabaseRepository()

    override fun onCreate() {
        super.onCreate()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "exit_channel",
                "MoonTalk Service",
                NotificationManager.IMPORTANCE_NONE
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, "exit_channel")
            .setContentTitle("MoonTalk")
            .setContentText("Приложение работает")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("ExSer", "service started")
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d("ExSer", "Swiped away")
        super.onTaskRemoved(rootIntent)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                authRepo.notOnlineAnymore()
                var currentRoomId = AppState.currentRoomId
                if (currentRoomId != null) {
                    authRepo.deleteRoom(currentRoomId)
                    authRepo.deleteAllMessagesFromRoom(currentRoomId)
                }
                Log.d("ExSer", "success")
                stopSelf()
            } catch (e: Exception) {
                Log.d("ExSer", "error, ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}