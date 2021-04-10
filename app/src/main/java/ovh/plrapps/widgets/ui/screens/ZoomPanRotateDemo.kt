package ovh.plrapps.widgets.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
            .size(500.dp, 500.dp)
            .background(Color.Gray),
        scaleRatioListener = state,
        rotationDeltaListener = state,
        offsetDeltaListener = state
    ) {
        Canvas(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
                .graphicsLayer {
                    scaleX = state.scale
                    scaleY = state.scale
                    rotationZ = state.rotation
                    translationX = state.offset.x
                    translationY = state.offset.y
                }
        ) {
            drawRect(Color.Blue)
        }
    }
}
