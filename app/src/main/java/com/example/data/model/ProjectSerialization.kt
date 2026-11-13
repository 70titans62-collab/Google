package com.example.data.model

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object ProjectSerialization {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val projectAdapter = moshi.adapter(ProjectDocument::class.java)

    fun toJson(project: ProjectDocument): String = projectAdapter.toJson(project)

    fun fromJson(json: String): ProjectDocument? = try {
        projectAdapter.fromJson(json)
    } catch (_: Exception) {
        null
    }
}

object ArchitectureMath {
    fun formatLength(meters: Float, unit: UnitSystem): String {
        val safe = abs(meters)
        return when (unit) {
            UnitSystem.FEET_INCHES -> {
                val totalInches = (safe * 39.37008f).roundToInt()
                val feet = totalInches / 12
                val inches = totalInches % 12
                "${feet}' ${inches}\""
            }
            UnitSystem.METERS -> String.format("%.2f m", safe)
            UnitSystem.CENTIMETERS -> "${(safe * 100f).roundToInt()} cm"
            UnitSystem.MILLIMETERS -> "${(safe * 1000f).roundToInt()} mm"
        }
    }

    fun formatArea(sqMeters: Double, unit: UnitSystem): String {
        return when (unit) {
            UnitSystem.FEET_INCHES -> "${(sqMeters * 10.7639).roundToInt()} sq ft"
            UnitSystem.METERS -> String.format("%.1f m²", sqMeters)
            UnitSystem.CENTIMETERS -> String.format("%.0f cm²", sqMeters * 10_000.0)
            UnitSystem.MILLIMETERS -> String.format("%.0f mm²", sqMeters * 1_000_000.0)
        }
    }

    fun wallLengthMeters(wall: WallSegment): Float =
        hypot(wall.endX - wall.startX, wall.endY - wall.startY)

    fun roomAreaSqM(room: RoomZone): Double =
        abs(room.widthM * room.depthM).toDouble()

    fun roomPerimeterM(room: RoomZone): Double =
        (2.0 * (abs(room.widthM) + abs(room.depthM)))

    fun snapToGrid(value: Float, stepMeters: Float = 0.25f): Float =
        (value / stepMeters).roundToInt() * stepMeters

    /**
     * Snaps a point to nearby wall endpoints if within [thresholdM], else snaps to grid if enabled.
     */
    fun snapPoint(
        x: Float,
        y: Float,
        walls: List<WallSegment>,
        snapEnabled: Boolean,
        thresholdM: Float = 0.35f
    ): Pair<Float, Float> {
        if (!snapEnabled) return x to y
        var bestDist = thresholdM
        var bestX = x
        var bestY = y
        var snappedToEndpoint = false
        for (w in walls) {
            val d1 = hypot(x - w.startX, y - w.startY)
            if (d1 < bestDist) {
                bestDist = d1
                bestX = w.startX
                bestY = w.startY
                snappedToEndpoint = true
            }
            val d2 = hypot(x - w.endX, y - w.endY)
            if (d2 < bestDist) {
                bestDist = d2
                bestX = w.endX
                bestY = w.endY
                snappedToEndpoint = true
            }
        }
        return if (snappedToEndpoint) {
            bestX to bestY
        } else {
            snapToGrid(x) to snapToGrid(y)
        }
    }

    /**
     * Detects disconnected walls, overlapping furniture, invalid openings, and missing measurements.
     */
    fun analyzeDesignConflicts(floor: FloorPlan): List<DesignConflictWarning> {
        val warnings = mutableListOf<DesignConflictWarning>()
        val walls = floor.walls

        // 1. Disconnected wall endpoints (loose ends not touching any other wall within 0.3m)
        if (walls.size >= 2) {
            var looseEnds = 0
            for (i in walls.indices) {
                val w = walls[i]
                val sConnected = walls.indices.any { j ->
                    i != j && (
                        hypot(w.startX - walls[j].startX, w.startY - walls[j].startY) < 0.35f ||
                        hypot(w.startX - walls[j].endX, w.startY - walls[j].endY) < 0.35f
                    )
                }
                val eConnected = walls.indices.any { j ->
                    i != j && (
                        hypot(w.endX - walls[j].startX, w.endY - walls[j].startY) < 0.35f ||
                        hypot(w.endX - walls[j].endX, w.endY - walls[j].endY) < 0.35f
                    )
                }
                if (!sConnected || !eConnected) looseEnds++
            }
            if (looseEnds > 0) {
                warnings += DesignConflictWarning(
                    id = "loose_walls_${floor.id}",
                    title = "$looseEnds Disconnected Wall Endpoint(s)",
                    description = "Some walls on ${floor.name} have unjoined endpoints. Enable Snapping to connect corners cleanly.",
                    severity = "WARNING"
                )
            }
        }

        // 2. Overlapping furniture bounding boxes
        val furn = floor.furniture
        for (i in furn.indices) {
            for (j in i + 1 until furn.size) {
                val a = furn[i]
                val b = furn[j]
                val overlapX = min(a.x + a.widthM, b.x + b.widthM) - max(a.x, b.x)
                val overlapY = min(a.y + a.depthM, b.y + b.depthM) - max(a.y, b.y)
                if (overlapX > 0.12f && overlapY > 0.12f) {
                    warnings += DesignConflictWarning(
                        id = "overlap_${a.id}_${b.id}",
                        title = "Overlapping Furniture",
                        description = "${a.type.label} overlaps with ${b.type.label} on ${floor.name}.",
                        severity = "WARNING"
                    )
                }
            }
        }

        // 3. Door/window wider than host wall or missing host wall
        for (op in floor.openings) {
            val host = op.hostWallId?.let { id -> walls.find { it.id == id } }
            if (host != null && op.widthM > wallLengthMeters(host) - 0.1f) {
                warnings += DesignConflictWarning(
                    id = "opening_fit_${op.id}",
                    title = "Dimension Conflict: ${op.type.label}",
                    description = "${op.type.label} (${String.format("%.2fm", op.widthM)}) exceeds host wall length.",
                    severity = "WARNING"
                )
            }
        }

        // 4. Missing rooms or missing custom dimension annotations
        if (floor.walls.isNotEmpty() && floor.rooms.isEmpty()) {
            warnings += DesignConflictWarning(
                id = "no_rooms_${floor.id}",
                title = "No Labeled Room Zones",
                description = "Add Room Zones on ${floor.name} to enable area calculations and tile/paint estimation.",
                severity = "INFO"
            )
        }

        return warnings
    }

