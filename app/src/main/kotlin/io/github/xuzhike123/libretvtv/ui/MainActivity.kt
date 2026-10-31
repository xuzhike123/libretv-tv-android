package io.github.xuzhike123.libretvtv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import io.github.xuzhike123.libretvtv.ui.LibreTvApp
import io.github.xuzhike123.libretvtv.ui.theme.LibreTvTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LibreTvTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LibreTvApp()
                }
            }
        }
    }
}
