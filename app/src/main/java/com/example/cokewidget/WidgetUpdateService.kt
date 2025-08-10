package com.example.cokewidget

import android.app.Service
import android.content.Intent
import android.os.IBinder

class WidgetUpdateService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // No-op placeholder
        stopSelf()
        return START_NOT_STICKY
    }
    override fun onBind(intent: Intent?) = null
}
