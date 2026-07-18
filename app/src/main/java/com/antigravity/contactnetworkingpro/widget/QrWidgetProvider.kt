package com.antigravity.contactnetworkingpro.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.graphics.Color
import android.widget.RemoteViews
import com.antigravity.contactnetworkingpro.QrDisplayActivity
import com.antigravity.contactnetworkingpro.R
import com.antigravity.contactnetworkingpro.data.ContactStorage
import com.antigravity.contactnetworkingpro.model.buildPanelList
import com.antigravity.contactnetworkingpro.qr.QrContentRepository
import com.antigravity.contactnetworkingpro.qr.createQrBitmap

class QrWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, widgetIds: IntArray) {
        widgetIds.forEach { update(context, manager, it) }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { QrWidgetStorage.delete(context, it) }
    }

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, QrWidgetProvider::class.java))
            ids.forEach { update(context, manager, it) }
        }

        fun update(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val storage = ContactStorage(context)
            val panelId = QrWidgetStorage.panelId(context, widgetId)
            val mode = QrWidgetStorage.mode(context, widgetId)
            val panels = buildPanelList(
                storage.getPanelCount(),
                listOf(storage.getCustomPanelName(0), storage.getCustomPanelName(1))
            )
            val label = panels.firstOrNull { it.id == panelId }?.label ?: "CONTACT"
            val content = QrContentRepository.contentFor(storage, panelId)
            val views = RemoteViews(context.packageName, R.layout.widget_qr)

            views.setTextViewText(R.id.widget_title, label)
            views.setTextViewText(
                R.id.widget_hint,
                when {
                    content.isBlank() -> "Open app to add this QR"
                    mode == WidgetMode.SHORTCUT -> "Tap to present"
                    else -> "Tap for full screen"
                }
            )

            if (mode == WidgetMode.QR && content.isNotBlank()) {
                views.setViewVisibility(R.id.widget_qr, View.VISIBLE)
                views.setViewVisibility(R.id.widget_icon, View.GONE)
                views.setImageViewBitmap(R.id.widget_qr, createQrBitmap(content, 600))
            } else {
                views.setViewVisibility(R.id.widget_qr, View.GONE)
                views.setViewVisibility(R.id.widget_icon, View.VISIBLE)
                views.setImageViewResource(R.id.widget_icon, R.drawable.ic_qr_tile)
                views.setInt(R.id.widget_icon, "setColorFilter", Color.rgb(196, 120, 74))
            }

            val intent = Intent(context, QrDisplayActivity::class.java)
                .putExtra(QrDisplayActivity.EXTRA_PANEL_ID, panelId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val pendingIntent = PendingIntent.getActivity(
                context,
                widgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            manager.updateAppWidget(widgetId, views)
        }
    }
}
