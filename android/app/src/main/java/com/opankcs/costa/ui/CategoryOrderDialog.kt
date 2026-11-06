package com.opankcs.costa.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.opankcs.costa.data.Category

@Composable
fun CategoryOrderDialog(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (List<Category>) -> Unit,
) {
    var items by remember { mutableStateOf(categories) }
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val rowHeight = with(LocalDensity.current) { 56.dp.toPx() }

    LaunchedEffect(categories) {
        if (draggingIndex == null) {
            items = categories
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Atur urutan kategori",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Tahan ikon ⠿ lalu geser untuk memindahkan posisi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(8.dp))

                LazyColumn(
                    state = rememberLazyListState(),
                    modifier = Modifier.heightIn(max = 420.dp),
                ) {
                    itemsIndexed(items, key = { _, category -> category.id }) { index, category ->
                        val isDragging = draggingIndex == index

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .zIndex(if (isDragging) 1f else 0f)
                                .graphicsLayer { translationY = if (isDragging) dragOffset else 0f }
                                .animateItem()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isDragging) MaterialTheme.colorScheme.surface
                                    else Color.Transparent,
                                )
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "⠿",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .pointerInput(category.id) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                draggingIndex = items.indexOfFirst { it.id == category.id }
                                                dragOffset = 0f
                                            },
                                            onDragEnd = {
                                                draggingIndex = null
                                                dragOffset = 0f
                                            },
                                            onDragCancel = {
                                                draggingIndex = null
                                                dragOffset = 0f
                                            },
                                            onDrag = { change, amount ->
                                                change.consume()
                                                dragOffset += amount.y

                                                val current = draggingIndex ?: return@detectDragGesturesAfterLongPress

                                                if (dragOffset > rowHeight / 2 && current < items.lastIndex) {
                                                    items = items.toMutableList().apply {
                                                        add(current + 1, removeAt(current))
                                                    }
                                                    draggingIndex = current + 1
                                                    dragOffset -= rowHeight
                                                } else if (dragOffset < -rowHeight / 2 && current > 0) {
                                                    items = items.toMutableList().apply {
                                                        add(current - 1, removeAt(current))
                                                    }
                                                    draggingIndex = current - 1
                                                    dragOffset += rowHeight
                                                }
                                            },
                                        )
                                    }
                                    .padding(8.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "${category.icon ?: ""} ${category.name}".trim(),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onSave(items) }) {
                        Text("Simpan urutan")
                    }
                }
            }
        }
    }
}
