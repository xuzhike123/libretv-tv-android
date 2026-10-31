package io.github.xuzhike123.libretvtv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.xuzhike123.libretvtv.data.searchHistory
import io.github.xuzhike123.libretvtv.data.searchHotWords
import io.github.xuzhike123.libretvtv.ui.components.FocusableSurface
import io.github.xuzhike123.libretvtv.ui.components.SectionHeader

/**
 * 搜索页：遥控器屏上键盘 + 历史词 / 热词。
 * 真实聚合检索接口尚未接入，因此这里只维护输入串。
 */
@Composable
fun SearchScreen(modifier: Modifier = Modifier) {
    var query by remember { mutableStateOf("") }
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalArrangement = Arrangement.spacedBy(40.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (query.isEmpty()) "输入片名后按“搜索”" else query,
                fontSize = 34.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .height(96.dp)
                    .padding(bottom = 20.dp)
            )
            val letters = remember { ('A'..'Z').map { it.toString() } }
            val digits = remember { ('0'..'9').map { it.toString() } }
            val keys = remember { digits + letters }
            keys.chunked(9).forEach { rowKeys ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowKeys.forEach { key ->
                        KeyButton(
                            text = key,
                            onClick = { if (query.length < 24) query += key }
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KeyButton(text = "空格", width = 2, onClick = { query += " " })
                KeyButton(text = "删除", width = 2, onClick = { query = query.dropLast(1) })
                KeyButton(text = "清空", width = 2, onClick = { query = "" })
                KeyButton(text = "搜索", width = 2, onClick = { })
            }
        }
        Column(modifier = Modifier.width(420.dp)) {
            SectionHeader(title = "搜索历史")
            FlowRow(
                words = searchHistory(),
                onWordClick = { query = it }
            )
            SectionHeader(title = "热门推荐")
            FlowRow(
                words = searchHotWords(),
                onWordClick = { query = it }
            )
        }
    }
}

@Composable
private fun KeyButton(
    text: String,
    width: Int = 1,
    onClick: () -> Unit
) {
    val buttonWidth: Dp = 88.dp * width
    FocusableSurface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .height(70.dp)
            .width(buttonWidth)
    ) {
        Text(
            text = text,
            fontSize = 26.sp,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun FlowRow(
    words: List<String>,
    onWordClick: (String) -> Unit
) {
    words.chunked(2).forEach { rowWords ->
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            rowWords.forEach { word ->
                FocusableSurface(
                    onClick = { onWordClick(word) },
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        text = word,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}
