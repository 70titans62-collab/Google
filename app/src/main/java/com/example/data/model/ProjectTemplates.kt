package com.example.data.model

import java.util.UUID

data class ProjectTemplateInfo(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: ProjectCategory,
    val floorsSummary: String,
    val areaSqFt: Int,
    val builder: (userId: String) -> ProjectDocument
)

object ProjectTemplates {
    val allTemplates: List<ProjectTemplateInfo> = listOf(
        ProjectTemplateInfo(
            id = "tpl_duplex_villa",
            title = "Irfan Modern Duplex Villa",
            subtitle = "3BHK • Ground + First Floor • Garage, Terrace & Full MEP",
            category = ProjectCategory.VILLA,
            floorsSummary = "2 Floors • 2,150 sq ft",
            areaSqFt = 2150,
            builder = { uid -> buildDuplexVilla(uid) }
        ),
        ProjectTemplateInfo(
            id = "tpl_2bhk_apartment",
            title = "Urban 2BHK Apartment",
            subtitle = "Open kitchen, living lounge, 2 bedrooms, 2 baths",
            category = ProjectCategory.APARTMENT,
            floorsSummary = "1 Floor • 1,180 sq ft",
            areaSqFt = 1180,
            builder = { uid -> buildTwoBhkApartment(uid) }
        ),
        ProjectTemplateInfo(
            id = "tpl_1bhk_studio",
            title = "Compact 1BHK Studio",
            subtitle = "Smart space-saving layout with sliding partitions",
            category = ProjectCategory.RESIDENTIAL,
            floorsSummary = "1 Floor • 640 sq ft",
            areaSqFt = 640,
            builder = { uid -> buildCompactStudio(uid) }
        ),
        ProjectTemplateInfo(
            id = "tpl_commercial_office",
            title = "Architectural Studio & Office",
            subtitle = "Reception, conference room, workstations & pantry",
            category = ProjectCategory.COMMERCIAL,
            floorsSummary = "1 Floor • 1,450 sq ft",
            areaSqFt = 1450,
            builder = { uid -> buildCommercialOffice(uid) }
        )
    )

    fun createBlankProject(
        userId: String,
        name: String = "New CSI Residence",
        category: ProjectCategory = ProjectCategory.RESIDENTIAL,
        unitSystem: UnitSystem = UnitSystem.FEET_INCHES
    ): ProjectDocument {
        val floorId = "floor_${UUID.randomUUID().toString().take(8)}"
        val now = System.currentTimeMillis()
        return ProjectDocument(
            id = "proj_${UUID.randomUUID().toString().take(10)}",
            userId = userId,
            name = name,
            category = category,
            unitSystem = unitSystem,
            floors = listOf(
                FloorPlan(
                    id = floorId,
                    name = "Ground Floor",
                    levelIndex = 0
                )
            ),
            activeFloorId = floorId,
            createdAtMillis = now,
            updatedAtMillis = now
        )
    }

