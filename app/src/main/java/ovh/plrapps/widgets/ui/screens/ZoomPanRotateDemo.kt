package ovh.plrapps.widgets.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ovh.plrapps.widgets.ui.widgets.ZoomPanRotateViewModel
import ovh.plrapps.widgets.ui.widgets.ZoomPanRotate

@Composable
fun ZoomPanRotateDemo(modifier: Modifier = Modifier) {
    val viewModel: ZoomPanRotateViewModel = viewModel()
    val state = viewModel.state

    ZoomPanRotate(
        modifier = modifier
            .size(350.dp, 500.dp)
            .background(Color.Black),
        gestureListener = state,
        layoutSizeChangeListener = state,
        paddingX = state.paddingX,
        paddingY = state.paddingY,
    ) {
        // This composable is a fake TileCanvas composable
        Canvas(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Gray)
        ) {
            withTransform({
                /* Geometric transformations seem to be applied in reversed order of declaration */
                translate(left = -state.scrollX, top = -state.scrollY)
                rotate(
                    degrees = state.rotation,
                    pivot = Offset(
                        x = state.centroidX.toFloat() * state.fullWidth * state.scale,
                        y = state.centroidY.toFloat() * state.fullHeight * state.scale
                    )
                )
                scale(scale = state.scale, Offset.Zero)
            }) {
                for (i in 0..100) {
                    for (j in 0..50) {
                        drawRect(
                            getColor(i, j),
                            topLeft = Offset(i * 256f, j * 256f),
                            size = Size(256f, 256f)
                        )
                    }
                }
            }
        }

        for (c in state.childComposables.values) {
            c()
        }
    }
}

private fun getColor(i: Int, j: Int): Color {
    // Corners in red
    if (i == 0 && (j == 0 || j == 50)) return Color.Red
    if (j == 0 && (i == 0 || i == 100)) return Color.Red

    // Center in red
    if (i == 50 && j == 25) return Color.Red

    return when ((i + j) % 4) {
        0 -> Color.Blue
        1 -> Color.Cyan
        2 -> Color.DarkGray
        3 -> Color.Yellow
        else -> Color.Black
    }
}
