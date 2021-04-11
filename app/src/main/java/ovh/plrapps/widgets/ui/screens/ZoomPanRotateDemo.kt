package ovh.plrapps.widgets.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
            .offset(x = 50.dp, y = 50.dp)
            .size(350.dp, 500.dp),
//            .background(Color.Gray),
        scaleRatioListener = state,
        rotationDeltaListener = state,
        panDeltaListener = state,
    ) {
        Canvas(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Gray)
                .graphicsLayer {
                    scaleX = state.scale
                    scaleY = state.scale
                    rotationZ = state.rotation
                    translationX = state.scrollX
                    translationY = state.scrollY
                }
        ) {
            drawRect(Color.Blue, topLeft = Offset.Zero, size = Size(256f ,256f))
            drawRect(Color.Cyan, topLeft = Offset(256f, 0f), size = Size(256f ,256f))
            drawRect(Color.DarkGray, topLeft = Offset(0f, 256f), size = Size(256f ,256f))
            drawRect(Color.Red, topLeft = Offset(256f, 256f), size = Size(256f ,256f))
        }
    }
}
