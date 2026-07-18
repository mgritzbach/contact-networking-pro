package com.antigravity.contactnetworkingpro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.contactnetworkingpro.api.ScanResult
import com.antigravity.contactnetworkingpro.model.ContactDraft
import com.antigravity.contactnetworkingpro.model.ContactDraftSaver
import com.antigravity.contactnetworkingpro.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScanReviewScreen(
    result: ScanResult,
    forOwnProfile: Boolean,
    onConfirm: (ContactDraft) -> Unit,
    onBack: () -> Unit
) {
    var draft by rememberSaveable(stateSaver = ContactDraftSaver) { mutableStateOf(result.draft) }
    val lines = result.lines

    Scaffold(containerColor = Background) { innerPadding ->
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
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text("REVIEW SCAN", style = MaterialTheme.typography.titleMedium, color = Copper)
                    Text(
                        if (forOwnProfile) "Assign lines to your profile fields"
                        else "Assign lines to contact fields",
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary
                    )
                }
            }

            if (lines.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No text detected on the card.", color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Spacer(Modifier.height(4.dp))

                    Text("DETECTED LINES", style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary, letterSpacing = 1.sp)

                    // Show all detected lines as chips
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        lines.forEach { line ->
                            SuggestionChip(
                                onClick = {},
                                label = { Text(line, style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary) },
                                shape = RoundedCornerShape(8.dp),
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = SurfaceElevated
                                ),
                                border = SuggestionChipDefaults.suggestionChipBorder(
                                    enabled = true,
                                    borderColor = Border
                                )
                            )
                        }
                    }

                    HorizontalDivider(color = Divider, modifier = Modifier.padding(vertical = 4.dp))

                    Text("ASSIGN FIELDS", style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary, letterSpacing = 1.sp)

                    ReviewDropdown("Full Name",    draft.fullName,    lines, Icons.Outlined.PersonOutline) { draft = draft.copy(fullName    = it) }
                    ReviewDropdown("Job Title",    draft.jobTitle,    lines, Icons.Outlined.WorkOutline)   { draft = draft.copy(jobTitle    = it) }
                    ReviewDropdown("Company",      draft.company,     lines, Icons.Outlined.Business)      { draft = draft.copy(company     = it) }
                    ReviewDropdown("Mobile Phone", draft.phoneMobile, lines, Icons.Outlined.PhoneAndroid)  { draft = draft.copy(phoneMobile = it) }
                    ReviewDropdown("Work Phone",   draft.phoneWork,   lines, Icons.Outlined.Phone)         { draft = draft.copy(phoneWork   = it) }
                    ReviewDropdown("Email",        draft.email,       lines, Icons.Outlined.MailOutline)   { draft = draft.copy(email       = it) }
                    ReviewDropdown("Website",      draft.website,     lines, Icons.Outlined.Language)      { draft = draft.copy(website     = it) }
                    ReviewDropdown("LinkedIn URL", draft.linkedinUrl, lines, Icons.Outlined.Link)          { draft = draft.copy(linkedinUrl = it) }

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = { onConfirm(draft) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Copper, contentColor = Background)
                    ) {
                        Icon(
                            if (forOwnProfile) Icons.Outlined.Save else Icons.Outlined.PersonAdd,
                            null, modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            if (forOwnProfile) "USE FOR MY PROFILE" else "CREATE CONTACT",
                            style = MaterialTheme.typography.labelLarge, letterSpacing = 2.sp
                        )
                    }

                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewDropdown(
    label: String,
    value: String,
    options: List<String>,
    icon: ImageVector,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, color = TextTertiary) },
            leadingIcon = { Icon(icon, null, tint = if (value.isNotBlank()) Copper else TextTertiary) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor      = Copper,
                unfocusedBorderColor    = if (value.isNotBlank()) Copper.copy(alpha = 0.5f) else Border,
                focusedLabelColor       = Copper,
                unfocusedLabelColor     = TextTertiary,
                cursorColor             = Copper,
                focusedLeadingIconColor = Copper,
                focusedTextColor        = TextPrimary,
                unfocusedTextColor      = TextPrimary,
                focusedTrailingIconColor   = Copper,
                unfocusedTrailingIconColor = TextTertiary
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(SurfaceDark)
        ) {
            DropdownMenuItem(
                text = { Text("— clear —", style = MaterialTheme.typography.bodySmall, color = TextTertiary) },
                onClick = { onValueChange(""); expanded = false }
            )
            options.forEach { line ->
                DropdownMenuItem(
                    text = { Text(line, color = TextPrimary, style = MaterialTheme.typography.bodyMedium) },
                    onClick = { onValueChange(line); expanded = false }
                )
            }
        }
    }
}
