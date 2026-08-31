package com.wakeupalarm.android.ui.components

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
fun WheelPicker(
    items: List<Int>,
    initialValue: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    continuous: Boolean = false
) {
    val itemHeight = 48.dp
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }
    
    val totalItemCount = if (continuous) 10000 * items.size else items.size
    val initialIndex = if (continuous) {
        (5000 * items.size) + items.indexOf(initialValue).coerceAtLeast(0)
    } else {
        items.indexOf(initialValue).coerceAtLeast(0)
    }
    
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    
    val currentIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val center = layoutInfo.viewportEndOffset / 2
            layoutInfo.visibleItemsInfo.minByOrNull { 
                abs((it.offset + it.size / 2) - center) 
            }?.index ?: initialIndex
        }
    }

    val actualIndex = currentIndex % items.size
    
    LaunchedEffect(actualIndex) {
        onValueChange(items[actualIndex])
    }

    Box(
        modifier = modifier
            .height(itemHeight * 3)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = itemHeight),
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(totalItemCount) { index ->
                val item = items[index % items.size]
                
                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth()
                        .graphicsLayer {
                            val layoutInfo = listState.layoutInfo
                            val center = layoutInfo.viewportEndOffset / 2
                            val itemInfo = layoutInfo.visibleItemsInfo.find { it.index == index }
                            val itemCenter = (itemInfo?.offset ?: 0) + (itemHeightPx / 2)
                            val distanceFromCenter = abs(itemCenter - center)
                            val scale = 1f - (distanceFromCenter / center).coerceIn(0f, 0.5f)
                            scaleX = scale
                            scaleY = scale
                            alpha = scale
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.toString().padStart(2, '0'),
                        style = MaterialTheme.typography.displaySmall,
                        color = if (index == currentIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
