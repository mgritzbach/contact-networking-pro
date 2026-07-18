package com.antigravity.contactnetworkingpro

import android.app.PendingIntent
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.antigravity.contactnetworkingpro.data.ContactStorage
import com.antigravity.contactnetworkingpro.model.buildPanelList
import com.antigravity.contactnetworkingpro.qr.QrContentRepository

class QrTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        val storage = ContactStorage(this)
        val panelId = storage.getQuickAccessPanelId()
        val panels = buildPanelList(
            storage.getPanelCount(),
            listOf(storage.getCustomPanelName(0), storage.getCustomPanelName(1))
        )
        val panel = panels.firstOrNull { it.id == panelId } ?: panels.first()
        qsTile?.apply {
            label = "${panel.label} QR"
            state = if (QrContentRepository.contentFor(storage, panel.id).isBlank()) {
                Tile.STATE_INACTIVE
            } else {
                Tile.STATE_ACTIVE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                subtitle = "Tap to present"
            }
            updateTile()
        }
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    @Suppress("DEPRECATION")
    override fun onClick() {
        super.onClick()
        val intent = Intent(this, QrDisplayActivity::class.java)
            .putExtra(QrDisplayActivity.EXTRA_PANEL_ID, ContactStorage(this).getQuickAccessPanelId())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                901,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            startActivityAndCollapse(intent)
        }
    }
}
