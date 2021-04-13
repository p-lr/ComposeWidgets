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
import ovh.plrapps.widgets.ui.widgets.MapViewViewModel
import ovh.plrapps.widgets.ui.widgets.ZoomPanRotate

@Composable
fun ZoomPanRotateDemo(modifier: Modifier = Modifier) {
    val viewModel: MapViewViewModel = viewModel()
    val state = viewModel.state

    ZoomPanRotate(
        modifier = modifier
            .size(350.dp, 500.dp)
            .background(Color.Black),
        scaleRatioListener = state,
        rotationDeltaListener = state,
        panDeltaListener = state,
        layoutSizeChangeListener = state
    ) {
        Canvas(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Gray)
        ) {
            withTransform({
                rotate(state.rotation)
                translate(left = -state.scrollX, top = -state.scrollY)
                scale(scale = state.scale, Offset.Zero)
            }) {
                for (i in 0..9) {
                    for (j in 0..4) {
                        drawRect(
                            getColor(i, j),
                            topLeft = Offset(i * 256f, j * 256f),
                            size = Size(256f, 256f)
                        )
                    }
                }
            }
        }
    }
}

private fun getColor(i: Int, j: Int): Color {
    return when ((i + j) % 4) {
        0 -> Color.Blue
        1 -> Color.Cyan
        2 -> Color.DarkGray
        3 -> Color.Red
        else -> Color.Black
    }
}
