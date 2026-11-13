package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.FloorPlan
import com.example.data.model.ProjectCategory
import com.example.data.model.ProjectDocument
import com.example.data.model.ProjectTemplates
import com.example.ui.viewmodel.StudioViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

@Composable
fun MyDesignsScreen(
    viewModel: StudioViewModel,
    onOpenProjectInStudio: (ProjectDocument) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allProjects by viewModel.allProjects.collectAsStateWithLifecycle()
    val versions by viewModel.activeProjectVersions.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Active, 1 = Favorites, 2 = Recycle Bin
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ProjectCategory?>(null) }
    var renameTarget by remember { mutableStateOf<ProjectDocument?>(null) }
    var versionProject by remember { mutableStateOf<ProjectDocument?>(null) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            if (!text.isNullOrBlank()) viewModel.importNativeCsiJson(text)
        }
    }

    val filtered = remember(allProjects, selectedTab, searchQuery, selectedCategory) {
        allProjects.filter { p ->
            val tabMatch = when (selectedTab) {
                0 -> !p.isDeleted
                1 -> !p.isDeleted && p.isFavorite
                else -> p.isDeleted
            }
            val queryMatch = searchQuery.isBlank() || p.name.contains(searchQuery, ignoreCase = true)
            val catMatch = selectedCategory == null || p.category == selectedCategory
            tabMatch && queryMatch && catMatch
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("My Designs (${allProjects.count { !it.isDeleted }})") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Favourites") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Recycle Bin (${allProjects.count { it.isDeleted }})") })
        }

        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    placeholder = { Text("Search projects...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("project_search_input")
                )
                OutlinedButton(onClick = { importLauncher.launch(arrayOf("*/*")) }) {
                    Icon(Icons.Filled.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Import .csi")
                }
            }

            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = selectedCategory == null, onClick = { selectedCategory = null }, label = { Text("All") })
                ProjectCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                        label = { Text(cat.label) }
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filtered, key = { it.id }) { proj ->
                val dateFmt = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.US) }
                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        if (!proj.isDeleted) onOpenProjectInStudio(proj)
                    },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        ProjectMiniThumbnail(floor = proj.floors.firstOrNull(), modifier = Modifier.size(72.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(proj.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "${proj.category.label} • ${proj.floors.size} floor(s) • v${proj.version}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Updated ${dateFmt.format(Date(proj.updatedAtMillis))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (!proj.isDeleted) {
                            Column(horizontalAlignment = Alignment.End) {
                                Row {
                                    IconButton(onClick = { viewModel.toggleFavorite(proj) }) {
                                        Icon(
                                            if (proj.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                                            contentDescription = "Favorite",
                                            tint = if (proj.isFavorite) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { renameTarget = proj }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Rename")
                                    }
                                    IconButton(onClick = {
                                        viewModel.selectProject(proj)
                                        versionProject = proj
                                    }) {
                                        Icon(Icons.Filled.History, contentDescription = "Version History")
                                    }
                                }
                                Row {
                                    IconButton(onClick = { viewModel.duplicateProject(proj) }) {
                                        Icon(Icons.Filled.ContentCopy, contentDescription = "Duplicate")
                                    }
                                    IconButton(onClick = { viewModel.moveToRecycleBin(proj) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Move to Recycle Bin", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        } else {
                            Row {
                                IconButton(onClick = { viewModel.restoreFromRecycleBin(proj) }) {
                                    Icon(Icons.Filled.RestoreFromTrash, contentDescription = "Restore", tint = MaterialTheme.colorScheme.tertiary)
                                }
                                IconButton(onClick = { viewModel.deletePermanently(proj.id) }) {
                                    Icon(Icons.Filled.DeleteForever, contentDescription = "Delete Forever", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (renameTarget != null) {
        val target = renameTarget!!
        var newName by remember { mutableStateOf(target.name) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename Project") },
            text = {
                OutlinedTextField(value = newName, onValueChange = { newName = it }, singleLine = true, label = { Text("Project Title") })
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.renameProject(target, newName)
                    renameTarget = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("Cancel") } }
        )
    }

    if (versionProject != null) {
        val dateFmt = remember { SimpleDateFormat("MMM dd, HH:mm:ss", Locale.US) }
        AlertDialog(
            onDismissRequest = { versionProject = null },
            title = { Text("Version History • ${versionProject!!.name}") },
            text = {
                if (versions.isEmpty()) {
                    Text("No saved snapshots yet. Use Save Revision in the studio to create checkpoints.")
                } else {
                    LazyColumn(modifier = Modifier.height(260.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(versions) { v ->
                            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("v${v.versionNumber} • ${v.summary}", style = MaterialTheme.typography.titleMedium)
                                        Text(dateFmt.format(Date(v.savedAtMillis)), style = MaterialTheme.typography.labelSmall)
                                    }
                                    TextButton(onClick = {
                                        viewModel.restoreVersion(v)
                                        versionProject = null
                                    }) { Text("Restore") }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { versionProject = null }) { Text("Close") } }
        )
    }
}

@Composable
fun ProjectMiniThumbnail(floor: FloorPlan?, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
        color = Color(0xFF0A1424),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF283D60))
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
            if (floor == null || floor.walls.isEmpty()) return@Canvas
            val minX = floor.walls.minOf { min(it.startX, it.endX) }
            val maxX = floor.walls.maxOf { max(it.startX, it.endX) }
            val minY = floor.walls.minOf { min(it.startY, it.endY) }
            val maxY = floor.walls.maxOf { max(it.startY, it.endY) }
            val scale = min(size.width / max(1f, maxX - minX), size.height / max(1f, maxY - minY))
            for (w in floor.walls) {
                drawLine(
                    color = Color(0xFF38BDF8),
                    start = Offset((w.startX - minX) * scale, (w.startY - minY) * scale),
                    end = Offset((w.endX - minX) * scale, (w.endY - minY) * scale),
                    strokeWidth = 2.5f
                )
            }
        }
    }
}

@Composable
fun TemplatesScreen(
    onSelectTemplate: ((String) -> ProjectDocument) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("CSI Architectural Templates", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Start immediately with pre-dimensioned 2D plans, 3D roofs, interiors, and MEP layouts.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(ProjectTemplates.allTemplates) { tpl ->
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(tpl.title, style = MaterialTheme.typography.titleLarge)
                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp)) {
                            Text(tpl.category.label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                    Text(tpl.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(tpl.floorsSummary, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                        Button(
                            onClick = { onSelectTemplate(tpl.builder) },
                            modifier = Modifier.testTag("use_template_${tpl.id}")
                        ) {
                            Text("Use Template")
                        }
                    }
                }
            }
        }
    }
}
