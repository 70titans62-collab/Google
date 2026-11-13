package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Roofing
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.CameraPreset
import com.example.data.model.ExteriorConfig
import com.example.data.model.ProjectDocument
import com.example.data.model.RoofStyle
import com.example.ui.components.CsiWatermarkBadge
import com.example.ui.components.parseHexColor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private data class Vec3(val x: Float, val y: Float, val z: Float)

private data class Face3D(
    val points: List<Vec3>,
    val color: Color,
    val strokeColor: Color = Color(0x440F172A)
)

@Composable
fun View3DScreen(
    project: ProjectDocument,
    onUpdateExterior: ((ExteriorConfig) -> ExteriorConfig) -> Unit,
    onToggleFloorVisibility: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var preset by remember { mutableStateOf(CameraPreset.ISOMETRIC) }
    var yawDeg by remember { mutableFloatStateOf(38f) }
    var pitchDeg by remember { mutableFloatStateOf(26f) }
    var zoom by remember { mutableFloatStateOf(1.0f) }

    // First-person walkthrough eye position (world meters)
    var fpX by remember { mutableFloatStateOf(3.0f) }
    var fpZ by remember { mutableFloatStateOf(2.5f) }
    var fpYawDeg by remember { mutableFloatStateOf(0f) }

    var showExteriorDialog by remember { mutableStateOf(false) }
    var showFloorsDialog by remember { mutableStateOf(false) }

    val ext = project.exteriorConfig

    // Build 3D faces from 2D floor plans dynamically so 2D & 3D stay 100% synchronized
    val faces = remember(project, preset) {
        buildHouseFaces(project, interiorCutaway = (preset == CameraPreset.INTERIOR_CUTAWAY))
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Camera Angle Presets & Exterior Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CameraPreset.entries.forEach { p ->
                FilterChip(
                    selected = preset == p,
                    onClick = {
                        preset = p
                        when (p) {
                            CameraPreset.ISOMETRIC -> { yawDeg = 38f; pitchDeg = 26f }
                            CameraPreset.FRONT -> { yawDeg = 0f; pitchDeg = 8f }
                            CameraPreset.SIDE -> { yawDeg = 90f; pitchDeg = 10f }
                            CameraPreset.TOP -> { yawDeg = 0f; pitchDeg = 82f }
                            CameraPreset.INTERIOR_CUTAWAY -> { yawDeg = 35f; pitchDeg = 42f }
                            CameraPreset.WALKTHROUGH -> { fpX = 3f; fpZ = 2.5f; fpYawDeg = 0f }
                        }
                    },
                    label = { Text(p.label) },
                    modifier = Modifier.testTag("camera_${p.name.lowercase()}")
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = { onUpdateExterior { it.copy(isNightMode = !it.isNightMode) } }) {
                Icon(Icons.Filled.Brightness4, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (ext.isNightMode) "Night Preview" else "Daylight Preview")
            }
            OutlinedButton(onClick = { showExteriorDialog = true }) {
                Icon(Icons.Filled.Roofing, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Roof & Exterior")
            }
            OutlinedButton(onClick = { showFloorsDialog = true }) {
                Icon(Icons.Filled.Layers, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Floors (${project.floors.count { it.isVisible }}/${project.floors.size})")
            }
        }

        // 3D Viewport Canvas
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val skyColors = if (ext.isNightMode) {
                listOf(Color(0xFF030712), Color(0xFF0F172A), Color(0xFF1E293B))
            } else {
                listOf(Color(0xFF0EA5E9), Color(0xFF7DD3FC), Color(0xFFE0F2FE))
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(skyColors))
                    .testTag("view_3d_canvas")
                    .pointerInput(preset) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            if (preset == CameraPreset.WALKTHROUGH) {
                                fpYawDeg = (fpYawDeg + dragAmount.x * 0.35f) % 360f
                            } else {
                                yawDeg = (yawDeg + dragAmount.x * 0.45f) % 360f
                                pitchDeg = (pitchDeg + dragAmount.y * 0.35f).coerceIn(2f, 88f)
                            }
                        }
                    }
            ) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val focal = min(size.width, size.height) * 0.9f * zoom

                val centerWorld = computeProjectCenter(project)

                val yawRad = Math.toRadians(if (preset == CameraPreset.WALKTHROUGH) fpYawDeg.toDouble() else yawDeg.toDouble())
                val pitchRad = Math.toRadians(if (preset == CameraPreset.WALKTHROUGH) 3.0 else pitchDeg.toDouble())
                val cosY = cos(yawRad).toFloat()
                val sinY = sin(yawRad).toFloat()
                val cosP = cos(pitchRad).toFloat()
                val sinP = sin(pitchRad).toFloat()

                val camDist = if (preset == CameraPreset.WALKTHROUGH) 0.1f else 22f

                fun toCameraSpace(v: Vec3): Vec3 {
                    val tx = if (preset == CameraPreset.WALKTHROUGH) v.x - fpX else v.x - centerWorld.x
                    val ty = if (preset == CameraPreset.WALKTHROUGH) v.y - 1.65f else v.y - centerWorld.y
                    val tz = if (preset == CameraPreset.WALKTHROUGH) v.z - fpZ else v.z - centerWorld.z

                    val rx = tx * cosY - tz * sinY
                    val rz1 = tx * sinY + tz * cosY

                    val ry = ty * cosP - rz1 * sinP
                    val rz = ty * sinP + rz1 * cosP + camDist
                    return Vec3(rx, ry, rz)
                }

                val sunDir = Vec3(
                    cos(Math.toRadians(ext.sunAzimuthDeg.toDouble())).toFloat(),
                    0.85f,
                    sin(Math.toRadians(ext.sunAzimuthDeg.toDouble())).toFloat()
                )

                // Painter's algorithm depth sort
                val projected = faces.mapNotNull { face ->
                    val camPts = face.points.map { toCameraSpace(it) }
                    if (camPts.any { it.z <= 0.35f }) return@mapNotNull null
                    val avgDepth = camPts.map { it.z }.average().toFloat()

                    // Directional shading
                    val shade = computeFaceShade(face.points, sunDir, ext.isNightMode)
                    val shadedColor = Color(
                        red = (face.color.red * shade).coerceIn(0f, 1f),
                        green = (face.color.green * shade).coerceIn(0f, 1f),
                        blue = (face.color.blue * shade).coerceIn(0f, 1f),
                        alpha = face.color.alpha
                    )
                    val screenPts = camPts.map { pt ->
                        Offset(
                            cx + (pt.x / pt.z) * focal,
                            cy - (pt.y / pt.z) * focal
                        )
                    }
                    Triple(avgDepth, screenPts, shadedColor to face.strokeColor)
                }.sortedByDescending { it.first }

                for ((_, pts, colors) in projected) {
                    if (pts.size < 3) continue
                    val path = Path().apply {
                        moveTo(pts[0].x, pts[0].y)
                        pts.drop(1).forEach { lineTo(it.x, it.y) }
                        close()
                    }
                    drawPath(path, color = colors.first)
                    drawPath(path, color = colors.second, style = Stroke(1.2f))
                }
            }

            // First-person walkthrough D-Pad controls
            if (preset == CameraPreset.WALKTHROUGH) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.align(Alignment.BottomStart).padding(14.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(6.dp)) {
                        IconButton(onClick = {
                            val rad = Math.toRadians(fpYawDeg.toDouble())
                            fpX += sin(rad).toFloat() * 0.8f
                            fpZ += cos(rad).toFloat() * 0.8f
                        }) { Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Walk Forward") }
                        Row {
                            IconButton(onClick = { fpYawDeg -= 20f }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Turn Left")
                            }
                            IconButton(onClick = {
                                val rad = Math.toRadians(fpYawDeg.toDouble())
                                fpX -= sin(rad).toFloat() * 0.8f
                                fpZ -= cos(rad).toFloat() * 0.8f
                            }) { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Walk Backward") }
                            IconButton(onClick = { fpYawDeg += 20f }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Turn Right")
                            }
                        }
                    }
                }
            } else {
                // Orbit Zoom slider
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .width(180.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text("Zoom", style = MaterialTheme.typography.labelSmall)
                        Slider(value = zoom, onValueChange = { zoom = it }, valueRange = 0.5f..2.2f)
                    }
                }
            }

            CsiWatermarkBadge(modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp))
        }
    }

    if (showExteriorDialog) {
        AlertDialog(
            onDismissRequest = { showExteriorDialog = false },
            title = { Text("Roof & Exterior Designer") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Roof Style", style = MaterialTheme.typography.labelLarge)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RoofStyle.entries.forEach { rs ->
                            FilterChip(
                                selected = ext.roofStyle == rs,
                                onClick = { onUpdateExterior { it.copy(roofStyle = rs) } },
                                label = { Text(rs.label) }
                            )
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Compound Wall & Gate")
                        Switch(checked = ext.hasCompoundWall, onCheckedChange = { onUpdateExterior { c -> c.copy(hasCompoundWall = it, hasMainGate = it) } })
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Garage & Parking Pad")
                        Switch(checked = ext.hasGarage, onCheckedChange = { onUpdateExterior { c -> c.copy(hasGarage = it) } })
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Balcony & Terrace")
                        Switch(checked = ext.hasBalconyTerrace, onCheckedChange = { onUpdateExterior { c -> c.copy(hasBalconyTerrace = it) } })
                    }
                    Text("Sun Shadow Angle: ${ext.sunAzimuthDeg.toInt()}°", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = ext.sunAzimuthDeg,
                        onValueChange = { deg -> onUpdateExterior { it.copy(sunAzimuthDeg = deg) } },
                        valueRange = 0f..360f
                    )
                }
            },
            confirmButton = { TextButton(onClick = { showExteriorDialog = false }) { Text("Done") } }
        )
    }

    if (showFloorsDialog) {
        AlertDialog(
            onDismissRequest = { showFloorsDialog = false },
            title = { Text("3D Floor Visibility") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    project.floors.forEach { fl ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("${fl.name} (Level ${fl.levelIndex})")
                            Switch(checked = fl.isVisible, onCheckedChange = { onToggleFloorVisibility(fl.id) })
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showFloorsDialog = false }) { Text("Close") } }
        )
    }
}

