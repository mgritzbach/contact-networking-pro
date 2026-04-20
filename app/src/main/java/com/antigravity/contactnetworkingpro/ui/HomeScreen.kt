package com.antigravity.contactnetworkingpro.ui

import android.graphics.Bitmap
import android.graphics.Color as AColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.antigravity.contactnetworkingpro.model.ContactDraft
import com.antigravity.contactnetworkingpro.model.PanelConfig
import com.antigravity.contactnetworkingpro.ui.theme.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

@Composable
fun HomeScreen(
    panels: List<PanelConfig>,
    savedContact: ContactDraft,
    savedUris: Map<String, String>,
    onEditClick: () -> Unit
) {
    var selectedId by remember(panels) { mutableStateOf(panels.first().id) }

    val contactQr = remember(savedContact) {
        if (savedContact.fullName.isBlank() && savedContact.phone.isBlank() && savedContact.email.isBlank()) null
        else generateQr(buildVCard(savedContact))
    }

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Copper))

            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp)) {
                Text("CONTACT NETWORKING PRO", style = MaterialTheme.typography.titleMedium, color = Copper)
                Spacer(Modifier.height(12.dp))
                if (savedContact.fullName.isNotBlank()) {
                    Text(
                        text = savedContact.fullName,
                        style = MaterialTheme.typography.displayMedium,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (savedContact.jobTitle.isNotBlank() || savedContact.company.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = listOf(savedContact.jobTitle, savedContact.company)
                                .filter { it.isNotBlank() }.joinToString(" · "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                } else {
                    Text("Share Your\nIdentity", style = MaterialTheme.typography.displayMedium, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    Text("Professional networking, elevated.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }

            // Dynamic panel tabs — scrollable row if count > 3
            val tabModifier = if (panels.size > 3)
                Modifier.fillMaxWidth().padding(horizontal = 24.dp).horizontalScroll(rememberScrollState())
            else
                Modifier.fillMaxWidth().padding(horizontal = 24.dp)

            Row(modifier = tabModifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                panels.forEach { panel ->
                    val weight = if (panels.size <= 3) Modifier.weight(1f) else Modifier.width(96.dp)
                    PanelTab(
                        modifier   = weight,
                        label      = panel.label,
                        icon       = panelIcon(panel.id),
                        selected   = selectedId == panel.id,
                        onClick    = { selectedId = panel.id }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // QR display card
            val activePanel = panels.first { it.id == selectedId }
            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceDark)
                    .border(1.dp, Border, RoundedCornerShape(20.dp))
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "YOUR ${activePanel.label} QR",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary
                    )

                    if (selectedId == "contact") {
                        if (contactQr != null) {
                            Image(
                                bitmap = contactQr.asImageBitmap(),
                                contentDescription = "Contact QR",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .padding(12.dp),
                                contentScale = ContentScale.FillWidth
                            )
                        } else EmptyQrState("No contact saved yet", "Tap Edit Identity to create your profile.")
                    } else {
                        val uri = savedUris[selectedId] ?: ""
                        if (uri.isNotBlank()) {
                            Image(
                                painter = rememberAsyncImagePainter(uri),
                                contentDescription = "${activePanel.label} QR",
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.FillWidth
                            )
                        } else {
                            EmptyQrState(
                                "No ${activePanel.label} QR saved",
                                "Tap Edit Identity to upload your ${activePanel.label} QR."
                            )
                        }
                    }
                }
            }
        }

        // Floating CTA
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Background), 0f, 80f))
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Button(
                onClick = onEditClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Copper, contentColor = TextPrimary)
            ) {
                Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text("EDIT IDENTITY", style = MaterialTheme.typography.labelLarge, letterSpacing = 2.sp)
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
    val bg         by animateColorAsState(if (selected) CopperContainer else SurfaceDark, label = "bg")
    val iconTint   by animateColorAsState(if (selected) Copper else TextTertiary, label = "icon")
    val textColor  by animateColorAsState(if (selected) Copper else TextTertiary, label = "text")
    val borderCol  by animateColorAsState(if (selected) Copper.copy(alpha = 0.4f) else Border, label = "border")

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, borderCol, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
        Text(label, style = MaterialTheme.typography.titleSmall, color = textColor, fontSize = 9.sp)
    }
}

@Composable
internal fun EmptyQrState(title: String, hint: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceElevated)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Outlined.QrCode2, null, modifier = Modifier.size(48.dp), tint = Copper)
        Text(title, style = MaterialTheme.typography.headlineSmall, color = TextPrimary, textAlign = TextAlign.Center)
        Text(hint, style = MaterialTheme.typography.bodySmall, color = TextSecondary, textAlign = TextAlign.Center)
    }
}

internal fun panelIcon(id: String): ImageVector = when (id) {
    "contact"  -> Icons.Outlined.ContactPhone
    "linkedin" -> Icons.Outlined.WorkOutline
    "whatsapp" -> Icons.AutoMirrored.Outlined.Chat
    else       -> Icons.Outlined.QrCode2
}

internal fun generateQr(content: String, size: Int = 900): Bitmap {
    val hints = mapOf(EncodeHintType.MARGIN to 1)
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    for (x in 0 until size) for (y in 0 until size)
        bmp.setPixel(x, y, if (matrix[x, y]) AColor.BLACK else AColor.WHITE)
    return bmp
}

internal fun buildVCard(d: ContactDraft): String = buildList {
    add("BEGIN:VCARD"); add("VERSION:3.0")
    if (d.fullName.isNotBlank())    add("FN:${vc(d.fullName)}")
    if (d.company.isNotBlank())     add("ORG:${vc(d.company)}")
    if (d.jobTitle.isNotBlank())    add("TITLE:${vc(d.jobTitle)}")
    if (d.phone.isNotBlank())       add("TEL;TYPE=CELL:${vc(d.phone)}")
    if (d.email.isNotBlank())       add("EMAIL:${vc(d.email)}")
    if (d.website.isNotBlank())     add("URL:${vc(d.website)}")
    if (d.linkedinUrl.isNotBlank()) add("URL;TYPE=LINKEDIN:${vc(d.linkedinUrl)}")
    if (d.address.isNotBlank())     add("ADR:;;${vc(d.address)};;;;")
    add("END:VCARD")
}.joinToString("\n")

private fun vc(v: String) = v
    .replace("\\", "\\\\").replace(";", "\\;")
    .replace(",", "\\,").replace("\n", "\\n")
