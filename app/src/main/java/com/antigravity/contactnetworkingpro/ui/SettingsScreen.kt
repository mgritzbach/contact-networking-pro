package com.antigravity.contactnetworkingpro.ui

import android.app.StatusBarManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.AddToHomeScreen
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.antigravity.contactnetworkingpro.BuildConfig
import com.antigravity.contactnetworkingpro.QrTileService
import com.antigravity.contactnetworkingpro.R
import com.antigravity.contactnetworkingpro.model.PanelConfig
import com.antigravity.contactnetworkingpro.ui.theme.*
import com.antigravity.contactnetworkingpro.widget.QrWidgetProvider

private const val PRIVACY_URL = "https://mgritzbach.github.io/contact-networking-pro/privacy.html"

@Composable
fun SettingsScreen(
    panelCount: Int,
    customNames: List<String>,
    panels: List<PanelConfig>,
    quickAccessPanelId: String,
    onSavePanels: (Int, List<String>) -> Unit,
    onQuickAccessPanelSelected: (String) -> Unit,
    onResetData: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedCount by rememberSaveable(panelCount) { mutableIntStateOf(panelCount) }
    var customName0 by rememberSaveable(customNames) { mutableStateOf(customNames.getOrElse(0) { "" }) }
    var customName1 by rememberSaveable(customNames) { mutableStateOf(customNames.getOrElse(1) { "" }) }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    var layoutSaved by rememberSaveable { mutableStateOf(false) }

    Scaffold(containerColor = Background) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(bottom = 36.dp)) {
            Box(Modifier.fillMaxWidth().height(2.dp).background(Copper))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = TextSecondary)
                }
                Column(Modifier.padding(start = 8.dp)) {
                    Text("SETTINGS", style = MaterialTheme.typography.titleMedium, color = Copper)
                    Text("Privacy, sharing and quick access", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }

            Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                PrivacyPromiseCard()

                SettingsSection("SHARING PANELS") {
                    Text("Contact, LinkedIn and WhatsApp are included. Add up to two custom QR panels.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        (3..5).forEach { count ->
                            FilterChip(
                                selected = selectedCount == count,
                                onClick = { selectedCount = count; layoutSaved = false },
                                label = { Text("$count") },
                                leadingIcon = if (selectedCount == count) ({ Icon(Icons.Outlined.Check, null, Modifier.size(16.dp)) }) else null,
                                modifier = Modifier.weight(1f),
                                colors = selectedChipColors()
                            )
                        }
                    }
                    if (selectedCount >= 4) SettingsTextField(customName0, { customName0 = it.take(20); layoutSaved = false }, "Fourth panel", "Telegram")
                    if (selectedCount >= 5) SettingsTextField(customName1, { customName1 = it.take(20); layoutSaved = false }, "Fifth panel", "Signal")
                    Button(
                        onClick = {
                            onSavePanels(selectedCount, listOf(customName0.trim().ifBlank { "PANEL 4" }, customName1.trim().ifBlank { "PANEL 5" }))
                            layoutSaved = true
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Copper, contentColor = Background)
                    ) {
                        Icon(if (layoutSaved) Icons.Outlined.Check else Icons.Outlined.Save, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (layoutSaved) "SAVED" else "SAVE PANEL LAYOUT")
                    }
                }

                SettingsSection("QUICK ACCESS") {
                    Text("HOME SCREEN", style = MaterialTheme.typography.labelLarge, color = Copper)
                    Text("Add a widget that shows a selected QR directly or acts as a quick presentation link.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    OutlinedButton(
                        onClick = { requestQrWidget(context) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        border = BorderStroke(1.dp, Copper.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Copper)
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.AddToHomeScreen, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("ADD HOME SCREEN WIDGET")
                    }
                    HorizontalDivider(color = Border)
                    Text("LOCK SCREEN", style = MaterialTheme.typography.labelLarge, color = Copper)
                    Text("Choose the QR opened by the Quick Settings tile, including while your phone is locked.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    panels.forEach { panel ->
                        FilterChip(
                            selected = quickAccessPanelId == panel.id,
                            onClick = { onQuickAccessPanelSelected(panel.id) },
                            label = { Text(panel.label) },
                            leadingIcon = if (quickAccessPanelId == panel.id) ({ Icon(Icons.Outlined.Check, null, Modifier.size(16.dp)) }) else null,
                            modifier = Modifier.fillMaxWidth(),
                            colors = selectedChipColors()
                        )
                    }
                    Button(
                        onClick = { requestQrTile(context) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Copper, contentColor = Background)
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.AddToHomeScreen, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("ADD QUICK SETTINGS TILE")
                    }
                    Text("The tile shows your QR only after you tap it. It does not bypass your device security.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }

                SettingsSection("PRIVACY & DATA") {
                    PrivacyRow(Icons.Outlined.Psychology, "On-device card scanning", "Card photos are processed locally and removed after scanning.")
                    PrivacyRow(Icons.Outlined.WifiOff, "No account, analytics or servers", "Your profile and QR content remain in private app storage.")
                    PrivacyRow(Icons.Outlined.Backup, "Cloud backup disabled", "Your networking profile is excluded from Android device backups.")
                    OutlinedButton(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_URL))) },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Copper.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Copper)
                    ) {
                        Icon(Icons.Outlined.Policy, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("READ PRIVACY POLICY")
                    }
                }

                SettingsSection("ABOUT") {
                    PrivacyRow(Icons.Outlined.Info, "Contact Networking Pro", "Version ${BuildConfig.VERSION_NAME} - fast, private contact exchange.")
                    TextButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)
                    ) {
                        Icon(Icons.Outlined.DeleteForever, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("RESET ALL APP DATA")
                    }
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = { Icon(Icons.Outlined.DeleteForever, null, tint = ErrorRed) },
            title = { Text("Reset all app data?") },
            text = { Text("This permanently removes your profile, saved QR codes and panel settings from this device.") },
            confirmButton = { TextButton(onClick = { showResetDialog = false; onResetData() }, colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)) { Text("RESET") } },
            dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text("CANCEL") } },
            containerColor = SurfaceDark,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }
}

