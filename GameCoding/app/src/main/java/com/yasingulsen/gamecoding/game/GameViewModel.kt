package com.yasingulsen.gamecoding.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class GameViewModel(application: Application) : AndroidViewModel(application) {
    
    private var gameEngine: GameEngine? = null
    
    val gameState: StateFlow<GameState>
        get() = gameEngine?.gameState ?: GameEngine(getApplication()).gameState
    
    fun initializeGame() {
        if (gameEngine == null) {
            gameEngine = GameEngine(getApplication())
        }
    }
    
    fun startGame() {
        gameEngine?.start()
        startGameLoop()
    }
    
    fun resetGame() {
        gameEngine?.reset()
    }
    
    fun stopGame() {
        gameEngine?.stop()
    }
    
    fun pauseGame() {
        gameEngine?.pause()
    }
    
    fun resumeGame() {
        gameEngine?.resume()
        startGameLoop()
    }
    
    fun setJoystickInput(x: Float, z: Float) {
        gameEngine?.setJoystickInput(x, z)
    }
    
    private fun startGameLoop() {
        viewModelScope.launch {
            while (true) {
                if (gameEngine != null) {
                    gameEngine?.update()
                }
                delay(16) // ~60 FPS
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        gameEngine?.stop()
        gameEngine = null
    }
} 