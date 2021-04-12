package ovh.plrapps.widgets.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ovh.plrapps.widgets.ui.widgets.MapViewViewModel
import ovh.plrapps.widgets.ui.widgets.ZoomPanRotate
import kotlin.math.roundToInt

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
    ) {
        Canvas(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Gray)
//                .graphicsLayer (
//                    rotationZ = state.rotation,
//                    translationX = -state.scrollX,
//                    translationY = -state.scrollY,
//                    scaleX = state.scale,
//                    scaleY = state.scale,
//
//                )
        ) {
            withTransform({
                rotate(state.rotation)
                translate(left = -state.scrollX, top = -state.scrollY)
                scale(scale = state.scale)

            }) {
                drawRect(Color.Blue, topLeft = Offset.Zero, size = Size(256f, 256f))
                drawRect(Color.Cyan, topLeft = Offset(256f, 0f), size = Size(256f, 256f))
                drawRect(Color.DarkGray, topLeft = Offset(0f, 256f), size = Size(256f, 256f))
                drawRect(Color.Red, topLeft = Offset(256f, 256f), size = Size(256f, 256f))
            }
        }
    }
}
