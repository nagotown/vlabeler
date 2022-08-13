package com.sdercolin.vlabeler.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.util.runIfNotNull

@Composable
fun SearchBar(
    text: String,
    onTextChange: (String) -> Unit,
    focusRequester: FocusRequester? = null,
    onPreviewKeyEvent: ((KeyEvent) -> Boolean)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().height(50.dp).padding(horizontal = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, null, tint = MaterialTheme.colors.onSurface)
        Spacer(Modifier.width(15.dp))
        BasicTextField(
            value = text,
            modifier = Modifier.fillMaxWidth()
                .padding(vertical = 10.dp)
                .runIfNotNull(focusRequester) { focusRequester(it) }
                .runIfNotNull(onPreviewKeyEvent) { onPreviewKeyEvent(it) },
            onValueChange = onTextChange,
            textStyle = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.onBackground),
            cursorBrush = SolidColor(MaterialTheme.colors.onBackground),
            singleLine = true
        )
    }
}
