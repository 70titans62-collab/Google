package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Plumbing
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ProjectDocument
import com.example.data.model.StudioMode
import com.example.ui.components.CsiWatermarkBadge
import com.example.ui.components.NewProjectDialog

enum class DashboardModuleRoute {
    NEW_PROJECT,
    MY_DESIGNS,
    DRAWING_2D,
    DESIGNER_3D,
    INTERIOR,
    ELECTRICAL,
    PLUMBING,
    CALCULATOR,
    TEMPLATES,
    SHARED_DESIGNS,
    BACKUP_RESTORE,
    SETTINGS,
    AI_ASSISTANT,
    EXPORT_CENTER
}

private data class DashboardTile(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color,
    val route: DashboardModuleRoute
)

@Composable
fun DashboardScreen(
    activeProject: ProjectDocument?,
    projectCount: Int,
    isSignedIn: Boolean,
    userDisplayName: String,
    onCreateNewProject: (String, com.example.data.model.ProjectCategory) -> Unit,
    onNavigateModule: (DashboardModuleRoute, StudioMode?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showNewDialog by remember { mutableStateOf(false) }

    val tiles = remember(projectCount) {
        listOf(
            DashboardTile("New Project", "Blank architectural canvas", Icons.Filled.AddCircleOutline, Color(0xFF38BDF8), DashboardModuleRoute.NEW_PROJECT),
            DashboardTile("My Designs", "$projectCount saved projects", Icons.Filled.FolderOpen, Color(0xFFF59E0B), DashboardModuleRoute.MY_DESIGNS),
            DashboardTile("2D Drawing", "Walls, shapes & snap", Icons.Filled.Draw, Color(0xFF38BDF8), DashboardModuleRoute.DRAWING_2D),
            DashboardTile("3D Designer", "360° & 1st-person walk", Icons.Filled.ViewInAr, Color(0xFF10B981), DashboardModuleRoute.DESIGNER_3D),
            DashboardTile("Interior Designer", "Paint, tiles & furniture", Icons.Filled.FormatPaint, Color(0xFFEC4899), DashboardModuleRoute.INTERIOR),
            DashboardTile("Electrical Designer", "Wiring, switches & load", Icons.Filled.Bolt, Color(0xFFF59E0B), DashboardModuleRoute.ELECTRICAL),
            DashboardTile("Plumbing Designer", "Pipelines & drainage", Icons.Filled.Plumbing, Color(0xFF06B6D4), DashboardModuleRoute.PLUMBING),
            DashboardTile("Material Calculator", "Tiles, paint & bricks", Icons.Filled.Calculate, Color(0xFF10B981), DashboardModuleRoute.CALCULATOR),
            DashboardTile("Templates", "Villa, 2BHK, Studio", Icons.Filled.DashboardCustomize, Color(0xFF8B5CF6), DashboardModuleRoute.TEMPLATES),
            DashboardTile("Shared Designs", "Links, QR & feedback", Icons.Filled.Share, Color(0xFF38BDF8), DashboardModuleRoute.SHARED_DESIGNS),
            DashboardTile("Backup & Restore", "Google Cloud sync", Icons.Filled.CloudUpload, Color(0xFF0EA5E9), DashboardModuleRoute.BACKUP_RESTORE),
            DashboardTile("Export Centre", "PDF, PNG, DXF, OBJ, GLB", Icons.Filled.IosShare, Color(0xFFF59E0B), DashboardModuleRoute.EXPORT_CENTER),
            DashboardTile("AI Design Assistant", "Smart layouts & checks", Icons.Filled.AutoAwesome, Color(0xFFA855F7), DashboardModuleRoute.AI_ASSISTANT),
            DashboardTile("Settings & Help", "Theme, units & guides", Icons.Filled.Settings, Color(0xFF94A3B8), DashboardModuleRoute.SETTINGS)
        )
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 156.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(190.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.img_blueprint_studio),
                        contentDescription = "CSI Studio Hero",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xEE0A1424),
                                        Color(0xAA0A1424),
                                        Color(0x440A1424)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CsiWatermarkBadge()
                            Surface(
                                color = if (isSignedIn) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (isSignedIn) "Cloud Connected" else "Offline Mode",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSignedIn) Color(0xFF34D399) else Color(0xFFFBBF24),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "Welcome S Irfan",
                                style = MaterialTheme.typography.headlineLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "CSI (Creation S Irfan) • HOMECRAFT 3D",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF38BDF8)
                            )
                        }

                        if (activeProject != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onNavigateModule(DashboardModuleRoute.DRAWING_2D, StudioMode.ARCHITECTURAL_2D) },
                                    modifier = Modifier.testTag("resume_project_button"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Filled.Architecture, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Resume: ${activeProject.name.take(18)}")
                                }
                            }
                        }
                    }
                }
            }
        }

        items(tiles) { tile ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable {
                        when (tile.route) {
                            DashboardModuleRoute.NEW_PROJECT -> showNewDialog = true
                            DashboardModuleRoute.DRAWING_2D -> onNavigateModule(tile.route, StudioMode.ARCHITECTURAL_2D)
                            DashboardModuleRoute.DESIGNER_3D -> onNavigateModule(tile.route, StudioMode.VIEW_3D)
                            DashboardModuleRoute.INTERIOR -> onNavigateModule(tile.route, StudioMode.INTERIOR)
                            DashboardModuleRoute.ELECTRICAL -> onNavigateModule(tile.route, StudioMode.ELECTRICAL)
                            DashboardModuleRoute.PLUMBING -> onNavigateModule(tile.route, StudioMode.PLUMBING)
                            else -> onNavigateModule(tile.route, null)
                        }
                    }
                    .testTag("dashboard_tile_${tile.route.name.lowercase()}"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = tile.accent.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(tile.icon, contentDescription = tile.title, tint = tile.accent, modifier = Modifier.size(24.dp))
                        }
                    }
                    Column {
                        Text(tile.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(tile.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (showNewDialog) {
        NewProjectDialog(
            onDismiss = { showNewDialog = false },
            onCreate = { name, cat ->
                showNewDialog = false
                onCreateNewProject(name, cat)
                onNavigateModule(DashboardModuleRoute.DRAWING_2D, StudioMode.ARCHITECTURAL_2D)
            }
        )
    }
}
