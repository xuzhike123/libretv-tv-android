package io.github.xuzhike123.libretvtv.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.lifecycle.lifecycleScope
import io.github.xuzhike123.libretvtv.data.LocalStore
import io.github.xuzhike123.libretvtv.data.SourceRepository
import io.github.xuzhike123.libretvtv.ui.LibreTvApp
import io.github.xuzhike123.libretvtv.ui.theme.LibreTvTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SourceRepository.init(this)
        LocalStore.init(this)
        lifecycleScope.launch {
            SourceRepository.seedIfEmpty()
        }
        setContent {
            LibreTvTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LibreTvApp()
                }
            }
        }
    }
}
