package com.example.data.model

import com.squareup.moshi.JsonClass

enum class ProjectCategory(val label: String) {
    RESIDENTIAL("Residential"),
    VILLA("Duplex / Villa"),
    APARTMENT("Apartment"),
    COMMERCIAL("Commercial"),
    RENOVATION("Renovation"),
    LANDSCAPE("Landscape")
}

enum class UnitSystem(val label: String, val shortLabel: String) {
    FEET_INCHES("Feet & Inches", "ft-in"),
    METERS("Meters", "m"),
    CENTIMETERS("Centimeters", "cm"),
    MILLIMETERS("Millimeters", "mm")
}

enum class StudioMode(val label: String) {
    ARCHITECTURAL_2D("2D Draw"),
    INTERIOR("Interior"),
    ELECTRICAL("Electrical"),
    PLUMBING("Plumbing"),
    VIEW_3D("3D View")
}

enum class DrawingTool(val label: String) {
    SELECT("Select"),
    WALL("Wall"),
    PENCIL("Pencil"),
    ERASER("Eraser"),
    LINE("Line"),
    RECTANGLE("Rectangle"),
    SQUARE("Square"),
    CIRCLE("Circle"),
    OVAL("Oval"),
    TRIANGLE("Triangle"),
    POLYGON("Polygon"),
    ARC("Arc"),
    CUSTOM_SHAPE("Custom"),
    DOOR_WINDOW("Door/Win"),
    MEASURE("Measure"),
    ROOM_ZONE("Room")
}

enum class LayerKind(val label: String) {
    ARCHITECTURAL("Architectural"),
    FURNITURE("Furniture"),
    ELECTRICAL("Electrical"),
    PLUMBING("Plumbing"),
    ANNOTATIONS("Dimensions"),
    CUSTOM("Custom Shapes")
}

enum class OpeningType(
    val label: String,
    val isDoor: Boolean,
    val defaultWidthM: Float,
    val defaultHeightM: Float,
    val defaultSillM: Float
) {
    MAIN_ENTRANCE_DOOR("Main Entrance Door", true, 1.2f, 2.2f, 0f),
    DOUBLE_DOOR("Double Door", true, 1.6f, 2.2f, 0f),
    WOODEN_DOOR("Wooden Door", true, 0.9f, 2.1f, 0f),
    GLASS_DOOR("Glass Door", true, 1.0f, 2.1f, 0f),
    SLIDING_DOOR("Sliding Door", true, 1.8f, 2.1f, 0f),
    BATHROOM_DOOR("Bathroom Door", true, 0.75f, 2.0f, 0f),
    SLIDING_WINDOW("Sliding Window", false, 1.5f, 1.2f, 0.9f),
    CASEMENT_WINDOW("Casement Window", false, 1.2f, 1.2f, 0.9f),
    VENTILATION_WINDOW("Ventilation Window", false, 0.6f, 0.45f, 1.8f)
}

enum class SwingDirection(val label: String) {
    LEFT_IN("Left In"),
    RIGHT_IN("Right In"),
    LEFT_OUT("Left Out"),
    RIGHT_OUT("Right Out"),
    SLIDE("Sliding")
}

enum class SurfaceMaterial(val label: String, val defaultHex: String, val isFloor: Boolean) {
    PAINT_MATTE("Matte Paint", "#F1F5F9", false),
    TEXTURE_STUCCO("Stucco Texture", "#E2E8F0", false),
    TEXTURE_BRICK("Exposed Brick", "#B45309", false),
    TEXTURE_STONE("Natural Stone", "#78716C", false),
    WALLPAPER_GEO("Geo Wallpaper", "#CBD5E1", false),
    CERAMIC_TILE("Ceramic Tiles", "#E0F2FE", true),
    VITRIFIED_TILE("Vitrified Tiles", "#F8FAFC", true),
    MARBLE_GRANITE("Marble & Granite", "#E7E5E4", true),
    WOODEN_FLOORING("Wooden Flooring", "#B47B44", true),
    BATH_KITCHEN_TILE("Bath/Kitchen Tile", "#CCFBF1", true)
}

enum class CeilingDesign(val label: String) {
    FLAT("Flat Plaster"),
    COFFERED("Coffered Grid"),
    COVE_LED("Cove LED Strip"),
    TRAY("Tray Ceiling"),
    GYPSUM_MODERN("Gypsum Layered")
}

enum class FurnitureType(
    val label: String,
    val defaultWidthM: Float,
    val defaultDepthM: Float,
    val defaultHeightM: Float,
    val defaultColorHex: String
) {
    SOFA("Sofa", 2.1f, 0.9f, 0.85f, "#475569"),
    BED("Double Bed", 2.0f, 1.8f, 0.6f, "#6366F1"),
    DINING_TABLE("Dining Table", 1.8f, 1.0f, 0.75f, "#92400E"),
    CHAIR("Chair", 0.55f, 0.55f, 0.85f, "#78350F"),
    KITCHEN_CABINET("Kitchen Cabinet", 2.4f, 0.65f, 0.9f, "#0F766E"),
    WARDROBE("Wardrobe", 1.8f, 0.6f, 2.2f, "#7C2D12"),
    STUDY_DESK("Study Desk", 1.4f, 0.7f, 0.75f, "#57534E"),
    STAIRCASE("Staircase", 1.2f, 3.0f, 3.0f, "#64748B")
}

