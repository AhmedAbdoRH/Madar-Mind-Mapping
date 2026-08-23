package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ThemeMode
import com.example.ui.dialogs.CreateMapDialog
import com.example.ui.screens.MindMapScreen
import com.example.ui.screens.UniverseListScreen
import com.example.ui.theme.AppTheme
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MindMapViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MindMapViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val systemInDark = isSystemInDarkTheme()
            val isDarkTheme = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> systemInDark
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                Surface(
                    color = AppTheme.colors.background,
                    modifier = Modifier.fillMaxSize()
                ) {
                    OrbitMindApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun OrbitMindApp(
    viewModel: MindMapViewModel
) {
    val activeMapId by viewModel.activeMapId.collectAsStateWithLifecycle()
    val allMaps by viewModel.allMaps.collectAsStateWithLifecycle()
    val isCreateMapOpen by viewModel.isCreateMapOpen.collectAsStateWithLifecycle()

    AnimatedContent(
        targetState = activeMapId,
        transitionSpec = {
            if (targetState != null) {
                // Navigating from List into MindMap (Entering map)
                (fadeIn(animationSpec = tween(350, easing = FastOutSlowInEasing)) +
                        scaleIn(initialScale = 0.90f, animationSpec = tween(350, easing = FastOutSlowInEasing)) +
                        slideInHorizontally(initialOffsetX = { it / 6 }, animationSpec = tween(350, easing = FastOutSlowInEasing)))
                    .togetherWith(
                        fadeOut(animationSpec = tween(250, easing = FastOutSlowInEasing)) +
                                scaleOut(targetScale = 1.06f, animationSpec = tween(250, easing = FastOutSlowInEasing))
                    )
            } else {
                // Navigating back from MindMap to List
                (fadeIn(animationSpec = tween(350, easing = FastOutSlowInEasing)) +
                        scaleIn(initialScale = 1.06f, animationSpec = tween(350, easing = FastOutSlowInEasing)) +
                        slideInHorizontally(initialOffsetX = { -it / 6 }, animationSpec = tween(350, easing = FastOutSlowInEasing)))
                    .togetherWith(
                        fadeOut(animationSpec = tween(250, easing = FastOutSlowInEasing)) +
                                scaleOut(targetScale = 0.92f, animationSpec = tween(250, easing = FastOutSlowInEasing))
                    )
            }
        },
        label = "screen_transition"
    ) { mapId ->
        if (mapId == null) {
            UniverseListScreen(
                maps = allMaps,
                viewModel = viewModel,
                onSelectMap = { selectedId -> viewModel.selectMap(selectedId) },
                onCreateNewMap = { viewModel.openCreateMapDialog() },
                onDuplicateMap = { selectedId -> viewModel.duplicateMap(selectedId) },
                onDeleteMap = { selectedId -> viewModel.deleteMap(selectedId) }
            )
        } else {
            MindMapScreen(
                viewModel = viewModel
            )
        }
    }

    if (isCreateMapOpen) {
        CreateMapDialog(
            onDismiss = { viewModel.closeCreateMapDialog() },
            onConfirm = { title, description, themeColorHex, rootIcon ->
                viewModel.createMap(title, description, themeColorHex, rootIcon)
            }
        )
    }
}

