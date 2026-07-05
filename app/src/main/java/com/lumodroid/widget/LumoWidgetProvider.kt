package com.lumodroid.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.lumodroid.MainActivity
import com.lumodroid.R

class LumoWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { widgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_layout)

            // Click anywhere opens app with quick focus
            val clickIntent = Intent(context, MainActivity::class.java).apply {
                action = "WIDGET_QUICK_ASK"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val clickPending = PendingIntent.getActivity(
                context,
                widgetId,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            
            // Make entire widget clickable
            views.setOnClickPendingIntent(R.id.widget_root, clickPending)
            views.setOnClickPendingIntent(R.id.widget_title, clickPending)
            views.setOnClickPendingIntent(R.id.widget_send_btn, clickPending)

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}
