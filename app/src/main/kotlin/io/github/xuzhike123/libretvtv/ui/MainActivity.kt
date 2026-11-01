package io.github.xuzhike123.libretvtv.ui

import android.os.Bundle
import android.view.KeyEvent
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
import io.github.xuzhike123.libretvtv.ui.util.PlayerKeys
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

    /**
     * 遥控器按键先交给播放页处理。
     *
     * 这样即使播放页内部当前没有节点持有焦点（例如底部操作栏已隐藏），
     * 方向键、OK、菜单键、返回键也依然有效。
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (PlayerKeys.dispatch(event)) return true
        return super.dispatchKeyEvent(event)
    }
}
