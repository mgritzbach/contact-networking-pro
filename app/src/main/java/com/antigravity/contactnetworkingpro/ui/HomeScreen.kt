package com.antigravity.contactnetworkingpro.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.antigravity.contactnetworkingpro.QrDisplayActivity
import com.antigravity.contactnetworkingpro.model.ContactDraft
import com.antigravity.contactnetworkingpro.model.PanelConfig
import com.antigravity.contactnetworkingpro.qr.createQrBitmap
import com.antigravity.contactnetworkingpro.ui.theme.*
import com.antigravity.contactnetworkingpro.vcard.VCardSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun HomeScreen(
    panels: List<PanelConfig>,
    savedContact: ContactDraft,
    savedQrContents: Map<String, String>,
    onEditClick: () -> Unit,
    onScanCardClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    initialPanelId: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedId by remember(panels, initialPanelId) {
        mutableStateOf(initialPanelId?.takeIf { id -> panels.any { it.id == id } } ?: panels.first().id)
    }
    var qrBitmaps by remember { mutableStateOf<Map<String, Bitmap>>(emptyMap()) }

    LaunchedEffect(savedContact) {
        val hasContact = listOf(
            savedContact.fullName, savedContact.jobTitle, savedContact.company,
            savedContact.phoneMobile, savedContact.phoneWork, savedContact.email,
            savedContact.website, savedContact.linkedinUrl, savedContact.address
        ).any(String::isNotBlank)
        val bitmap = if (hasContact) withContext(Dispatchers.Default) {
            createQrBitmap(VCardSerializer.serialize(savedContact))
        } else null
        qrBitmaps = if (bitmap != null) qrBitmaps + ("contact" to bitmap) else qrBitmaps - "contact"
    }

    LaunchedEffect(savedQrContents) {
        savedQrContents.forEach { (panelId, content) ->
            val bitmap = if (content.isNotBlank()) withContext(Dispatchers.Default) {
                createQrBitmap(content)
            } else null
            qrBitmaps = if (bitmap != null) qrBitmaps + (panelId to bitmap) else qrBitmaps - panelId
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 108.dp)
        ) {
            Box(Modifier.fillMaxWidth().height(2.dp).background(Copper))
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 26.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("CONTACT NETWORKING PRO", style = MaterialTheme.typography.titleMedium, color = Copper)
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Outlined.Settings, "Open settings", tint = TextSecondary)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Your networking QR", style = MaterialTheme.typography.displaySmall, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                Text("Pick what to share, then present or send it.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }

            val tabModifier = if (panels.size > 3) {
                Modifier.fillMaxWidth().padding(horizontal = 24.dp).horizontalScroll(rememberScrollState())
            } else {
                Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            }
            Row(modifier = tabModifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                panels.forEach { panel ->
                    val sizeModifier = if (panels.size <= 3) Modifier.weight(1f) else Modifier.width(104.dp)
                    PanelTab(
                        modifier = sizeModifier,
                        label = panel.label,
                        icon = panelIcon(panel.id),
                        selected = selectedId == panel.id,
                        onClick = { selectedId = panel.id }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            val activePanel = panels.first { it.id == selectedId }
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceDark)
                    .border(1.dp, Border, RoundedCornerShape(20.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("YOUR ${activePanel.label} QR", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                val bitmap = qrBitmaps[selectedId]
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "${activePanel.label} QR code",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(12.dp),
                        contentScale = ContentScale.Fit
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                context.startActivity(
                                    Intent(context, QrDisplayActivity::class.java)
                                        .putExtra(QrDisplayActivity.EXTRA_PANEL_ID, selectedId)
                                )
                            },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, Copper.copy(alpha = 0.7f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Copper)
                        ) {
                            Icon(Icons.Outlined.Fullscreen, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("PRESENT")
                        }
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    runCatching { shareQr(context, bitmap, activePanel.label) }
                                        .onFailure { Toast.makeText(context, "Could not share this QR.", Toast.LENGTH_SHORT).show() }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, Border),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                        ) {
                            Icon(Icons.Outlined.Share, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("SHARE")
                        }
                    }
                } else if (selectedId == "contact") {
                    EmptyQrState("No contact saved yet", "Tap Edit Profile to create your contact card.")
                } else {
                    EmptyQrState("No ${activePanel.label} QR saved", "Tap Edit Profile, then open the ${activePanel.label} tab.")
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Background), 0f, 80f))
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onScanCardClick,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark, contentColor = Copper)
                ) {
                    Icon(Icons.Outlined.CameraAlt, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("SCAN A CARD", style = MaterialTheme.typography.labelLarge, letterSpacing = 0.8.sp)
                }
                Button(
                    onClick = onEditClick,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Copper, contentColor = Background)
                ) {
                    Icon(Icons.Outlined.Edit, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("EDIT PROFILE", style = MaterialTheme.typography.labelLarge, letterSpacing = 0.8.sp)
                }
            }
        }
    }
}

@Composable
private fun PanelTab(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(if (selected) CopperContainer else SurfaceDark, label = "tab background")
    val tint by animateColorAsState(if (selected) Copper else TextTertiary, label = "tab content")
    val borderColor by animateColorAsState(if (selected) Copper.copy(alpha = 0.4f) else Border, label = "tab border")
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .semantics { this.selected = selected }
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Text(label, style = MaterialTheme.typography.titleSmall, color = tint, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun EmptyQrState(title: String, hint: String) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceElevated).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Outlined.QrCode2, null, modifier = Modifier.size(48.dp), tint = Copper)
        Text(title, style = MaterialTheme.typography.headlineSmall, color = TextPrimary, textAlign = TextAlign.Center)
        Text(hint, style = MaterialTheme.typography.bodySmall, color = TextSecondary, textAlign = TextAlign.Center)
    }
}

internal fun panelIcon(id: String): ImageVector = when (id) {
    "contact" -> Icons.Outlined.ContactPhone
    "linkedin" -> Icons.Outlined.WorkOutline
    "whatsapp" -> Icons.AutoMirrored.Outlined.Chat
    else -> Icons.Outlined.QrCode2
}

internal fun generateQr(content: String, size: Int = 900): Bitmap = createQrBitmap(content, size)

private suspend fun shareQr(context: Context, bitmap: Bitmap, label: String) {
    val file = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, "shared_qr").also { it.mkdirs() }
        File(directory, "${label.lowercase().replace(Regex("[^a-z0-9]+"), "-")}-qr.png").also { output ->
            output.outputStream().use { stream -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream) }
        }
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, "Scan my $label QR")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share $label QR"))
}
