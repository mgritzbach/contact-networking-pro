package com.antigravity.contactnetworkingpro.ui

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.antigravity.contactnetworkingpro.BuildConfig
import com.antigravity.contactnetworkingpro.api.ClaudeVisionClient
import com.antigravity.contactnetworkingpro.model.ContactDraft
import com.antigravity.contactnetworkingpro.model.ContactDraftSaver
import com.antigravity.contactnetworkingpro.model.PanelConfig
import com.antigravity.contactnetworkingpro.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ContactEditorScreen(
    panels: List<PanelConfig>,
    initialContact: ContactDraft,
    savedUris: Map<String, String>,
    onSaveContact: (ContactDraft) -> Unit,
    onSaveUri: (panelId: String, uri: String) -> Unit,
    onBack: () -> Unit
) {
    val context  = LocalContext.current
    val scope    = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var activeId    by rememberSaveable { mutableStateOf(panels.first().id) }
    var draft       by rememberSaveable(stateSaver = ContactDraftSaver) { mutableStateOf(initialContact) }
    var draftUris   by remember { mutableStateOf(savedUris.toMutableMap()) }

    var isScanning  by remember { mutableStateOf(false) }
    var scanError   by remember { mutableStateOf<String?>(null) }

    // Camera launcher
    val cameraUri = remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            val uri = cameraUri.value ?: return@rememberLauncherForActivityResult
            scope.launch {
                isScanning = true; scanError = null
                val bmp = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                if (bmp == null) { scanError = "Could not read photo."; isScanning = false; return@launch }
                ClaudeVisionClient.scanBusinessCard(bmp, BuildConfig.ANTHROPIC_API_KEY)
                    .onSuccess  { result -> draft = result; snackbar.showSnackbar("Card scanned — review and save.") }
                    .onFailure  { err -> scanError = err.message; snackbar.showSnackbar("Scan failed: ${err.message}") }
                isScanning = false
            }
        }
    }

    // Image pickers — one per non-contact panel
    val imagePickers = panels
        .filter { it.id != "contact" }
        .associate { panel ->
            panel.id to rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                if (uri != null) draftUris = (draftUris + (panel.id to uri.toString())).toMutableMap()
            }
        }

    val liveQr = remember(draft) {
        if (draft.fullName.isBlank() && draft.phone.isBlank() && draft.email.isBlank()) null
        else generateQr(buildVCard(draft))
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, containerColor = Background) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Copper))

                // Top nav
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = TextSecondary)
                    }
                    Text("YOUR IDENTITY", style = MaterialTheme.typography.titleMedium, color = Copper,
                        modifier = Modifier.padding(start = 8.dp))
                }

                // Tab row — scrollable if > 3 panels
                val scrollMod = if (panels.size > 3)
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp).horizontalScroll(rememberScrollState())
                else
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp)

                Row(modifier = scrollMod, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    panels.forEach { panel ->
                        val w = if (panels.size <= 3) Modifier.weight(1f) else Modifier.width(90.dp)
                        EditorTab(
                            modifier = w,
                            label    = panel.label,
                            icon     = panelIcon(panel.id),
                            selected = activeId == panel.id,
                            onClick  = { activeId = panel.id }
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                val activePanel = panels.first { it.id == activeId }

                if (activeId == "contact") {
                    ContactTab(
                        draft = draft,
                        onDraftChange = { draft = it },
                        liveQr = liveQr,
                        isScanning = isScanning,
                        scanError = scanError,
                        onScanClick = {
                            val file = File(context.cacheDir, "camera_photos").also { it.mkdirs() }
                                .let { File(it, "business_card.jpg") }
                            cameraUri.value = FileProvider.getUriForFile(
                                context, "${context.packageName}.fileprovider", file)
                            cameraLauncher.launch(cameraUri.value!!)
                        },
                        onSave = {
                            onSaveContact(draft)
                            scope.launch { snackbar.showSnackbar("Contact saved.") }
                        }
                    )
                } else {
                    val picker = imagePickers[activeId]
                    QrImageTab(
                        panelLabel = activePanel.label,
                        hint       = qrHint(activePanel),
                        packageName = knownPackage(activePanel.id),
                        draftUri   = draftUris[activeId] ?: "",
                        onPickImage = { picker?.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        onSave = {
                            val uri = draftUris[activeId] ?: return@QrImageTab
                            onSaveUri(activeId, uri)
                            scope.launch { snackbar.showSnackbar("${activePanel.label} QR saved.") }
                        },
                        context = context
                    )
                }

                Spacer(Modifier.height(32.dp))
            }

            // Scanning overlay
            AnimatedVisibility(visible = isScanning, enter = fadeIn(), exit = fadeOut(),
                modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Background.copy(alpha = 0.94f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        CircularProgressIndicator(color = Copper, strokeWidth = 3.dp, modifier = Modifier.size(48.dp))
                        Text("READING CARD", style = MaterialTheme.typography.titleLarge, color = Copper)
                        Text("Claude is extracting your\ncontact details…",
                            style = MaterialTheme.typography.bodyMedium, color = TextSecondary,
                            textAlign = TextAlign.Center)
                    }
                }
            }
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
            color = if (selected) Copper else TextTertiary, fontSize = 9.sp)
    }
}

