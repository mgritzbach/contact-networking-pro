package com.antigravity.contactnetworkingpro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.content.ComponentName
import android.service.quicksettings.TileService
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.antigravity.contactnetworkingpro.api.CardScanner
import com.antigravity.contactnetworkingpro.api.ScanResult
import com.antigravity.contactnetworkingpro.data.ContactStorage
import com.antigravity.contactnetworkingpro.model.ContactDraft
import com.antigravity.contactnetworkingpro.model.buildPanelList
import com.antigravity.contactnetworkingpro.ui.*
import com.antigravity.contactnetworkingpro.ui.theme.*
import com.antigravity.contactnetworkingpro.widget.QrWidgetProvider
import kotlinx.coroutines.launch
import java.io.File

private sealed class Screen {
    object Intro  : Screen()
    object Home   : Screen()
    object Editor : Screen()
    object Settings : Screen()
    class ScanReview(val result: ScanResult, val forOwnProfile: Boolean) : Screen()
}

private enum class ScanMode { ForOther, ForOwnProfile }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ContactNetworkingProTheme { ContactNetworkingApp() }
        }
    }
}

@Composable
private fun ContactNetworkingApp() {
    val context  = LocalContext.current
    val storage  = remember { ContactStorage(context) }
    val scope    = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var screen by remember {
        mutableStateOf<Screen>(if (storage.isSetupComplete()) Screen.Home else Screen.Intro)
    }

    var panels by remember {
        mutableStateOf(buildPanelList(storage.getPanelCount(),
            listOf(storage.getCustomPanelName(0), storage.getCustomPanelName(1))))
    }

    var savedContact by remember { mutableStateOf(storage.loadContact()) }
    var quickAccessPanelId by remember { mutableStateOf(storage.getQuickAccessPanelId()) }
    var editorPrefill by remember { mutableStateOf<ContactDraft?>(null) }
    var savedQrContents by remember {
        mutableStateOf(panels.filter { it.id != "contact" }
            .associate { it.id to storage.loadPanelQrContent(it.id) })
    }

    // ── Camera / scan state ────────────────────────────────────────────────────
    var isScanning    by remember { mutableStateOf(false) }
    var scanMode      by remember { mutableStateOf(ScanMode.ForOther) }
    val scanCameraUri = remember { mutableStateOf<Uri?>(null) }
    val scanCameraFile = remember { mutableStateOf<File?>(null) }
    var triggerCamera by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) triggerCamera = true
        else scope.launch { snackbar.showSnackbar("Camera permission required to scan cards.") }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (!success) {
            scanCameraFile.value?.delete()
            scanCameraFile.value = null
            return@rememberLauncherForActivityResult
        }
        val uri = scanCameraUri.value ?: return@rememberLauncherForActivityResult
        scope.launch {
            isScanning = true
            val bmp = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            if (bmp == null) {
                isScanning = false
                scanCameraFile.value?.delete()
                scanCameraFile.value = null
                snackbar.showSnackbar("Could not read photo.")
                return@launch
            }
            val result = CardScanner.scanBusinessCard(bmp)
            scanCameraFile.value?.delete()
            scanCameraFile.value = null
            result
                .onSuccess { result ->
                    isScanning = false
                    screen = Screen.ScanReview(result, forOwnProfile = scanMode == ScanMode.ForOwnProfile)
                }
                .onFailure { err ->
                    isScanning = false
                    snackbar.showSnackbar("Scan failed: ${err.message}")
                }
        }
    }

    LaunchedEffect(triggerCamera) {
        if (triggerCamera) {
            triggerCamera = false
            val file = File(context.cacheDir, "camera_photos").also { it.mkdirs() }
                .let { File(it, "card_scan.jpg") }
            scanCameraFile.value = file
            scanCameraUri.value = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file)
            cameraLauncher.launch(scanCameraUri.value!!)
        }
    }

    fun launchCamera(mode: ScanMode) {
        scanMode = mode
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            val file = File(context.cacheDir, "camera_photos").also { it.mkdirs() }
                .let { File(it, "card_scan.jpg") }
            scanCameraFile.value = file
            scanCameraUri.value = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file)
            cameraLauncher.launch(scanCameraUri.value!!)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun refreshQuickAccess() {
        QrWidgetProvider.updateAll(context)
        TileService.requestListeningState(context, ComponentName(context, QrTileService::class.java))
    }

    BackHandler(enabled = screen != Screen.Home && screen != Screen.Intro) {
        editorPrefill = null
        screen = when (screen) {
            is Screen.ScanReview -> if ((screen as Screen.ScanReview).forOwnProfile) Screen.Editor else Screen.Home
            else -> Screen.Home
        }
    }

    // ── Navigation ─────────────────────────────────────────────────────────────
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, containerColor = Background) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {

            when (val s = screen) {
                Screen.Intro -> IntroScreen(onComplete = {
                    val count = 3
                    val names = listOf("", "")
                    storage.savePanelCount(count)
                    storage.markSetupComplete()
                    panels = buildPanelList(count, names)
                    savedQrContents = panels.filter { it.id != "contact" }
                        .associate { it.id to storage.loadPanelQrContent(it.id) }
                    screen = Screen.Editor
                })

                Screen.Home -> HomeScreen(
                    panels          = panels,
                    savedContact    = savedContact,
                    savedQrContents = savedQrContents,
                    onEditClick     = { screen = Screen.Editor },
                    onScanCardClick = { launchCamera(ScanMode.ForOther) },
                    onSettingsClick = { screen = Screen.Settings }
                )

                Screen.Settings -> SettingsScreen(
                    panelCount = storage.getPanelCount(),
                    customNames = listOf(storage.getCustomPanelName(0), storage.getCustomPanelName(1)),
                    panels = panels,
                    quickAccessPanelId = quickAccessPanelId,
                    onSavePanels = { count, names ->
                        storage.savePanelCount(count)
                        names.forEachIndexed { index, name -> storage.saveCustomPanelName(index, name) }
                        panels = buildPanelList(count, names)
                        if (panels.none { it.id == quickAccessPanelId }) {
                            quickAccessPanelId = "contact"
                            storage.saveQuickAccessPanelId("contact")
                        }
                        savedQrContents = panels.filter { it.id != "contact" }
                            .associate { it.id to storage.loadPanelQrContent(it.id) }
                        refreshQuickAccess()
                    },
                    onQuickAccessPanelSelected = { panelId ->
                        quickAccessPanelId = panelId
                        storage.saveQuickAccessPanelId(panelId)
                        refreshQuickAccess()
                    },
                    onResetData = {
                        storage.clearAll()
                        savedContact = ContactDraft()
                        editorPrefill = null
                        quickAccessPanelId = "contact"
                        panels = buildPanelList(3, listOf("", ""))
                        savedQrContents = panels.filter { it.id != "contact" }.associate { it.id to "" }
                        refreshQuickAccess()
                        screen = Screen.Intro
                    },
                    onBack = { screen = Screen.Home }
                )

                Screen.Editor -> ContactEditorScreen(
                    panels          = panels,
                    initialContact  = editorPrefill ?: savedContact,
                    savedQrContents = savedQrContents,
                    onSaveContact   = { draft ->
                        savedContact  = draft
                        editorPrefill = null
                        storage.saveContact(draft)
                        refreshQuickAccess()
                    },
                    onSaveQrContent = { panelId, content ->
                        savedQrContents = savedQrContents + (panelId to content)
                        storage.savePanelQrContent(panelId, content)
                        refreshQuickAccess()
                    },
                    onScanMyCard    = { launchCamera(ScanMode.ForOwnProfile) },
                    onBack          = { editorPrefill = null; screen = Screen.Home }
                )

                is Screen.ScanReview -> ScanReviewScreen(
                    result         = s.result,
                    forOwnProfile  = s.forOwnProfile,
                    onConfirm      = { confirmed ->
                        if (s.forOwnProfile) {
                            editorPrefill = confirmed
                            screen = Screen.Editor
                        } else {
                            screen = Screen.Home
                            launchAddContactIntent(context, confirmed)
                        }
                    },
                    onBack = { screen = if (s.forOwnProfile) Screen.Editor else Screen.Home }
                )
            }

            // Scanning overlay
            AnimatedVisibility(visible = isScanning, enter = fadeIn(), exit = fadeOut(),
                modifier = Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().background(Background.copy(alpha = 0.94f)),
                    contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        CircularProgressIndicator(color = Copper, strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp))
                        Text("READING CARD", style = MaterialTheme.typography.titleLarge, color = Copper)
                        Text("Scanning with on-device AI…",
                            style = MaterialTheme.typography.bodyMedium, color = TextSecondary,
                            textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

private fun launchAddContactIntent(context: android.content.Context, d: ContactDraft) {
    val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
        type = ContactsContract.RawContacts.CONTENT_TYPE
        if (d.fullName.isNotBlank())    putExtra(ContactsContract.Intents.Insert.NAME,      d.fullName)
        if (d.company.isNotBlank())     putExtra(ContactsContract.Intents.Insert.COMPANY,   d.company)
        if (d.jobTitle.isNotBlank())    putExtra(ContactsContract.Intents.Insert.JOB_TITLE, d.jobTitle)
        if (d.email.isNotBlank())       putExtra(ContactsContract.Intents.Insert.EMAIL,     d.email)
        val phone = d.phoneMobile.ifBlank { d.phoneWork }
        if (phone.isNotBlank()) {
            putExtra(ContactsContract.Intents.Insert.PHONE, phone)
            putExtra(ContactsContract.Intents.Insert.PHONE_TYPE,
                ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
        }
        if (d.phoneWork.isNotBlank() && d.phoneMobile.isNotBlank()) {
            putExtra(ContactsContract.Intents.Insert.SECONDARY_PHONE, d.phoneWork)
            putExtra(ContactsContract.Intents.Insert.SECONDARY_PHONE_TYPE,
                ContactsContract.CommonDataKinds.Phone.TYPE_WORK)
        }
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
