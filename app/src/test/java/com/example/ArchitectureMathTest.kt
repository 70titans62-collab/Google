package com.example

import com.example.data.model.ArchitectureMath
import com.example.data.model.FurnitureItem
import com.example.data.model.FurnitureType
import com.example.data.model.MaterialPrices
import com.example.data.model.ProjectSerialization
import com.example.data.model.ProjectTemplates
import com.example.data.model.UnitSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchitectureMathTest {

    @Test
    fun formatLength_allUnits_accurate() {
        assertEquals("10' 0\"", ArchitectureMath.formatLength(3.048f, UnitSystem.FEET_INCHES))
        assertEquals("3.50 m", ArchitectureMath.formatLength(3.5f, UnitSystem.METERS))
        assertEquals("350 cm", ArchitectureMath.formatLength(3.5f, UnitSystem.CENTIMETERS))
        assertEquals("3500 mm", ArchitectureMath.formatLength(3.5f, UnitSystem.MILLIMETERS))
    }

    @Test
    fun serialization_roundTrip_preservesProjectGeometry() {
        val villa = ProjectTemplates.buildDuplexVilla("user_irfan")
        val json = ProjectSerialization.toJson(villa)
        val restored = ProjectSerialization.fromJson(json)
        assertNotNull(restored)
        assertEquals(villa.name, restored!!.name)
        assertEquals(villa.floors.size, restored.floors.size)
        assertEquals(villa.floors.first().walls.size, restored.floors.first().walls.size)
    }

    @Test
    fun estimation_calculatesNonZeroQuantitiesAndCosts() {
        val villa = ProjectTemplates.buildDuplexVilla("user_irfan")
        val est = ArchitectureMath.calculateEstimate(villa, MaterialPrices())
        assertTrue(est.totalFloorAreaSqM > 50.0)
        assertTrue(est.floorTileCount > 100)
        assertTrue(est.brickCount > 1000)
        assertTrue(est.cementBags50Kg > 20)
        assertTrue(est.totalEstimatedCost > 1000.0)
    }

    @Test
    fun conflictDetector_detectsOverlappingFurniture() {
        val blank = ProjectTemplates.createBlankProject("u1")
        val floor = blank.floors.first().copy(
            furniture = listOf(
                FurnitureItem("a", FurnitureType.SOFA, 1f, 1f, 2f, 1f),
                FurnitureItem("b", FurnitureType.BED, 1.5f, 1.2f, 2f, 1.5f)
            )
        )
        val warnings = ArchitectureMath.analyzeDesignConflicts(floor)
        assertTrue(warnings.any { it.title.contains("Overlapping") })
    }
}
