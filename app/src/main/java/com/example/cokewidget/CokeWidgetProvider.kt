package com.example.cokewidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.widget.RemoteViews

class CokeWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateAll(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            updateAll(context)
        }
    }

    private fun updateAll(context: Context) {
        val wm = AppWidgetManager.getInstance(context)
        val thisWidget = ComponentName(context, CokeWidgetProvider::class.java)
        val ids = wm.getAppWidgetIds(thisWidget)
        for (id in ids) {
            val views = RemoteViews(context.packageName, R.layout.widget_root)
            val bmp: Bitmap = WidgetRenderer.render(context, 1000, 600)
            views.setImageViewBitmap(R.id.image, bmp)

            // tap to refresh
            val intent = Intent(context, CokeWidgetProvider::class.java).apply { action = ACTION_REFRESH }
            val pi = PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
            )
            views.setOnClickPendingIntent(R.id.image, pi)

            wm.updateAppWidget(id, views)
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.example.cokewidget.REFRESH"
    }
}
