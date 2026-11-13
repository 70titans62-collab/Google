package com.example.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.model.ArchitectureMath
import com.example.data.model.FloorPlan
import com.example.data.model.MaterialPrices
import com.example.data.model.ProjectDocument
import com.example.data.model.ProjectSerialization
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

enum class ExportFormat(val label: String, val ext: String, val mime: String) {
    PDF_A4("PDF (A4 Sheet)", "pdf", "application/pdf"),
    PDF_A3("PDF (A3 Blueprint)", "pdf", "application/pdf"),
    PNG("High-Res PNG", "png", "image/png"),
    JPG("High-Res JPG", "jpg", "image/jpeg"),
    SVG("Vector SVG", "svg", "image/svg+xml"),
    DXF("AutoCAD DXF", "dxf", "application/dxf"),
    OBJ("3D Model OBJ", "obj", "text/plain"),
    GLB("3D Model GLB", "glb", "model/gltf-binary"),
    CSI_NATIVE("Editable .csi Project", "csi", "application/json")
}

object ExportEngine {

    private fun exportsDir(context: Context): File {
        val dir = File(context.cacheDir, "exports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun safeFileName(project: ProjectDocument, suffix: String, ext: String): String {
        val clean = project.name.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(32).ifBlank { "CSI_Design" }
        return "${clean}_${suffix}.$ext"
    }

    fun renderFloorPlanBitmap(
        project: ProjectDocument,
        floor: FloorPlan,
        widthPx: Int = 1600,
        heightPx: Int = 1120,
        includeMep: Boolean = true
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.parseColor("#0A1424"))

        // Architectural border & title block
        val borderPaint = Paint().apply {
            color = Color.parseColor("#38BDF8")
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRect(24f, 24f, widthPx - 24f, heightPx - 24f, borderPaint)

        // Compute world bounds
        val xs = floor.walls.flatMap { listOf(it.startX, it.endX) } + floor.rooms.flatMap { listOf(it.x, it.x + it.widthM) }
        val ys = floor.walls.flatMap { listOf(it.startY, it.endY) } + floor.rooms.flatMap { listOf(it.y, it.y + it.depthM) }
        val minX = (xs.minOrNull() ?: 0f) - 1.5f
        val maxX = (xs.maxOrNull() ?: 12f) + 1.5f
        val minY = (ys.minOrNull() ?: 0f) - 1.5f
        val maxY = (ys.maxOrNull() ?: 9f) + 1.5f
        val worldW = max(2f, maxX - minX)
        val worldH = max(2f, maxY - minY)

        val drawLeft = 80f
        val drawTop = 90f
        val drawW = widthPx - 160f
        val drawH = heightPx - 230f
        val scale = min(drawW / worldW, drawH / worldH)
        val offsetX = drawLeft + (drawW - worldW * scale) / 2f - minX * scale
        val offsetY = drawTop + (drawH - worldH * scale) / 2f - minY * scale

        fun tx(x: Float) = offsetX + x * scale
        fun ty(y: Float) = offsetY + y * scale

        // Grid
        val gridPaint = Paint().apply {
            color = Color.parseColor("#172742")
            strokeWidth = 1.5f
        }
        for (gx in minX.toInt()..maxX.toInt()) {
            canvas.drawLine(tx(gx.toFloat()), drawTop, tx(gx.toFloat()), drawTop + drawH, gridPaint)
        }
        for (gy in minY.toInt()..maxY.toInt()) {
            canvas.drawLine(drawLeft, ty(gy.toFloat()), drawLeft + drawW, ty(gy.toFloat()), gridPaint)
        }

        // Rooms
        val roomFill = Paint().apply { style = Paint.Style.FILL }
        val roomText = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            textSize = 24f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val dimText = Paint().apply {
            color = Color.parseColor("#38BDF8")
            textSize = 18f
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        for (r in floor.rooms) {
            roomFill.color = try {
                Color.parseColor(r.floorColorHex)
            } catch (_: Exception) {
                Color.parseColor("#1E293B")
            }
            roomFill.alpha = 55
            canvas.drawRect(tx(r.x), ty(r.y), tx(r.x + r.widthM), ty(r.y + r.depthM), roomFill)
            val cx = tx(r.x + r.widthM / 2f)
            val cy = ty(r.y + r.depthM / 2f)
            canvas.drawText(r.name, cx, cy - 6f, roomText)
            val areaLabel = ArchitectureMath.formatArea(ArchitectureMath.roomAreaSqM(r), project.unitSystem)
            canvas.drawText(areaLabel, cx, cy + 20f, dimText)
        }

        // Walls & dimension labels
        val wallPaint = Paint().apply {
            color = Color.parseColor("#F8FAFC")
            strokeCap = Paint.Cap.SQUARE
            isAntiAlias = true
        }
        for (w in floor.walls) {
            wallPaint.strokeWidth = max(6f, w.thicknessM * scale)
            canvas.drawLine(tx(w.startX), ty(w.startY), tx(w.endX), ty(w.endY), wallPaint)
            val len = ArchitectureMath.wallLengthMeters(w)
            val label = ArchitectureMath.formatLength(len, project.unitSystem)
            val mx = tx((w.startX + w.endX) / 2f)
            val my = ty((w.startY + w.endY) / 2f) - 10f
            canvas.drawText(label, mx, my, dimText)
        }

        // Openings
        val opPaint = Paint().apply {
            strokeWidth = 6f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        for (op in floor.openings) {
            opPaint.color = if (op.type.isDoor) Color.parseColor("#F59E0B") else Color.parseColor("#38BDF8")
            val rad = Math.toRadians(op.angleDeg.toDouble())
            val dx = (cos(rad) * op.widthM * 0.5).toFloat()
            val dy = (sin(rad) * op.widthM * 0.5).toFloat()
            canvas.drawLine(tx(op.x - dx), ty(op.y - dy), tx(op.x + dx), ty(op.y + dy), opPaint)
        }

        // Furniture
        val furnPaint = Paint().apply {
            color = Color.parseColor("#94A3B8")
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        for (f in floor.furniture) {
            canvas.drawRect(tx(f.x), ty(f.y), tx(f.x + f.widthM), ty(f.y + f.depthM), furnPaint)
        }

        // MEP Electrical & Plumbing overlays
        if (includeMep) {
            val elecPaint = Paint().apply {
                color = Color.parseColor("#F59E0B")
                style = Paint.Style.FILL
                textSize = 16f
                isAntiAlias = true
            }
            for (e in floor.electricalNodes) {
                canvas.drawCircle(tx(e.x), ty(e.y), 10f, elecPaint)
                canvas.drawText(e.type.symbol, tx(e.x) + 12f, ty(e.y) + 5f, elecPaint)
            }
            val plumbPaint = Paint().apply {
                color = Color.parseColor("#10B981")
                style = Paint.Style.FILL
                textSize = 16f
                isAntiAlias = true
            }
            for (p in floor.plumbingNodes) {
                canvas.drawCircle(tx(p.x), ty(p.y), 10f, plumbPaint)
                canvas.drawText(p.type.symbol, tx(p.x) + 12f, ty(p.y) + 5f, plumbPaint)
            }
        }

        // Diagonal subtle watermark + CSI Title Block
        val wmPaint = Paint().apply {
            color = Color.parseColor("#38BDF8")
            alpha = 34
            textSize = 56f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.save()
        canvas.rotate(-22f, widthPx / 2f, heightPx / 2f)
        canvas.drawText("CSI • CREATION S IRFAN", widthPx / 2f, heightPx / 2f, wmPaint)
        canvas.restore()

        val footerPaint = Paint().apply {
            color = Color.parseColor("#101D32")
            style = Paint.Style.FILL
        }
        canvas.drawRect(24f, heightPx - 110f, widthPx - 24f, heightPx - 24f, footerPaint)
        canvas.drawLine(24f, heightPx - 110f, widthPx - 24f, heightPx - 110f, borderPaint)

        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 26f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val subPaint = Paint().apply {
            color = Color.parseColor("#94A3B8")
            textSize = 18f
            typeface = Typeface.MONOSPACE
            isAntiAlias = true
        }
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        canvas.drawText("CSI (Creation S Irfan) • HOMECRAFT 3D", 48f, heightPx - 70f, titlePaint)
        canvas.drawText(
            "${project.name} | ${floor.name} | Scale 1:${project.drawingScale} | Units: ${project.unitSystem.shortLabel} | $dateStr",
            48f,
            heightPx - 40f,
            subPaint
        )

        return bitmap
    }

    fun generateExportFile(
        context: Context,
        project: ProjectDocument,
        format: ExportFormat,
        prices: MaterialPrices = MaterialPrices()
    ): File {
        val dir = exportsDir(context)
        val file = File(dir, safeFileName(project, format.name.lowercase(), format.ext))
        val activeFloor = project.floors.find { it.id == project.activeFloorId } ?: project.floors.first()

        when (format) {
            ExportFormat.PNG, ExportFormat.JPG -> {
                val bmp = renderFloorPlanBitmap(project, activeFloor)
                FileOutputStream(file).use { out ->
                    val comp = if (format == ExportFormat.PNG) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                    bmp.compress(comp, 95, out)
                }
                bmp.recycle()
            }
            ExportFormat.PDF_A4, ExportFormat.PDF_A3 -> {
                val isA3 = format == ExportFormat.PDF_A3
                val pageW = if (isA3) 1191 else 842
                val pageH = if (isA3) 842 else 595
                val doc = PdfDocument()
                project.floors.forEachIndexed { idx, floor ->
                    val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, idx + 1).create()
                    val page = doc.startPage(pageInfo)
                    val bmp = renderFloorPlanBitmap(project, floor, widthPx = pageW * 2, heightPx = pageH * 2)
                    val scaled = Bitmap.createScaledBitmap(bmp, pageW, pageH, true)
                    page.canvas.drawBitmap(scaled, 0f, 0f, null)
                    doc.finishPage(page)
                    bmp.recycle()
                    scaled.recycle()
                }
                // Estimation summary page
                val estPageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, project.floors.size + 1).create()
                val estPage = doc.startPage(estPageInfo)
                drawEstimationSheetPage(estPage.canvas, project, prices, pageW, pageH)
                doc.finishPage(estPage)

                FileOutputStream(file).use { out -> doc.writeTo(out) }
                doc.close()
            }
            ExportFormat.SVG -> {
                file.writeText(buildSvgString(project, activeFloor))
            }
            ExportFormat.DXF -> {
                file.writeText(buildDxfString(project, activeFloor))
            }
            ExportFormat.OBJ -> {
                file.writeText(buildObjString(project))
            }
            ExportFormat.GLB -> {
                file.writeBytes(buildGlbBytes(project))
            }
            ExportFormat.CSI_NATIVE -> {
                file.writeText(ProjectSerialization.toJson(project))
            }
        }
        return file
    }

    private fun drawEstimationSheetPage(
        canvas: Canvas,
        project: ProjectDocument,
        prices: MaterialPrices,
        w: Int,
        h: Int
    ) {
        canvas.drawColor(Color.WHITE)
        val est = ArchitectureMath.calculateEstimate(project, prices)
        val header = Paint().apply {
            color = Color.parseColor("#0A1424")
            textSize = 22f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val body = Paint().apply {
            color = Color.parseColor("#1E293B")
            textSize = 14f
            typeface = Typeface.MONOSPACE
        }
        val note = Paint().apply {
            color = Color.parseColor("#B45309")
            textSize = 12f
        }
        canvas.drawText("CSI (Creation S Irfan) • Measurement & Material Estimate Sheet", 40f, 52f, header)
        canvas.drawText("Project: ${project.name}  |  Category: ${project.category.label}", 40f, 82f, body)
        val lines = listOf(
            "Total Floor Area       : ${String.format("%.1f", est.totalFloorAreaSqFt)} sq ft (${String.format("%.1f", est.totalFloorAreaSqM)} m²)",
            "Net Wall Surface Area  : ${String.format("%.1f", est.netWallSurfaceAreaSqM)} m²",
            "Floor Tiles (600x600)  : ${est.floorTileCount} pcs (${String.format("%.1f", est.floorTileAreaSqM)} m²)  -> ${prices.currencySymbol}${String.format("%.0f", est.floorTileCost)}",
            "Wall Tiles (Wet Zones) : ${est.wallTileCount} pcs (${String.format("%.1f", est.wallTileAreaSqM)} m²)  -> ${prices.currencySymbol}${String.format("%.0f", est.wallTileCost)}",
            "Paint Coverage (2-coat): ${est.paintLiters} L  -> ${prices.currencySymbol}${String.format("%.0f", est.paintCost)}",
            "Approx Cement (50kg)   : ${est.cementBags50Kg} bags  -> ${prices.currencySymbol}${String.format("%.0f", est.cementCost)}",
            "Approx Modular Bricks  : ${est.brickCount} units  -> ${prices.currencySymbol}${String.format("%.0f", est.brickCost)}",
            "Doors / Windows Count  : ${est.doorCount} Doors, ${est.windowCount} Windows  -> ${prices.currencySymbol}${String.format("%.0f", est.doorsWindowsCost)}",
            "-----------------------------------------------------------------------",
            "APPROXIMATE TOTAL COST : ${prices.currencySymbol}${String.format("%.2f", est.totalEstimatedCost)}"
        )
        lines.forEachIndexed { i, text ->
            canvas.drawText(text, 40f, 130f + i * 28f, body)
        }
        canvas.drawText("DISCLAIMER: Approximate architectural estimate only. Not a certified construction quotation.", 40f, h - 55f, note)
        canvas.drawText("Watermark: CSI • Creation S Irfan | HOMECRAFT 3D", 40f, h - 32f, note)
    }

    fun buildSvgString(project: ProjectDocument, floor: FloorPlan): String {
        val sb = StringBuilder()
        sb.append("""<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="900" viewBox="-2 -2 18 14">""")
        sb.append("""<rect x="-2" y="-2" width="18" height="14" fill="#0A1424"/>""")
        for (r in floor.rooms) {
            sb.append("""<rect x="${r.x}" y="${r.y}" width="${r.widthM}" height="${r.depthM}" fill="${r.floorColorHex}" fill-opacity="0.22" stroke="#38BDF8" stroke-width="0.03"/>""")
            sb.append("""<text x="${r.x + r.widthM / 2}" y="${r.y + r.depthM / 2}" fill="#F8FAFC" font-size="0.35" text-anchor="middle">${r.name}</text>""")
        }
        for (w in floor.walls) {
            sb.append("""<line x1="${w.startX}" y1="${w.startY}" x2="${w.endX}" y2="${w.endY}" stroke="#F8FAFC" stroke-width="${w.thicknessM}" stroke-linecap="square"/>""")
        }
        for (op in floor.openings) {
            val col = if (op.type.isDoor) "#F59E0B" else "#38BDF8"
            sb.append("""<circle cx="${op.x}" cy="${op.y}" r="0.2" fill="$col"/>""")
        }
        sb.append("""<text x="14.5" y="11.2" fill="#38BDF8" font-size="0.4" text-anchor="end">CSI • Creation S Irfan | ${project.name}</text>""")
        sb.append("</svg>")
        return sb.toString()
    }

    fun buildDxfString(project: ProjectDocument, floor: FloorPlan): String {
        val sb = StringBuilder()
        sb.append("0\nSECTION\n2\nHEADER\n9\n\$ACADVER\n1\nAC1009\n0\nENDSEC\n")
        sb.append("0\nSECTION\n2\nENTITIES\n")
        for (w in floor.walls) {
            sb.append("0\nLINE\n8\nWALLS\n10\n${w.startX}\n20\n${w.startY}\n30\n0.0\n11\n${w.endX}\n21\n${w.endY}\n31\n0.0\n")
        }
        for (op in floor.openings) {
            sb.append("0\nCIRCLE\n8\nOPENINGS\n10\n${op.x}\n20\n${op.y}\n30\n0.0\n40\n${op.widthM / 2f}\n")
        }
        sb.append("0\nTEXT\n8\nWATERMARK\n10\n0.0\n20\n-1.0\n30\n0.0\n40\n0.4\n1\nCSI - Creation S Irfan - ${project.name}\n")
        sb.append("0\nENDSEC\n0\nEOF\n")
        return sb.toString()
    }

    fun buildObjString(project: ProjectDocument): String {
        val sb = StringBuilder()
        sb.append("# CSI (Creation S Irfan) HOMECRAFT 3D - Wavefront OBJ\n")
        sb.append("# Project: ${project.name}\n")
        var vOffset = 1
        var yBase = 0f
        for (floor in project.floors.filter { it.isVisible }) {
            sb.append("g ${floor.name.replace(" ", "_")}\n")
            for (w in floor.walls) {
                val dx = w.endX - w.startX
                val dz = w.endY - w.startY
                val len = max(0.01f, kotlin.math.hypot(dx, dz))
                val nx = (-dz / len) * (w.thicknessM / 2f)
                val nz = (dx / len) * (w.thicknessM / 2f)
                val h = w.heightM
                val verts = listOf(
                    Triple(w.startX + nx, yBase, w.startY + nz),
                    Triple(w.endX + nx, yBase, w.endY + nz),
                    Triple(w.endX - nx, yBase, w.endY - nz),
                    Triple(w.startX - nx, yBase, w.startY - nz),
                    Triple(w.startX + nx, yBase + h, w.startY + nz),
                    Triple(w.endX + nx, yBase + h, w.endY + nz),
                    Triple(w.endX - nx, yBase + h, w.endY - nz),
                    Triple(w.startX - nx, yBase + h, w.startY - nz)
                )
                verts.forEach { (vx, vy, vz) -> sb.append("v $vx $vy $vz\n") }
                val b = vOffset
                sb.append("f $b ${b + 1} ${b + 5} ${b + 4}\n")
                sb.append("f ${b + 1} ${b + 2} ${b + 6} ${b + 5}\n")
                sb.append("f ${b + 2} ${b + 3} ${b + 7} ${b + 6}\n")
                sb.append("f ${b + 3} $b ${b + 4} ${b + 7}\n")
                sb.append("f ${b + 4} ${b + 5} ${b + 6} ${b + 7}\n")
                vOffset += 8
            }
            yBase += floor.ceilingHeightM
        }
        return sb.toString()
    }

    fun buildGlbBytes(project: ProjectDocument): ByteArray {
        // Build minimal valid glTF 2.0 binary (.glb) from wall bounding boxes
        val positions = mutableListOf<Float>()
        val indices = mutableListOf<Short>()
        var yBase = 0f
        var vCount = 0
        for (floor in project.floors.filter { it.isVisible }) {
            for (w in floor.walls) {
                val dx = w.endX - w.startX
                val dz = w.endY - w.startY
                val len = max(0.01f, kotlin.math.hypot(dx, dz))
                val nx = (-dz / len) * (w.thicknessM / 2f)
                val nz = (dx / len) * (w.thicknessM / 2f)
                val h = w.heightM
                val pts = floatArrayOf(
                    w.startX + nx, yBase, w.startY + nz,
                    w.endX + nx, yBase, w.endY + nz,
                    w.endX - nx, yBase, w.endY - nz,
                    w.startX - nx, yBase, w.startY - nz,
                    w.startX + nx, yBase + h, w.startY + nz,
                    w.endX + nx, yBase + h, w.endY + nz,
                    w.endX - nx, yBase + h, w.endY - nz,
                    w.startX - nx, yBase + h, w.startY - nz
                )
                pts.forEach { positions.add(it) }
                val boxIndices = intArrayOf(
                    0, 1, 5, 0, 5, 4,
                    1, 2, 6, 1, 6, 5,
                    2, 3, 7, 2, 7, 6,
                    3, 0, 4, 3, 4, 7,
                    4, 5, 6, 4, 6, 7
                )
                boxIndices.forEach { indices.add((vCount + it).toShort()) }
                vCount += 8
            }
            yBase += floor.ceilingHeightM
        }
        if (positions.isEmpty()) {
            positions.addAll(listOf(0f, 0f, 0f, 1f, 0f, 0f, 1f, 1f, 0f))
            indices.addAll(listOf(0, 1, 2))
            vCount = 3
        }
        val posByteLen = positions.size * 4
        val idxByteLen = indices.size * 2
        val idxPad = (4 - (idxByteLen % 4)) % 4
        val binLen = posByteLen + idxByteLen + idxPad

        val json = """{"asset":{"version":"2.0","generator":"CSI-HomeCraft3D"},"scene":0,"scenes":[{"nodes":[0]}],"nodes":[{"mesh":0,"name":"CSI_${project.name.replace("\"", "")}"}],"meshes":[{"primitives":[{"attributes":{"POSITION":0},"indices":1}]}],"buffers":[{"byteLength":$binLen}],"bufferViews":[{"buffer":0,"byteOffset":0,"byteLength":$posByteLen,"target":34962},{"buffer":0,"byteOffset":$posByteLen,"byteLength":$idxByteLen,"target":34963}],"accessors":[{"bufferView":0,"componentType":5126,"count":$vCount,"type":"VEC3"},{"bufferView":1,"componentType":5123,"count":${indices.size},"type":"SCALAR"}]}"""
        val jsonRaw = json.toByteArray(Charsets.UTF_8)
        val jsonPad = (4 - (jsonRaw.size % 4)) % 4
        val jsonChunkLen = jsonRaw.size + jsonPad
        val totalLen = 12 + 8 + jsonChunkLen + 8 + binLen

        val out = ByteArrayOutputStream(totalLen)
        val header = ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN)
        header.putInt(0x46546C67) // glTF magic
        header.putInt(2)
        header.putInt(totalLen)
        header.putInt(jsonChunkLen)
        header.putInt(0x4E4F534A) // JSON
        out.write(header.array())
        out.write(jsonRaw)
        repeat(jsonPad) { out.write(' '.code) }

        val binHead = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
        binHead.putInt(binLen)
        binHead.putInt(0x004E4942) // BIN
        out.write(binHead.array())

        val binBuf = ByteBuffer.allocate(binLen).order(ByteOrder.LITTLE_ENDIAN)
        positions.forEach { binBuf.putFloat(it) }
        indices.forEach { binBuf.putShort(it) }
        out.write(binBuf.array())
        return out.toByteArray()
    }

    fun shareFile(context: Context, file: File, mimeType: String, subject: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, "$subject — Created with CSI (Creation S Irfan) HOMECRAFT 3D")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share CSI Design")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun saveToDownloads(context: Context, file: File, mimeType: String): Result<String> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, file.name)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.RELATIVE_PATH, "Download/CSI-HomeCraft3D")
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return Result.failure(IllegalStateException("Unable to create Downloads entry"))
                resolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(file).use { input -> input.copyTo(out) }
                }
                Result.success("Saved to Downloads/CSI-HomeCraft3D/${file.name}")
            } else {
                Result.success("Exported to ${file.absolutePath}")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun printPdf(context: Context, pdfFile: File, jobName: String, isA3: Boolean) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder(pdfFile.name)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                try {
                    FileInputStream(pdfFile).use { input ->
                        FileOutputStream(destination.fileDescriptor).use { out ->
                            input.copyTo(out)
                        }
                    }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                }
            }
        }
        val attrs = PrintAttributes.Builder()
            .setMediaSize(if (isA3) PrintAttributes.MediaSize.ISO_A3 else PrintAttributes.MediaSize.ISO_A4)
            .build()
        printManager.print(jobName, adapter, attrs)
    }
}
