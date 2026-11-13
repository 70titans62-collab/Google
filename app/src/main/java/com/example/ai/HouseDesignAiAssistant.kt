package com.example.ai

import android.util.Log
import com.example.data.model.ArchitectureMath
import com.example.data.model.FloorPlan
import com.example.data.model.FurnitureItem
import com.example.data.model.FurnitureType
import com.example.data.model.ProjectDocument
import com.example.data.model.RoomZone
import com.example.data.model.SurfaceMaterial
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.UUID

data class AiProposedDesignPatch(
    val title: String,
    val explanation: String,
    val suggestedPaintHex: String,
    val suggestedFloorMaterial: SurfaceMaterial,
    val roomNotes: List<String>,
    val addFurniture: List<FurnitureItem>,
    val addRooms: List<RoomZone>
)

sealed interface AiAssistantState {
    data object Idle : AiAssistantState
    data object Loading : AiAssistantState
    data class ProposalReady(
        val diagnosisSummary: String,
        val proposal: AiProposedDesignPatch
    ) : AiAssistantState
    data class ChatResponse(val answer: String) : AiAssistantState
    data class Error(val message: String) : AiAssistantState
}

class HouseDesignAiAssistant {

    private val structuredSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string("Short title for the design recommendation"),
            "explanation" to Schema.string("Simple, beginner-friendly explanation of the architectural advice"),
            "suggestedWallPaintHex" to Schema.string("6-digit hex color like #E0F2FE for interior walls"),
            "suggestedFloorMaterial" to Schema.enumeration(
                SurfaceMaterial.entries.filter { it.isFloor }.map { it.name },
                "Recommended floor finish"
            ),
            "roomArrangementNotes" to Schema.array(
                items = Schema.string(),
                description = "3 to 5 actionable room arrangement, furniture, or space-saving tips"
            ),
            "recommendedFurniture" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "type" to Schema.enumeration(FurnitureType.entries.map { it.name }),
                        "x" to Schema.double("X coordinate in meters"),
                        "y" to Schema.double("Y coordinate in meters")
                    )
                ),
                description = "0 to 3 suggested furniture items to add"
            )
        )
    )

    private val structuredModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = structuredSchema
            }
        )
    }

    private val chatModel by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    suspend fun analyzeAndSuggest(
        project: ProjectDocument,
        activeFloor: FloorPlan,
        userGoal: String
    ): Result<Pair<String, AiProposedDesignPatch>> {
        val conflicts = ArchitectureMath.analyzeDesignConflicts(activeFloor)
        val diagSummary = if (conflicts.isEmpty()) {
            "No overlapping objects or disconnected wall endpoints detected on ${activeFloor.name}."
        } else {
            conflicts.joinToString("\n") { "• ${it.title}: ${it.description}" }
        }

        val prompt = buildString {
            appendLine("You are CSI HOMECRAFT 3D's architectural house design assistant for S Irfan.")
            appendLine("Project: ${project.name} (${project.category.label}), Floor: ${activeFloor.name}")
            appendLine("Rooms (${activeFloor.rooms.size}): ${activeFloor.rooms.joinToString { "${it.name}(${it.widthM}x${it.depthM}m at ${it.x},${it.y})" }}")
            appendLine("Walls: ${activeFloor.walls.size}, Doors/Windows: ${activeFloor.openings.size}, Furniture: ${activeFloor.furniture.size}")
            appendLine("Detected geometry issues:\n$diagSummary")
            appendLine("User request: $userGoal")
            appendLine("Provide practical room arrangements, paint/tile palette, and safe non-overlapping furniture placement in simple language.")
        }

        return try {
            val response = withTimeout(60_000L) {
                structuredModel.generateContent(prompt)
            }
            val jsonText = response.text ?: throw IllegalStateException("AI returned empty response.")
            val root = JSONObject(jsonText)
            val title = root.optString("title", "AI Architectural Suggestion")
            val explanation = root.optString("explanation", "")
            val paintHex = root.optString("suggestedWallPaintHex", "#F1F5F9").let {
                if (it.startsWith("#") && it.length == 7) it else "#F1F5F9"
            }
            val floorMatName = root.optString("suggestedFloorMaterial", SurfaceMaterial.WOODEN_FLOORING.name)
            val floorMat = SurfaceMaterial.entries.find { it.name == floorMatName } ?: SurfaceMaterial.WOODEN_FLOORING
            val notesArr = root.optJSONArray("roomArrangementNotes")
            val notes = buildList {
                if (notesArr != null) {
                    for (i in 0 until notesArr.length()) add(notesArr.getString(i))
                }
            }
            val furnArr = root.optJSONArray("recommendedFurniture")
            val furnList = buildList {
                if (furnArr != null) {
                    for (i in 0 until furnArr.length()) {
                        val obj = furnArr.getJSONObject(i)
                        val fType = FurnitureType.entries.find { it.name == obj.optString("type") } ?: FurnitureType.CHAIR
                        add(
                            FurnitureItem(
                                id = "ai_f_${UUID.randomUUID().toString().take(6)}",
                                type = fType,
                                x = obj.optDouble("x", 2.0).toFloat().coerceIn(0.3f, 12f),
                                y = obj.optDouble("y", 2.0).toFloat().coerceIn(0.3f, 10f),
                                widthM = fType.defaultWidthM,
                                depthM = fType.defaultDepthM,
                                heightM = fType.defaultHeightM,
                                colorHex = fType.defaultColorHex
                            )
                        )
                    }
                }
            }
            val patch = AiProposedDesignPatch(
                title = title,
                explanation = explanation,
                suggestedPaintHex = paintHex,
                suggestedFloorMaterial = floorMat,
                roomNotes = notes,
                addFurniture = furnList,
                addRooms = emptyList()
            )
            Result.success(diagSummary to patch)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI design analysis timed out", e)
            Result.failure(Exception("AI design assistant timed out after 60 seconds. Please retry."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI design call failed", e)
            Result.failure(e)
        }
    }

    suspend fun askSimpleQuestion(project: ProjectDocument, floor: FloorPlan, question: String): Result<String> {
        val prompt = """
            You are CSI (Creation S Irfan) HOMECRAFT 3D architectural assistant.
            Project: ${project.name}, Active Floor: ${floor.name}, Rooms: ${floor.rooms.map { it.name }}.
            Answer in clear, simple language in under 140 words:
            $question
        """.trimIndent()
        return try {
            val response = withTimeout(60_000L) { chatModel.generateContent(prompt) }
            val txt = response.text
            if (!txt.isNullOrBlank()) Result.success(txt)
            else Result.failure(Exception("Model returned an empty response."))
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI question timed out", e)
            Result.failure(Exception("AI request timed out after 60 seconds."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI question failed", e)
            Result.failure(e)
        }
    }

    private companion object {
        const val TAG = "HouseDesignAi"
    }
}
