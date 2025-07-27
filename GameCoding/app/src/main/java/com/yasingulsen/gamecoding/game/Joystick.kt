package com.yasingulsen.gamecoding.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.*

@Composable
fun Joystick(
    onJoystickMoved: (x: Float, z: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var joystickCenter by remember { mutableStateOf(Offset.Zero) }
    var joystickPosition by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }
    
    val joystickRadius = 40f
    val innerRadius = 15f
    
    Box(
        modifier = modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.3f))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        joystickCenter = offset
                        joystickPosition = offset
                        isDragging = true
                    },
                    onDrag = { _, dragAmount ->
                        joystickPosition += dragAmount
                        
                        // Limit joystick movement to circle
                        val distance = sqrt(
                            (joystickPosition.x - joystickCenter.x).pow(2) +
                            (joystickPosition.y - joystickCenter.y).pow(2)
                        )
                        
                        if (distance > joystickRadius) {
                            val angle = atan2(
                                joystickPosition.y - joystickCenter.y,
                                joystickPosition.x - joystickCenter.x
                            )
                            joystickPosition = Offset(
                                joystickCenter.x + cos(angle) * joystickRadius,
                                joystickCenter.y + sin(angle) * joystickRadius
                            )
                        }
                        
                        // Calculate normalized input (-1 to 1)
                        val normalizedX = (joystickPosition.x - joystickCenter.x) / joystickRadius
                        val normalizedZ = (joystickPosition.y - joystickCenter.y) / joystickRadius
                        
                        onJoystickMoved(normalizedX, normalizedZ)
                    },
                    onDragEnd = {
                        joystickPosition = joystickCenter
                        isDragging = false
                        onJoystickMoved(0f, 0f) // Stop movement
                    }
                )
            }
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            
            // Draw outer circle
            drawCircle(
                color = Color.White.copy(alpha = 0.3f),
                radius = joystickRadius,
                center = center,
                style = Stroke(width = 2f)
            )
            
            // Draw inner circle (joystick handle)
            val handleCenter = if (isDragging) {
                joystickPosition
            } else {
                center
            }
            
            drawCircle(
                color = Color.White.copy(alpha = 0.8f),
                radius = innerRadius,
                center = handleCenter
            )
            
            // Draw center dot
            drawCircle(
                color = Color.White,
                radius = 3f,
                center = center
            )
        }
    }
} 