private fun computeProjectCenter(project: ProjectDocument): Vec3 {
    val allWalls = project.floors.flatMap { it.walls }
    if (allWalls.isEmpty()) return Vec3(5f, 1.5f, 4f)
    val minX = allWalls.minOf { min(it.startX, it.endX) }
    val maxX = allWalls.maxOf { max(it.startX, it.endX) }
    val minZ = allWalls.minOf { min(it.startY, it.endY) }
    val maxZ = allWalls.maxOf { max(it.startY, it.endY) }
    return Vec3((minX + maxX) / 2f, 1.8f, (minZ + maxZ) / 2f)
}

private fun computeFaceShade(pts: List<Vec3>, sunDir: Vec3, isNight: Boolean): Float {
    if (pts.size < 3) return 1f
    val u = Vec3(pts[1].x - pts[0].x, pts[1].y - pts[0].y, pts[1].z - pts[0].z)
    val v = Vec3(pts[2].x - pts[0].x, pts[2].y - pts[0].y, pts[2].z - pts[0].z)
    val nx = u.y * v.z - u.z * v.y
    val ny = u.z * v.x - u.x * v.z
    val nz = u.x * v.y - u.y * v.x
    val len = max(0.001f, hypot(hypot(nx, ny), nz))
    val dot = ((nx / len) * sunDir.x + (ny / len) * sunDir.y + (nz / len) * sunDir.z)
    val base = if (isNight) 0.42f else 0.65f
    return (base + kotlin.math.abs(dot) * 0.35f).coerceIn(0.35f, 1.0f)
}

