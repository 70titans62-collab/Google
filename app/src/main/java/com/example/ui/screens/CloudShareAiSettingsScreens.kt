package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.AiAssistantState
import com.example.data.model.ProjectDocument
import com.example.data.model.UnitSystem
import com.example.data.repository.ShareCommentRecord
import com.example.data.repository.SharedDesignRecord
import com.example.export.ExportEngine
import com.example.export.ExportFormat
import com.example.ui.components.ShareQrCodeCard
import com.example.ui.viewmodel.StudioViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SharedDesignsScreen(
    viewModel: StudioViewModel,
    onRequestGoogleSignIn: () -> Unit,
    onOpenSharedProject: (ProjectDocument) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeProject by viewModel.activeProject.collectAsStateWithLifecycle()
    val myShares by viewModel.mySharedDesigns.collectAsStateWithLifecycle()
    val publicShares by viewModel.publicSharedDesigns.collectAsStateWithLifecycle()

    var isPublic by remember { mutableStateOf(true) }
    var permission by remember { mutableStateOf("VIEW_ONLY") }
    var password by remember { mutableStateOf("") }
    var expiryHours by remember { mutableIntStateOf(168) } // 7 days
    var generatedShareId by remember { mutableStateOf<String?>(null) }
    var shareError by remember { mutableStateOf<String?>(null) }

    var openShareIdInput by remember { mutableStateOf("") }
    var openSharePassword by remember { mutableStateOf("") }
    var activeSharedRecord by remember { mutableStateOf<SharedDesignRecord?>(null) }
    var showQrForId by remember { mutableStateOf<String?>(null) }

    if (!viewModel.isSignedIn) {
        Column(
            modifier = modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.CloudOff, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Text("Sign in with Google for Cloud Sharing", style = MaterialTheme.typography.headlineMedium)
            Text("You can still export & share PDF, PNG, JPG, and .csi files offline via WhatsApp, Gmail, Bluetooth, or Nearby Share.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRequestGoogleSignIn) { Text("Sign in with Google") }
            if (activeProject != null) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = {
                    val file = ExportEngine.generateExportFile(context, activeProject!!, ExportFormat.PDF_A4)
                    ExportEngine.shareFile(context, file, ExportFormat.PDF_A4.mime, activeProject!!.name)
                }) {
                    Icon(Icons.Filled.Share, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Share Active PDF via Android Sharesheet")
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Share Design & Collaboration", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            if (activeProject != null) {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Create Shareable Link: ${activeProject!!.name}", style = MaterialTheme.typography.titleLarge)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Public Link (else Private Invite)")
                            Switch(checked = isPublic, onCheckedChange = { isPublic = it })
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = permission == "VIEW_ONLY", onClick = { permission = "VIEW_ONLY" }, label = { Text("View Only") })
                            FilterChip(selected = permission == "EDITABLE", onClick = { permission = "EDITABLE" }, label = { Text("Editable Collab") })
                        }
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Optional Link Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(24 to "24h", 168 to "7d", 720 to "30d", 0 to "Never").forEach { (hrs, lbl) ->
                                FilterChip(selected = expiryHours == hrs, onClick = { expiryHours = hrs }, label = { Text("Exp: $lbl") })
                            }
                        }
                        if (shareError != null) {
                            Text(shareError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    shareError = null
                                    viewModel.createShareLink(activeProject!!, isPublic, permission, password.ifBlank { null }, expiryHours) { res ->
                                        if (res.isSuccess) {
                                            generatedShareId = res.getOrThrow()
                                            showQrForId = generatedShareId
                                        } else {
                                            shareError = res.exceptionOrNull()?.localizedMessage
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("generate_share_link_button")
                            ) {
                                Icon(Icons.Filled.Link, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Generate Link & QR")
                            }
                            OutlinedButton(
                                onClick = {
                                    val file = ExportEngine.generateExportFile(context, activeProject!!, ExportFormat.PNG)
                                    ExportEngine.shareFile(context, file, ExportFormat.PNG.mime, activeProject!!.name)
                                }
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = "Android Sharesheet")
                            }
                        }
                        if (generatedShareId != null) {
                            Text("Share ID: $generatedShareId", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Open Shared Design by Link / ID", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = openShareIdInput,
                        onValueChange = { openShareIdInput = it },
                        label = { Text("Enter Share ID (e.g. csi_...)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = openSharePassword,
                        onValueChange = { openSharePassword = it },
                        label = { Text("Password (if protected)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            shareError = null
                            viewModel.openSharedDesignById(
                                shareId = openShareIdInput,
                                enteredPassword = openSharePassword,
                                onSuccess = { rec, proj ->
                                    activeSharedRecord = rec
                                    onOpenSharedProject(proj)
                                },
                                onError = { shareError = it }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Load Shared Design") }
                }
            }
        }

        item { Text("My Shared Links (${myShares.size})", style = MaterialTheme.typography.titleLarge) }
        items(myShares, key = { it.id }) { item ->
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(item.projectName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("ID: ${item.id} • ${item.permission} • ${if (item.revoked) "REVOKED" else if (item.isPublic) "Public" else "Private"}", style = MaterialTheme.typography.labelSmall)
                        }
                        Row {
                            OutlinedButton(onClick = { showQrForId = item.id }) {
                                Icon(Icons.Filled.QrCode2, contentDescription = "QR")
                            }
                            if (!item.revoked) {
                                Spacer(Modifier.width(6.dp))
                                TextButton(onClick = { viewModel.revokeShareLink(item.id) }) {
                                    Text("Revoke", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                    TextButton(onClick = { activeSharedRecord = item }) {
                        Icon(Icons.Filled.Comment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Comments & Feedback")
                    }
                }
            }
        }

        if (publicShares.isNotEmpty()) {
            item { Text("Public Shared Showcase", style = MaterialTheme.typography.titleLarge) }
            items(publicShares, key = { "pub_${it.id}" }) { item ->
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(item.projectName, style = MaterialTheme.typography.titleMedium)
                            Text("By ${item.ownerDisplayName} • ${item.permission}", style = MaterialTheme.typography.labelSmall)
                        }
                        Button(onClick = {
                            viewModel.openSharedDesignById(item.id, null, { rec, proj ->
                                activeSharedRecord = rec
                                onOpenSharedProject(proj)
                            }, { shareError = it })
                        }) { Text("Open") }
                    }
                }
            }
        }
    }

    if (showQrForId != null) {
        val shareId = showQrForId!!
        AlertDialog(
            onDismissRequest = { showQrForId = null },
            title = { Text("CSI Project Share QR") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    ShareQrCodeCard(payload = "csi://share/$shareId")
                    Text("Share ID: $shareId", style = MaterialTheme.typography.labelLarge)
                    Text("Owner: ${viewModel.userDisplayName}", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = { TextButton(onClick = { showQrForId = null }) { Text("Done") } }
        )
    }

    if (activeSharedRecord != null) {
        val rec = activeSharedRecord!!
        val commentsFlow = remember(rec.id) { viewModel.observeComments(rec.id) }
        val comments: List<ShareCommentRecord> by (commentsFlow?.collectAsStateWithLifecycle(initialValue = emptyList()) ?: remember { mutableStateOf(emptyList()) })
        var commentText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { activeSharedRecord = null },
            title = { Text("Comments • ${rec.projectName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LazyColumn(modifier = Modifier.height(180.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(comments) { c ->
                            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)) {
                                Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                                    Text(c.authorName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                    Text(c.text, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        label = { Text("Add feedback...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.postShareComment(rec.id, commentText)
                    commentText = ""
                }) { Text("Post") }
            },
            dismissButton = { TextButton(onClick = { activeSharedRecord = null }) { Text("Close") } }
        )
    }
}

@Composable
fun BackupAndRestoreScreen(
    viewModel: StudioViewModel,
    onRequestGoogleSignIn: () -> Unit,
    onDisconnectGoogle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val cloudBackups by viewModel.cloudBackups.collectAsStateWithLifecycle()
    val backupState by viewModel.backupState.collectAsStateWithLifecycle()
    val selectedToRestore = remember { mutableStateListOf<String>() }
    val dateFmt = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Google Account Backup & Restore", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(
                            if (viewModel.isSignedIn) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                            contentDescription = null,
                            tint = if (viewModel.isSignedIn) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                        )
                        Column(Modifier.weight(1f)) {
                            Text(if (viewModel.isSignedIn) "Connected: ${viewModel.userDisplayName}" else "Not Connected", style = MaterialTheme.typography.titleMedium)
                            Text("OAuth2 Secure Google Sign-In (passwords are never stored)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (viewModel.isSignedIn) {
                            OutlinedButton(onClick = onDisconnectGoogle) { Text("Disconnect") }
                        } else {
                            Button(onClick = onRequestGoogleSignIn) { Text("Connect Google") }
                        }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Automatic Cloud Backup")
                        Switch(checked = settings.autoBackupEnabled, onCheckedChange = { viewModel.setAutoBackup(it) })
                    }

                    if (backupState.isRunning) {
                        LinearProgressIndicator(progress = { backupState.progress }, modifier = Modifier.fillMaxWidth())
                    }
                    if (backupState.statusMessage.isNotBlank()) {
                        Text(backupState.statusMessage, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    if (backupState.errorMessage != null) {
                        Text(backupState.errorMessage!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    }

                    Button(
                        onClick = { viewModel.backupAllProjects() },
                        enabled = viewModel.isSignedIn && !backupState.isRunning,
                        modifier = Modifier.fillMaxWidth().testTag("manual_backup_button")
                    ) {
                        Icon(Icons.Filled.CloudUpload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Manual Backup All Projects Now")
                    }
                }
            }
        }

        if (viewModel.isSignedIn) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Cloud Backup Folders (${cloudBackups.size})", style = MaterialTheme.typography.titleLarge)
                    if (selectedToRestore.isNotEmpty()) {
                        Button(onClick = {
                            val chosen = cloudBackups.filter { it.id in selectedToRestore }
                            viewModel.restoreSelectedCloudBackups(chosen)
                            selectedToRestore.clear()
                        }) {
                            Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Restore (${selectedToRestore.size})")
                        }
                    }
                }
            }
            items(cloudBackups, key = { it.id }) { rec ->
                val checked = rec.id in selectedToRestore
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().clickable {
                        if (checked) selectedToRestore.remove(rec.id) else selectedToRestore.add(rec.id)
                    }
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = checked, onCheckedChange = {
                            if (it) selectedToRestore.add(rec.id) else selectedToRestore.remove(rec.id)
                        })
                        Column(Modifier.weight(1f)) {
                            Text(rec.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("Folder: ${rec.folderPath}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            val tsText = rec.updatedAt?.toDate()?.let { dateFmt.format(it) } ?: "Synced"
                            Text("v${rec.version} • ${rec.floorCount} floor(s) • $tsText", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AiDesignAssistantScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    val project by viewModel.activeProject.collectAsStateWithLifecycle()
    var promptText by remember { mutableStateOf("Optimize furniture layout, suggest warm modern paint & wood flooring, and check for conflicts.") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("CSI AI House Design Assistant", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Analyzes ${project?.name ?: "your design"} for overlapping objects, disconnected walls, paint & tile palettes, and room arrangement. Changes require your explicit approval.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = promptText,
            onValueChange = { promptText = it },
            label = { Text("Describe what you want to improve or ask") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth().testTag("ai_prompt_input")
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { viewModel.runAiDesignAnalysis(promptText) },
                enabled = aiState !is AiAssistantState.Loading,
                modifier = Modifier.weight(1f).testTag("ai_suggest_button")
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Analyze & Suggest")
            }
            OutlinedButton(
                onClick = { viewModel.askAiQuestion(promptText) },
                enabled = aiState !is AiAssistantState.Loading
            ) {
                Text("Ask Q&A")
            }
        }

        when (val st = aiState) {
            AiAssistantState.Idle -> {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(14.dp)) {
                    Text(
                        "Tap 'Analyze & Suggest' to inspect room layout, paint/tile pairings, and geometry conflicts.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            AiAssistantState.Loading -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(26.dp))
                    Text("Analyzing floor geometry & generating architectural suggestions...")
                }
            }
            is AiAssistantState.ChatResponse -> {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Text(st.answer, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(16.dp))
                }
            }
            is AiAssistantState.ProposalReady -> {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Geometry Audit", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                        Text(st.diagnosisSummary, style = MaterialTheme.typography.bodyMedium)
                        HorizontalDivider()
                        Text(st.proposal.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(st.proposal.explanation, style = MaterialTheme.typography.bodyMedium)
                        Text("Suggested Wall Paint: ${st.proposal.suggestedPaintHex} • Flooring: ${st.proposal.suggestedFloorMaterial.label}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        st.proposal.roomNotes.forEach { note ->
                            Text("• $note", style = MaterialTheme.typography.bodyMedium)
                        }
                        if (st.proposal.addFurniture.isNotEmpty()) {
                            Text("Proposed Furniture Additions: ${st.proposal.addFurniture.joinToString { it.type.label }}", style = MaterialTheme.typography.labelMedium)
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.approveAndApplyAiProposal(st.proposal) },
                                modifier = Modifier.weight(1f).testTag("ai_approve_button")
                            ) {
                                Icon(Icons.Filled.Check, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Approve & Apply")
                            }
                            OutlinedButton(
                                onClick = { viewModel.dismissAiProposal() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Discard")
                            }
                        }
                    }
                }
            }
            is AiAssistantState.Error -> {
                Surface(color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp)) {
                    Text(st.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(14.dp))
                }
            }
        }
    }
}

@Composable
fun SettingsAndTutorialsScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Settings & Beginner Tutorials", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Dark Architectural Blueprint Theme")
                        Switch(checked = settings.darkTheme, onCheckedChange = { viewModel.setDarkTheme(it) })
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Automatic Local Saving")
                        Switch(checked = settings.autoSaveEnabled, onCheckedChange = { viewModel.setAutoSave(it) })
                    }
                    Text("Default Measurement Units", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        UnitSystem.entries.forEach { u ->
                            FilterChip(
                                selected = settings.defaultUnit == u,
                                onClick = { viewModel.setDefaultUnit(u) },
                                label = { Text(u.shortLabel) }
                            )
                        }
                    }
                }
            }
        }

        item { Text("Quick Architectural Tutorials", style = MaterialTheme.typography.titleLarge) }
        val guides = listOf(
            "1. Drawing Connected Walls" to "Select the Wall tool in 2D Draw and drag between points. Endpoint snapping automatically connects corners and updates live dimensions.",
            "2. Adding Doors & Windows" to "Pick Door/Win, select Sliding/Double/Wooden/Ventilation, set Swing direction, and tap directly on a wall to auto-align.",
            "3. 3D Walkthrough & Roofs" to "Switch to 3D View to orbit 360°, toggle Day/Night lighting, customize Gable/Hip/Flat roofs, or enter 1st-Person Walk mode.",
            "4. Electrical & Plumbing Modes" to "Switch to Electrical or Plumbing tabs to place boards, fans, AC points, tanks, and sinks with automatic wiring and pipeline routing.",
            "5. Estimation, AI & Sharing" to "Use Material Calculator for approximate tiles/cement/bricks, AI Assistant for approved layout tips, and Share Design for QR links or PDF/DXF/OBJ/GLB exports."
        )
        items(guides) { (title, body) ->
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(body, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
