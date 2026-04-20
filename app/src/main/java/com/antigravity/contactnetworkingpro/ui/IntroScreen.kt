package com.antigravity.contactnetworkingpro.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.contactnetworkingpro.ui.theme.*

@Composable
fun IntroScreen(onComplete: (panelCount: Int, customNames: List<String>) -> Unit) {
    var selectedCount by remember { mutableIntStateOf(3) }
    var customName0   by remember { mutableStateOf("") }
    var customName1   by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 120.dp)
        ) {
            // Copper accent bar
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Copper))

            Spacer(Modifier.height(56.dp))

            // Hero text
            Column(modifier = Modifier.padding(horizontal = 32.dp)) {
                Text(
                    text = "CONTACT\nNETWORKING\nPRO",
                    style = MaterialTheme.typography.displayLarge,
                    color = TextPrimary,
                    lineHeight = 58.sp
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Professional networking, elevated.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                    color = TextSecondary
                )
            }

            Spacer(Modifier.height(48.dp))
            HorizontalDivider(color = Divider, modifier = Modifier.padding(horizontal = 32.dp))
            Spacer(Modifier.height(40.dp))

            // Configuration section
            Column(modifier = Modifier.padding(horizontal = 32.dp)) {
                Text("CONFIGURE YOUR PANELS", style = MaterialTheme.typography.titleLarge, color = Copper)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Your Contact QR is always included. Choose how many sharing panels you want alongside it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )

                Spacer(Modifier.height(28.dp))

                // Panel count selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf(
                        Triple(3, "STANDARD", "Contact\nLinkedIn\nWhatsApp"),
                        Triple(4, "EXPANDED", "Contact\nLinkedIn\nWhatsApp\n+ 1 custom"),
                        Triple(5, "COMPLETE", "Contact\nLinkedIn\nWhatsApp\n+ 2 custom")
                    ).forEach { (count, tag, desc) ->
                        CountCard(
                            modifier   = Modifier.weight(1f),
                            count      = count,
                            tag        = tag,
                            desc       = desc,
                            selected   = selectedCount == count,
                            onClick    = { selectedCount = count }
                        )
                    }
                }

                // Custom panel name inputs
                if (selectedCount >= 4) {
                    Spacer(Modifier.height(28.dp))
                    HorizontalDivider(color = Divider)
                    Spacer(Modifier.height(24.dp))
                    Text("NAME YOUR CUSTOM PANELS", style = MaterialTheme.typography.titleMedium, color = Copper)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = customName0,
                        onValueChange = { if (it.length <= 20) customName0 = it },
                        label = { Text("4th panel name  (e.g. Telegram)", color = TextTertiary) },
                        leadingIcon = { Icon(Icons.Outlined.QrCode2, null, tint = TextTertiary) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        colors = introFieldColors()
                    )
                }

                if (selectedCount >= 5) {
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = customName1,
                        onValueChange = { if (it.length <= 20) customName1 = it },
                        label = { Text("5th panel name  (e.g. Signal)", color = TextTertiary) },
                        leadingIcon = { Icon(Icons.Outlined.QrCode2, null, tint = TextTertiary) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        colors = introFieldColors()
                    )
                }
            }
        }

        // Fixed bottom CTA
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Background)
                .padding(horizontal = 32.dp, vertical = 24.dp)
        ) {
            Button(
                onClick = {
                    onComplete(
                        selectedCount,
                        listOf(
                            customName0.ifBlank { "PANEL 4" },
                            customName1.ifBlank { "PANEL 5" }
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Copper, contentColor = TextPrimary)
            ) {
                Text("GET STARTED", style = MaterialTheme.typography.labelLarge, letterSpacing = 3.sp)
            }
        }
    }
}

@Composable
private fun CountCard(
    modifier: Modifier = Modifier,
    count: Int,
    tag: String,
    desc: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        if (selected) CopperContainer else SurfaceDark, label = "cardBg"
    )
    val borderColor by animateColorAsState(
        if (selected) Copper else Border, label = "cardBorder"
    )
    val numColor by animateColorAsState(
        if (selected) Copper else TextTertiary, label = "numColor"
    )
    val tagColor by animateColorAsState(
        if (selected) CopperLight else TextTertiary, label = "tagColor"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 20.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "$count",
            style = MaterialTheme.typography.displayMedium,
            color = numColor,
            fontSize = 38.sp
        )
        Text(
            text = tag,
            style = MaterialTheme.typography.titleSmall,
            color = tagColor,
            fontSize = 8.sp,
            letterSpacing = 1.5.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = desc,
            style = MaterialTheme.typography.bodySmall,
            color = if (selected) TextSecondary else TextTertiary,
            textAlign = TextAlign.Center,
            fontSize = 10.sp,
            lineHeight = 15.sp
        )
    }
}

@Composable
private fun introFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = Copper,
    unfocusedBorderColor = Border,
    focusedLabelColor    = Copper,
    cursorColor          = Copper,
    focusedTextColor     = TextPrimary,
    unfocusedTextColor   = TextPrimary
)
