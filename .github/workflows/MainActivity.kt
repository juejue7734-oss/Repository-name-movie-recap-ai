package com.movierecap.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.movierecap.ai.data.AppViewModel
import com.movierecap.ai.ui.MovieRecapApp
import com.movierecap.ai.ui.MovieRecapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val model: AppViewModel = viewModel()
            val settings by model.settings.collectAsStateWithLifecycle()
            MovieRecapTheme(settings.darkTheme) { MovieRecapApp(model) }
        }
    }
}
