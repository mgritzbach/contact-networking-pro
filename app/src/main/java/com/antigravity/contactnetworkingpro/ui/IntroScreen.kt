package com.antigravity.contactnetworkingpro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.antigravity.contactnetworkingpro.ui.theme.*

@Composable
fun IntroScreen(onComplete: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 116.dp)
        ) {
            Box(Modifier.fillMaxWidth().height(2.dp).background(Copper))

            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 40.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "CONTACT NETWORKING PRO",
                    style = MaterialTheme.typography.titleMedium,
                    color = Copper
                )
                Text(
                    "Make the\nconnection.",
                    style = MaterialTheme.typography.displayLarge,
                    color = TextPrimary
                )
                Text(
                    "Exchange details in seconds — without an account, a subscription or a network connection.",
                    style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                    color = TextSecondary
                )
            }

            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BenefitCard(
                    Icons.Outlined.QrCode2,
                    "One QR, every essential detail",
                    "Share a contact card that Android and iPhone can save instantly."
                )
                BenefitCard(
                    Icons.Outlined.DocumentScanner,
                    "Turn paper cards into contacts",
                    "Scan on-device, review every field, then save to your phone."
                )
                BenefitCard(
                    Icons.Outlined.Shield,
                    "Private by design",
                    "No sign-in, no analytics and no contact data sent to a server."
                )
            }

            Spacer(Modifier.height(28.dp))
            Text(
                "You can add LinkedIn, WhatsApp and custom QR panels later in Settings.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 28.dp)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Background.copy(alpha = 0f), Background),
                        startY = 0f,
                        endY = 90f
                    )
                )
                .padding(horizontal = 24.dp, vertical = 22.dp)
        ) {
            Button(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Copper,
                    contentColor = Background
                )
            ) {
                Text("CREATE MY CONTACT QR", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.width(10.dp))
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun BenefitCard(icon: ImageVector, title: String, body: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .border(1.dp, Border, RoundedCornerShape(16.dp))
            .padding(18.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CopperContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Copper, modifier = Modifier.size(24.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
            Text(body, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}
