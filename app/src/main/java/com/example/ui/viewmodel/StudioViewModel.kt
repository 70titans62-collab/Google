package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ai.AiAssistantState
import com.example.ai.AiProposedDesignPatch
import com.example.ai.HouseDesignAiAssistant
import com.example.data.local.AppSettings
import com.example.data.local.CsiDatabase
import com.example.data.local.LocalVersionEntity
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.ArchitectureMath
import com.example.data.model.CeilingDesign
import com.example.data.model.ConstructionEstimate
import com.example.data.model.DesignConflictWarning
import com.example.data.model.DrawingTool
import com.example.data.model.ElectricalNode
import com.example.data.model.ElectricalType
import com.example.data.model.ExteriorConfig
import com.example.data.model.FloorPlan
import com.example.data.model.FurnitureItem
import com.example.data.model.FurnitureType
import com.example.data.model.LayerKind
import com.example.data.model.MaterialPrices
import com.example.data.model.MeasurementAnnotation
import com.example.data.model.OpeningItem
import com.example.data.model.OpeningType
import com.example.data.model.PlumbingNode
import com.example.data.model.PlumbingType
import com.example.data.model.Point2D
import com.example.data.model.ProjectCategory
import com.example.data.model.ProjectDocument
import com.example.data.model.ProjectSerialization
import com.example.data.model.ProjectTemplates
import com.example.data.model.RoomZone
import com.example.data.model.SketchShape
import com.example.data.model.SketchShapeType
import com.example.data.model.StudioMode
import com.example.data.model.SurfaceMaterial
import com.example.data.model.SwingDirection
import com.example.data.model.UnitSystem
import com.example.data.model.WallSegment
import com.example.data.repository.CloudProjectRecord
import com.example.data.repository.CloudProjectRepository
import com.example.data.repository.DesignRepository
import com.example.data.repository.ShareCommentRecord
import com.example.data.repository.SharedDesignRecord
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface SelectionItem {
    data class Wall(val id: String) : SelectionItem
    data class Room(val id: String) : SelectionItem
    data class Opening(val id: String) : SelectionItem
    data class Shape(val id: String) : SelectionItem
    data class Furniture(val id: String) : SelectionItem
    data class Electrical(val id: String) : SelectionItem
    data class Plumbing(val id: String) : SelectionItem
}

data class BackupProgressState(
    val isRunning: Boolean = false,
    val progress: Float = 0f,
    val statusMessage: String = "",
    val errorMessage: String? = null
)

