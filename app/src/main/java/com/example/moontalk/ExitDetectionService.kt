package com.example.moontalk
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
//import java.security.Provider

class ExitDetectionService : Service() {

    private val authRepo = SupabaseRepository()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d("ExSer", "Swiped away")
        super.onTaskRemoved(rootIntent)
        kotlinx.coroutines.runBlocking {
            authRepo.notOnlineAnymore()
            Log.d("ExSer", "Noionlineanymore")
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}