package com.antigravity.contactnetworkingpro.widget

import android.content.Context

enum class WidgetMode { QR, SHORTCUT }

object QrWidgetStorage {
    private const val PREFS = "cnp_widgets"

    fun save(context: Context, widgetId: Int, panelId: String, mode: WidgetMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("panel_$widgetId", panelId)
            .putString("mode_$widgetId", mode.name)
            .apply()
    }

    fun panelId(context: Context, widgetId: Int): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("panel_$widgetId", "contact") ?: "contact"

    fun mode(context: Context, widgetId: Int): WidgetMode = runCatching {
        WidgetMode.valueOf(
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString("mode_$widgetId", WidgetMode.QR.name) ?: WidgetMode.QR.name
        )
    }.getOrDefault(WidgetMode.QR)

    fun delete(context: Context, widgetId: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove("panel_$widgetId")
            .remove("mode_$widgetId")
            .apply()
    }
}