class StudioViewModel(
    private val designRepo: DesignRepository,
    private val cloudRepo: CloudProjectRepository?,
    private val prefsRepo: UserPreferencesRepository,
    private val aiAssistant: HouseDesignAiAssistant,
    val ownerUid: String,
    val isSignedIn: Boolean,
    val userDisplayName: String
) : ViewModel() {

    val settings: StateFlow<AppSettings> = prefsRepo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AppSettings())

    val allProjects: StateFlow<List<ProjectDocument>> = designRepo.observeLocalProjects(ownerUid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _activeProject = MutableStateFlow<ProjectDocument?>(null)
    val activeProject: StateFlow<ProjectDocument?> = _activeProject.asStateFlow()

    private val _studioMode = MutableStateFlow(StudioMode.ARCHITECTURAL_2D)
    val studioMode: StateFlow<StudioMode> = _studioMode.asStateFlow()

    private val _activeTool = MutableStateFlow(DrawingTool.WALL)
    val activeTool: StateFlow<DrawingTool> = _activeTool.asStateFlow()

    private val _activeColorHex = MutableStateFlow("#38BDF8")
    val activeColorHex: StateFlow<String> = _activeColorHex.asStateFlow()

    private val _eraserRadiusM = MutableStateFlow(0.6f)
    val eraserRadiusM: StateFlow<Float> = _eraserRadiusM.asStateFlow()

    private val _polygonSides = MutableStateFlow(5)
    val polygonSides: StateFlow<Int> = _polygonSides.asStateFlow()

    private val _selectedOpeningType = MutableStateFlow(OpeningType.WOODEN_DOOR)
    val selectedOpeningType: StateFlow<OpeningType> = _selectedOpeningType.asStateFlow()

    private val _selectedSwing = MutableStateFlow(SwingDirection.LEFT_IN)
    val selectedSwing: StateFlow<SwingDirection> = _selectedSwing.asStateFlow()

    private val _selectedFurnitureType = MutableStateFlow(FurnitureType.SOFA)
    val selectedFurnitureType: StateFlow<FurnitureType> = _selectedFurnitureType.asStateFlow()

    private val _selectedElectricalType = MutableStateFlow(ElectricalType.LIGHT_POINT)
    val selectedElectricalType: StateFlow<ElectricalType> = _selectedElectricalType.asStateFlow()

    private val _selectedPlumbingType = MutableStateFlow(PlumbingType.WASH_BASIN)
    val selectedPlumbingType: StateFlow<PlumbingType> = _selectedPlumbingType.asStateFlow()

    private val _selectedSurfaceMaterial = MutableStateFlow(SurfaceMaterial.WOODEN_FLOORING)
    val selectedSurfaceMaterial: StateFlow<SurfaceMaterial> = _selectedSurfaceMaterial.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<SelectionItem>>(emptySet())
    val selectedIds: StateFlow<Set<SelectionItem>> = _selectedIds.asStateFlow()

    private val _clipboardShape = MutableStateFlow<SketchShape?>(null)
    private val _clipboardFurniture = MutableStateFlow<FurnitureItem?>(null)

    private val undoStack = ArrayDeque<ProjectDocument>()
    private val redoStack = ArrayDeque<ProjectDocument>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    // Cloud backups & shared designs (only collected when isSignedIn == true)
    private val _cloudBackups = MutableStateFlow<List<CloudProjectRecord>>(emptyList())
    val cloudBackups: StateFlow<List<CloudProjectRecord>> = _cloudBackups.asStateFlow()

    private val _mySharedDesigns = MutableStateFlow<List<SharedDesignRecord>>(emptyList())
    val mySharedDesigns: StateFlow<List<SharedDesignRecord>> = _mySharedDesigns.asStateFlow()

    private val _publicSharedDesigns = MutableStateFlow<List<SharedDesignRecord>>(emptyList())
    val publicSharedDesigns: StateFlow<List<SharedDesignRecord>> = _publicSharedDesigns.asStateFlow()

    private val _backupState = MutableStateFlow(BackupProgressState())
    val backupState: StateFlow<BackupProgressState> = _backupState.asStateFlow()

    private val _aiState = MutableStateFlow<AiAssistantState>(AiAssistantState.Idle)
    val aiState: StateFlow<AiAssistantState> = _aiState.asStateFlow()

    private val _activeProjectVersions = MutableStateFlow<List<LocalVersionEntity>>(emptyList())
    val activeProjectVersions: StateFlow<List<LocalVersionEntity>> = _activeProjectVersions.asStateFlow()

    private var autoSaveJob: Job? = null
    private var versionObserveJob: Job? = null

    init {
        viewModelScope.launch {
            designRepo.ensureSeedTemplates(ownerUid)
            allProjects.collect { projects ->
                if (_activeProject.value == null && projects.isNotEmpty()) {
                    val lastId = settings.value.lastActiveProjectId
                    val target = projects.find { it.id == lastId && !it.isDeleted }
                        ?: projects.firstOrNull { !it.isDeleted }
                        ?: projects.first()
                    selectProject(target)
                }
            }
        }
        if (isSignedIn && cloudRepo != null) {
            observeCloudCollections()
        }
    }

    private fun observeCloudCollections() {
        val repo = cloudRepo ?: return
        viewModelScope.launch {
            repo.observeCloudProjects()
                .catch { e ->
                    if (e is CancellationException) throw e
                    Log.w(TAG, "Cloud projects stream error", e)
                }
                .collect { _cloudBackups.value = it }
        }
        viewModelScope.launch {
            repo.observeMySharedDesigns()
                .catch { e ->
                    if (e is CancellationException) throw e
                    Log.w(TAG, "My shared designs stream error", e)
                }
                .collect { _mySharedDesigns.value = it }
        }
        viewModelScope.launch {
            repo.observePublicSharedDesigns()
                .catch { e ->
                    if (e is CancellationException) throw e
                    Log.w(TAG, "Public shared designs stream error", e)
                }
                .collect { _publicSharedDesigns.value = it }
        }
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun showMessage(msg: String) {
        _statusBanner.value = msg
    }

    fun selectProject(project: ProjectDocument) {
        _activeProject.value = project
        _selectedIds.value = emptySet()
        undoStack.clear()
        redoStack.clear()
        _canUndo.value = false
        _canRedo.value = false
        viewModelScope.launch {
            prefsRepo.setLastActiveProjectId(project.id)
        }
        versionObserveJob?.cancel()
        versionObserveJob = viewModelScope.launch {
            designRepo.observeLocalVersions(project.id).collect {
                _activeProjectVersions.value = it
            }
        }
    }

    fun activeFloor(): FloorPlan? {
        val p = _activeProject.value ?: return null
        return p.floors.find { it.id == p.activeFloorId } ?: p.floors.firstOrNull()
    }

    fun computeConflicts(): List<DesignConflictWarning> {
        val floor = activeFloor() ?: return emptyList()
        return ArchitectureMath.analyzeDesignConflicts(floor)
    }

    fun computeEstimate(): ConstructionEstimate? {
        val p = _activeProject.value ?: return null
        return ArchitectureMath.calculateEstimate(p, settings.value.materialPrices)
    }

    private fun commitProjectEdit(saveVersion: Boolean = false, versionSummary: String = "Auto-saved", transform: (ProjectDocument) -> ProjectDocument) {
        val current = _activeProject.value ?: return
        val next = transform(current)
        if (next == current) return

        undoStack.addLast(current)
        if (undoStack.size > 40) undoStack.removeFirst()
        redoStack.clear()
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = false
        _activeProject.value = next

        scheduleAutoSave(next, saveVersion, versionSummary)
    }

    private fun commitFloorEdit(saveVersion: Boolean = false, transform: (FloorPlan) -> FloorPlan) {
        commitProjectEdit(saveVersion) { proj ->
            val floorId = proj.activeFloorId
            val updatedFloors = proj.floors.map { fl ->
                if (fl.id == floorId) transform(fl) else fl
            }
            proj.copy(floors = updatedFloors)
        }
    }

    private fun scheduleAutoSave(project: ProjectDocument, saveVersion: Boolean, versionSummary: String) {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(350L)
            val saved = designRepo.saveProjectLocally(
                project = project,
                pendingSync = isSignedIn,
                saveVersion = saveVersion,
                versionSummary = versionSummary
            )
            _activeProject.value = saved
            if (isSignedIn && settings.value.autoBackupEnabled) {
                cloudRepo?.backupProject(saved)
            }
        }
    }

    fun saveManualRevision(summary: String) {
        val current = _activeProject.value ?: return
        viewModelScope.launch {
            val saved = designRepo.saveProjectLocally(current, pendingSync = isSignedIn, saveVersion = true, versionSummary = summary)
            _activeProject.value = saved
            if (isSignedIn) {
                cloudRepo?.backupProject(saved)
                cloudRepo?.saveCloudVersion(saved, summary)
            }
            _statusBanner.value = "Saved version v${saved.version}"
        }
    }

    fun restoreVersion(version: LocalVersionEntity) {
        val restored = ProjectSerialization.fromJson(version.snapshotJson) ?: return
        commitProjectEdit(saveVersion = true, versionSummary = "Restored from v${version.versionNumber}") {
            restored.copy(version = it.version)
        }
        _statusBanner.value = "Restored design to v${version.versionNumber}"
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val current = _activeProject.value ?: return
        val prev = undoStack.removeLast()
        redoStack.addLast(current)
        _activeProject.value = prev
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
        scheduleAutoSave(prev, false, "Undo")
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val current = _activeProject.value ?: return
        val next = redoStack.removeLast()
        undoStack.addLast(current)
        _activeProject.value = next
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
        scheduleAutoSave(next, false, "Redo")
    }

    // --- Studio Tool & Mode Controls ---
    fun setStudioMode(mode: StudioMode) { _studioMode.value = mode }
    fun setDrawingTool(tool: DrawingTool) { _activeTool.value = tool }
    fun setActiveColor(hex: String) { _activeColorHex.value = hex }
    fun setEraserRadius(r: Float) { _eraserRadiusM.value = r }
    fun setPolygonSides(sides: Int) { _polygonSides.value = sides.coerceIn(3, 12) }
    fun setOpeningType(type: OpeningType) { _selectedOpeningType.value = type }
    fun setSwingDirection(swing: SwingDirection) { _selectedSwing.value = swing }
    fun setFurnitureType(type: FurnitureType) { _selectedFurnitureType.value = type }
    fun setElectricalType(type: ElectricalType) { _selectedElectricalType.value = type }
    fun setPlumbingType(type: PlumbingType) { _selectedPlumbingType.value = type }
    fun setSurfaceMaterial(mat: SurfaceMaterial) { _selectedSurfaceMaterial.value = mat }

    // --- Canvas Settings ---
    fun toggleGrid() = commitProjectEdit { it.copy(gridEnabled = !it.gridEnabled) }
    fun toggleSnap() = commitProjectEdit { it.copy(snapEnabled = !it.snapEnabled) }
    fun toggleRuler() = commitProjectEdit { it.copy(rulerEnabled = !it.rulerEnabled) }
    fun toggleGuides() = commitProjectEdit { it.copy(guidesEnabled = !it.guidesEnabled) }
    fun setUnitSystem(unit: UnitSystem) = commitProjectEdit { it.copy(unitSystem = unit) }
    fun setDrawingScale(scale: Int) = commitProjectEdit { it.copy(drawingScale = scale) }

    fun toggleLayerVisibility(kind: LayerKind) {
        commitFloorEdit { fl ->
            fl.copy(layers = fl.layers.map { if (it.kind == kind) it.copy(visible = !it.visible) else it })
        }
    }

    fun toggleLayerLock(kind: LayerKind) {
        commitFloorEdit { fl ->
            fl.copy(layers = fl.layers.map { if (it.kind == kind) it.copy(locked = !it.locked) else it })
        }
    }

    // --- Multi-Floor Management ---
    fun switchFloor(floorId: String) {
        commitProjectEdit { it.copy(activeFloorId = floorId) }
        _selectedIds.value = emptySet()
    }

    fun addFloor(name: String, levelIndex: Int) {
        val newFloor = FloorPlan(
            id = "floor_${UUID.randomUUID().toString().take(8)}",
            name = name,
            levelIndex = levelIndex
        )
        commitProjectEdit(saveVersion = true, versionSummary = "Added $name") { proj ->
            val sorted = (proj.floors + newFloor).sortedBy { it.levelIndex }
            proj.copy(floors = sorted, activeFloorId = newFloor.id)
        }
    }

    fun duplicateActiveFloor() {
        val active = activeFloor() ?: return
        val nextLevel = (activeProject.value?.floors?.maxOfOrNull { it.levelIndex } ?: 0) + 1
        val copy = active.copy(
            id = "floor_${UUID.randomUUID().toString().take(8)}",
            name = "${active.name} (Level $nextLevel)",
            levelIndex = nextLevel
        )
        commitProjectEdit(saveVersion = true, versionSummary = "Duplicated floor layout") { proj ->
            proj.copy(floors = (proj.floors + copy).sortedBy { it.levelIndex }, activeFloorId = copy.id)
        }
    }

    fun toggleFloorVisibility(floorId: String) {
        commitProjectEdit { proj ->
            proj.copy(floors = proj.floors.map { if (it.id == floorId) it.copy(isVisible = !it.isVisible) else it })
        }
    }

    // --- Drawing Actions ---
    fun addWall(x1: Float, y1: Float, x2: Float, y2: Float, thicknessM: Float = 0.23f) {
        if (kotlin.math.hypot(x2 - x1, y2 - y1) < 0.2f) return
        val wall = WallSegment(
            id = "w_${UUID.randomUUID().toString().take(7)}",
            startX = x1,
            startY = y1,
            endX = x2,
            endY = y2,
            thicknessM = thicknessM,
            interiorColorHex = _activeColorHex.value
        )
        commitFloorEdit { fl -> fl.copy(walls = fl.walls + wall) }
    }

    fun addRoomZone(x: Float, y: Float, w: Float, h: Float, name: String) {
        if (kotlin.math.abs(w) < 0.5f || kotlin.math.abs(h) < 0.5f) return
        val normX = minOf(x, x + w)
        val normY = minOf(y, y + h)
        val room = RoomZone(
            id = "r_${UUID.randomUUID().toString().take(7)}",
            name = name,
            x = normX,
            y = normY,
            widthM = kotlin.math.abs(w),
            depthM = kotlin.math.abs(h),
            floorMaterial = _selectedSurfaceMaterial.value,
            floorColorHex = _selectedSurfaceMaterial.value.defaultHex
        )
        commitFloorEdit { fl -> fl.copy(rooms = fl.rooms + room) }
    }

    fun addSketchShape(shape: SketchShape) {
        commitFloorEdit { fl -> fl.copy(shapes = fl.shapes + shape) }
    }

    fun addOpeningAt(x: Float, y: Float) {
        val fl = activeFloor() ?: return
        val type = _selectedOpeningType.value
        // Find nearest wall to snap angle & attach hostWallId
        var nearestWall: WallSegment? = null
        var bestDist = 1.0f
        for (w in fl.walls) {
            val mx = (w.startX + w.endX) / 2f
            val my = (w.startY + w.endY) / 2f
            // Distance to segment
            val dx = w.endX - w.startX
            val dy = w.endY - w.startY
            val lenSq = dx * dx + dy * dy
            val t = if (lenSq > 0.001f) (((x - w.startX) * dx + (y - w.startY) * dy) / lenSq).coerceIn(0f, 1f) else 0f
            val px = w.startX + t * dx
            val py = w.startY + t * dy
            val d = kotlin.math.hypot(x - px, y - py)
            if (d < bestDist) {
                bestDist = d
                nearestWall = w
            }
        }
        val angleDeg = if (nearestWall != null) {
            Math.toDegrees(kotlin.math.atan2((nearestWall.endY - nearestWall.startY).toDouble(), (nearestWall.endX - nearestWall.startX).toDouble())).toFloat()
        } else 0f

        val op = OpeningItem(
            id = "op_${UUID.randomUUID().toString().take(7)}",
            type = type,
            x = x,
            y = y,
            angleDeg = angleDeg,
            widthM = type.defaultWidthM,
            heightM = type.defaultHeightM,
            sillHeightM = type.defaultSillM,
            swing = _selectedSwing.value,
            colorHex = _activeColorHex.value,
            hostWallId = nearestWall?.id
        )
        commitFloorEdit { f -> f.copy(openings = f.openings + op) }
    }

    fun addFurnitureAt(x: Float, y: Float) {
        val type = _selectedFurnitureType.value
        val item = FurnitureItem(
            id = "f_${UUID.randomUUID().toString().take(7)}",
            type = type,
            x = x - type.defaultWidthM / 2f,
            y = y - type.defaultDepthM / 2f,
            widthM = type.defaultWidthM,
            depthM = type.defaultDepthM,
            heightM = type.defaultHeightM,
            colorHex = type.defaultColorHex
        )
        commitFloorEdit { fl -> fl.copy(furniture = fl.furniture + item) }
    }

    fun addElectricalAt(x: Float, y: Float) {
        val fl = activeFloor() ?: return
        val type = _selectedElectricalType.value
        val connectTo = fl.electricalNodes.lastOrNull {
            it.type == ElectricalType.SWITCH_BOARD || it.type == ElectricalType.DISTRIBUTION_BOARD
        }?.id
        val node = ElectricalNode(
            id = "e_${UUID.randomUUID().toString().take(7)}",
            type = type,
            x = x,
            y = y,
            circuitName = "C${(fl.electricalNodes.size / 4) + 1}",
            connectedToId = if (type == ElectricalType.DISTRIBUTION_BOARD) null else connectTo,
            wattage = type.watts
        )
        commitFloorEdit { f -> f.copy(electricalNodes = f.electricalNodes + node) }
    }

    fun addPlumbingAt(x: Float, y: Float) {
        val fl = activeFloor() ?: return
        val type = _selectedPlumbingType.value
        val connectTo = fl.plumbingNodes.firstOrNull { it.type == PlumbingType.WATER_TANK }?.id
        val node = PlumbingNode(
            id = "p_${UUID.randomUUID().toString().take(7)}",
            type = type,
            x = x,
            y = y,
            pipeDiameterMm = if (type.isDrainage) 110 else 25,
            isDrainage = type.isDrainage,
            connectedToId = if (type == PlumbingType.WATER_TANK) null else connectTo
        )
        commitFloorEdit { f -> f.copy(plumbingNodes = f.plumbingNodes + node) }
    }

    fun addMeasurementAnnotation(x1: Float, y1: Float, x2: Float, y2: Float, label: String = "") {
        val ann = MeasurementAnnotation(
            id = "m_${UUID.randomUUID().toString().take(7)}",
            x1 = x1,
            y1 = y1,
            x2 = x2,
            y2 = y2,
            label = label
        )
        commitFloorEdit { fl -> fl.copy(annotations = fl.annotations + ann) }
    }

    fun eraseNear(x: Float, y: Float) {
        val r = _eraserRadiusM.value
        commitFloorEdit { fl ->
            fl.copy(
                walls = fl.walls.filterNot { w ->
                    w.locked || kotlin.math.hypot((w.startX + w.endX) / 2f - x, (w.startY + w.endY) / 2f - y) < r
                },
                openings = fl.openings.filterNot { op ->
                    op.locked || kotlin.math.hypot(op.x - x, op.y - y) < r
                },
                furniture = fl.furniture.filterNot { f ->
                    f.locked || kotlin.math.hypot(f.x + f.widthM / 2f - x, f.y + f.depthM / 2f - y) < r
                },
                shapes = fl.shapes.filterNot { s ->
                    s.locked || kotlin.math.hypot(s.x - x, s.y - y) < r || s.points.any { p -> kotlin.math.hypot(p.x - x, p.y - y) < r }
                },
                electricalNodes = fl.electricalNodes.filterNot { e ->
                    kotlin.math.hypot(e.x - x, e.y - y) < r
                },
                plumbingNodes = fl.plumbingNodes.filterNot { p ->
                    kotlin.math.hypot(p.x - x, p.y - y) < r
                },
                annotations = fl.annotations.filterNot { a ->
                    kotlin.math.hypot((a.x1 + a.x2) / 2f - x, (a.y1 + a.y2) / 2f - y) < r
                }
            )
        }
    }

    // --- Selection, Move, Resize, Rotate, Lock, Group, Copy/Paste/Delete ---
    fun selectAt(x: Float, y: Float, multiSelect: Boolean = false) {
        val fl = activeFloor() ?: return
        val hit: SelectionItem? =
            fl.furniture.firstOrNull { x in it.x..(it.x + it.widthM) && y in it.y..(it.y + it.depthM) }?.let { SelectionItem.Furniture(it.id) }
                ?: fl.openings.firstOrNull { kotlin.math.hypot(it.x - x, it.y - y) < 0.7f }?.let { SelectionItem.Opening(it.id) }
                ?: fl.walls.firstOrNull { w ->
                    val dx = w.endX - w.startX
                    val dy = w.endY - w.startY
                    val lenSq = dx * dx + dy * dy
                    val t = if (lenSq > 0.001f) (((x - w.startX) * dx + (y - w.startY) * dy) / lenSq).coerceIn(0f, 1f) else 0f
                    kotlin.math.hypot(x - (w.startX + t * dx), y - (w.startY + t * dy)) < 0.45f
                }?.let { SelectionItem.Wall(it.id) }
                ?: fl.shapes.firstOrNull { s ->
                    kotlin.math.hypot(s.x - x, s.y - y) < 1.0f || (x in s.x..(s.x + s.width) && y in s.y..(s.y + s.height))
                }?.let { SelectionItem.Shape(it.id) }
                ?: fl.rooms.firstOrNull { x in it.x..(it.x + it.widthM) && y in it.y..(it.y + it.depthM) }?.let { SelectionItem.Room(it.id) }

        if (hit == null) {
            if (!multiSelect) _selectedIds.value = emptySet()
        } else {
            _selectedIds.value = if (multiSelect) {
                if (hit in _selectedIds.value) _selectedIds.value - hit else _selectedIds.value + hit
            } else {
                setOf(hit)
            }
        }
    }

    fun moveSelectedBy(dx: Float, dy: Float) {
        val sel = _selectedIds.value
        if (sel.isEmpty()) return
        commitFloorEdit { fl ->
            fl.copy(
                walls = fl.walls.map { w ->
                    if (!w.locked && SelectionItem.Wall(w.id) in sel) {
                        w.copy(startX = w.startX + dx, startY = w.startY + dy, endX = w.endX + dx, endY = w.endY + dy)
                    } else w
                },
                furniture = fl.furniture.map { f ->
                    if (!f.locked && SelectionItem.Furniture(f.id) in sel) f.copy(x = f.x + dx, y = f.y + dy) else f
                },
                openings = fl.openings.map { op ->
                    if (!op.locked && SelectionItem.Opening(op.id) in sel) op.copy(x = op.x + dx, y = op.y + dy) else op
                },
                shapes = fl.shapes.map { s ->
                    if (!s.locked && SelectionItem.Shape(s.id) in sel) {
                        s.copy(x = s.x + dx, y = s.y + dy, points = s.points.map { Point2D(it.x + dx, it.y + dy) })
                    } else s
                },
                rooms = fl.rooms.map { r ->
                    if (!r.locked && SelectionItem.Room(r.id) in sel) r.copy(x = r.x + dx, y = r.y + dy) else r
                }
            )
        }
    }

    fun rotateSelectedBy(deltaDeg: Float) {
        val sel = _selectedIds.value
        if (sel.isEmpty()) return
        commitFloorEdit { fl ->
            fl.copy(
                furniture = fl.furniture.map { f ->
                    if (!f.locked && SelectionItem.Furniture(f.id) in sel) f.copy(rotationDeg = (f.rotationDeg + deltaDeg) % 360f) else f
                },
                openings = fl.openings.map { op ->
                    if (!op.locked && SelectionItem.Opening(op.id) in sel) op.copy(angleDeg = (op.angleDeg + deltaDeg) % 360f) else op
                },
                shapes = fl.shapes.map { s ->
                    if (!s.locked && SelectionItem.Shape(s.id) in sel) s.copy(rotationDeg = (s.rotationDeg + deltaDeg) % 360f) else s
                }
            )
        }
    }

    fun resizeSelected(scaleFactor: Float) {
        val sel = _selectedIds.value
        if (sel.isEmpty()) return
        commitFloorEdit { fl ->
            fl.copy(
                furniture = fl.furniture.map { f ->
                    if (!f.locked && SelectionItem.Furniture(f.id) in sel) {
                        f.copy(widthM = (f.widthM * scaleFactor).coerceIn(0.3f, 6f), depthM = (f.depthM * scaleFactor).coerceIn(0.3f, 6f))
                    } else f
                },
                openings = fl.openings.map { op ->
                    if (!op.locked && SelectionItem.Opening(op.id) in sel) op.copy(widthM = (op.widthM * scaleFactor).coerceIn(0.4f, 4f)) else op
                },
                shapes = fl.shapes.map { s ->
                    if (!s.locked && SelectionItem.Shape(s.id) in sel) {
                        s.copy(width = (s.width * scaleFactor).coerceIn(0.2f, 20f), height = (s.height * scaleFactor).coerceIn(0.2f, 20f))
                    } else s
                },
                rooms = fl.rooms.map { r ->
                    if (!r.locked && SelectionItem.Room(r.id) in sel) {
                        r.copy(widthM = (r.widthM * scaleFactor).coerceIn(1f, 30f), depthM = (r.depthM * scaleFactor).coerceIn(1f, 30f))
                    } else r
                }
            )
        }
    }

    fun toggleLockSelected() {
        val sel = _selectedIds.value
        if (sel.isEmpty()) return
        commitFloorEdit { fl ->
            fl.copy(
                walls = fl.walls.map { if (SelectionItem.Wall(it.id) in sel) it.copy(locked = !it.locked) else it },
                furniture = fl.furniture.map { if (SelectionItem.Furniture(it.id) in sel) it.copy(locked = !it.locked) else it },
                openings = fl.openings.map { if (SelectionItem.Opening(it.id) in sel) it.copy(locked = !it.locked) else it },
                shapes = fl.shapes.map { if (SelectionItem.Shape(it.id) in sel) it.copy(locked = !it.locked) else it },
                rooms = fl.rooms.map { if (SelectionItem.Room(it.id) in sel) it.copy(locked = !it.locked) else it }
            )
        }
    }

    fun groupSelected() {
        val sel = _selectedIds.value
        if (sel.size < 2) return
        val grp = "grp_${UUID.randomUUID().toString().take(6)}"
        commitFloorEdit { fl ->
            fl.copy(
                walls = fl.walls.map { if (SelectionItem.Wall(it.id) in sel) it.copy(groupId = grp) else it },
                furniture = fl.furniture.map { if (SelectionItem.Furniture(it.id) in sel) it.copy(groupId = grp) else it },
                shapes = fl.shapes.map { if (SelectionItem.Shape(it.id) in sel) it.copy(groupId = grp) else it },
                rooms = fl.rooms.map { if (SelectionItem.Room(it.id) in sel) it.copy(groupId = grp) else it }
            )
        }
        _statusBanner.value = "Grouped ${sel.size} objects"
    }

    fun copySelected() {
        val fl = activeFloor() ?: return
        val sel = _selectedIds.value
        _clipboardFurniture.value = fl.furniture.firstOrNull { SelectionItem.Furniture(it.id) in sel }
        _clipboardShape.value = fl.shapes.firstOrNull { SelectionItem.Shape(it.id) in sel }
        if (_clipboardFurniture.value != null || _clipboardShape.value != null) {
            _statusBanner.value = "Copied object to clipboard"
        }
    }

    fun pasteClipboard() {
        val f = _clipboardFurniture.value
        val s = _clipboardShape.value
        if (f != null) {
            val pasted = f.copy(id = "f_${UUID.randomUUID().toString().take(7)}", x = f.x + 0.6f, y = f.y + 0.6f)
            commitFloorEdit { fl -> fl.copy(furniture = fl.furniture + pasted) }
        } else if (s != null) {
            val pasted = s.copy(
                id = "s_${UUID.randomUUID().toString().take(7)}",
                x = s.x + 0.6f,
                y = s.y + 0.6f,
                points = s.points.map { Point2D(it.x + 0.6f, it.y + 0.6f) }
            )
            commitFloorEdit { fl -> fl.copy(shapes = fl.shapes + pasted) }
        }
    }

    fun duplicateSelected() {
        copySelected()
        pasteClipboard()
    }

    fun deleteSelected() {
        val sel = _selectedIds.value
        if (sel.isEmpty()) return
        commitFloorEdit { fl ->
            fl.copy(
                walls = fl.walls.filterNot { !it.locked && SelectionItem.Wall(it.id) in sel },
                furniture = fl.furniture.filterNot { !it.locked && SelectionItem.Furniture(it.id) in sel },
                openings = fl.openings.filterNot { !it.locked && SelectionItem.Opening(it.id) in sel },
                shapes = fl.shapes.filterNot { !it.locked && SelectionItem.Shape(it.id) in sel },
                rooms = fl.rooms.filterNot { !it.locked && SelectionItem.Room(it.id) in sel }
            )
        }
        _selectedIds.value = emptySet()
    }

    // --- Interior & Exterior Materials Application ---
    fun applyMaterialToSelectionOrFloor(material: SurfaceMaterial, colorHex: String, ceilingDesign: CeilingDesign? = null) {
        val sel = _selectedIds.value
        commitFloorEdit { fl ->
            if (sel.isEmpty()) {
                // Apply to all rooms or walls matching type
                fl.copy(
                    rooms = fl.rooms.map { r ->
                        var nr = r
                        if (material.isFloor) nr = nr.copy(floorMaterial = material, floorColorHex = colorHex)
                        else nr = nr.copy(wallColorHex = colorHex)
                        if (ceilingDesign != null) nr = nr.copy(ceilingDesign = ceilingDesign)
                        nr
                    },
                    walls = if (!material.isFloor) fl.walls.map { it.copy(material = material, interiorColorHex = colorHex) } else fl.walls
                )
            } else {
                fl.copy(
                    rooms = fl.rooms.map { r ->
                        if (SelectionItem.Room(r.id) in sel) {
                            var nr = if (material.isFloor) r.copy(floorMaterial = material, floorColorHex = colorHex) else r.copy(wallColorHex = colorHex)
                            if (ceilingDesign != null) nr = nr.copy(ceilingDesign = ceilingDesign)
                            nr
                        } else r
                    },
                    walls = fl.walls.map { w ->
                        if (SelectionItem.Wall(w.id) in sel) w.copy(material = material, interiorColorHex = colorHex) else w
                    }
                )
            }
        }
    }

    fun updateExteriorConfig(transform: (ExteriorConfig) -> ExteriorConfig) {
        commitProjectEdit { proj -> proj.copy(exteriorConfig = transform(proj.exteriorConfig)) }
    }

    // --- Project Management (Create, Template, Rename, Duplicate, Recycle Bin, Favorite) ---
    fun createNewProject(name: String, category: ProjectCategory) {
        viewModelScope.launch {
            val blank = ProjectTemplates.createBlankProject(ownerUid, name, category, settings.value.defaultUnit)
            val saved = designRepo.saveProjectLocally(blank, pendingSync = isSignedIn, saveVersion = true, versionSummary = "Created project")
            selectProject(saved)
        }
    }

    fun createFromTemplate(builder: (String) -> ProjectDocument) {
        viewModelScope.launch {
            val proj = builder(ownerUid)
            val saved = designRepo.saveProjectLocally(proj, pendingSync = isSignedIn, saveVersion = true, versionSummary = "Created from template")
            selectProject(saved)
        }
    }

    fun renameProject(project: ProjectDocument, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            val updated = designRepo.saveProjectLocally(project.copy(name = newName.trim()), pendingSync = isSignedIn)
            if (_activeProject.value?.id == updated.id) _activeProject.value = updated
        }
    }

    fun toggleFavorite(project: ProjectDocument) {
        viewModelScope.launch {
            val updated = designRepo.saveProjectLocally(project.copy(isFavorite = !project.isFavorite), pendingSync = isSignedIn)
            if (_activeProject.value?.id == updated.id) _activeProject.value = updated
        }
    }

    fun duplicateProject(project: ProjectDocument) {
        viewModelScope.launch {
            val copy = designRepo.duplicateProject(project, ownerUid)
            selectProject(copy)
            _statusBanner.value = "Duplicated '${project.name}'"
        }
    }

    fun moveToRecycleBin(project: ProjectDocument) {
        viewModelScope.launch {
            designRepo.moveToRecycleBin(project)
            if (_activeProject.value?.id == project.id) {
                val other = allProjects.value.firstOrNull { it.id != project.id && !it.isDeleted }
                _activeProject.value = other
            }
            _statusBanner.value = "Moved '${project.name}' to Recycle Bin"
        }
    }

    fun restoreFromRecycleBin(project: ProjectDocument) {
        viewModelScope.launch {
            designRepo.restoreFromRecycleBin(project)
            _statusBanner.value = "Restored '${project.name}'"
        }
    }

    fun deletePermanently(projectId: String) {
        viewModelScope.launch {
            designRepo.deletePermanently(projectId, isSignedIn)
            _statusBanner.value = "Deleted permanently"
        }
    }

    fun importNativeCsiJson(json: String) {
        val parsed = ProjectSerialization.fromJson(json)
        if (parsed == null) {
            _statusBanner.value = "Invalid .csi project file"
            return
        }
        viewModelScope.launch {
            val imported = parsed.copy(
                id = "proj_${UUID.randomUUID().toString().take(10)}",
                userId = ownerUid,
                name = "${parsed.name} (Imported)"
            )
            val saved = designRepo.saveProjectLocally(imported, pendingSync = isSignedIn, saveVersion = true, versionSummary = "Imported .csi design")
            selectProject(saved)
            _statusBanner.value = "Imported '${saved.name}' (fully editable)"
        }
    }

    // --- Google Cloud Backup & Restore ---
    fun backupAllProjects() {
        if (!isSignedIn) {
            _backupState.value = BackupProgressState(errorMessage = "Please sign in with Google to back up projects.")
            return
        }
        val activeList = allProjects.value.filter { !it.isDeleted }
        if (activeList.isEmpty()) return
        viewModelScope.launch {
            _backupState.value = BackupProgressState(isRunning = true, progress = 0.05f, statusMessage = "Preparing ${activeList.size} projects...")
            var failures = 0
            activeList.forEachIndexed { idx, proj ->
                _backupState.value = BackupProgressState(
                    isRunning = true,
                    progress = (idx + 0.5f) / activeList.size,
                    statusMessage = "Backing up ${proj.name}..."
                )
                val res = designRepo.backupProjectToCloud(proj)
                if (res.isFailure) failures++
            }
            _backupState.value = if (failures == 0) {
                BackupProgressState(isRunning = false, progress = 1f, statusMessage = "Backed up ${activeList.size} projects to Google Cloud")
            } else {
                BackupProgressState(isRunning = false, progress = 1f, statusMessage = "Completed with $failures error(s)", errorMessage = "Some backups failed. Check connection and try again.")
            }
        }
    }

    fun restoreSelectedCloudBackups(selected: List<CloudProjectRecord>) {
        if (!isSignedIn || selected.isEmpty()) return
        viewModelScope.launch {
            _backupState.value = BackupProgressState(isRunning = true, progress = 0.4f, statusMessage = "Restoring ${selected.size} project(s)...")
            val count = designRepo.restoreSelectedFromCloud(selected, ownerUid)
            _backupState.value = BackupProgressState(isRunning = false, progress = 1f, statusMessage = "Restored $count project(s) to this device")
        }
    }

    // --- Sharing & Collaboration ---
    fun createShareLink(
        project: ProjectDocument,
        isPublic: Boolean,
        permission: String,
        password: String?,
        expiryHours: Int,
        onResult: (Result<String>) -> Unit
    ) {
        val repo = cloudRepo
        if (!isSignedIn || repo == null) {
            onResult(Result.failure(IllegalStateException("Google Sign-In required to generate cloud share links.")))
            return
        }
        val expiresAt = if (expiryHours <= 0) 0L else System.currentTimeMillis() + expiryHours * 3600_000L
        viewModelScope.launch {
            val res = repo.createSharedDesign(
                project = project,
                ownerDisplayName = userDisplayName,
                isPublic = isPublic,
                permission = permission,
                password = password,
                expiresAtMillis = expiresAt
            )
            onResult(res)
        }
    }

    fun revokeShareLink(shareId: String) {
        val repo = cloudRepo ?: return
        viewModelScope.launch {
            val res = repo.revokeSharedDesign(shareId)
            _statusBanner.value = if (res.isSuccess) "Share link revoked" else "Failed to revoke share link"
        }
    }

    fun openSharedDesignById(
        shareId: String,
        enteredPassword: String?,
        onSuccess: (SharedDesignRecord, ProjectDocument) -> Unit,
        onError: (String) -> Unit
    ) {
        val repo = cloudRepo
        if (!isSignedIn || repo == null) {
            onError("Google Sign-In required to open cloud shared links.")
            return
        }
        viewModelScope.launch {
            val res = repo.getSharedDesign(shareId.trim())
            if (res.isFailure) {
                onError(res.exceptionOrNull()?.localizedMessage ?: "Unable to fetch shared design")
                return@launch
            }
            val rec = res.getOrNull()
            if (rec == null) {
                onError("Shared design not found or access denied.")
                return@launch
            }
            if (rec.revoked) {
                onError("This share link has been revoked by the owner.")
                return@launch
            }
            if (rec.expiresAtMillis > 0L && System.currentTimeMillis() > rec.expiresAtMillis) {
                onError("This share link has expired.")
                return@launch
            }
            if (rec.passwordProtected) {
                val hash = CloudProjectRepository.sha256(enteredPassword.orEmpty())
                if (hash != rec.passwordHash) {
                    onError("Incorrect password for this protected design.")
                    return@launch
                }
            }
            val parsed = ProjectSerialization.fromJson(rec.snapshotJson)
            if (parsed == null) {
                onError("Shared design data is corrupted.")
                return@launch
            }
            onSuccess(rec, parsed)
        }
    }

    fun observeComments(shareId: String) = cloudRepo?.observeShareComments(shareId)

    fun postShareComment(shareId: String, text: String) {
        val repo = cloudRepo ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            repo.addShareComment(shareId, userDisplayName, text)
        }
    }

    // --- AI House Design Assistant ---
    fun runAiDesignAnalysis(goal: String) {
        val proj = _activeProject.value ?: return
        val floor = activeFloor() ?: return
        viewModelScope.launch {
            _aiState.value = AiAssistantState.Loading
            val res = aiAssistant.analyzeAndSuggest(proj, floor, goal)
            _aiState.value = if (res.isSuccess) {
                val (diag, patch) = res.getOrThrow()
                AiAssistantState.ProposalReady(diag, patch)
            } else {
                AiAssistantState.Error(res.exceptionOrNull()?.localizedMessage ?: "AI analysis failed.")
            }
        }
    }

    fun askAiQuestion(question: String) {
        val proj = _activeProject.value ?: return
        val floor = activeFloor() ?: return
        viewModelScope.launch {
            _aiState.value = AiAssistantState.Loading
            val res = aiAssistant.askSimpleQuestion(proj, floor, question)
            _aiState.value = if (res.isSuccess) {
                AiAssistantState.ChatResponse(res.getOrThrow())
            } else {
                AiAssistantState.Error(res.exceptionOrNull()?.localizedMessage ?: "AI query failed.")
            }
        }
    }

    fun approveAndApplyAiProposal(patch: AiProposedDesignPatch) {
        commitFloorEdit(saveVersion = true) { fl ->
            fl.copy(
                rooms = fl.rooms.map { r ->
                    r.copy(
                        wallColorHex = patch.suggestedPaintHex,
                        floorMaterial = patch.suggestedFloorMaterial,
                        floorColorHex = patch.suggestedFloorMaterial.defaultHex
                    )
                },
                furniture = fl.furniture + patch.addFurniture
            )
        }
        _aiState.value = AiAssistantState.Idle
        _statusBanner.value = "Applied AI recommendations after your approval"
    }

    fun dismissAiProposal() {
        _aiState.value = AiAssistantState.Idle
    }

    // --- Settings ---
    fun setDarkTheme(enabled: Boolean) { viewModelScope.launch { prefsRepo.setDarkTheme(enabled) } }
    fun setDefaultUnit(unit: UnitSystem) { viewModelScope.launch { prefsRepo.setDefaultUnit(unit) } }
    fun setAutoSave(enabled: Boolean) { viewModelScope.launch { prefsRepo.setAutoSave(enabled) } }
    fun setAutoBackup(enabled: Boolean) { viewModelScope.launch { prefsRepo.setAutoBackup(enabled) } }
    fun saveMaterialPrices(prices: MaterialPrices) { viewModelScope.launch { prefsRepo.saveMaterialPrices(prices) } }

    companion object {
        private const val TAG = "StudioVM"

        fun factory(
            context: Context,
            databaseId: String,
            ownerUid: String,
            isSignedIn: Boolean,
            userDisplayName: String
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val appContext = context.applicationContext
                val db = CsiDatabase.getInstance(appContext)
                val cloudRepo = if (isSignedIn) {
                    CloudProjectRepository(FirebaseFirestore.getInstance(databaseId))
                } else null
                val designRepo = DesignRepository(db.projectDao(), db.versionDao(), cloudRepo)
                val prefsRepo = UserPreferencesRepository(appContext)
                return StudioViewModel(
                    designRepo = designRepo,
                    cloudRepo = cloudRepo,
                    prefsRepo = prefsRepo,
                    aiAssistant = HouseDesignAiAssistant(),
                    ownerUid = ownerUid,
                    isSignedIn = isSignedIn,
                    userDisplayName = userDisplayName
                ) as T
            }
        }
    }
}
