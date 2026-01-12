package com.lea.stamp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.lea.stamp.data.FoodItem

@Composable
fun SegmentedImageView(
    imageUrl: String,
    foodItems: List<FoodItem>,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var imageSize by remember { mutableStateOf(Size.Zero) }
    val textMeasurer = rememberTextMeasurer()
    
    // Decode all masks once
    val decodedMasks = remember(foodItems) {
        foodItems.map { foodItem ->
            val base64Data = foodItem.mask.substringAfter(",", foodItem.mask)
            decodeBase64Mask(base64Data)
        }
    }
    
    Box(modifier = modifier) {
        // Background image
        AsyncImage(
            model = imageUrl,
            contentDescription = "Food Image",
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale,
            onSuccess = { state ->
                imageSize = state.painter.intrinsicSize
            }
        )
        
        // Overlay canvas for segmentation masks and bounding boxes
        if (foodItems.isNotEmpty() && imageSize != Size.Zero) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { canvasSize = it }
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                
                // Calculate the actual image rectangle inside the canvas based on ContentScale
                // We initially support Fit (default) and FillWidth (for detail view)
                // For a robust implementation, we can simulate the scale logic:
                
                val srcWidth = imageSize.width
                val srcHeight = imageSize.height
                
                // Compute scale factor
                val scale = when (contentScale) {
                    ContentScale.Fit -> minOf(canvasWidth / srcWidth, canvasHeight / srcHeight)
                    ContentScale.FillWidth -> canvasWidth / srcWidth
                    ContentScale.Crop -> maxOf(canvasWidth / srcWidth, canvasHeight / srcHeight)
                    else -> minOf(canvasWidth / srcWidth, canvasHeight / srcHeight) // Fallback to Fit
                }
                
                val scaledWidth = srcWidth * scale
                val scaledHeight = srcHeight * scale
                
                // Compute offsets to center the image content
                val offsetX = (canvasWidth - scaledWidth) / 2
                val offsetY = (canvasHeight - scaledHeight) / 2
                
                foodItems.forEachIndexed { index, foodItem ->
                    // Denormalize bounding box coordinates (0-1000 scale) to the scaled image
                    val box = foodItem.box_2d
                    // box is [y0, x0, y1, x1]
                    val y0Norm = box[0]
                    val x0Norm = box[1]
                    val y1Norm = box[2]
                    val x1Norm = box[3]
                    
                    val pixelY0 = offsetY + (y0Norm * scaledHeight) / 1000f
                    val pixelX0 = offsetX + (x0Norm * scaledWidth) / 1000f
                    val pixelY1 = offsetY + (y1Norm * scaledHeight) / 1000f
                    val pixelX1 = offsetX + (x1Norm * scaledWidth) / 1000f
                    
                    val boxWidth = pixelX1 - pixelX0
                    val boxHeight = pixelY1 - pixelY0
                    
                    val color = getColorForIndex(index)
                    
                    // Draw mask if available
                    val maskBitmap = decodedMasks.getOrNull(index)
                    if (maskBitmap != null) {
                        drawImage(
                            image = maskBitmap,
                            dstOffset = IntOffset(pixelX0.toInt(), pixelY0.toInt()),
                            dstSize = IntSize(boxWidth.toInt(), boxHeight.toInt()),
                            colorFilter = ColorFilter.tint(
                                color = color.copy(alpha = 0.4f),
                                blendMode = BlendMode.SrcIn
                            )
                        )
                    } else {
                        // Fallback: draw semi-transparent rectangle
                        drawRect(
                            color = color.copy(alpha = 0.3f),
                            topLeft = Offset(pixelX0, pixelY0),
                            size = Size(boxWidth, boxHeight)
                        )
                    }
                    
                    // Draw bounding box outline
                    drawRect(
                        color = color,
                        topLeft = Offset(pixelX0, pixelY0),
                        size = Size(boxWidth, boxHeight),
                        style = Stroke(width = 4f)
                    )
                    
                    // Draw label background and text
                    val labelHeight = 32f
                    val labelY = (pixelY0 - labelHeight).coerceAtLeast(0f)
                    
                    drawRect(
                        color = color,
                        topLeft = Offset(pixelX0, labelY),
                        size = Size(boxWidth.coerceAtLeast(120f), labelHeight)
                    )
                    
                    // Draw label text
                    val textLayoutResult = textMeasurer.measure(
                        text = foodItem.label,
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    )
                    
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(
                            pixelX0 + 8f,
                            labelY + (labelHeight - textLayoutResult.size.height) / 2
                        )
                    )
                }
            }
        }
    }
}

private fun getColorForIndex(index: Int): Color {
    val colors = listOf(
        Color(0xFF4CAF50), // Green
        Color(0xFF2196F3), // Blue
        Color(0xFFFF9800), // Orange
        Color(0xFFE91E63), // Pink
        Color(0xFF9C27B0), // Purple
        Color(0xFFFFEB3B), // Yellow
        Color(0xFF00BCD4), // Cyan
        Color(0xFFFF5722)  // Deep Orange
    )
    return colors[index % colors.size]
}
