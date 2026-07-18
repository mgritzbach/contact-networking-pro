package com.antigravity.contactnetworkingpro.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.contactnetworkingpro.model.ContactDraft
import com.antigravity.contactnetworkingpro.model.ContactDraftSaver
import com.antigravity.contactnetworkingpro.model.PanelConfig
import com.antigravity.contactnetworkingpro.ui.theme.*
import com.antigravity.contactnetworkingpro.vcard.VCardSerializer
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ContactEditorScreen(
    panels: List<PanelConfig>,
    initialContact: ContactDraft,
    savedQrContents: Map<String, String>,
    onSaveContact: (ContactDraft) -> Unit,
    onSaveQrContent: (panelId: String, content: String) -> Unit,
    onScanMyCard: () -> Unit = {},
    onBack: () -> Unit
) {
    val context  = LocalContext.current
    val scope    = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var activeId  by rememberSaveable { mutableStateOf(panels.first().id) }
    var draft     by rememberSaveable(stateSaver = ContactDraftSaver) { mutableStateOf(initialContact) }

    // For non-contact panels: draft QR content strings (decoded from uploaded images)
    var draftQrContents by remember { mutableStateOf(savedQrContents) }
    // Track decode state per panel
    var decodingPanel   by remember { mutableStateOf<String?>(null) }
    var decodeErrors    by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    // Image pickers — pick image → decode QR → store content string
    val imagePickers = panels
        .filter { it.id != "contact" }
        .associate { panel ->
            panel.id to rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                if (uri != null) {
                    scope.launch {
                        decodingPanel = panel.id
                        decodeErrors  = decodeErrors - panel.id
                        val bmp = withContext(Dispatchers.IO) {
                            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                        }
                        if (bmp == null) {
                            decodeErrors  = decodeErrors + (panel.id to "Could not read image.")
                            decodingPanel = null
                            return@launch
                        }
                        val content = withContext(Dispatchers.Default) { decodeQr(bmp) }
                        if (content != null) {
                            draftQrContents = draftQrContents + (panel.id to content)
                        } else {
                            decodeErrors = decodeErrors + (panel.id to
                                "No QR code found. Upload the original app screenshot — not a photo of a screen.")
                        }
                        decodingPanel = null
                    }
                }
            }
        }

    var liveQr by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(draft) {
        liveQr = if (listOf(draft.fullName, draft.jobTitle, draft.company, draft.phoneMobile,
                draft.phoneWork, draft.email, draft.website, draft.linkedinUrl).all { it.isBlank() }) {
            null
        } else {
            withContext(Dispatchers.Default) { generateQr(VCardSerializer.serialize(draft)) }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, containerColor = Background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Copper))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = TextSecondary)
                }
                Text("EDIT PROFILE", style = MaterialTheme.typography.titleMedium, color = Copper,
                    modifier = Modifier.padding(start = 8.dp))
            }

            val scrollMod = if (panels.size > 3)
                Modifier.fillMaxWidth().padding(horizontal = 24.dp).horizontalScroll(rememberScrollState())
            else
                Modifier.fillMaxWidth().padding(horizontal = 24.dp)

            Row(modifier = scrollMod, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                panels.forEach { panel ->
                    val w = if (panels.size <= 3) Modifier.weight(1f) else Modifier.width(90.dp)
                    EditorTab(modifier = w, label = panel.label, icon = panelIcon(panel.id),
                        selected = activeId == panel.id, onClick = { activeId = panel.id })
                }
            }

            Spacer(Modifier.height(20.dp))

            val activePanel = panels.first { it.id == activeId }

            if (activeId == "contact") {
                ContactTab(
                    draft         = draft,
                    onDraftChange = { draft = it },
                    liveQr        = liveQr,
                    onScanClick   = onScanMyCard,
                    onSave        = {
                        onSaveContact(draft)
                        scope.launch { snackbar.showSnackbar("Contact saved.") }
                    }
                )
            } else {
                val picker = imagePickers[activeId]
                QrContentTab(
                    panelLabel   = activePanel.label,
                    hint         = qrHint(activePanel),
                    packageName  = knownPackage(activePanel.id),
                    qrContent    = draftQrContents[activeId] ?: "",
                    isDecoding   = decodingPanel == activeId,
                    decodeError  = decodeErrors[activeId],
                    onPickImage  = { picker?.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    onSave = {
                        val content = draftQrContents[activeId] ?: return@QrContentTab
                        onSaveQrContent(activeId, content)
                        scope.launch { snackbar.showSnackbar("${activePanel.label} QR saved.") }
                    },
                    context = context
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun EditorTab(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) CopperContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (selected) Copper else TextTertiary, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.titleSmall,
            color = if (selected) Copper else TextTertiary, fontSize = 11.sp)
    }
}

