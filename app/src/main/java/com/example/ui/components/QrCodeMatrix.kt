package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.security.MessageDigest

/**
 * Generates a high-contrast 25x25 deterministic architectural matrix code with
 * standard QR finder patterns, timing tracks, and SHA-256 encoded payload bits.
 */
object ShareMatrixEncoder {
    fun encode(data: String, size: Int = 25): Array<BooleanArray> {
        val grid = Array(size) { BooleanArray(size) }

        fun drawFinder(top: Int, left: Int) {
            for (r in 0..6) {
                for (c in 0..6) {
                    val border = r == 0 || r == 6 || c == 0 || c == 6
                    val inner = r in 2..4 && c in 2..4
                    grid[top + r][left + c] = border || inner
                }
            }
        }

        drawFinder(0, 0)
        drawFinder(0, size - 7)
        drawFinder(size - 7, 0)

        // Timing patterns
        for (i in 8 until size - 8) {
            grid[6][i] = i % 2 == 0
            grid[i][6] = i % 2 == 0
        }

        // Alignment marker at bottom-right
        val alignPos = size - 9
        for (r in 0..4) {
            for (c in 0..4) {
                val border = r == 0 || r == 4 || c == 0 || c == 4
                val center = r == 2 && c == 2
                grid[alignPos + r][alignPos + c] = border || center
            }
        }

        // Fill data modules deterministically from SHA-256 + raw bytes
        val digest = MessageDigest.getInstance("SHA-256").digest(data.toByteArray())
        val raw = data.toByteArray() + digest
        var bitIdx = 0
        for (r in 0 until size) {
            for (c in 0 until size) {
                val inFinderTopLeft = r < 8 && c < 8
                val inFinderTopRight = r < 8 && c >= size - 8
                val inFinderBottomLeft = r >= size - 8 && c < 8
                val inTiming = r == 6 || c == 6
                val inAlign = r in alignPos..(alignPos + 4) && c in alignPos..(alignPos + 4)
                if (!inFinderTopLeft && !inFinderTopRight && !inFinderBottomLeft && !inTiming && !inAlign) {
                    val byteVal = raw[(bitIdx / 8) % raw.size].toInt() and 0xFF
                    val bit = (byteVal shr (bitIdx % 8)) and 1
                    val mask = ((r + c) % 2 == 0)
                    grid[r][c] = (bit == 1) xor mask
                    bitIdx++
                }
            }
        }
        return grid
    }
}

@Composable
fun ShareQrCodeCard(
    payload: String,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp
) {
    val matrix = remember(payload) { ShareMatrixEncoder.encode(payload) }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(12.dp)
            .semantics { contentDescription = "Project share QR code for $payload" }
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val n = matrix.size
            val cell = this.size.width / n
            for (r in 0 until n) {
                for (c in 0 until n) {
                    if (matrix[r][c]) {
                        drawRect(
                            color = Color(0xFF0A1424),
                            topLeft = Offset(c * cell, r * cell),
                            size = Size(cell + 0.5f, cell + 0.5f)
                        )
                    }
                }
            }
        }
    }
}
