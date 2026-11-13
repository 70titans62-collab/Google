package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ArchitectureMath
import com.example.data.model.MaterialPrices
import com.example.export.ExportEngine
import com.example.export.ExportFormat
import com.example.ui.viewmodel.StudioViewModel

@Composable
fun MaterialCalculatorScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val project by viewModel.activeProject.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val current = project
    if (current == null) {
        Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
            Text("Select a project to compute approximate material estimates.")
        }
        return
    }

    var prices by remember(settings.materialPrices) { mutableStateOf(settings.materialPrices) }
    val estimate = remember(current, prices) { ArchitectureMath.calculateEstimate(current, prices) }
    val sym = prices.currencySymbol

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Construction Material Estimator", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Project: ${current.name}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Approximate Estimation Only — Quantities and costs are planning estimates, not certified construction quotations.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Calculated Quantities & Costs", style = MaterialTheme.typography.titleLarge)
                    EstimateRow("Total Floor Area", "${String.format("%.1f", estimate.totalFloorAreaSqFt)} sq ft (${String.format("%.1f", estimate.totalFloorAreaSqM)} m²)")
                    EstimateRow("Net Wall Surface Area", "${String.format("%.1f", estimate.netWallSurfaceAreaSqM)} m²")
                    EstimateRow("Floor Tiles (600x600mm +10%)", "${estimate.floorTileCount} pcs", "$sym${String.format("%.0f", estimate.floorTileCost)}")
                    EstimateRow("Wall Tiles (Wet Zones +10%)", "${estimate.wallTileCount} pcs", "$sym${String.format("%.0f", estimate.wallTileCost)}")
                    EstimateRow("Paint Coverage (2 coats)", "${estimate.paintLiters} Liters", "$sym${String.format("%.0f", estimate.paintCost)}")
                    EstimateRow("Approx. Cement (50kg bags)", "${estimate.cementBags50Kg} bags", "$sym${String.format("%.0f", estimate.cementCost)}")
                    EstimateRow("Approx. Modular Bricks", "${estimate.brickCount} bricks", "$sym${String.format("%.0f", estimate.brickCost)}")
                    EstimateRow("Doors & Windows", "${estimate.doorCount} Doors, ${estimate.windowCount} Win", "$sym${String.format("%.0f", estimate.doorsWindowsCost)}")
                    HorizontalDivider()
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Approx. Total Material Cost", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("$sym${String.format("%,.2f", estimate.totalEstimatedCost)}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Local Material Unit Prices", style = MaterialTheme.typography.titleLarge)
                    PriceField("Currency Symbol", prices.currencySymbol, isText = true) { prices = prices.copy(currencySymbol = it.ifBlank { "$" }) }
                    PriceField("Floor Tile ($sym / m²)", prices.floorTilePerSqM.toString()) { v -> v.toDoubleOrNull()?.let { prices = prices.copy(floorTilePerSqM = it) } }
                    PriceField("Wall Tile ($sym / m²)", prices.wallTilePerSqM.toString()) { v -> v.toDoubleOrNull()?.let { prices = prices.copy(wallTilePerSqM = it) } }
                    PriceField("Paint ($sym / Liter)", prices.paintPerLiter.toString()) { v -> v.toDoubleOrNull()?.let { prices = prices.copy(paintPerLiter = it) } }
                    PriceField("Cement ($sym / 50kg bag)", prices.cementPerBag.toString()) { v -> v.toDoubleOrNull()?.let { prices = prices.copy(cementPerBag = it) } }
                    PriceField("Brick ($sym / unit)", prices.brickPerUnit.toString()) { v -> v.toDoubleOrNull()?.let { prices = prices.copy(brickPerUnit = it) } }
                    PriceField("Door Unit ($sym)", prices.doorUnitPrice.toString()) { v -> v.toDoubleOrNull()?.let { prices = prices.copy(doorUnitPrice = it) } }
                    PriceField("Window Unit ($sym)", prices.windowUnitPrice.toString()) { v -> v.toDoubleOrNull()?.let { prices = prices.copy(windowUnitPrice = it) } }
                    Button(
                        onClick = {
                            viewModel.saveMaterialPrices(prices)
                            viewModel.showMessage("Saved local material prices")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Local Pricing")
                    }
                }
            }
        }
    }
}

@Composable
private fun EstimateRow(label: String, qty: String, cost: String? = null) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1.2f))
        Text(qty, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
        if (cost != null) {
            Text(cost, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun PriceField(label: String, value: String, isText: Boolean = false, onValueChange: (String) -> Unit) {
    var text by remember(value) { mutableStateOf(value) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onValueChange(it)
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (isText) KeyboardType.Text else KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun ExportCenterScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val project by viewModel.activeProject.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val current = project
    if (current == null) {
        Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
            Text("Select a project to export.")
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("CSI Professional Export Centre", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "All exports include exact dimensions, multi-floor / MEP sheets, and the CSI • Creation S Irfan title block.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(ExportFormat.entries) { fmt ->
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(fmt.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(".${fmt.ext} • ${fmt.mime}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (fmt == ExportFormat.PDF_A4 || fmt == ExportFormat.PDF_A3) {
                            OutlinedButton(
                                onClick = {
                                    val file = ExportEngine.generateExportFile(context, current, fmt, settings.materialPrices)
                                    ExportEngine.printPdf(context, file, "CSI_${current.name}", fmt == ExportFormat.PDF_A3)
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Filled.Print, contentDescription = "Print", modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Print")
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                val file = ExportEngine.generateExportFile(context, current, fmt, settings.materialPrices)
                                val res = ExportEngine.saveToDownloads(context, file, fmt.mime)
                                viewModel.showMessage(res.getOrElse { it.localizedMessage ?: "Download failed" })
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("download_${fmt.name.lowercase()}")
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = "Download", modifier = Modifier.size(16.dp))
                        }
                        Button(
                            onClick = {
                                val file = ExportEngine.generateExportFile(context, current, fmt, settings.materialPrices)
                                ExportEngine.shareFile(context, file, fmt.mime, "${current.name} (${fmt.label})")
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("share_${fmt.name.lowercase()}")
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Share")
                        }
                    }
                }
            }
        }
    }
}
