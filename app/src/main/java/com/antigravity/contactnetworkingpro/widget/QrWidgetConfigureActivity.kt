package com.antigravity.contactnetworkingpro.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.antigravity.contactnetworkingpro.data.ContactStorage
import com.antigravity.contactnetworkingpro.model.buildPanelList
import com.antigravity.contactnetworkingpro.ui.theme.*

class QrWidgetConfigureActivity : ComponentActivity() {
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        widgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val storage = ContactStorage(this)
        val panels = buildPanelList(
            storage.getPanelCount(),
            listOf(storage.getCustomPanelName(0), storage.getCustomPanelName(1))
        )
        setContent {
            ContactNetworkingProTheme {
                var panelId by remember { mutableStateOf(panels.first().id) }
                var mode by remember { mutableStateOf(WidgetMode.QR) }
                Surface(Modifier.fillMaxSize(), color = Background) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Spacer(Modifier.height(12.dp))
                        Text("ADD QR WIDGET", style = MaterialTheme.typography.titleMedium, color = Copper)
                        Text("Choose what this widget should show.", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)

                        Text("QR", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                        panels.forEach { panel ->
                            FilterChip(
                                selected = panelId == panel.id,
                                onClick = { panelId = panel.id },
                                label = { Text(panel.label) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Text("STYLE", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            WidgetMode.entries.forEach { candidate ->
                                FilterChip(
                                    selected = mode == candidate,
                                    onClick = { mode = candidate },
                                    label = { Text(if (candidate == WidgetMode.QR) "SHOW QR" else "QUICK LINK") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Text(
                            if (mode == WidgetMode.QR) "People can scan directly from your home screen."
                            else "A discreet shortcut opens the selected QR full screen.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = { saveAndFinish(panelId, mode) },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Copper, contentColor = Background)
                        ) { Text("ADD TO HOME SCREEN") }
                    }
                }
            }
        }
    }

    private fun saveAndFinish(panelId: String, mode: WidgetMode) {
        QrWidgetStorage.save(this, widgetId, panelId, mode)
        QrWidgetProvider.update(this, AppWidgetManager.getInstance(this), widgetId)
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        finish()
    }
}