    fun buildDuplexVilla(userId: String): ProjectDocument {
        val now = System.currentTimeMillis()
        val gfId = "floor_gf_${UUID.randomUUID().toString().take(6)}"
        val ffId = "floor_ff_${UUID.randomUUID().toString().take(6)}"

        val gfWalls = listOf(
            // Outer shell (12m x 9m)
            WallSegment("w1", 0f, 0f, 12f, 0f, 0.23f, 3.1f, "#F8FAFC", "#CBD5E1", SurfaceMaterial.TEXTURE_STUCCO),
            WallSegment("w2", 12f, 0f, 12f, 9f, 0.23f, 3.1f, "#F8FAFC", "#CBD5E1", SurfaceMaterial.TEXTURE_STUCCO),
            WallSegment("w3", 12f, 9f, 0f, 9f, 0.23f, 3.1f, "#F8FAFC", "#CBD5E1", SurfaceMaterial.TEXTURE_STUCCO),
            WallSegment("w4", 0f, 9f, 0f, 0f, 0.23f, 3.1f, "#F8FAFC", "#CBD5E1", SurfaceMaterial.TEXTURE_STUCCO),
            // Interior partitions
            WallSegment("w5", 6.5f, 0f, 6.5f, 5f, 0.15f, 3.1f, "#E0F2FE", "#F1F5F9", SurfaceMaterial.PAINT_MATTE),
            WallSegment("w6", 0f, 5f, 12f, 5f, 0.15f, 3.1f, "#F1F5F9", "#F1F5F9", SurfaceMaterial.PAINT_MATTE),
            WallSegment("w7", 4.5f, 5f, 4.5f, 9f, 0.15f, 3.1f, "#FEF3C7", "#F1F5F9", SurfaceMaterial.PAINT_MATTE),
            WallSegment("w8", 8.5f, 5f, 8.5f, 9f, 0.15f, 3.1f, "#CCFBF1", "#F1F5F9", SurfaceMaterial.BATH_KITCHEN_TILE)
        )

        val gfRooms = listOf(
            RoomZone("r1", "Living Lounge", 0f, 0f, 6.5f, 5f, SurfaceMaterial.MARBLE_GRANITE, "#F1F5F9", "#F8FAFC", CeilingDesign.COVE_LED),
            RoomZone("r2", "Dining & Kitchen", 6.5f, 0f, 5.5f, 5f, SurfaceMaterial.VITRIFIED_TILE, "#E2E8F0", "#FEF3C7", CeilingDesign.TRAY),
            RoomZone("r3", "Master Bedroom", 0f, 5f, 4.5f, 4f, SurfaceMaterial.WOODEN_FLOORING, "#B47B44", "#F1F5F9", CeilingDesign.COFFERED),
            RoomZone("r4", "Guest Suite", 4.5f, 5f, 4f, 4f, SurfaceMaterial.WOODEN_FLOORING, "#C08552", "#F8FAFC", CeilingDesign.FLAT),
            RoomZone("r5", "Bath & Utility", 8.5f, 5f, 3.5f, 4f, SurfaceMaterial.BATH_KITCHEN_TILE, "#CCFBF1", "#E0F2FE", CeilingDesign.FLAT)
        )

        val gfOpenings = listOf(
            OpeningItem("op1", OpeningType.MAIN_ENTRANCE_DOOR, 2.5f, 0f, 0f, 1.2f, 2.2f, 0f, SwingDirection.RIGHT_IN, "#7C2D12", "w1"),
            OpeningItem("op2", OpeningType.SLIDING_WINDOW, 4.8f, 0f, 0f, 1.8f, 1.3f, 0.9f, SwingDirection.SLIDE, "#38BDF8", "w1"),
            OpeningItem("op3", OpeningType.SLIDING_WINDOW, 9.2f, 0f, 0f, 1.6f, 1.2f, 0.9f, SwingDirection.SLIDE, "#38BDF8", "w1"),
            OpeningItem("op4", OpeningType.WOODEN_DOOR, 2.2f, 5f, 0f, 0.9f, 2.1f, 0f, SwingDirection.LEFT_IN, "#92400E", "w6"),
            OpeningItem("op5", OpeningType.WOODEN_DOOR, 6.2f, 5f, 0f, 0.9f, 2.1f, 0f, SwingDirection.RIGHT_IN, "#92400E", "w6"),
            OpeningItem("op6", OpeningType.BATHROOM_DOOR, 9.8f, 5f, 0f, 0.75f, 2.0f, 0f, SwingDirection.LEFT_IN, "#475569", "w6"),
            OpeningItem("op7", OpeningType.VENTILATION_WINDOW, 10.2f, 9f, 0f, 0.7f, 0.5f, 1.8f, SwingDirection.SLIDE, "#38BDF8", "w3")
        )

        val gfFurniture = listOf(
            FurnitureItem("f1", FurnitureType.SOFA, 1.0f, 1.2f, 2.3f, 0.95f, 0.85f, 0f, "#334155"),
            FurnitureItem("f2", FurnitureType.DINING_TABLE, 7.6f, 1.4f, 1.8f, 1.0f, 0.75f, 0f, "#78350F"),
            FurnitureItem("f3", FurnitureType.KITCHEN_CABINET, 9.4f, 0.3f, 2.3f, 0.65f, 0.9f, 0f, "#0F766E"),
            FurnitureItem("f4", FurnitureType.BED, 0.6f, 6.1f, 2.0f, 1.8f, 0.6f, 0f, "#4F46E5"),
            FurnitureItem("f5", FurnitureType.WARDROBE, 3.6f, 5.3f, 0.65f, 2.0f, 2.2f, 0f, "#7C2D12"),
            FurnitureItem("f6", FurnitureType.STAIRCASE, 5.2f, 1.6f, 1.1f, 3.1f, 3.1f, 0f, "#64748B")
        )

        val gfElectrical = listOf(
            ElectricalNode("e1", ElectricalType.DISTRIBUTION_BOARD, 0.5f, 0.5f, "MAIN", null, 0),
            ElectricalNode("e2", ElectricalType.SWITCH_BOARD, 1.8f, 0.4f, "C1", "e1", 0),
            ElectricalNode("e3", ElectricalType.LIGHT_POINT, 3.2f, 2.4f, "C1", "e2", 24),
            ElectricalNode("e4", ElectricalType.FAN_POINT, 3.2f, 3.0f, "C1", "e2", 75),
            ElectricalNode("e5", ElectricalType.AC_POINT, 0.5f, 7.2f, "C2", "e1", 1800),
            ElectricalNode("e6", ElectricalType.PLUG_SOCKET, 11.4f, 0.7f, "C3", "e1", 500)
        )

        val gfPlumbing = listOf(
            PlumbingNode("p1", PlumbingType.WATER_TANK, 11.2f, 8.2f, 32, false, null),
            PlumbingNode("p2", PlumbingType.KITCHEN_SINK, 10.5f, 0.6f, 25, false, "p1"),
            PlumbingNode("p3", PlumbingType.WASH_BASIN, 9.2f, 6.2f, 25, false, "p1"),
            PlumbingNode("p4", PlumbingType.SHOWER, 11.2f, 6.0f, 25, false, "p1"),
            PlumbingNode("p5", PlumbingType.TOILET, 9.2f, 8.2f, 110, true, "p3")
        )

        val ffWalls = listOf(
            WallSegment("fw1", 0f, 0f, 10f, 0f, 0.23f, 3.0f, "#F8FAFC", "#CBD5E1", SurfaceMaterial.PAINT_MATTE),
            WallSegment("fw2", 10f, 0f, 10f, 8f, 0.23f, 3.0f, "#F8FAFC", "#CBD5E1", SurfaceMaterial.PAINT_MATTE),
            WallSegment("fw3", 10f, 8f, 0f, 8f, 0.23f, 3.0f, "#F8FAFC", "#CBD5E1", SurfaceMaterial.PAINT_MATTE),
            WallSegment("fw4", 0f, 8f, 0f, 0f, 0.23f, 3.0f, "#F8FAFC", "#CBD5E1", SurfaceMaterial.PAINT_MATTE),
            WallSegment("fw5", 5f, 0f, 5f, 8f, 0.15f, 3.0f, "#F1F5F9", "#F1F5F9", SurfaceMaterial.PAINT_MATTE)
        )

        val ffRooms = listOf(
            RoomZone("fr1", "Upper Lounge & Study", 0f, 0f, 5f, 8f, SurfaceMaterial.WOODEN_FLOORING, "#C08552", "#F8FAFC", CeilingDesign.GYPSUM_MODERN),
            RoomZone("fr2", "Sky Terrace Bedroom", 5f, 0f, 5f, 8f, SurfaceMaterial.VITRIFIED_TILE, "#F8FAFC", "#E0F2FE", CeilingDesign.COVE_LED)
        )

        val ffOpenings = listOf(
            OpeningItem("fop1", OpeningType.GLASS_DOOR, 7.2f, 0f, 0f, 1.2f, 2.1f, 0f, SwingDirection.SLIDE, "#38BDF8", "fw1"),
            OpeningItem("fop2", OpeningType.WOODEN_DOOR, 5f, 3.5f, 90f, 0.9f, 2.1f, 0f, SwingDirection.LEFT_IN, "#7C2D12", "fw5")
        )

        val ffFurniture = listOf(
            FurnitureItem("ff1", FurnitureType.STUDY_DESK, 0.8f, 1.0f, 1.6f, 0.8f, 0.75f, 0f, "#57534E"),
            FurnitureItem("ff2", FurnitureType.BED, 6.4f, 2.0f, 2.0f, 1.8f, 0.6f, 0f, "#0EA5E9")
        )

        return ProjectDocument(
            id = "proj_${UUID.randomUUID().toString().take(10)}",
            userId = userId,
            name = "Irfan Modern Duplex Villa",
            category = ProjectCategory.VILLA,
            isFavorite = true,
            unitSystem = UnitSystem.FEET_INCHES,
            floors = listOf(
                FloorPlan(
                    id = gfId,
                    name = "Ground Floor",
                    levelIndex = 0,
                    ceilingHeightM = 3.1f,
                    walls = gfWalls,
                    rooms = gfRooms,
                    openings = gfOpenings,
                    furniture = gfFurniture,
                    electricalNodes = gfElectrical,
                    plumbingNodes = gfPlumbing,
                    annotations = listOf(
                        MeasurementAnnotation("m1", 0f, -0.6f, 12f, -0.6f, "Overall Width"),
                        MeasurementAnnotation("m2", -0.6f, 0f, -0.6f, 9f, "Overall Depth")
                    )
                ),
                FloorPlan(
                    id = ffId,
                    name = "First Floor",
                    levelIndex = 1,
                    ceilingHeightM = 3.0f,
                    walls = ffWalls,
                    rooms = ffRooms,
                    openings = ffOpenings,
                    furniture = ffFurniture
                )
            ),
            activeFloorId = gfId,
            exteriorConfig = ExteriorConfig(
                roofStyle = RoofStyle.GABLE,
                roofColorHex = "#1E293B",
                facadeColorHex = "#F1F5F9",
                hasCompoundWall = true,
                hasMainGate = true,
                hasGarage = true,
                hasBalconyTerrace = true
            ),
            createdAtMillis = now,
            updatedAtMillis = now
        )
    }