@Composable
private fun selectedChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = CopperContainer,
    selectedLabelColor = Copper,
    selectedLeadingIconColor = Copper
)

private fun requestQrTile(context: android.content.Context) {
    val component = ComponentName(context, QrTileService::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.getSystemService(StatusBarManager::class.java).requestAddTileService(
            component,
            context.getString(R.string.quick_tile_label),
            Icon.createWithResource(context, R.drawable.ic_qr_tile),
            context.mainExecutor
        ) { result ->
            val success = result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED || result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED
            Toast.makeText(context, if (success) "Contact QR is available in Quick Settings." else "You can add Contact QR from Quick Settings edit mode.", Toast.LENGTH_LONG).show()
        }
    } else {
        Toast.makeText(context, "Add Contact QR from Quick Settings edit mode.", Toast.LENGTH_LONG).show()
        runCatching { context.startActivity(Intent("android.settings.QUICK_SETTINGS_SETTINGS")) }
    }
    TileService.requestListeningState(context, component)
}

private fun requestQrWidget(context: android.content.Context) {
    val manager = AppWidgetManager.getInstance(context)
    val provider = ComponentName(context, QrWidgetProvider::class.java)
    if (!manager.isRequestPinAppWidgetSupported || !manager.requestPinAppWidget(provider, null, null)) {
        Toast.makeText(context, "Add Contact QR from your launcher's Widgets menu.", Toast.LENGTH_LONG).show()
    }
}

@Composable
private fun PrivacyPromiseCard() {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CopperContainer).border(1.dp, Copper.copy(alpha = 0.45f), RoundedCornerShape(18.dp)).padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Shield, null, tint = Copper, modifier = Modifier.size(34.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("PRIVATE BY DESIGN", style = MaterialTheme.typography.titleMedium, color = Copper)
            Text("No sign-in. No tracking. No contact data sent to a server.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Copper)
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(SurfaceDark).border(1.dp, Border, RoundedCornerShape(16.dp)).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content
        )
    }
}

@Composable
private fun PrivacyRow(icon: ImageVector, title: String, body: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = Copper, modifier = Modifier.size(22.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
            Text(body, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

@Composable
private fun SettingsTextField(value: String, onValueChange: (String) -> Unit, label: String, placeholder: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Copper,
            unfocusedBorderColor = Border,
            focusedLabelColor = Copper,
            unfocusedLabelColor = TextSecondary,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = Copper
        )
    )
}
