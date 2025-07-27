package com.yasingulsen.gamecoding

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yasingulsen.gamecoding.game.GameScreen
import com.yasingulsen.gamecoding.game.GameViewModel
import com.yasingulsen.gamecoding.ui.MainMenuScreen
import com.yasingulsen.gamecoding.ui.theme.GameCodingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameCodingTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GameApp()
                }
            }
        }
    }
}

@Composable
fun GameApp() {
    var currentScreen by remember { mutableStateOf<GameScreen>(GameScreen.MainMenu) }
    
    when (currentScreen) {
        GameScreen.MainMenu -> {
            MainMenuScreen(
                onStartGame = {
                    currentScreen = GameScreen.Game
                }
            )
        }
        GameScreen.Game -> {
            GameScreen(
                onBackPressed = {
                    currentScreen = GameScreen.MainMenu
                }
            )
        }
    }
}

enum class GameScreen {
    MainMenu,
    Game
}