    fun buildTwoBhkApartment(userId: String): ProjectDocument {
        val now = System.currentTimeMillis()
        val gfId = "floor_apt_${UUID.randomUUID().toString().take(6)}"
        val walls = listOf(
            WallSegment("w1", 0f, 0f, 10f, 0f, 0.2f, 3.0f),
            WallSegment("w2", 10f, 0f, 10f, 8f, 0.2f, 3.0f),
            WallSegment("w3", 10f, 8f, 0f, 8f, 0.2f, 3.0f),
            WallSegment("w4", 0f, 8f, 0f, 0f, 0.2f, 3.0f),
            WallSegment("w5", 5f, 0f, 5f, 8f, 0.15f, 3.0f),
            WallSegment("w6", 5f, 4f, 10f, 4f, 0.15f, 3.0f)
        )
        val rooms = listOf(
            RoomZone("r1", "Living & Kitchen", 0f, 0f, 5f, 8f, SurfaceMaterial.VITRIFIED_TILE),
            RoomZone("r2", "Bedroom 1", 5f, 0f, 5f, 4f, SurfaceMaterial.WOODEN_FLOORING, "#B47B44"),
            RoomZone("r3", "Bedroom 2", 5f, 4f, 5f, 4f, SurfaceMaterial.WOODEN_FLOORING, "#C08552")
        )
        val openings = listOf(
            OpeningItem("op1", OpeningType.MAIN_ENTRANCE_DOOR, 1.5f, 0f, 0f, 1.1f, 2.1f, 0f, SwingDirection.LEFT_IN, "#7C2D12", "w1"),
            OpeningItem("op2", OpeningType.WOODEN_DOOR, 5f, 2.0f, 90f, 0.9f, 2.1f, 0f, SwingDirection.RIGHT_IN, "#92400E", "w5"),
            OpeningItem("op3", OpeningType.WOODEN_DOOR, 5f, 6.0f, 90f, 0.9f, 2.1f, 0f, SwingDirection.LEFT_IN, "#92400E", "w5"),
            OpeningItem("op4", OpeningType.SLIDING_WINDOW, 7.5f, 0f, 0f, 1.5f, 1.2f, 0.9f, SwingDirection.SLIDE, "#38BDF8", "w1")
        )
        val furniture = listOf(
            FurnitureItem("f1", FurnitureType.SOFA, 0.8f, 1.6f, 2.1f, 0.9f, 0.85f, 0f, "#334155"),
            FurnitureItem("f2", FurnitureType.KITCHEN_CABINET, 0.4f, 6.8f, 2.5f, 0.65f, 0.9f, 0f, "#0F766E"),
            FurnitureItem("f3", FurnitureType.BED, 6.5f, 0.8f, 2.0f, 1.7f, 0.6f, 0f, "#6366F1"),
            FurnitureItem("f4", FurnitureType.BED, 6.5f, 5.2f, 2.0f, 1.7f, 0.6f, 0f, "#0EA5E9")
        )
        return ProjectDocument(
            id = "proj_${UUID.randomUUID().toString().take(10)}",
            userId = userId,
            name = "Urban 2BHK Apartment",
            category = ProjectCategory.APARTMENT,
            unitSystem = UnitSystem.FEET_INCHES,
            floors = listOf(
                FloorPlan(
                    id = gfId,
                    name = "Ground Floor",
                    levelIndex = 0,
                    walls = walls,
                    rooms = rooms,
                    openings = openings,
                    furniture = furniture
                )
            ),
            activeFloorId = gfId,
            exteriorConfig = ExteriorConfig(roofStyle = RoofStyle.FLAT, hasCompoundWall = false, hasGarage = false),
            createdAtMillis = now,
            updatedAtMillis = now
        )
    }