@Composable
private fun ContactTab(
    draft: ContactDraft,
    onDraftChange: (ContactDraft) -> Unit,
    liveQr: android.graphics.Bitmap?,
    isScanning: Boolean,
    scanError: String?,
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
                .clickable(enabled = !isScanning, onClick = onScanClick)
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(Icons.Outlined.CameraAlt, null, tint = Copper, modifier = Modifier.size(28.dp))
            Column {
                Text("SCAN BUSINESS CARD", style = MaterialTheme.typography.titleMedium, color = Copper)
                Spacer(Modifier.height(2.dp))
                Text("Point camera at a card · AI fills the form",
                    style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }

        if (scanError != null)
            Text("⚠ $scanError", style = MaterialTheme.typography.bodySmall, color = ErrorRed)

        HorizontalDivider(color = Divider)

        LuxuryField(draft.fullName,    { onDraftChange(draft.copy(fullName    = it)) }, "Full name",    Icons.Outlined.PersonOutline)
        LuxuryField(draft.jobTitle,    { onDraftChange(draft.copy(jobTitle    = it)) }, "Job title",    Icons.Outlined.WorkOutline)
        LuxuryField(draft.company,     { onDraftChange(draft.copy(company     = it)) }, "Company",      Icons.Outlined.Business)
        LuxuryField(draft.phone,       { onDraftChange(draft.copy(phone       = it)) }, "Phone",        Icons.Outlined.PhoneAndroid, KeyboardType.Phone)
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
            androidx.compose.foundation.Image(
                bitmap = liveQr.asImageBitmap(),
                contentDescription = "Live QR",
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(Color.White).padding(10.dp),
                contentScale = ContentScale.FillWidth
            )
        } else EmptyQrState("Add name, phone or email", "A live QR preview will appear here.")

        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Copper, contentColor = TextPrimary)
        ) {
            Icon(Icons.Outlined.Save, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text("SAVE CONTACT", style = MaterialTheme.typography.labelLarge, letterSpacing = 2.sp)
        }
    }
}

@Composable
private fun QrImageTab(
    panelLabel: String,
    hint: String,
    packageName: String?,
    draftUri: String,
    onPickImage: () -> Unit,
    onSave: () -> Unit,
    context: Context
) {
    Column(
        modifier = Modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(hint, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (packageName != null) {
                OutlinedButton(
                    onClick = {
                        context.packageManager.getLaunchIntentForPackage(packageName)
                            ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                            ?.let { context.startActivity(it) }
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Copper.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Copper)
                ) { Text("OPEN APP", style = MaterialTheme.typography.labelMedium, letterSpacing = 1.sp) }
            }
            Button(
                onClick = onPickImage,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CopperContainer, contentColor = Copper)
            ) {
                Icon(Icons.Outlined.Image, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("UPLOAD QR", style = MaterialTheme.typography.labelMedium, letterSpacing = 1.sp)
            }
        }

        if (draftUri.isBlank()) {
            EmptyQrState("No QR uploaded yet", "Upload a screenshot of your $panelLabel QR.")
        } else {
            androidx.compose.foundation.Image(
                painter = rememberAsyncImagePainter(draftUri),
                contentDescription = "$panelLabel QR",
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.FillWidth
            )
        }

        Button(
            onClick = onSave, enabled = draftUri.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Copper, contentColor = TextPrimary)
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
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label, color = TextTertiary) },
        leadingIcon = { Icon(icon, null, tint = TextTertiary) },
        modifier = Modifier.fillMaxWidth(), singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = luxuryFieldColors()
    )
}

@Composable
private fun luxuryFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = Copper,
    unfocusedBorderColor    = Border,
    focusedLabelColor       = Copper,
    unfocusedLabelColor     = TextTertiary,
    cursorColor             = Copper,
    focusedLeadingIconColor = Copper,
    focusedTextColor        = TextPrimary,
    unfocusedTextColor      = TextPrimary
)

private fun qrHint(panel: PanelConfig) = when (panel.id) {
    "linkedin" -> "Open LinkedIn → Your QR code → screenshot, then upload here."
    "whatsapp" -> "Open WhatsApp → Settings → QR code → screenshot, then upload here."
    else       -> "Open ${panel.label}, find your QR code, screenshot it, then upload here."
}

private fun knownPackage(id: String) = when (id) {
    "linkedin" -> "com.linkedin.android"
    "whatsapp" -> "com.whatsapp"
    else       -> null
}