private fun addBoxFaces(
    out: MutableList<Face3D>,
    x1: Float, y1: Float, z1: Float,
    x2: Float, y2: Float, z2: Float,
    color: Color
) {
    val p0 = Vec3(x1, y1, z1)
    val p1 = Vec3(x2, y1, z1)
    val p2 = Vec3(x2, y1, z2)
    val p3 = Vec3(x1, y1, z2)
    val p4 = Vec3(x1, y2, z1)
    val p5 = Vec3(x2, y2, z1)
    val p6 = Vec3(x2, y2, z2)
    val p7 = Vec3(x1, y2, z2)

    out += Face3D(listOf(p0, p1, p5, p4), color)
    out += Face3D(listOf(p1, p2, p6, p5), color)
    out += Face3D(listOf(p2, p3, p7, p6), color)
    out += Face3D(listOf(p3, p0, p4, p7), color)
    out += Face3D(listOf(p4, p5, p6, p7), color)
}

private fun buildHouseFaces(project: ProjectDocument, interiorCutaway: Boolean): List<Face3D> {
    val out = mutableListOf<Face3D>()
    val ext = project.exteriorConfig
    val visibleFloors = project.floors.filter { it.isVisible }.sortedBy { it.levelIndex }

    // Ground plot & cast shadow pad
    val allWalls = visibleFloors.flatMap { it.walls }
    val minX = (allWalls.minOfOrNull { min(it.startX, it.endX) } ?: 0f)
    val maxX = (allWalls.maxOfOrNull { max(it.startX, it.endX) } ?: 10f)
    val minZ = (allWalls.minOfOrNull { min(it.startY, it.endY) } ?: 0f)
    val maxZ = (allWalls.maxOfOrNull { max(it.startY, it.endY) } ?: 8f)

    // Plot lawn
    out += Face3D(
        listOf(
            Vec3(minX - 3.5f, -0.02f, minZ - 3.5f),
            Vec3(maxX + 3.5f, -0.02f, minZ - 3.5f),
            Vec3(maxX + 3.5f, -0.02f, maxZ + 3.5f),
            Vec3(minX - 3.5f, -0.02f, maxZ + 3.5f)
        ),
        color = if (ext.isNightMode) Color(0xFF064E3B) else Color(0xFF15803D)
    )

    // Compound Wall & Gate
    if (ext.hasCompoundWall) {
        val cwColor = Color(0xFF94A3B8)
        val cx1 = minX - 2.2f
        val cx2 = maxX + 2.2f
        val cz1 = minZ - 2.2f
        val cz2 = maxZ + 2.2f
        addBoxFaces(out, cx1, 0f, cz1, cx1 + 0.2f, 1.3f, cz2, cwColor)
        addBoxFaces(out, cx2 - 0.2f, 0f, cz1, cx2, 1.3f, cz2, cwColor)
        addBoxFaces(out, cx1, 0f, cz2 - 0.2f, cx2, 1.3f, cz2, cwColor)
        if (ext.hasMainGate) {
            val midX = (cx1 + cx2) / 2f
            addBoxFaces(out, cx1, 0f, cz1, midX - 1.4f, 1.3f, cz1 + 0.2f, cwColor)
            addBoxFaces(out, midX + 1.4f, 0f, cz1, cx2, 1.3f, cz1 + 0.2f, cwColor)
            addBoxFaces(out, midX - 1.35f, 0.05f, cz1, midX + 1.35f, 1.45f, cz1 + 0.12f, Color(0xFFF59E0B))
        }
    }

    // Garage & Parking Pad
    if (ext.hasGarage) {
        addBoxFaces(out, maxX + 0.1f, 0f, minZ + 0.5f, maxX + 2.8f, 0.08f, minZ + 5.2f, Color(0xFF475569))
        if (!interiorCutaway) {
            addBoxFaces(out, maxX + 0.1f, 2.5f, minZ + 0.5f, maxX + 2.8f, 2.7f, minZ + 5.2f, parseHexColor(ext.roofColorHex))
        }
    }

    // Stack visible floors
    var topY = 0f
    for (floor in visibleFloors) {
        val baseY = floor.levelIndex * floor.ceilingHeightM
        val wallTop = baseY + (if (interiorCutaway) floor.ceilingHeightM * 0.55f else floor.ceilingHeightM)
        topY = max(topY, baseY + floor.ceilingHeightM)

        // Room slabs
        for (r in floor.rooms) {
            val fColor = parseHexColor(r.floorColorHex)
            out += Face3D(
                listOf(
                    Vec3(r.x, baseY + 0.02f, r.y),
                    Vec3(r.x + r.widthM, baseY + 0.02f, r.y),
                    Vec3(r.x + r.widthM, baseY + 0.02f, r.y + r.depthM),
                    Vec3(r.x, baseY + 0.02f, r.y + r.depthM)
                ),
                color = if (ext.isNightMode) fColor.copy(alpha = 0.95f) else fColor
            )
        }

        // Walls as 3D extruded prisms
        for (w in floor.walls) {
            val dx = w.endX - w.startX
            val dz = w.endY - w.startY
            val len = max(0.01f, hypot(dx, dz))
            val nx = (-dz / len) * (w.thicknessM / 2f)
            val nz = (dx / len) * (w.thicknessM / 2f)
            val wCol = parseHexColor(w.interiorColorHex, Color(0xFFE2E8F0))

            val b0 = Vec3(w.startX + nx, baseY, w.startY + nz)
            val b1 = Vec3(w.endX + nx, baseY, w.endY + nz)
            val b2 = Vec3(w.endX - nx, baseY, w.endY - nz)
            val b3 = Vec3(w.startX - nx, baseY, w.startY - nz)
            val t0 = Vec3(b0.x, wallTop, b0.z)
            val t1 = Vec3(b1.x, wallTop, b1.z)
            val t2 = Vec3(b2.x, wallTop, b2.z)
            val t3 = Vec3(b3.x, wallTop, b3.z)

            out += Face3D(listOf(b0, b1, t1, t0), wCol)
            out += Face3D(listOf(b1, b2, t2, t1), wCol)
            out += Face3D(listOf(b2, b3, t3, t2), wCol)
            out += Face3D(listOf(b3, b0, t0, t3), wCol)
            out += Face3D(listOf(t0, t1, t2, t3), wCol)
        }

        // Doors & Windows
        for (op in floor.openings) {
            val rad = Math.toRadians(op.angleDeg.toDouble())
            val dx = (cos(rad) * op.widthM * 0.5).toFloat()
            val dz = (sin(rad) * op.widthM * 0.5).toFloat()
            val y0 = baseY + op.sillHeightM
            val y1 = baseY + op.sillHeightM + op.heightM
            val opColor = if (op.type.isDoor) parseHexColor(op.colorHex, Color(0xFF7C2D12)) else Color(0xAA38BDF8)
            out += Face3D(
                listOf(
                    Vec3(op.x - dx, y0, op.y - dz),
                    Vec3(op.x + dx, y0, op.y + dz),
                    Vec3(op.x + dx, y1, op.y + dz),
                    Vec3(op.x - dx, y1, op.y - dz)
                ),
                color = opColor
            )
        }

        // Furniture 3D blocks
        for (f in floor.furniture) {
            addBoxFaces(
                out,
                f.x, baseY + 0.03f, f.y,
                f.x + f.widthM, baseY + f.heightM, f.y + f.depthM,
                parseHexColor(f.colorHex, Color(0xFF6366F1))
            )
        }

        // Balcony / Terrace on upper floors
        if (ext.hasBalconyTerrace && floor.levelIndex >= 1) {
            addBoxFaces(out, minX, baseY - 0.1f, minZ - 1.4f, maxX, baseY + 0.05f, minZ, Color(0xFFCBD5E1))
            addBoxFaces(out, minX, baseY + 0.05f, minZ - 1.4f, maxX, baseY + 0.95f, minZ - 1.32f, Color(0x8838BDF8))
        }
    }

    // Roof Designer (hidden in interior cutaway mode)
    if (!interiorCutaway && visibleFloors.isNotEmpty()) {
        val roofCol = parseHexColor(ext.roofColorHex, Color(0xFF334155))
        val ridgeY = topY + 1.8f
        val midZ = (minZ + maxZ) / 2f
        val midX = (minX + maxX) / 2f
        when (ext.roofStyle) {
            RoofStyle.GABLE -> {
                out += Face3D(listOf(Vec3(minX - 0.3f, topY, minZ - 0.3f), Vec3(maxX + 0.3f, topY, minZ - 0.3f), Vec3(maxX + 0.3f, ridgeY, midZ), Vec3(minX - 0.3f, ridgeY, midZ)), roofCol)
                out += Face3D(listOf(Vec3(maxX + 0.3f, topY, maxZ + 0.3f), Vec3(minX - 0.3f, topY, maxZ + 0.3f), Vec3(minX - 0.3f, ridgeY, midZ), Vec3(maxX + 0.3f, ridgeY, midZ)), roofCol)
            }
            RoofStyle.HIP -> {
                val apex = Vec3(midX, ridgeY, midZ)
                out += Face3D(listOf(Vec3(minX, topY, minZ), Vec3(maxX, topY, minZ), apex), roofCol)
                out += Face3D(listOf(Vec3(maxX, topY, minZ), Vec3(maxX, topY, maxZ), apex), roofCol)
                out += Face3D(listOf(Vec3(maxX, topY, maxZ), Vec3(minX, topY, maxZ), apex), roofCol)
                out += Face3D(listOf(Vec3(minX, topY, maxZ), Vec3(minX, topY, minZ), apex), roofCol)
            }
            RoofStyle.FLAT -> {
                addBoxFaces(out, minX - 0.2f, topY, minZ - 0.2f, maxX + 0.2f, topY + 0.25f, maxZ + 0.2f, roofCol)
            }
            RoofStyle.SHED -> {
                out += Face3D(listOf(Vec3(minX - 0.2f, topY, minZ - 0.2f), Vec3(maxX + 0.2f, topY, minZ - 0.2f), Vec3(maxX + 0.2f, ridgeY, maxZ + 0.2f), Vec3(minX - 0.2f, ridgeY, maxZ + 0.2f)), roofCol)
            }
            RoofStyle.MANSARD -> {
                addBoxFaces(out, minX + 0.8f, topY + 0.9f, minZ + 0.8f, maxX - 0.8f, topY + 1.1f, maxZ - 0.8f, roofCol)
                out += Face3D(listOf(Vec3(minX, topY, minZ), Vec3(maxX, topY, minZ), Vec3(maxX - 0.8f, topY + 1.0f, minZ + 0.8f), Vec3(minX + 0.8f, topY + 1.0f, minZ + 0.8f)), roofCol)
                out += Face3D(listOf(Vec3(maxX, topY, maxZ), Vec3(minX, topY, maxZ), Vec3(minX + 0.8f, topY + 1.0f, maxZ - 0.8f), Vec3(maxX - 0.8f, topY + 1.0f, maxZ - 0.8f)), roofCol)
            }
        }
    }

    return out
}
