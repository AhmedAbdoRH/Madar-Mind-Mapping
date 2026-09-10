package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ThemeMode
import com.example.ui.dialogs.CreateMapDialog
import com.example.ui.screens.MindMapScreen
import com.example.ui.screens.UniverseListScreen
import com.example.ui.theme.AppTheme
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.util.ShortcutHelper
import com.example.ui.viewmodel.MindMapViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MindMapViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ShortcutHelper.syncAllShortcuts(applicationContext)
        handleShortcutIntent(intent)
        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

            MyApplicationTheme(themeMode = themeMode) {
                Surface(
                    color = AppTheme.colors.background,
                    modifier = Modifier.fillMaxSize()
                ) {
                    OrbitMindApp(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShortcutIntent(intent)
    }

    private fun handleShortcutIntent(intent: Intent?) {
        if (intent != null && intent.hasExtra(ShortcutHelper.EXTRA_MAP_ID)) {
            val mapId = intent.getLongExtra(ShortcutHelper.EXTRA_MAP_ID, -1L)
            val nodeId = intent.getStringExtra(ShortcutHelper.EXTRA_NODE_ID)
            if (mapId != -1L) {
                viewModel.openNodeFromShortcut(mapId, nodeId)
            }
        }
    }
}

@Composable
fun OrbitMindApp(
    viewModel: MindMapViewModel = viewModel()
) {
    val activeMapId by viewModel.activeMapId.collectAsStateWithLifecycle()
    val allMaps by viewModel.allMaps.collectAsStateWithLifecycle()
    val isCreateMapOpen by viewModel.isCreateMapOpen.collectAsStateWithLifecycle()
    val feedbackMessage by viewModel.userFeedbackMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(feedbackMessage) {
        val msg = feedbackMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedbackMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )
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

