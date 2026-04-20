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
import androidx.compose.material.icons.automirrored.outlined.Chat
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
import androidx.compose.ui.text.font.FontWeight
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
import com.antigravity.contactnetworkingpro.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

private enum class EditorTab { CONTACT, LINKEDIN, WHATSAPP }

@Composable
fun ContactEditorScreen(
    initialContact: ContactDraft,
    savedLinkedinUri: String,
    savedWhatsappUri: String,
    onSaveContact: (ContactDraft) -> Unit,
    onSaveLinkedin: (String) -> Unit,
    onSaveWhatsapp: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var activeTab by rememberSaveable { mutableStateOf(EditorTab.CONTACT) }
    var draft    by rememberSaveable(stateSaver = ContactDraftSaver) { mutableStateOf(initialContact) }
    var linkedinDraft by rememberSaveable { mutableStateOf(savedLinkedinUri) }
    var whatsappDraft by rememberSaveable { mutableStateOf(savedWhatsappUri) }

    var isScanning by remember { mutableStateOf(false) }
    var scanError  by remember { mutableStateOf<String?>(null) }

    // Camera for business card scanning
    val cameraUri = remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            val uri = cameraUri.value ?: return@rememberLauncherForActivityResult
            scope.launch {
                isScanning = true
                scanError  = null
                val bmp = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                if (bmp == null) { scanError = "Could not read photo."; isScanning = false; return@launch }
                ClaudeVisionClient.scanBusinessCard(bmp, BuildConfig.ANTHROPIC_API_KEY)
                    .onSuccess { result ->
                        draft = result
                        snackbar.showSnackbar("Card scanned — review and save.")
                    }
                    .onFailure { err ->
                        scanError = err.message ?: "Scan failed."
                        snackbar.showSnackbar("Scan failed: ${err.message}")
                    }
                isScanning = false
            }
        }
    }

    // Image pickers for LinkedIn / WhatsApp
    val linkedinPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) linkedinDraft = uri.toString()
    }
    val whatsappPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) whatsappDraft = uri.toString()
    }

    val liveQr = remember(draft) {
        if (draft.fullName.isBlank() && draft.phone.isBlank() && draft.email.isBlank()) null
        else generateQr(buildVCard(draft))
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = Background
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Copper top bar
                Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Copper))

                // Top nav
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = TextSecondary)
                    }
                    Text(
                        text = "YOUR IDENTITY",
                        style = MaterialTheme.typography.titleMedium,
                        color = Copper,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                // Tab selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    listOf(
                        Triple(EditorTab.CONTACT,  "CONTACT",  Icons.Outlined.ContactPhone),
                        Triple(EditorTab.LINKEDIN, "LINKEDIN", Icons.Outlined.WorkOutline),
                        Triple(EditorTab.WHATSAPP, "WHATSAPP", Icons.AutoMirrored.Outlined.Chat)
                    ).forEach { (tab, label, icon) ->
                        val sel = activeTab == tab
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (sel) CopperContainer else Color.Transparent)
                                .clickable { activeTab = tab }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(icon, null, tint = if (sel) Copper else TextTertiary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(label, style = MaterialTheme.typography.titleSmall,
                                color = if (sel) Copper else TextTertiary, fontSize = 9.sp)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                when (activeTab) {
                    EditorTab.CONTACT -> ContactTab(
                        draft = draft,
                        onDraftChange = { draft = it },
                        liveQr = liveQr,
                        isScanning = isScanning,
                        scanError = scanError,
                        onScanClick = {
                            val file = File(context.cacheDir, "camera_photos").also { it.mkdirs() }
                                .let { File(it, "business_card.jpg") }
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            cameraUri.value = uri
                            cameraLauncher.launch(uri)
                        },
                        onSave = {
                            onSaveContact(draft)
                            scope.launch { snackbar.showSnackbar("Contact saved.") }
                        }
                    )
                    EditorTab.LINKEDIN -> QrImageTab(
                        title = "LinkedIn QR",
                        hint = "Open LinkedIn → Your QR code → screenshot it, then upload.",
                        packageName = "com.linkedin.android",
                        draftUri = linkedinDraft,
                        onPickImage = { linkedinPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        onSave = {
                            onSaveLinkedin(linkedinDraft)
                            scope.launch { snackbar.showSnackbar("LinkedIn QR saved.") }
                        },
                        context = context
                    )
                    EditorTab.WHATSAPP -> QrImageTab(
                        title = "WhatsApp QR",
                        hint = "Open WhatsApp → Settings → QR code → screenshot it, then upload.",
                        packageName = "com.whatsapp",
                        draftUri = whatsappDraft,
                        onPickImage = { whatsappPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        onSave = {
                            onSaveWhatsapp(whatsappDraft)
                            scope.launch { snackbar.showSnackbar("WhatsApp QR saved.") }
                        },
                        context = context
                    )
                }

                Spacer(Modifier.height(32.dp))
            }

            // Scanning overlay
            AnimatedVisibility(
                visible = isScanning,
                enter = fadeIn(), exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Background.copy(alpha = 0.94f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        CircularProgressIndicator(color = Copper, strokeWidth = 3.dp, modifier = Modifier.size(48.dp))
                        Text("READING CARD", style = MaterialTheme.typography.titleLarge, color = Copper)
                        Text(
                            "Claude is extracting your\ncontact details…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
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
        // Scan business card CTA
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
            Icon(Icons.Outlined.CameraAlt, contentDescription = null, tint = Copper, modifier = Modifier.size(28.dp))
            Column {
                Text("SCAN BUSINESS CARD", style = MaterialTheme.typography.titleMedium, color = Copper)
                Spacer(Modifier.height(2.dp))
                Text(
                    "Point camera at a card · AI fills the form",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        if (scanError != null) {
            Text("⚠ $scanError", style = MaterialTheme.typography.bodySmall, color = ErrorRed)
        }

        HorizontalDivider(color = Divider)

        // Form fields
        LuxuryField(draft.fullName,    { onDraftChange(draft.copy(fullName    = it)) }, "Full name",     Icons.Outlined.PersonOutline)
        LuxuryField(draft.jobTitle,    { onDraftChange(draft.copy(jobTitle    = it)) }, "Job title",     Icons.Outlined.WorkOutline)
        LuxuryField(draft.company,     { onDraftChange(draft.copy(company     = it)) }, "Company",       Icons.Outlined.Business)
        LuxuryField(draft.phone,       { onDraftChange(draft.copy(phone       = it)) }, "Phone",         Icons.Outlined.PhoneAndroid,  KeyboardType.Phone)
        LuxuryField(draft.email,       { onDraftChange(draft.copy(email       = it)) }, "Email",         Icons.Outlined.MailOutline,   KeyboardType.Email)
        LuxuryField(draft.website,     { onDraftChange(draft.copy(website     = it)) }, "Website",       Icons.Outlined.Language,      KeyboardType.Uri)
        LuxuryField(draft.linkedinUrl, { onDraftChange(draft.copy(linkedinUrl = it)) }, "LinkedIn URL",  Icons.Outlined.WorkOutline,   KeyboardType.Uri)

        OutlinedTextField(
            value = draft.address,
            onValueChange = { onDraftChange(draft.copy(address = it)) },
            label = { Text("Address", color = TextTertiary) },
            leadingIcon = { Icon(Icons.Outlined.LocationOn, null, tint = TextTertiary) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            colors = luxuryFieldColors()
        )

        HorizontalDivider(color = Divider)

        // Live QR preview
        if (liveQr != null) {
            Text("PREVIEW", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
            Image(
                bitmap = liveQr.asImageBitmap(),
                contentDescription = "Live QR preview",
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(10.dp),
                contentScale = ContentScale.FillWidth
            )
        } else {
            EmptyQrState("Add name, phone or email", "A live QR preview will appear here.")
        }

        // Save button
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
    title: String,
    hint: String,
    packageName: String,
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
            EmptyQrState("No QR uploaded yet", "Upload a screenshot of your $title.")
        } else {
            Image(
                painter = rememberAsyncImagePainter(draftUri),
                contentDescription = "$title preview",
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.FillWidth
            )
        }

        Button(
            onClick = onSave,
            enabled = draftUri.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Copper, contentColor = TextPrimary)
        ) {
            Icon(Icons.Outlined.Save, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text("SAVE $title".uppercase(), style = MaterialTheme.typography.labelLarge, letterSpacing = 2.sp)
        }
    }
}

@Composable
private fun LuxuryField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextTertiary) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = TextTertiary) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
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