@Composable
private fun ContactTab(
    draft: ContactDraft,
    onDraftChange: (ContactDraft) -> Unit,
    liveQr: Bitmap?,
    onScanClick: () -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CopperContainer)
                .border(1.dp, Copper.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                .clickable(onClick = onScanClick)
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(Icons.Outlined.CameraAlt, null, tint = Copper, modifier = Modifier.size(28.dp))
            Column {
                Text("SCAN MY OWN CARD", style = MaterialTheme.typography.titleMedium, color = Copper)
                Spacer(Modifier.height(2.dp))
                Text("Point camera at your own card · review & assign fields",
                    style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }

        HorizontalDivider(color = Divider)

        LuxuryField(draft.fullName,    { onDraftChange(draft.copy(fullName    = it)) }, "Full name",    Icons.Outlined.PersonOutline)
        LuxuryField(draft.jobTitle,    { onDraftChange(draft.copy(jobTitle    = it)) }, "Job title",    Icons.Outlined.WorkOutline)
        LuxuryField(draft.company,     { onDraftChange(draft.copy(company     = it)) }, "Company",      Icons.Outlined.Business)
        LuxuryField(draft.phoneMobile, { onDraftChange(draft.copy(phoneMobile = it)) }, "Mobile (private)", Icons.Outlined.PhoneAndroid, KeyboardType.Phone)
        LuxuryField(draft.phoneWork,   { onDraftChange(draft.copy(phoneWork   = it)) }, "Phone (work)",     Icons.Outlined.Phone,        KeyboardType.Phone)
        LuxuryField(draft.email,       { onDraftChange(draft.copy(email       = it)) }, "Email",        Icons.Outlined.MailOutline,  KeyboardType.Email)
        LuxuryField(draft.website,     { onDraftChange(draft.copy(website     = it)) }, "Website",      Icons.Outlined.Language,     KeyboardType.Uri)
        LuxuryField(draft.linkedinUrl, { onDraftChange(draft.copy(linkedinUrl = it)) }, "LinkedIn URL", Icons.Outlined.WorkOutline,  KeyboardType.Uri)

        OutlinedTextField(
            value = draft.address, onValueChange = { onDraftChange(draft.copy(address = it)) },
            label = { Text("Address", color = TextTertiary) },
            leadingIcon = { Icon(Icons.Outlined.LocationOn, null, tint = TextTertiary) },
            modifier = Modifier.fillMaxWidth(), minLines = 2, colors = luxuryFieldColors()
        )

        HorizontalDivider(color = Divider)

        if (liveQr != null) {
            Text("PREVIEW", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
            Image(bitmap = liveQr.asImageBitmap(), contentDescription = "Live QR",
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(Color.White).padding(10.dp),
                contentScale = ContentScale.FillWidth)
        } else {
            EmptyQrState("Add name, phone or email", "A live QR preview will appear here.")
        }

        Button(onClick = onSave, modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Copper, contentColor = Background)
        ) {
            Icon(Icons.Outlined.Save, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text("SAVE CONTACT", style = MaterialTheme.typography.labelLarge, letterSpacing = 2.sp)
        }
    }
}

@Composable
private fun QrContentTab(
    panelLabel: String,
    hint: String,
    packageName: String?,
    qrContent: String,
    isDecoding: Boolean,
    decodeError: String?,
    onPickImage: () -> Unit,
    onSave: () -> Unit,
    context: Context
) {
    // Generate preview bitmap internally from the decoded content string
    var previewBitmap by remember(qrContent) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(qrContent) {
        previewBitmap = if (qrContent.isNotBlank()) {
            withContext(Dispatchers.Default) { generateQr(qrContent) }
        } else null
    }

    Column(
        modifier = Modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(hint, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (packageName != null) {
                OutlinedButton(
                    onClick = {
                        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                            ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                            ?: Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl(packageName)))
                                .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                        context.startActivity(intent)
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Copper.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Copper)
                ) { Text("OPEN APP", style = MaterialTheme.typography.labelMedium, letterSpacing = 1.sp) }
            }
            Button(
                onClick = onPickImage, enabled = !isDecoding,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CopperContainer, contentColor = Copper)
            ) {
                if (isDecoding) {
                    CircularProgressIndicator(color = Copper, strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp))
                } else {
                    Icon(Icons.Outlined.Image, null, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(6.dp))
                Text(if (isDecoding) "READING…" else "UPLOAD QR",
                    style = MaterialTheme.typography.labelMedium, letterSpacing = 1.sp)
            }
        }

        when {
            decodeError != null -> {
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(SurfaceElevated).padding(20.dp)) {
                    Text("⚠ $decodeError",
                        style = MaterialTheme.typography.bodySmall, color = ErrorRed,
                        textAlign = TextAlign.Center)
                }
            }
            previewBitmap != null -> {
                Text("QR READY — tap SAVE to keep it", style = MaterialTheme.typography.labelSmall,
                    color = Copper)
                Image(bitmap = previewBitmap!!.asImageBitmap(),
                    contentDescription = "$panelLabel QR",
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(Color.White).padding(10.dp),
                    contentScale = ContentScale.FillWidth)
            }
            else -> EmptyQrState("No QR uploaded yet",
                "Upload a screenshot from $panelLabel · QR is decoded and regenerated internally.")
        }

        Button(
            onClick = onSave, enabled = qrContent.isNotBlank() && !isDecoding,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Copper, contentColor = Background)
        ) {
            Icon(Icons.Outlined.Save, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text("SAVE $panelLabel", style = MaterialTheme.typography.labelLarge, letterSpacing = 2.sp)
        }
    }
}

