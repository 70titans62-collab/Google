package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.GroupWork
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ArchitectureMath
import com.example.data.model.CeilingDesign
import com.example.data.model.DrawingTool
import com.example.data.model.ElectricalType
import com.example.data.model.FloorPlan
import com.example.data.model.FurnitureType
import com.example.data.model.LayerKind
import com.example.data.model.OpeningType
import com.example.data.model.PlumbingType
import com.example.data.model.Point2D
import com.example.data.model.ProjectDocument
import com.example.data.model.SketchShape
import com.example.data.model.SketchShapeType
import com.example.data.model.StudioMode
import com.example.data.model.SurfaceMaterial
import com.example.data.model.SwingDirection
import com.example.data.model.UnitSystem
import com.example.ui.components.CsiWatermarkBadge
import com.example.ui.components.CustomColorPickerDialog
import com.example.ui.components.parseHexColor
import com.example.ui.viewmodel.SelectionItem
import com.example.ui.viewmodel.StudioViewModel
import java.util.UUID
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

@Composable
fun StudioWorkspaceScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val project by viewModel.activeProject.collectAsStateWithLifecycle()
    val studioMode by viewModel.studioMode.collectAsStateWithLifecycle()
    val activeTool by viewModel.activeTool.collectAsStateWithLifecycle()
    val activeColorHex by viewModel.activeColorHex.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val eraserRadius by viewModel.eraserRadiusM.collectAsStateWithLifecycle()
    val polygonSides by viewModel.polygonSides.collectAsStateWithLifecycle()
    val selectedOpening by viewModel.selectedOpeningType.collectAsStateWithLifecycle()
    val selectedSwing by viewModel.selectedSwing.collectAsStateWithLifecycle()
    val selectedFurniture by viewModel.selectedFurnitureType.collectAsStateWithLifecycle()
    val selectedElectrical by viewModel.selectedElectricalType.collectAsStateWithLifecycle()
    val selectedPlumbing by viewModel.selectedPlumbingType.collectAsStateWithLifecycle()
    val selectedMaterial by viewModel.selectedSurfaceMaterial.collectAsStateWithLifecycle()

    val currentProject = project
    if (currentProject == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Create or select a project to start designing.")
        }
        return
    }
    val activeFloor = currentProject.floors.find { it.id == currentProject.activeFloorId } ?: currentProject.floors.first()

    var showColorPicker by remember { mutableStateOf(false) }
    var showLayersDialog by remember { mutableStateOf(false) }
    var showAddFloorDialog by remember { mutableStateOf(false) }
    var showConflictsDialog by remember { mutableStateOf(false) }
    var showUnitsScaleDialog by remember { mutableStateOf(false) }
    var showRoomNameDialog by remember { mutableStateOf<Pair<Offset, Offset>?>(null) }
    var multiSelectMode by remember { mutableStateOf(false) }

    val conflicts = remember(activeFloor) { ArchitectureMath.analyzeDesignConflicts(activeFloor) }

    Column(modifier = modifier.fillMaxSize()) {
        // 1. Studio Mode Bar (2D, Interior, Electrical, Plumbing, 3D)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StudioMode.entries.forEach { mode ->
                FilterChip(
                    selected = studioMode == mode,
                    onClick = { viewModel.setStudioMode(mode) },
                    label = { Text(mode.label) },
                    modifier = Modifier.testTag("studio_mode_${mode.name.lowercase()}")
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(onClick = { viewModel.saveManualRevision("Manual checkpoint") }, modifier = Modifier.testTag("save_revision_button")) {
                Icon(Icons.Filled.Save, contentDescription = "Save Revision")
            }
        }

        if (studioMode == StudioMode.VIEW_3D) {
            View3DScreen(
                project = currentProject,
                onUpdateExterior = { viewModel.updateExteriorConfig(it) },
                onToggleFloorVisibility = { viewModel.toggleFloorVisibility(it) },
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
            return@Column
        }

        // 2. Floor Switcher & Quick Controls Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            currentProject.floors.forEach { fl ->
                val isActive = fl.id == activeFloor.id
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    modifier = Modifier.clickable { viewModel.switchFloor(fl.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = fl.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = if (fl.isVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = "Toggle floor visibility",
                            tint = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { viewModel.toggleFloorVisibility(fl.id) }
                        )
                    }
                }
            }
            OutlinedButton(onClick = { showAddFloorDialog = true }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Text("Floor", style = MaterialTheme.typography.labelMedium)
            }
            OutlinedButton(onClick = { viewModel.duplicateActiveFloor() }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                Text("Dup Floor", style = MaterialTheme.typography.labelMedium)
            }
            if (conflicts.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { showConflictsDialog = true }
                ) {
                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, contentDescription = "Warnings", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${conflicts.size}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // 3. Edit / History / Precision Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.undo() }, enabled = canUndo, modifier = Modifier.testTag("undo_button")) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
            }
            IconButton(onClick = { viewModel.redo() }, enabled = canRedo, modifier = Modifier.testTag("redo_button")) {
                Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
            }
            IconButton(onClick = { viewModel.toggleGrid() }) {
                Icon(Icons.Filled.GridOn, contentDescription = "Grid", tint = if (currentProject.gridEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { viewModel.toggleSnap() }) {
                Icon(Icons.Filled.FilterCenterFocus, contentDescription = "Snap", tint = if (currentProject.snapEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { showUnitsScaleDialog = true }) {
                Icon(Icons.Filled.Straighten, contentDescription = "Units & Scale")
            }
            IconButton(onClick = { showLayersDialog = true }) {
                Icon(Icons.Filled.Layers, contentDescription = "Drawing Layers")
            }
            IconButton(onClick = { showColorPicker = true }) {
                Icon(Icons.Filled.Palette, contentDescription = "Colour Picker", tint = parseHexColor(activeColorHex))
            }
            if (selectedIds.isNotEmpty()) {
                IconButton(onClick = { viewModel.copySelected() }) { Icon(Icons.Filled.ContentCopy, contentDescription = "Copy") }
                IconButton(onClick = { viewModel.pasteClipboard() }) { Icon(Icons.Filled.ContentPaste, contentDescription = "Paste") }
                IconButton(onClick = { viewModel.rotateSelectedBy(45f) }) { Icon(Icons.Filled.RotateRight, contentDescription = "Rotate 45°") }
                IconButton(onClick = { viewModel.resizeSelected(1.15f) }) { Icon(Icons.Filled.AspectRatio, contentDescription = "Resize +15%") }
                IconButton(onClick = { viewModel.toggleLockSelected() }) { Icon(Icons.Filled.Lock, contentDescription = "Lock/Unlock") }
                if (selectedIds.size >= 2) {
                    IconButton(onClick = { viewModel.groupSelected() }) { Icon(Icons.Filled.GroupWork, contentDescription = "Group") }
                }
                IconButton(onClick = { viewModel.deleteSelected() }) { Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error) }
            }
        }

        // 4. Interactive CAD Canvas (with Rulers, Grid, Snap, Alignment Guides, Watermark)
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Architectural2DCanvas(
                project = currentProject,
                floor = activeFloor,
                studioMode = studioMode,
                activeTool = activeTool,
                activeColorHex = activeColorHex,
                polygonSides = polygonSides,
                selectedIds = selectedIds,
                multiSelectMode = multiSelectMode,
                onTapCanvas = { wx, wy ->
                    when (studioMode) {
                        StudioMode.ARCHITECTURAL_2D -> when (activeTool) {
                            DrawingTool.SELECT -> viewModel.selectAt(wx, wy, multiSelectMode)
                            DrawingTool.ERASER -> viewModel.eraseNear(wx, wy)
                            DrawingTool.DOOR_WINDOW -> viewModel.addOpeningAt(wx, wy)
                            else -> {}
                        }
                        StudioMode.INTERIOR -> {
                            if (activeTool == DrawingTool.SELECT) viewModel.selectAt(wx, wy, multiSelectMode)
                            else viewModel.addFurnitureAt(wx, wy)
                        }
                        StudioMode.ELECTRICAL -> viewModel.addElectricalAt(wx, wy)
                        StudioMode.PLUMBING -> viewModel.addPlumbingAt(wx, wy)
                        StudioMode.VIEW_3D -> {}
                    }
                },
                onFinishWall = { x1, y1, x2, y2 -> viewModel.addWall(x1, y1, x2, y2) },
                onFinishRoom = { x1, y1, x2, y2 -> showRoomNameDialog = Offset(x1, y1) to Offset(x2, y2) },
                onFinishMeasure = { x1, y1, x2, y2 -> viewModel.addMeasurementAnnotation(x1, y1, x2, y2) },
                onFinishShape = { shape -> viewModel.addSketchShape(shape) },
                onDragSelected = { dx, dy -> viewModel.moveSelectedBy(dx, dy) },
                onEraseDrag = { wx, wy -> viewModel.eraseNear(wx, wy) }
            )
        }

        // 5. Contextual Mode Bottom Tool Palette
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            when (studioMode) {
                StudioMode.ARCHITECTURAL_2D -> {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DrawingTool.entries.forEach { tool ->
                                FilterChip(
                                    selected = activeTool == tool,
                                    onClick = { viewModel.setDrawingTool(tool) },
                                    label = { Text(tool.label) },
                                    modifier = Modifier.testTag("tool_${tool.name.lowercase()}")
                                )
                            }
                        }
                        if (activeTool == DrawingTool.SELECT) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = multiSelectMode,
                                    onClick = { multiSelectMode = !multiSelectMode },
                                    label = { Text("Multi-Select (${selectedIds.size})") }
                                )
                                Text("Drag selected item to move", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else if (activeTool == DrawingTool.ERASER) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Eraser: ${String.format("%.1fm", eraserRadius)}", style = MaterialTheme.typography.labelMedium)
                                Spacer(modifier = Modifier.width(8.dp))
                                Slider(value = eraserRadius, onValueChange = { viewModel.setEraserRadius(it) }, valueRange = 0.25f..2.0f, modifier = Modifier.weight(1f))
                            }
                        } else if (activeTool == DrawingTool.POLYGON) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Polygon Sides: $polygonSides", style = MaterialTheme.typography.labelMedium)
                                Spacer(modifier = Modifier.width(8.dp))
                                Slider(value = polygonSides.toFloat(), onValueChange = { viewModel.setPolygonSides(it.toInt()) }, valueRange = 3f..10f, steps = 6, modifier = Modifier.weight(1f))
                            }
                        } else if (activeTool == DrawingTool.DOOR_WINDOW) {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OpeningType.entries.forEach { op ->
                                    FilterChip(
                                        selected = selectedOpening == op,
                                        onClick = { viewModel.setOpeningType(op) },
                                        label = { Text(op.label) }
                                    )
                                }
                                SwingDirection.entries.forEach { sw ->
                                    FilterChip(
                                        selected = selectedSwing == sw,
                                        onClick = { viewModel.setSwingDirection(sw) },
                                        label = { Text(sw.label) }
                                    )
                                }
                            }
                        }
                    }
                }
                StudioMode.INTERIOR -> {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = activeTool == DrawingTool.SELECT,
                                onClick = { viewModel.setDrawingTool(DrawingTool.SELECT) },
                                label = { Text("Select Surface") }
                            )
                            FurnitureType.entries.forEach { ft ->
                                FilterChip(
                                    selected = activeTool != DrawingTool.SELECT && selectedFurniture == ft,
                                    onClick = {
                                        viewModel.setDrawingTool(DrawingTool.WALL)
                                        viewModel.setFurnitureType(ft)
                                    },
                                    label = { Text("+ ${ft.label}") }
                                )
                            }
                        }
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            SurfaceMaterial.entries.forEach { mat ->
                                FilterChip(
                                    selected = selectedMaterial == mat,
                                    onClick = {
                                        viewModel.setSurfaceMaterial(mat)
                                        viewModel.applyMaterialToSelectionOrFloor(mat, activeColorHex)
                                    },
                                    label = { Text(mat.label) }
                                )
                            }
                            CeilingDesign.entries.forEach { cd ->
                                OutlinedButton(
                                    onClick = { viewModel.applyMaterialToSelectionOrFloor(selectedMaterial, activeColorHex, cd) },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                ) {
                                    Text("Ceiling: ${cd.label}", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
                StudioMode.ELECTRICAL -> {
                    val totalWatts = activeFloor.electricalNodes.sumOf { it.wattage }
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text(
                            text = "Electrical Mode • ${activeFloor.electricalNodes.size} points • Load: ${totalWatts}W (${String.format("%.1fA", totalWatts / 230.0)}) — Tap canvas to place & auto-wire",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ElectricalType.entries.forEach { et ->
                                FilterChip(
                                    selected = selectedElectrical == et,
                                    onClick = { viewModel.setElectricalType(et) },
                                    label = { Text("${et.symbol}: ${et.label}") }
                                )
                            }
                        }
                    }
                }
                StudioMode.PLUMBING -> {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text(
                            text = "Plumbing Mode • ${activeFloor.plumbingNodes.size} fixtures — Tap canvas to place & route water/drain lines",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            PlumbingType.entries.forEach { pt ->
                                FilterChip(
                                    selected = selectedPlumbing == pt,
                                    onClick = { viewModel.setPlumbingType(pt) },
                                    label = { Text("${pt.symbol}: ${pt.label}") }
                                )
                            }
                        }
                    }
                }
                StudioMode.VIEW_3D -> {}
            }
        }
    }

    if (showColorPicker) {
        CustomColorPickerDialog(
            initialHex = activeColorHex,
            onDismiss = { showColorPicker = false },
            onSelectHex = { hex ->
                viewModel.setActiveColor(hex)
                if (studioMode == StudioMode.INTERIOR) {
                    viewModel.applyMaterialToSelectionOrFloor(selectedMaterial, hex)
                }
                showColorPicker = false
            }
        )
    }

    if (showLayersDialog) {
        AlertDialog(
            onDismissRequest = { showLayersDialog = false },
            title = { Text("Drawing Layers (${activeFloor.name})") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    activeFloor.layers.forEach { layer ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(layer.kind.label, style = MaterialTheme.typography.bodyLarge)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { viewModel.toggleLayerVisibility(layer.kind) }) {
                                    Icon(
                                        if (layer.visible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = "Toggle layer visibility"
                                    )
                                }
                                IconButton(onClick = { viewModel.toggleLayerLock(layer.kind) }) {
                                    Icon(
                                        Icons.Filled.Lock,
                                        contentDescription = "Lock layer",
                                        tint = if (layer.locked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showLayersDialog = false }) { Text("Done") } }
        )
    }

    if (showAddFloorDialog) {
        AlertDialog(
            onDismissRequest = { showAddFloorDialog = false },
            title = { Text("Add Building Level") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val presets = listOf(
                        "Basement" to -1,
                        "Ground Floor" to 0,
                        "First Floor" to 1,
                        "Second Floor" to 2,
                        "Third Floor" to 3
                    )
                    presets.forEach { (label, idx) ->
                        OutlinedButton(
                            onClick = {
                                viewModel.addFloor(label, idx)
                                showAddFloorDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAddFloorDialog = false }) { Text("Close") } }
        )
    }

    if (showConflictsDialog) {
        AlertDialog(
            onDismissRequest = { showConflictsDialog = false },
            title = { Text("Dimension & Geometry Warnings") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    conflicts.forEach { c ->
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(10.dp)) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(c.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                                Text(c.description, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showConflictsDialog = false }) { Text("OK") } }
        )
    }

    if (showUnitsScaleDialog) {
        AlertDialog(
            onDismissRequest = { showUnitsScaleDialog = false },
            title = { Text("Units, Scale & Guides") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Measurement Unit", style = MaterialTheme.typography.labelLarge)
                    UnitSystem.entries.forEach { u ->
                        FilterChip(
                            selected = currentProject.unitSystem == u,
                            onClick = { viewModel.setUnitSystem(u) },
                            label = { Text("${u.label} (${u.shortLabel})") }
                        )
                    }
                    Text("Drawing Scale", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(20, 50, 100, 200).forEach { s ->
                            FilterChip(
                                selected = currentProject.drawingScale == s,
                                onClick = { viewModel.setDrawingScale(s) },
                                label = { Text("1:$s") }
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Rulers")
                        Switch(checked = currentProject.rulerEnabled, onCheckedChange = { viewModel.toggleRuler() })
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Alignment Guides")
                        Switch(checked = currentProject.guidesEnabled, onCheckedChange = { viewModel.toggleGuides() })
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showUnitsScaleDialog = false }) { Text("Done") } }
        )
    }

    if (showRoomNameDialog != null) {
        val (p1, p2) = showRoomNameDialog!!
        var rName by remember { mutableStateOf("Bedroom / Lounge") }
        AlertDialog(
            onDismissRequest = { showRoomNameDialog = null },
            title = { Text("Name Room Zone") },
            text = {
                OutlinedTextField(
                    value = rName,
                    onValueChange = { rName = it },
                    label = { Text("Room Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.addRoomZone(p1.x, p1.y, p2.x - p1.x, p2.y - p1.y, rName.ifBlank { "Room" })
                    showRoomNameDialog = null
                }) { Text("Add Room") }
            },
            dismissButton = {
                TextButton(onClick = { showRoomNameDialog = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun Architectural2DCanvas(
    project: ProjectDocument,
    floor: FloorPlan,
    studioMode: StudioMode,
    activeTool: DrawingTool,
    activeColorHex: String,
    polygonSides: Int,
    selectedIds: Set<SelectionItem>,
    multiSelectMode: Boolean,
    onTapCanvas: (Float, Float) -> Unit,
    onFinishWall: (Float, Float, Float, Float) -> Unit,
    onFinishRoom: (Float, Float, Float, Float) -> Unit,
    onFinishMeasure: (Float, Float, Float, Float) -> Unit,
    onFinishShape: (SketchShape) -> Unit,
    onDragSelected: (Float, Float) -> Unit,
    onEraseDrag: (Float, Float) -> Unit
) {
    val textMeasurer: TextMeasurer = rememberTextMeasurer()
    var zoomPxPerMeter by remember { mutableFloatStateOf(52f) }
    var panOffset by remember { mutableStateOf(Offset(90f, 110f)) }

    var dragStartWorld by remember { mutableStateOf<Offset?>(null) }
    var dragCurrentWorld by remember { mutableStateOf<Offset?>(null) }
    val pencilPoints = remember { mutableStateListOf<Point2D>() }
    val customVertices = remember { mutableStateListOf<Point2D>() }

    fun screenToWorld(pos: Offset): Offset {
        return Offset(
            (pos.x - panOffset.x) / zoomPxPerMeter,
            (pos.y - panOffset.y) / zoomPxPerMeter
        )
    }

    fun worldToScreen(wx: Float, wy: Float): Offset {
        return Offset(
            panOffset.x + wx * zoomPxPerMeter,
            panOffset.y + wy * zoomPxPerMeter
        )
    }

    fun isLayerVisible(kind: LayerKind): Boolean =
        floor.layers.find { it.kind == kind }?.visible ?: true

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A1424))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("architectural_2d_canvas")
                .pointerInput(studioMode, activeTool, project.snapEnabled, floor.walls, selectedIds) {
                    detectTapGestures { offset ->
                        val w = screenToWorld(offset)
                        val (sx, sy) = ArchitectureMath.snapPoint(w.x, w.y, floor.walls, project.snapEnabled)
                        if (studioMode == StudioMode.ARCHITECTURAL_2D && activeTool == DrawingTool.CUSTOM_SHAPE) {
                            customVertices.add(Point2D(sx, sy))
                        } else {
                            onTapCanvas(sx, sy)
                        }
                    }
                }
                .pointerInput(studioMode, activeTool, project.snapEnabled, floor.walls, selectedIds, zoomPxPerMeter, panOffset) {
                    detectDragGestures(
                        onDragStart = { start ->
                            val w = screenToWorld(start)
                            val (sx, sy) = ArchitectureMath.snapPoint(w.x, w.y, floor.walls, project.snapEnabled)
                            dragStartWorld = Offset(sx, sy)
                            dragCurrentWorld = Offset(sx, sy)
                            if (activeTool == DrawingTool.PENCIL) {
                                pencilPoints.clear()
                                pencilPoints.add(Point2D(w.x, w.y))
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val w = screenToWorld(change.position)
                            if (activeTool == DrawingTool.SELECT && selectedIds.isEmpty()) {
                                panOffset += dragAmount
                            } else if (activeTool == DrawingTool.SELECT && selectedIds.isNotEmpty()) {
                                onDragSelected(dragAmount.x / zoomPxPerMeter, dragAmount.y / zoomPxPerMeter)
                            } else if (activeTool == DrawingTool.ERASER) {
                                onEraseDrag(w.x, w.y)
                            } else if (activeTool == DrawingTool.PENCIL) {
                                pencilPoints.add(Point2D(w.x, w.y))
                                dragCurrentWorld = w
                            } else {
                                val (sx, sy) = ArchitectureMath.snapPoint(w.x, w.y, floor.walls, project.snapEnabled)
                                dragCurrentWorld = Offset(sx, sy)
                            }
                        },
                        onDragEnd = {
                            val p1 = dragStartWorld
                            val p2 = dragCurrentWorld
                            if (p1 != null && p2 != null && studioMode == StudioMode.ARCHITECTURAL_2D) {
                                when (activeTool) {
                                    DrawingTool.WALL -> onFinishWall(p1.x, p1.y, p2.x, p2.y)
                                    DrawingTool.ROOM_ZONE -> onFinishRoom(p1.x, p1.y, p2.x, p2.y)
                                    DrawingTool.MEASURE -> onFinishMeasure(p1.x, p1.y, p2.x, p2.y)
                                    DrawingTool.PENCIL -> if (pencilPoints.size >= 2) {
                                        onFinishShape(
                                            SketchShape(
                                                id = "s_${UUID.randomUUID().toString().take(7)}",
                                                type = SketchShapeType.PENCIL,
                                                points = pencilPoints.toList(),
                                                strokeColorHex = activeColorHex
                                            )
                                        )
                                        pencilPoints.clear()
                                    }
                                    DrawingTool.LINE, DrawingTool.RECTANGLE, DrawingTool.SQUARE,
                                    DrawingTool.CIRCLE, DrawingTool.OVAL, DrawingTool.TRIANGLE,
                                    DrawingTool.POLYGON, DrawingTool.ARC -> {
                                        val sType = when (activeTool) {
                                            DrawingTool.LINE -> SketchShapeType.LINE
                                            DrawingTool.RECTANGLE -> SketchShapeType.RECTANGLE
                                            DrawingTool.SQUARE -> SketchShapeType.SQUARE
                                            DrawingTool.CIRCLE -> SketchShapeType.CIRCLE
                                            DrawingTool.OVAL -> SketchShapeType.OVAL
                                            DrawingTool.TRIANGLE -> SketchShapeType.TRIANGLE
                                            DrawingTool.POLYGON -> SketchShapeType.POLYGON
                                            else -> SketchShapeType.ARC
                                        }
                                        val w = abs(p2.x - p1.x).coerceAtLeast(0.4f)
                                        val h = if (sType == SketchShapeType.SQUARE || sType == SketchShapeType.CIRCLE) w else abs(p2.y - p1.y).coerceAtLeast(0.4f)
                                        onFinishShape(
                                            SketchShape(
                                                id = "s_${UUID.randomUUID().toString().take(7)}",
                                                type = sType,
                                                points = listOf(Point2D(p1.x, p1.y), Point2D(p2.x, p2.y)),
                                                x = min(p1.x, p2.x),
                                                y = min(p1.y, p2.y),
                                                width = w,
                                                height = h,
                                                polygonSides = polygonSides,
                                                strokeColorHex = activeColorHex
                                            )
                                        )
                                    }
                                    else -> {}
                                }
                            }
                            dragStartWorld = null
                            dragCurrentWorld = null
                        }
                    )
                }
        ) {
            // 1. Blueprint Grid
            if (project.gridEnabled) {
                val startMeterX = ((-panOffset.x) / zoomPxPerMeter).toInt() - 1
                val endMeterX = ((size.width - panOffset.x) / zoomPxPerMeter).toInt() + 1
                val startMeterY = ((-panOffset.y) / zoomPxPerMeter).toInt() - 1
                val endMeterY = ((size.height - panOffset.y) / zoomPxPerMeter).toInt() + 1
                for (mx in startMeterX..endMeterX) {
                    val sx = panOffset.x + mx * zoomPxPerMeter
                    drawLine(
                        color = if (mx % 5 == 0) Color(0xFF1E3A5F) else Color(0xFF132238),
                        start = Offset(sx, 0f),
                        end = Offset(sx, size.height),
                        strokeWidth = if (mx % 5 == 0) 1.5f else 0.8f
                    )
                }
                for (my in startMeterY..endMeterY) {
                    val sy = panOffset.y + my * zoomPxPerMeter
                    drawLine(
                        color = if (my % 5 == 0) Color(0xFF1E3A5F) else Color(0xFF132238),
                        start = Offset(0f, sy),
                        end = Offset(size.width, sy),
                        strokeWidth = if (my % 5 == 0) 1.5f else 0.8f
                    )
                }
            }

            // 2. Rooms (floor material tint + name + area & perimeter)
            if (isLayerVisible(LayerKind.ARCHITECTURAL)) {
                for (r in floor.rooms) {
                    val tl = worldToScreen(r.x, r.y)
                    val sz = Size(r.widthM * zoomPxPerMeter, r.depthM * zoomPxPerMeter)
                    val isSel = SelectionItem.Room(r.id) in selectedIds
                    drawRect(
                        color = parseHexColor(r.floorColorHex).copy(alpha = 0.22f),
                        topLeft = tl,
                        size = sz
                    )
                    if (isSel) {
                        drawRect(color = Color(0xFFF59E0B), topLeft = tl, size = sz, style = Stroke(3f))
                    }
                    val areaStr = ArchitectureMath.formatArea(ArchitectureMath.roomAreaSqM(r), project.unitSystem)
                    val perimStr = ArchitectureMath.formatLength(ArchitectureMath.roomPerimeterM(r).toFloat(), project.unitSystem)
                    val labelLayout = textMeasurer.measure(
                        "${r.name}\n$areaStr • P:$perimStr",
                        style = TextStyle(color = Color(0xFFE2E8F0), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    )
                    drawText(
                        labelLayout,
                        topLeft = Offset(
                            tl.x + (sz.width - labelLayout.size.width) / 2f,
                            tl.y + (sz.height - labelLayout.size.height) / 2f
                        )
                    )
                }
            }

            // 3. Furniture
            if (isLayerVisible(LayerKind.FURNITURE)) {
                for (f in floor.furniture) {
                    val tl = worldToScreen(f.x, f.y)
                    val sz = Size(f.widthM * zoomPxPerMeter, f.depthM * zoomPxPerMeter)
                    val isSel = SelectionItem.Furniture(f.id) in selectedIds
                    rotate(f.rotationDeg, pivot = Offset(tl.x + sz.width / 2f, tl.y + sz.height / 2f)) {
                        drawRect(color = parseHexColor(f.colorHex).copy(alpha = 0.55f), topLeft = tl, size = sz)
                        drawRect(
                            color = if (isSel) Color(0xFFF59E0B) else Color(0xFF94A3B8),
                            topLeft = tl,
                            size = sz,
                            style = Stroke(if (isSel) 4f else 2f)
                        )
                        val lbl = textMeasurer.measure(f.type.label, style = TextStyle(color = Color.White, fontSize = 9.sp))
                        drawText(lbl, topLeft = Offset(tl.x + 4f, tl.y + 4f))
                    }
                }
            }

            // 4. Walls + automatic dimension lines
            if (isLayerVisible(LayerKind.ARCHITECTURAL)) {
                for (w in floor.walls) {
                    val s = worldToScreen(w.startX, w.startY)
                    val e = worldToScreen(w.endX, w.endY)
                    val isSel = SelectionItem.Wall(w.id) in selectedIds
                    drawLine(
                        color = if (isSel) Color(0xFFF59E0B) else parseHexColor(w.interiorColorHex, Color.White),
                        start = s,
                        end = e,
                        strokeWidth = max(5f, w.thicknessM * zoomPxPerMeter),
                        cap = StrokeCap.Square
                    )
                    if (isLayerVisible(LayerKind.ANNOTATIONS)) {
                        val lenText = ArchitectureMath.formatLength(ArchitectureMath.wallLengthMeters(w), project.unitSystem)
                        val dim = textMeasurer.measure(lenText, style = TextStyle(color = Color(0xFF38BDF8), fontSize = 10.sp))
                        drawText(dim, topLeft = Offset((s.x + e.x) / 2f - dim.size.width / 2f, (s.y + e.y) / 2f - 18f))
                    }
                }
            }

            // 5. Doors & Windows with swing arcs
            if (isLayerVisible(LayerKind.ARCHITECTURAL)) {
                for (op in floor.openings) {
                    val c = worldToScreen(op.x, op.y)
                    val half = (op.widthM * zoomPxPerMeter) / 2f
                    val col = if (op.type.isDoor) Color(0xFFF59E0B) else Color(0xFF38BDF8)
                    val isSel = SelectionItem.Opening(op.id) in selectedIds
                    rotate(op.angleDeg, pivot = c) {
                        drawLine(
                            color = if (isSel) Color.White else col,
                            start = Offset(c.x - half, c.y),
                            end = Offset(c.x + half, c.y),
                            strokeWidth = if (op.type.isDoor) 6f else 5f
                        )
                        if (op.type.isDoor && op.swing != SwingDirection.SLIDE) {
                            drawArc(
                                color = col.copy(alpha = 0.65f),
                                startAngle = 0f,
                                sweepAngle = 90f,
                                useCenter = false,
                                topLeft = Offset(c.x - half, c.y - half * 2f),
                                size = Size(half * 2f, half * 2f),
                                style = Stroke(2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
                            )
                        }
                    }
                }
            }

            // 6. Custom & Geometric Shapes
            if (isLayerVisible(LayerKind.CUSTOM)) {
                for (sh in floor.shapes) {
                    val col = if (SelectionItem.Shape(sh.id) in selectedIds) Color(0xFFF59E0B) else parseHexColor(sh.strokeColorHex)
                    when (sh.type) {
                        SketchShapeType.PENCIL, SketchShapeType.CUSTOM -> {
                            if (sh.points.size >= 2) {
                                val path = Path().apply {
                                    val p0 = worldToScreen(sh.points.first().x, sh.points.first().y)
                                    moveTo(p0.x, p0.y)
                                    sh.points.drop(1).forEach { pt ->
                                        val sp = worldToScreen(pt.x, pt.y)
                                        lineTo(sp.x, sp.y)
                                    }
                                    if (sh.type == SketchShapeType.CUSTOM) close()
                                }
                                drawPath(path, color = col, style = Stroke(sh.strokeWidth))
                            }
                        }
                        SketchShapeType.LINE -> if (sh.points.size >= 2) {
                            drawLine(
                                col,
                                worldToScreen(sh.points[0].x, sh.points[0].y),
                                worldToScreen(sh.points[1].x, sh.points[1].y),
                                strokeWidth = sh.strokeWidth
                            )
                        }
                        SketchShapeType.RECTANGLE, SketchShapeType.SQUARE -> {
                            val tl = worldToScreen(sh.x, sh.y)
                            drawRect(col, tl, Size(sh.width * zoomPxPerMeter, sh.height * zoomPxPerMeter), style = Stroke(sh.strokeWidth))
                        }
                        SketchShapeType.CIRCLE, SketchShapeType.OVAL -> {
                            val tl = worldToScreen(sh.x, sh.y)
                            drawOval(col, tl, Size(sh.width * zoomPxPerMeter, sh.height * zoomPxPerMeter), style = Stroke(sh.strokeWidth))
                        }
                        SketchShapeType.TRIANGLE -> {
                            val tl = worldToScreen(sh.x, sh.y)
                            val wPx = sh.width * zoomPxPerMeter
                            val hPx = sh.height * zoomPxPerMeter
                            val path = Path().apply {
                                moveTo(tl.x + wPx / 2f, tl.y)
                                lineTo(tl.x + wPx, tl.y + hPx)
                                lineTo(tl.x, tl.y + hPx)
                                close()
                            }
                            drawPath(path, col, style = Stroke(sh.strokeWidth))
                        }
                        SketchShapeType.POLYGON -> {
                            val tl = worldToScreen(sh.x, sh.y)
                            val cx = tl.x + (sh.width * zoomPxPerMeter) / 2f
                            val cy = tl.y + (sh.height * zoomPxPerMeter) / 2f
                            val rad = min(sh.width, sh.height) * zoomPxPerMeter / 2f
                            val n = sh.polygonSides.coerceIn(3, 12)
                            val path = Path().apply {
                                for (i in 0 until n) {
                                    val ang = (2.0 * PI * i / n) - PI / 2.0
                                    val px = cx + (rad * cos(ang)).toFloat()
                                    val py = cy + (rad * sin(ang)).toFloat()
                                    if (i == 0) moveTo(px, py) else lineTo(px, py)
                                }
                                close()
                            }
                            drawPath(path, col, style = Stroke(sh.strokeWidth))
                        }
                        SketchShapeType.ARC -> {
                            val tl = worldToScreen(sh.x, sh.y)
                            drawArc(col, startAngle = 180f, sweepAngle = 180f, useCenter = false, topLeft = tl, size = Size(sh.width * zoomPxPerMeter, sh.height * zoomPxPerMeter), style = Stroke(sh.strokeWidth))
                        }
                    }
                }
            }

            // 7. Electrical Wiring & Nodes
            if (isLayerVisible(LayerKind.ELECTRICAL)) {
                val nodeMap = floor.electricalNodes.associateBy { it.id }
                for (e in floor.electricalNodes) {
                    val p = worldToScreen(e.x, e.y)
                    e.connectedToId?.let { targetId ->
                        nodeMap[targetId]?.let { t ->
                            drawLine(
                                color = Color(0xFFF59E0B).copy(alpha = 0.7f),
                                start = p,
                                end = worldToScreen(t.x, t.y),
                                strokeWidth = 2.5f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
                            )
                        }
                    }
                    drawCircle(color = Color(0xFFF59E0B), radius = 14f, center = p)
                    val sym = textMeasurer.measure(e.type.symbol, style = TextStyle(color = Color(0xFF0A1424), fontSize = 9.sp, fontWeight = FontWeight.Bold))
                    drawText(sym, topLeft = Offset(p.x - sym.size.width / 2f, p.y - sym.size.height / 2f))
                }
            }

            // 8. Plumbing Pipelines & Fixtures
            if (isLayerVisible(LayerKind.PLUMBING)) {
                val pMap = floor.plumbingNodes.associateBy { it.id }
                for (pNode in floor.plumbingNodes) {
                    val pos = worldToScreen(pNode.x, pNode.y)
                    pNode.connectedToId?.let { targetId ->
                        pMap[targetId]?.let { t ->
                            drawLine(
                                color = if (pNode.isDrainage) Color(0xFF10B981) else Color(0xFF06B6D4),
                                start = pos,
                                end = worldToScreen(t.x, t.y),
                                strokeWidth = if (pNode.isDrainage) 4.5f else 3f
                            )
                        }
                    }
                    drawCircle(color = if (pNode.isDrainage) Color(0xFF10B981) else Color(0xFF06B6D4), radius = 14f, center = pos)
                    val sym = textMeasurer.measure(pNode.type.symbol, style = TextStyle(color = Color(0xFF0A1424), fontSize = 9.sp, fontWeight = FontWeight.Bold))
                    drawText(sym, topLeft = Offset(pos.x - sym.size.width / 2f, pos.y - sym.size.height / 2f))
                }
            }

            // 9. Measurement Annotations
            if (isLayerVisible(LayerKind.ANNOTATIONS)) {
                for (ann in floor.annotations) {
                    val s = worldToScreen(ann.x1, ann.y1)
                    val e = worldToScreen(ann.x2, ann.y2)
                    drawLine(Color(0xFF38BDF8), s, e, strokeWidth = 2f)
                    val dist = hypot(ann.x2 - ann.x1, ann.y2 - ann.y1)
                    val label = ArchitectureMath.formatLength(dist, project.unitSystem)
                    val lbl = textMeasurer.measure(label, style = TextStyle(color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold))
                    drawText(lbl, topLeft = Offset((s.x + e.x) / 2f - lbl.size.width / 2f, (s.y + e.y) / 2f - 16f))
                }
            }

            // 10. Live Drag Preview + Alignment Guides
            val d1 = dragStartWorld
            val d2 = dragCurrentWorld
            if (d1 != null && d2 != null && activeTool != DrawingTool.SELECT && activeTool != DrawingTool.ERASER) {
                val s1 = worldToScreen(d1.x, d1.y)
                val s2 = worldToScreen(d2.x, d2.y)
                if (project.guidesEnabled) {
                    val guideEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                    drawLine(Color(0xFF38BDF8).copy(alpha = 0.5f), Offset(s2.x, 0f), Offset(s2.x, size.height), strokeWidth = 1.2f, pathEffect = guideEffect)
                    drawLine(Color(0xFF38BDF8).copy(alpha = 0.5f), Offset(0f, s2.y), Offset(size.width, s2.y), strokeWidth = 1.2f, pathEffect = guideEffect)
                }
                drawLine(Color(0xFFF59E0B), s1, s2, strokeWidth = 4f)
                val liveDist = ArchitectureMath.formatLength(hypot(d2.x - d1.x, d2.y - d1.y), project.unitSystem)
                val liveLbl = textMeasurer.measure(liveDist, style = TextStyle(color = Color(0xFFF59E0B), fontSize = 12.sp, fontWeight = FontWeight.Bold))
                drawText(liveLbl, topLeft = Offset(s2.x + 12f, s2.y - 24f))
            }

            // Custom shape in-progress vertices
            if (customVertices.isNotEmpty()) {
                customVertices.forEachIndexed { idx, pt ->
                    val sp = worldToScreen(pt.x, pt.y)
                    drawCircle(Color(0xFFF59E0B), 8f, sp)
                    if (idx > 0) {
                        val prev = worldToScreen(customVertices[idx - 1].x, customVertices[idx - 1].y)
                        drawLine(Color(0xFFF59E0B), prev, sp, strokeWidth = 3f)
                    }
                }
            }

            // 11. Calibrated Architectural Rulers (Top & Left)
            if (project.rulerEnabled) {
                drawRect(Color(0xCC0F172A), Offset.Zero, Size(size.width, 26f))
                drawRect(Color(0xCC0F172A), Offset.Zero, Size(32f, size.height))
                val startM = ((-panOffset.x) / zoomPxPerMeter).toInt()
                val endM = ((size.width - panOffset.x) / zoomPxPerMeter).toInt()
                for (m in startM..endM step 2) {
                    val sx = panOffset.x + m * zoomPxPerMeter
                    if (sx > 34f) {
                        val txt = textMeasurer.measure("${m}m", style = TextStyle(color = Color(0xFF94A3B8), fontSize = 9.sp))
                        drawText(txt, topLeft = Offset(sx + 2f, 5f))
                    }
                }
            }
        }

        // Custom shape finish button
        if (customVertices.size >= 3) {
            Button(
                onClick = {
                    onFinishShape(
                        SketchShape(
                            id = "s_${UUID.randomUUID().toString().take(7)}",
                            type = SketchShapeType.CUSTOM,
                            points = customVertices.toList(),
                            strokeColorHex = activeColorHex
                        )
                    )
                    customVertices.clear()
                },
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 34.dp)
            ) {
                Text("Close Custom Shape (${customVertices.size} pts)")
            }
        }

        // Zoom controls + Watermark
        Row(
            modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)) {
                IconButton(onClick = { zoomPxPerMeter = (zoomPxPerMeter * 0.85f).coerceAtLeast(22f) }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.ZoomOut, contentDescription = "Zoom Out", modifier = Modifier.size(18.dp))
                }
            }
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)) {
                IconButton(onClick = { zoomPxPerMeter = (zoomPxPerMeter * 1.18f).coerceAtMost(140f) }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.ZoomIn, contentDescription = "Zoom In", modifier = Modifier.size(18.dp))
                }
            }
            CsiWatermarkBadge()
        }
    }
}