    fun buildCompactStudio(userId: String): ProjectDocument {
        val now = System.currentTimeMillis()
        val gfId = "floor_st_${UUID.randomUUID().toString().take(6)}"
        val walls = listOf(
            WallSegment("w1", 0f, 0f, 7f, 0f, 0.2f, 2.9f),
            WallSegment("w2", 7f, 0f, 7f, 6f, 0.2f, 2.9f),
            WallSegment("w3", 7f, 6f, 0f, 6f, 0.2f, 2.9f),
            WallSegment("w4", 0f, 6f, 0f, 0f, 0.2f, 2.9f),
            WallSegment("w5", 4.5f, 3.8f, 7f, 3.8f, 0.12f, 2.9f)
        )
        val rooms = listOf(
            RoomZone("r1", "Studio Living", 0f, 0f, 7f, 3.8f, SurfaceMaterial.WOODEN_FLOORING, "#C08552"),
            RoomZone("r2", "Bath & Dressing", 4.5f, 3.8f, 2.5f, 2.2f, SurfaceMaterial.BATH_KITCHEN_TILE, "#CCFBF1")
        )
        val openings = listOf(
            OpeningItem("op1", OpeningType.MAIN_ENTRANCE_DOOR, 1.2f, 0f, 0f, 1.0f, 2.1f, 0f, SwingDirection.RIGHT_IN, "#7C2D12", "w1"),
            OpeningItem("op2", OpeningType.SLIDING_DOOR, 5.2f, 3.8f, 0f, 1.1f, 2.0f, 0f, SwingDirection.SLIDE, "#38BDF8", "w5")
        )
        return ProjectDocument(
            id = "proj_${UUID.randomUUID().toString().take(10)}",
            userId = userId,
            name = "Compact 1BHK Studio",
            category = ProjectCategory.RESIDENTIAL,
            unitSystem = UnitSystem.METERS,
            floors = listOf(FloorPlan(id = gfId, name = "Ground Floor", walls = walls, rooms = rooms, openings = openings)),
            activeFloorId = gfId,
            createdAtMillis = now,
            updatedAtMillis = now
        )
    }

