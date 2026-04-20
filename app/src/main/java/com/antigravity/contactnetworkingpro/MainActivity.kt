package com.antigravity.contactnetworkingpro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.antigravity.contactnetworkingpro.data.ContactStorage
import com.antigravity.contactnetworkingpro.model.ContactDraft
import com.antigravity.contactnetworkingpro.ui.ContactEditorScreen
import com.antigravity.contactnetworkingpro.ui.HomeScreen
import com.antigravity.contactnetworkingpro.ui.theme.ContactNetworkingProTheme

private sealed class Screen {
    object Home   : Screen()
    object Editor : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ContactNetworkingProTheme {
                ContactNetworkingApp()
            }
        }
    }
}

@Composable
private fun ContactNetworkingApp() {
    val context = LocalContext.current
    val storage = remember { ContactStorage(context) }

    var screen          by remember { mutableStateOf<Screen>(Screen.Home) }
    var savedContact    by remember { mutableStateOf(storage.loadContact()) }
    var savedLinkedin   by remember { mutableStateOf(storage.loadLinkedinUri()) }
    var savedWhatsapp   by remember { mutableStateOf(storage.loadWhatsappUri()) }

    when (screen) {
        Screen.Home -> HomeScreen(
            savedContact     = savedContact,
            savedLinkedinUri = savedLinkedin,
            savedWhatsappUri = savedWhatsapp,
            onEditClick      = { screen = Screen.Editor }
        )
        Screen.Editor -> ContactEditorScreen(
            initialContact   = savedContact,
            savedLinkedinUri = savedLinkedin,
            savedWhatsappUri = savedWhatsapp,
            onSaveContact    = { draft: ContactDraft ->
                savedContact = draft
                storage.saveContact(draft)
            },
            onSaveLinkedin   = { uri ->
                savedLinkedin = uri
                storage.saveLinkedinUri(uri)
            },
            onSaveWhatsapp   = { uri ->
                savedWhatsapp = uri
                storage.saveWhatsappUri(uri)
            },
            onBack           = { screen = Screen.Home }
        )
    }
}