enum class ElectricalType(val label: String, val watts: Int, val symbol: String) {
    DISTRIBUTION_BOARD("Distribution Board", 0, "DB"),
    SWITCH_BOARD("Switch Board", 0, "SB"),
    ELECTRICAL_SWITCH("Switch", 0, "SW"),
    PLUG_SOCKET("Plug Socket", 250, "SK"),
    FAN_POINT("Fan Point", 75, "FN"),
    LIGHT_POINT("Light Point", 18, "LT"),
    AC_POINT("AC Point", 1800, "AC")
}

enum class PlumbingType(val label: String, val isDrainage: Boolean, val symbol: String) {
    WATER_TANK("Water Tank", false, "WT"),
    WASH_BASIN("Wash Basin", false, "WB"),
    TOILET("Toilet", true, "WC"),
    SHOWER("Shower", false, "SH"),
    KITCHEN_SINK("Kitchen Sink", false, "KS")
}

enum class RoofStyle(val label: String) {
    GABLE("Gable Roof"),
    HIP("Hip Roof"),
    FLAT("Flat Terrace"),
    SHED("Modern Shed"),
    MANSARD("Mansard")
}

enum class CameraPreset(val label: String) {
    ISOMETRIC("Isometric 360"),
    FRONT("Front Elevation"),
    SIDE("Side Elevation"),
    TOP("Top Aerial"),
    INTERIOR_CUTAWAY("Interior Cutaway"),
    WALKTHROUGH("1st Person Walk")
}

@JsonClass(generateAdapter = true)
data class Point2D(
    val x: Float = 0f,
    val y: Float = 0f
)

@JsonClass(generateAdapter = true)
data class DrawingLayer(
    val kind: LayerKind = LayerKind.ARCHITECTURAL,
    val visible: Boolean = true,
    val locked: Boolean = false
)

@JsonClass(generateAdapter = true)
data class WallSegment(
    val id: String = "",
    val startX: Float = 0f,
    val startY: Float = 0f,
    val endX: Float = 0f,
    val endY: Float = 0f,
    val thicknessM: Float = 0.23f,
    val heightM: Float = 3.0f,
    val interiorColorHex: String = "#F1F5F9",
    val exteriorColorHex: String = "#E2E8F0",
    val material: SurfaceMaterial = SurfaceMaterial.PAINT_MATTE,
    val locked: Boolean = false,
    val groupId: String? = null,
    val layer: LayerKind = LayerKind.ARCHITECTURAL
)

@JsonClass(generateAdapter = true)
data class RoomZone(
    val id: String = "",
    val name: String = "Room",
    val x: Float = 0f,
    val y: Float = 0f,
    val widthM: Float = 4f,
    val depthM: Float = 3.5f,
    val floorMaterial: SurfaceMaterial = SurfaceMaterial.VITRIFIED_TILE,
    val floorColorHex: String = "#F8FAFC",
    val wallColorHex: String = "#F1F5F9",
    val ceilingDesign: CeilingDesign = CeilingDesign.FLAT,
    val ceilingColorHex: String = "#FFFFFF",
    val locked: Boolean = false,
    val groupId: String? = null
)

@JsonClass(generateAdapter = true)
data class OpeningItem(
    val id: String = "",
    val type: OpeningType = OpeningType.WOODEN_DOOR,
    val x: Float = 0f,
    val y: Float = 0f,
    val angleDeg: Float = 0f,
    val widthM: Float = 0.9f,
    val heightM: Float = 2.1f,
    val sillHeightM: Float = 0f,
    val swing: SwingDirection = SwingDirection.LEFT_IN,
    val colorHex: String = "#7C2D12",
    val hostWallId: String? = null,
    val locked: Boolean = false
)

enum class SketchShapeType {
    PENCIL, LINE, RECTANGLE, SQUARE, CIRCLE, OVAL, TRIANGLE, POLYGON, ARC, CUSTOM
}

@JsonClass(generateAdapter = true)
data class SketchShape(
    val id: String = "",
    val type: SketchShapeType = SketchShapeType.LINE,
    val points: List<Point2D> = emptyList(),
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 1f,
    val height: Float = 1f,
    val rotationDeg: Float = 0f,
    val polygonSides: Int = 5,
    val strokeWidth: Float = 3f,
    val strokeColorHex: String = "#38BDF8",
    val fillColorHex: String? = null,
    val locked: Boolean = false,
    val groupId: String? = null,
    val layer: LayerKind = LayerKind.CUSTOM
)