@Composable
private fun LuxuryField(
    value: String, onValueChange: (String) -> Unit,
    label: String, icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(value = value, onValueChange = onValueChange,
        label = { Text(label, color = TextTertiary) },
        leadingIcon = { Icon(icon, null, tint = TextTertiary) },
        modifier = Modifier.fillMaxWidth(), singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = luxuryFieldColors())
}

@Composable
private fun luxuryFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = Copper, unfocusedBorderColor    = Border,
    focusedLabelColor       = Copper, unfocusedLabelColor     = TextTertiary,
    cursorColor             = Copper, focusedLeadingIconColor = Copper,
    focusedTextColor        = TextPrimary, unfocusedTextColor  = TextPrimary
)

private fun qrHint(panel: PanelConfig) = when (panel.id) {
    "linkedin" -> "Open LinkedIn → QR code (top-right of profile) → screenshot, upload here."
    "whatsapp" -> "Open WhatsApp → Settings → QR code → screenshot, upload here."
    else       -> "Open ${panel.label}, find your QR code, screenshot it, upload here."
}

private fun knownPackage(id: String) = when (id) {
    "linkedin" -> "com.linkedin.android"
    "whatsapp" -> "com.whatsapp"
    else       -> null
}

private fun fallbackUrl(packageName: String) = when (packageName) {
    "com.linkedin.android" -> "https://www.linkedin.com"
    "com.whatsapp"         -> "https://wa.me"
    else                   -> "https://play.google.com/store/apps/details?id=$packageName"
}

// Decodes a QR code from a Bitmap. Tries HybridBinarizer first, then
// GlobalHistogramBinarizer as fallback, with TRY_HARDER hint on both passes.
internal fun decodeQr(bitmap: Bitmap): String? {
    val width  = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    val source = RGBLuminanceSource(width, height, pixels)
    val hints  = mapOf(com.google.zxing.DecodeHintType.TRY_HARDER to true)

    for (binarizer in listOf(HybridBinarizer(source), GlobalHistogramBinarizer(source))) {
        try {
            return MultiFormatReader().apply { setHints(hints) }
                .decode(BinaryBitmap(binarizer)).text
        } catch (_: NotFoundException) {}
    }
    return null
}
