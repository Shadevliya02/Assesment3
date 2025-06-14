package com.sharla0139.assesment3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.sharla0139.assesment3.ui.screen.AboutScreen
import com.sharla0139.assesment3.ui.screen.AppScreen
import com.sharla0139.assesment3.ui.screen.MainScreen
import com.sharla0139.assesment3.ui.theme.Assesment3Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Assesment3Theme {
                MyApp()
            }
        }
    }
}

@Composable
fun MyApp() {
    var currentScreen by remember { mutableStateOf(AppScreen.MAIN) }

    when (currentScreen) {
        AppScreen.MAIN -> MainScreen(
            onAboutClick = { currentScreen = AppScreen.ABOUT }
        )
        AppScreen.ABOUT -> AboutScreen(
            onBackClick = { currentScreen = AppScreen.MAIN }
        )
    }
}