    fun buildCommercialOffice(userId: String): ProjectDocument {
        val now = System.currentTimeMillis()
        val gfId = "floor_of_${UUID.randomUUID().toString().take(6)}"
        val walls = listOf(
            WallSegment("w1", 0f, 0f, 11f, 0f, 0.23f, 3.3f),
            WallSegment("w2", 11f, 0f, 11f, 8f, 0.23f, 3.3f),
            WallSegment("w3", 11f, 8f, 0f, 8f, 0.23f, 3.3f),
            WallSegment("w4", 0f, 8f, 0f, 0f, 0.23f, 3.3f),
            WallSegment("w5", 6f, 0f, 6f, 5f, 0.15f, 3.3f)
        )
        val rooms = listOf(
            RoomZone("r1", "Workstation Bay", 0f, 0f, 6f, 8f, SurfaceMaterial.VITRIFIED_TILE),
            RoomZone("r2", "Conference & Director", 6f, 0f, 5f, 8f, SurfaceMaterial.MARBLE_GRANITE)
        )
        val openings = listOf(
            OpeningItem("op1", OpeningType.DOUBLE_DOOR, 2.5f, 0f, 0f, 1.6f, 2.2f, 0f, SwingDirection.LEFT_OUT, "#38BDF8", "w1"),
            OpeningItem("op2", OpeningType.GLASS_DOOR, 6f, 2.2f, 90f, 1.0f, 2.1f, 0f, SwingDirection.RIGHT_IN, "#38BDF8", "w5")
        )
        val furniture = listOf(
            FurnitureItem("f1", FurnitureType.STUDY_DESK, 1.0f, 2.0f, 1.6f, 0.8f, 0.75f, 0f, "#334155"),
            FurnitureItem("f2", FurnitureType.STUDY_DESK, 1.0f, 4.5f, 1.6f, 0.8f, 0.75f, 0f, "#334155"),
            FurnitureItem("f3", FurnitureType.DINING_TABLE, 7.2f, 2.5f, 2.6f, 1.2f, 0.75f, 0f, "#78350F")
        )
        return ProjectDocument(
            id = "proj_${UUID.randomUUID().toString().take(10)}",
            userId = userId,
            name = "Architectural Studio & Office",
            category = ProjectCategory.COMMERCIAL,
            unitSystem = UnitSystem.METERS,
            floors = listOf(FloorPlan(id = gfId, name = "Ground Floor", walls = walls, rooms = rooms, openings = openings, furniture = furniture)),
            activeFloorId = gfId,
            exteriorConfig = ExteriorConfig(roofStyle = RoofStyle.FLAT, hasGarage = false, hasCompoundWall = true),
            createdAtMillis = now,
            updatedAtMillis = now
        )
    }
}
