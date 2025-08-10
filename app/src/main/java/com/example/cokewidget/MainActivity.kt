package com.example.cokewidget

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.work.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Open settings so user can configure maintenance
        startActivity(Intent(this, SettingsActivity::class.java))

        // schedule background updates (min 15 min)
        val req = PeriodicWorkRequestBuilder<WidgetWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "widget-update", ExistingPeriodicWorkPolicy.UPDATE, req
        )
        finish()
    }
}
