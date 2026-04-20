package com.antigravity.contactnetworkingpro.model

data class PanelConfig(
    val id: String,     // "contact" | "linkedin" | "whatsapp" | "custom_0" | "custom_1"
    val label: String   // all-caps display label, e.g. "LINKEDIN" or user-chosen "TELEGRAM"
)

fun buildPanelList(panelCount: Int, customNames: List<String>): List<PanelConfig> {
    val panels = mutableListOf(
        PanelConfig("contact",  "CONTACT"),
        PanelConfig("linkedin", "LINKEDIN"),
        PanelConfig("whatsapp", "WHATSAPP")
    )
    if (panelCount >= 4) panels += PanelConfig("custom_0", customNames.getOrElse(0) { "PANEL 4" }.uppercase())
    if (panelCount >= 5) panels += PanelConfig("custom_1", customNames.getOrElse(1) { "PANEL 5" }.uppercase())
    return panels
}
