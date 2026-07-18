package com.antigravity.contactnetworkingpro

import android.os.Bundle
import android.os.Build
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.antigravity.contactnetworkingpro.data.ContactStorage
import com.antigravity.contactnetworkingpro.model.buildPanelList
import com.antigravity.contactnetworkingpro.qr.QrContentRepository
import com.antigravity.contactnetworkingpro.qr.createQrBitmap
import com.antigravity.contactnetworkingpro.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class QrDisplayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.attributes = window.attributes.apply { screenBrightness = 1f }
        enableEdgeToEdge()

        val storage = ContactStorage(this)
        val requestedPanel = intent.getStringExtra(EXTRA_PANEL_ID)
            ?: storage.getQuickAccessPanelId()
        val panels = buildPanelList(
            storage.getPanelCount(),
            listOf(storage.getCustomPanelName(0), storage.getCustomPanelName(1))
        )
        val panel = panels.firstOrNull { it.id == requestedPanel } ?: panels.first()
        val content = QrContentRepository.contentFor(storage, panel.id)

        setContent {
            ContactNetworkingProTheme {
                var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
                LaunchedEffect(content) {
                    bitmap = if (content.isBlank()) null else withContext(Dispatchers.Default) {
                        createQrBitmap(content)
                    }
                }
                Surface(Modifier.fillMaxSize(), color = Background) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .systemBarsPadding()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("PRESENTING", style = MaterialTheme.typography.titleMedium, color = Copper)
                                Text(panel.label, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                            }
                            IconButton(onClick = ::finish) {
                                Icon(Icons.Outlined.Close, "Close QR", tint = TextPrimary)
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap!!.asImageBitmap(),
                                contentDescription = "${panel.label} QR code",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(Color.White)
                                    .padding(16.dp),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(Modifier.height(20.dp))
                            Text(
                                "Hold steady while the other person scans.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        } else {
                            Icon(Icons.Outlined.QrCode2, null, tint = Copper, modifier = Modifier.size(72.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("No QR saved for ${panel.label}", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                            Spacer(Modifier.height(8.dp))
                            Text("Open the app to add this QR first.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = ::finish, colors = ButtonDefaults.textButtonColors(contentColor = Copper)) {
                            Text("CLOSE")
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_PANEL_ID = "panel_id"
    }
}
