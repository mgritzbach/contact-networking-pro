package com.antigravity.contactnetworkingpro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.antigravity.contactnetworkingpro.data.ContactStorage
import com.antigravity.contactnetworkingpro.model.ContactDraft
import com.antigravity.contactnetworkingpro.model.PanelConfig
import com.antigravity.contactnetworkingpro.model.buildPanelList
import com.antigravity.contactnetworkingpro.ui.ContactEditorScreen
import com.antigravity.contactnetworkingpro.ui.HomeScreen
import com.antigravity.contactnetworkingpro.ui.IntroScreen
import com.antigravity.contactnetworkingpro.ui.theme.ContactNetworkingProTheme

private sealed class Screen {
    object Intro  : Screen()
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

    var screen  by remember {
        mutableStateOf<Screen>(if (storage.isSetupComplete()) Screen.Home else Screen.Intro)
    }

    var panels  by remember {
        mutableStateOf(
            buildPanelList(
                panelCount   = storage.getPanelCount(),
                customNames  = listOf(
                    storage.getCustomPanelName(0),
                    storage.getCustomPanelName(1)
                )
            )
        )
    }

    var savedContact by remember { mutableStateOf(storage.loadContact()) }
    var savedUris    by remember {
        mutableStateOf(
            panels
                .filter { it.id != "contact" }
                .associate { it.id to storage.loadPanelUri(it.id) }
        )
    }

    when (screen) {
        Screen.Intro -> IntroScreen(
            onComplete = { count, customNames ->
                storage.savePanelCount(count)
                customNames.forEachIndexed { i, name -> storage.saveCustomPanelName(i, name) }
                storage.markSetupComplete()
                panels = buildPanelList(count, customNames)
                savedUris = panels.filter { it.id != "contact" }.associate { it.id to storage.loadPanelUri(it.id) }
                screen = Screen.Home
            }
        )

        Screen.Home -> HomeScreen(
            panels       = panels,
            savedContact = savedContact,
            savedUris    = savedUris,
            onEditClick  = { screen = Screen.Editor }
        )

        Screen.Editor -> ContactEditorScreen(
            panels        = panels,
            initialContact = savedContact,
            savedUris      = savedUris,
            onSaveContact  = { draft: ContactDraft ->
                savedContact = draft
                storage.saveContact(draft)
            },
            onSaveUri      = { panelId, uri ->
                savedUris = savedUris + (panelId to uri)
                storage.savePanelUri(panelId, uri)
            },
            onBack         = { screen = Screen.Home }
        )
    }
}