@JsonClass(generateAdapter = true)
data class FurnitureItem(
    val id: String = "",
    val type: FurnitureType = FurnitureType.SOFA,
    val x: Float = 0f,
    val y: Float = 0f,
    val widthM: Float = 2.0f,
    val depthM: Float = 0.9f,
    val heightM: Float = 0.85f,
    val rotationDeg: Float = 0f,
    val colorHex: String = "#475569",
    val locked: Boolean = false,
    val groupId: String? = null
)

@JsonClass(generateAdapter = true)
data class ElectricalNode(
    val id: String = "",
    val type: ElectricalType = ElectricalType.LIGHT_POINT,
    val x: Float = 0f,
    val y: Float = 0f,
    val circuitName: String = "C1",
    val connectedToId: String? = null,
    val wattage: Int = 18
)

@JsonClass(generateAdapter = true)
data class PlumbingNode(
    val id: String = "",
    val type: PlumbingType = PlumbingType.WASH_BASIN,
    val x: Float = 0f,
    val y: Float = 0f,
    val pipeDiameterMm: Int = 25,
    val isDrainage: Boolean = false,
    val connectedToId: String? = null
)

@JsonClass(generateAdapter = true)
data class MeasurementAnnotation(
    val id: String = "",
    val x1: Float = 0f,
    val y1: Float = 0f,
    val x2: Float = 1f,
    val y2: Float = 0f,
    val label: String = ""
)

@JsonClass(generateAdapter = true)
data class FloorPlan(
    val id: String = "",
    val name: String = "Ground Floor",
    val levelIndex: Int = 0,
    val ceilingHeightM: Float = 3.0f,
    val isVisible: Boolean = true,
    val layers: List<DrawingLayer> = LayerKind.entries.map { DrawingLayer(it) },
    val walls: List<WallSegment> = emptyList(),
    val rooms: List<RoomZone> = emptyList(),
    val openings: List<OpeningItem> = emptyList(),
    val shapes: List<SketchShape> = emptyList(),
    val furniture: List<FurnitureItem> = emptyList(),
    val electricalNodes: List<ElectricalNode> = emptyList(),
    val plumbingNodes: List<PlumbingNode> = emptyList(),
    val annotations: List<MeasurementAnnotation> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ExteriorConfig(
    val roofStyle: RoofStyle = RoofStyle.GABLE,
    val roofColorHex: String = "#334155",
    val facadeColorHex: String = "#E2E8F0",
    val hasCompoundWall: Boolean = true,
    val hasMainGate: Boolean = true,
    val hasGarage: Boolean = true,
    val hasBalconyTerrace: Boolean = true,
    val isNightMode: Boolean = false,
    val sunAzimuthDeg: Float = 42f
)

@JsonClass(generateAdapter = true)
data class ProjectDocument(
    val id: String = "",
    val userId: String = "",
    val name: String = "Untitled Design",
    val category: ProjectCategory = ProjectCategory.RESIDENTIAL,
    val isFavorite: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAtMillis: Long = 0L,
    val unitSystem: UnitSystem = UnitSystem.FEET_INCHES,
    val drawingScale: Int = 50, // 1:50
    val gridEnabled: Boolean = true,
    val snapEnabled: Boolean = true,
    val rulerEnabled: Boolean = true,
    val guidesEnabled: Boolean = true,
    val floors: List<FloorPlan> = emptyList(),
    val activeFloorId: String = "",
    val exteriorConfig: ExteriorConfig = ExteriorConfig(),
    val version: Int = 1,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L,
    val lastBackupMillis: Long = 0L
)

data class DesignConflictWarning(
    val id: String,
    val title: String,
    val description: String,
    val severity: String // "WARNING" or "INFO"
)

data class MaterialPrices(
    val floorTilePerSqM: Double = 45.0,
    val wallTilePerSqM: Double = 38.0,
    val paintPerLiter: Double = 14.0,
    val cementPerBag: Double = 9.5,
    val brickPerUnit: Double = 0.35,
    val doorUnitPrice: Double = 240.0,
    val windowUnitPrice: Double = 185.0,
    val currencySymbol: String = "$"
)

data class ConstructionEstimate(
    val totalFloorAreaSqM: Double,
    val totalFloorAreaSqFt: Double,
    val netWallSurfaceAreaSqM: Double,
    val floorTileCount: Int,
    val floorTileAreaSqM: Double,
    val wallTileCount: Int,
    val wallTileAreaSqM: Double,
    val paintLiters: Double,
    val cementBags50Kg: Int,
    val brickCount: Int,
    val doorCount: Int,
    val windowCount: Int,
    val floorTileCost: Double,
    val wallTileCost: Double,
    val paintCost: Double,
    val cementCost: Double,
    val brickCost: Double,
    val doorsWindowsCost: Double,
    val totalEstimatedCost: Double
)
