package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.firebase.onGoogleSignInClicked
import kotlin.math.roundToInt

fun parseHexColor(hex: String, fallback: Color = Color(0xFF38BDF8)): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        fallback
    }
}

@Composable
fun CsiWatermarkBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Architecture,
                contentDescription = "CSI Watermark",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "CSI • Creation S Irfan",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun WelcomeAuthScreen(
    onSignedIn: () -> Unit,
    onContinueOffline: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A1424))
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_welcome_villa),
            contentDescription = "CSI HOMECRAFT 3D Architectural Villa",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x660A1424),
                            Color(0xDD0A1424),
                            Color(0xFF0A1424)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(140.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF0C4A6E).copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "CSI  •  CREATION S IRFAN",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF38BDF8),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Welcome S Irfan",
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "HOMECRAFT 3D Architectural Suite",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFFF59E0B),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Precision 2D CAD floor planning, interactive 360° 3D walkthroughs, interior & MEP layouts, AI design assistance, material estimation, and cloud collaboration.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFCBD5E1),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF101D32)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (errorMessage != null) {
                        Surface(
                            color = Color(0xFFF43F5E).copy(alpha = 0.16f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = Color(0xFFFDA4AF),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            isLoading = true
                            errorMessage = null
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isLoading = false
                                    onSignedIn()
                                },
                                onAuthError = { msg ->
                                    isLoading = false
                                    errorMessage = msg
                                },
                                scope = scope,
                                onAuthCancelled = { isLoading = false }
                            )
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("google_sign_in_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8), contentColor = Color(0xFF041C2C)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Color(0xFF041C2C))
                        } else {
                            Icon(Icons.Filled.CloudDone, contentDescription = null)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Sign in with Google", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = onContinueOffline,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("offline_studio_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.WifiOff, contentDescription = null, tint = Color(0xFF94A3B8))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Continue in Offline Drawing Mode", color = Color(0xFFE2E8F0))
                    }

                    Text(
                        text = "Google Sign-In unlocks Cloud Backup & Restore, Shared Design links, and AI House Design Assistant. All sketches carry the CSI • Creation S Irfan watermark.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun CustomColorPickerDialog(
    initialHex: String,
    onDismiss: () -> Unit,
    onSelectHex: (String) -> Unit
) {
    val swatches = listOf(
        "#F8FAFC", "#E2E8F0", "#CBD5E1", "#94A3B8", "#334155", "#0F172A",
        "#38BDF8", "#0284C7", "#0C4A6E", "#10B981", "#0F766E", "#CCFBF1",
        "#F59E0B", "#D97706", "#B45309", "#7C2D12", "#FEF3C7", "#F43F5E",
        "#8B5CF6", "#6366F1", "#EC4899", "#B47B44", "#C08552", "#78716C"
    )
    val init = remember(initialHex) {
        try {
            android.graphics.Color.parseColor(initialHex)
        } catch (_: Exception) {
            android.graphics.Color.parseColor("#38BDF8")
        }
    }
    var r by remember { mutableFloatStateOf(android.graphics.Color.red(init).toFloat()) }
    var g by remember { mutableFloatStateOf(android.graphics.Color.green(init).toFloat()) }
    var b by remember { mutableFloatStateOf(android.graphics.Color.blue(init).toFloat()) }
    val currentHex = String.format("#%02X%02X%02X", r.roundToInt(), g.roundToInt(), b.roundToInt())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Architectural Colour Picker") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(r.roundToInt(), g.roundToInt(), b.roundToInt()))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                    )
                    Text(currentHex, style = MaterialTheme.typography.titleMedium)
                }
                Text("Red: ${r.roundToInt()}", style = MaterialTheme.typography.labelMedium)
                Slider(value = r, onValueChange = { r = it }, valueRange = 0f..255f)
                Text("Green: ${g.roundToInt()}", style = MaterialTheme.typography.labelMedium)
                Slider(value = g, onValueChange = { g = it }, valueRange = 0f..255f)
                Text("Blue: ${b.roundToInt()}", style = MaterialTheme.typography.labelMedium)
                Slider(value = b, onValueChange = { b = it }, valueRange = 0f..255f)

                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    modifier = Modifier.height(130.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(swatches) { hex ->
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(hex))
                                .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .clickable {
                                    val c = android.graphics.Color.parseColor(hex)
                                    r = android.graphics.Color.red(c).toFloat()
                                    g = android.graphics.Color.green(c).toFloat()
                                    b = android.graphics.Color.blue(c).toFloat()
                                }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSelectHex(currentHex) }) { Text("Apply Colour") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun NewProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (String, com.example.data.model.ProjectCategory) -> Unit
) {
    var name by remember { mutableStateOf("Irfan Dream Residence") }
    var category by remember { mutableStateOf(com.example.data.model.ProjectCategory.RESIDENTIAL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Architectural Project") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Project Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("new_project_name_input")
                )
                Text("Category", style = MaterialTheme.typography.labelLarge)
                com.example.data.model.ProjectCategory.entries.chunked(2).forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        rowItems.forEach { cat ->
                            val selected = cat == category
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { category = cat },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = cat.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(10.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onCreate(name.trim(), category) },
                modifier = Modifier.testTag("create_project_confirm_button")
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