    /**
     * Approximate construction materials calculator across all visible floors.
     */
    fun calculateEstimate(project: ProjectDocument, prices: MaterialPrices): ConstructionEstimate {
        val visibleFloors = project.floors.filter { it.isVisible }
        val rooms = visibleFloors.flatMap { it.rooms }
        val walls = visibleFloors.flatMap { it.walls }
        val openings = visibleFloors.flatMap { it.openings }

        val roomAreaSum = rooms.sumOf { roomAreaSqM(it) }
        val totalWallLengthM = walls.sumOf { wallLengthMeters(it).toDouble() }
        val avgWallHeightM = if (walls.isNotEmpty()) walls.map { it.heightM.toDouble() }.average() else 3.0
        val grossWallArea = totalWallLengthM * avgWallHeightM
        val openingArea = openings.sumOf { (it.widthM * it.heightM).toDouble() }
        val netWallArea = max(0.0, grossWallArea - openingArea)

        // Fallback if user drew walls without room zones
        val floorAreaSqM = if (roomAreaSum > 0.1) roomAreaSum else max(0.0, (totalWallLengthM / 4.0) * (totalWallLengthM / 4.0))
        val floorAreaSqFt = floorAreaSqM * 10.7639104

        // Standard 600x600mm (0.36 sq.m) floor tile with 10% wastage
        val floorTileArea = floorAreaSqM * 1.10
        val floorTileCount = ceil(floorTileArea / 0.36).toInt()

        // Wall tile area: bath/kitchen rooms or 25% of net wall area (wet zones) with 10% wastage
        val wetRooms = rooms.filter {
            it.name.contains("bath", ignoreCase = true) ||
                it.name.contains("kitchen", ignoreCase = true) ||
                it.name.contains("toilet", ignoreCase = true) ||
                it.floorMaterial == SurfaceMaterial.BATH_KITCHEN_TILE
        }
        val wallTileArea = if (wetRooms.isNotEmpty()) {
            wetRooms.sumOf { roomPerimeterM(it) * 2.1 } * 1.10
        } else {
            netWallArea * 0.20 * 1.10
        }
        // Standard 300x450mm (0.135 sq.m) wall tile
        val wallTileCount = ceil(wallTileArea / 0.135).toInt()

        // Paint: 2 coats on both sides of remaining wall surface (~10 sq.m per liter per coat => /5 for 2 coats)
        val paintableArea = max(0.0, (netWallArea * 1.6) - wallTileArea)
        val paintLiters = (paintableArea / 5.5).let { (it * 10.0).roundToInt() / 10.0 }

        // Approximate masonry bricks (~55 bricks per sq.m for 230mm/9" wall, 40 for 115mm)
        val brickCount = walls.sumOf { w ->
            val area = wallLengthMeters(w) * w.heightM
            val perSqM = if (w.thicknessM >= 0.18f) 95.0 else 50.0
            area * perSqM
        }.minus(openingArea * 70.0).coerceAtLeast(0.0).roundToInt()

        // Approximate 50kg cement bags (brickwork mortar + plastering + floor screed)
        val cementBags = ceil((floorAreaSqM * 0.38) + (netWallArea * 0.16)).toInt()

        val doorCount = openings.count { it.type.isDoor }
        val windowCount = openings.count { !it.type.isDoor }

        val floorTileCost = floorTileArea * prices.floorTilePerSqM
        val wallTileCost = wallTileArea * prices.wallTilePerSqM
        val paintCost = paintLiters * prices.paintPerLiter
        val cementCost = cementBags * prices.cementPerBag
        val brickCost = brickCount * prices.brickPerUnit
        val doorsWindowsCost = (doorCount * prices.doorUnitPrice) + (windowCount * prices.windowUnitPrice)
        val totalCost = floorTileCost + wallTileCost + paintCost + cementCost + brickCost + doorsWindowsCost

        return ConstructionEstimate(
            totalFloorAreaSqM = floorAreaSqM,
            totalFloorAreaSqFt = floorAreaSqFt,
            netWallSurfaceAreaSqM = netWallArea,
            floorTileCount = floorTileCount,
            floorTileAreaSqM = floorTileArea,
            wallTileCount = wallTileCount,
            wallTileAreaSqM = wallTileArea,
            paintLiters = paintLiters,
            cementBags50Kg = cementBags,
            brickCount = brickCount,
            doorCount = doorCount,
            windowCount = windowCount,
            floorTileCost = floorTileCost,
            wallTileCost = wallTileCost,
            paintCost = paintCost,
            cementCost = cementCost,
            brickCost = brickCost,
            doorsWindowsCost = doorsWindowsCost,
            totalEstimatedCost = totalCost
        )
    }
}
