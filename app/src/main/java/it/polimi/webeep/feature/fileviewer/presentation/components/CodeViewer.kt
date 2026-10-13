package it.polimi.webeep.feature.fileviewer.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import it.polimi.webeep.core.ui.theme.WeBeepSpacing

// No type-scale token is monospace at this size, so the code text sizes live here.
private val CodeFontSize = 13.sp
private val CodeLineHeight = 20.sp

@Composable
fun CodeViewer(content: String, modifier: Modifier = Modifier, contentPadding: PaddingValues = PaddingValues(), onTap: () -> Unit = {}) {
    val lazyListState = rememberLazyListState()
    val lines = remember(content) { content.lines() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .pointerInput(Unit) { detectTapGestures(onTap = { onTap() }) }
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            // Constant padding keeps the text clear of the overlaid top bar.
            contentPadding = PaddingValues(
                start = WeBeepSpacing.md,
                end = WeBeepSpacing.md,
                top = contentPadding.calculateTopPadding() + WeBeepSpacing.md,
                bottom = contentPadding.calculateBottomPadding() + WeBeepSpacing.md
            )
        ) {
            items(
                count = lines.size,
                key = { index -> index },
                contentType = { "code_line" }
            ) { index ->
                Text(
                    text = lines[index],
                    fontFamily = FontFamily.Monospace,
                    fontSize = CodeFontSize,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = CodeLineHeight
                )
            }
        }
    }
}
