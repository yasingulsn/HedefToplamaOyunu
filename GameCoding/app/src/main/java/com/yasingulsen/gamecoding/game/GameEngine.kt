package com.yasingulsen.gamecoding.game

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

data class BallState(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val velocityX: Float = 0f,
    val velocityY: Float = 0f,
    val velocityZ: Float = 0f,
    val isOnGround: Boolean = false
)

data class Target(
    val x: Float,
    val z: Float,
    val collected: Boolean = false
)

data class GameState(
    val ball: BallState = BallState(),
    val targets: List<Target> = emptyList(),
    val score: Int = 0,
    val isGameOver: Boolean = false,
    val isPaused: Boolean = false,
    val level: Int = 1,
    val message: String = "Joystick ile kırmızı topu hareket ettir!",
    val targetsCollected: Int = 0,
    val totalTargetsInLevel: Int = 0
)

class GameEngine(private val context: Context) {
    
    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()
    
    private var lastUpdateTime = System.currentTimeMillis()
    
    // Simplified physics
    private val gravity = -15f
    private val friction = 0.9f
    private val maxVelocity = 5f
    private val groundLevel = -3f
    private val boundaryX = 6f
    private val boundaryZ = 6f
    
    // Joystick controls
    private var moveX = 0f
    private var moveZ = 0f
    
    fun stop() {
        // Cleanup if needed
    }
    
    fun pause() {
        _gameState.value = _gameState.value.copy(isPaused = true)
    }
    
    fun resume() {
        _gameState.value = _gameState.value.copy(isPaused = false)
        lastUpdateTime = System.currentTimeMillis()
    }
    
    fun reset() {
        _gameState.value = GameState()
        lastUpdateTime = System.currentTimeMillis()
        moveX = 0f
        moveZ = 0f
        generateLevel(1)
    }
    
    fun start() {
        lastUpdateTime = System.currentTimeMillis()
        if (_gameState.value.targets.isEmpty()) {
            generateLevel(1)
        }
    }
    
    private fun generateLevel(level: Int) {
        val currentState = _gameState.value
        val numTargets = minOf(5 + level * 2, 15) // Level 1: 7, Level 2: 9, Level 3: 11, etc.
        val targets = mutableListOf<Target>()
        
        // Generate random targets
        repeat(numTargets) {
            val x = (-5f + (it * 2f)) % 10f - 5f // Spread targets across the platform
            val z = (-5f + (it * 1.5f)) % 10f - 5f
            targets.add(Target(x, z))
        }
        
        // Add some random targets for variety
        repeat(3) {
            val x = Random.nextFloat() * 10f - 5f
            val z = Random.nextFloat() * 10f - 5f
            targets.add(Target(x, z))
        }
        
        _gameState.value = currentState.copy(
            targets = targets,
            level = level,
            targetsCollected = 0,
            totalTargetsInLevel = targets.size,
            message = "Level $level - ${targets.size} hedefi topla!"
        )
    }
    
    private fun nextLevel() {
        val currentState = _gameState.value
        val nextLevel = currentState.level + 1
        generateLevel(nextLevel)
    }
    
    // Joystick control methods
    fun setJoystickInput(x: Float, z: Float) {
        moveX = x.coerceIn(-1f, 1f)
        moveZ = z.coerceIn(-1f, 1f)
    }
    
    fun update() {
        if (_gameState.value.isPaused) return
        
        val currentTime = System.currentTimeMillis()
        val deltaTime = (currentTime - lastUpdateTime) / 1000f
        
        if (deltaTime >= 0.016f) { // ~60 FPS
            updatePhysics(deltaTime)
            checkTargetCollisions()
            lastUpdateTime = currentTime
        }
    }
    
    private fun updatePhysics(deltaTime: Float) {
        val currentState = _gameState.value
        val ball = currentState.ball
        
        // Joystick movement
        val newVelocityX = moveX * 3f
        val newVelocityZ = moveZ * 3f
        
        // Apply gravity
        val newVelocityY = if (ball.isOnGround) {
            0f
        } else {
            ball.velocityY + gravity * deltaTime
        }
        
        // Update position
        val newX = ball.x + newVelocityX * deltaTime
        val newY = ball.y + newVelocityY * deltaTime
        val newZ = ball.z + newVelocityZ * deltaTime
        
        // Boundary collision
        val finalX = max(-boundaryX, min(boundaryX, newX))
        val finalZ = max(-boundaryZ, min(boundaryZ, newZ))
        
        // Ground collision
        val finalY = max(groundLevel, newY)
        val isOnGround = finalY <= groundLevel
        
        // Simple bounce
        val finalVelocityY = if (isOnGround && newVelocityY < 0) {
            -newVelocityY * 0.5f
        } else {
            newVelocityY
        }
        
        // Update ball state
        val newBall = ball.copy(
            x = finalX,
            y = finalY,
            z = finalZ,
            velocityX = newVelocityX,
            velocityY = finalVelocityY,
            velocityZ = newVelocityZ,
            isOnGround = isOnGround
        )
        
        _gameState.value = currentState.copy(ball = newBall)
    }
    
    private fun checkTargetCollisions() {
        val currentState = _gameState.value
        val ball = currentState.ball
        val targets = currentState.targets.toMutableList()
        var newScore = currentState.score
        var message = currentState.message
        var targetsCollected = currentState.targetsCollected
        var collisionDetected = false
        
        for (i in targets.indices) {
            val target = targets[i]
            if (!target.collected) {
                // Calculate distance in 2D (X and Z only, ignore Y)
                val distanceX = ball.x - target.x
                val distanceZ = ball.z - target.z
                val distance = kotlin.math.sqrt(distanceX * distanceX + distanceZ * distanceZ)
                
                // Much larger collision radius for easier collection
                if (distance < 3.0f) {
                    targets[i] = target.copy(collected = true)
                    newScore += 10
                    targetsCollected++
                    collisionDetected = true
                    message = "Hedef toplandı! +10 puan (${targetsCollected}/${currentState.totalTargetsInLevel})"
                }
            }
        }
        
        // Only update message if no collision happened
        if (!collisionDetected && message.contains("Hedef toplandı")) {
            message = "Level ${currentState.level} - ${targetsCollected}/${currentState.totalTargetsInLevel} hedef toplandı"
        }
        
        // Check if all targets collected
        val allCollected = targets.all { it.collected }
        if (allCollected) {
            message = "Tebrikler! Level ${currentState.level} tamamlandı!"
            // Auto-advance to next level after 2 seconds
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                kotlinx.coroutines.delay(2000)
                nextLevel()
            }
        }
        
        _gameState.value = currentState.copy(
            targets = targets,
            score = newScore,
            message = message,
            targetsCollected = targetsCollected
        )
    }
} 