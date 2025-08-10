package com.example.cokewidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class WidgetWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val mgr = AppWidgetManager.getInstance(ctx)
        val ids = mgr.getAppWidgetIds(ComponentName(ctx, CokeWidgetProvider::class.java))
        if (ids.isNotEmpty()) {
            // Trigger provider update
            val provider = CokeWidgetProvider()
            provider.onUpdate(ctx, mgr, ids)
        }
        return Result.success()
    }
}
