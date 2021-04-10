package ovh.plrapps.widgets.ui.widgets

import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
internal fun ZoomPanRotate(
    modifier: Modifier = Modifier,
    scaleRatioListener: ScaleRatioListener,
    rotationDeltaListener: RotationDeltaListener,
    offsetDeltaListener: OffsetDeltaListener,
    content: @Composable () -> Unit
) {
    Box(
        modifier
            .pointerInput(Unit) {
                detectTransformGestures(
                    onGesture = { _, pan, gestureZoom, gestureRotate ->
                        rotationDeltaListener.onRotationDelta(gestureRotate)
                        scaleRatioListener.onScaleRatio(gestureZoom)
                        offsetDeltaListener.onOffsetDelta(pan)
//                        val rotRad = state.rotation * PI.toFloat() / 180f
//                        state.offsetX += (pan.x * cos(rotRad) - pan.y * sin(rotRad)) * state.scale
//                        state.offsetY += (pan.y * cos(rotRad) + pan.x * sin(rotRad)) * state.scale
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { println("double tap") })
            }
            .fillMaxSize()

    ) {
        content()
    }
}

class MapViewState : ScaleRatioListener, RotationDeltaListener, OffsetDeltaListener {
    private val scope = CoroutineScope(Dispatchers.Main)

    /* A handy tool to animate scale, rotation, and offset */
    private val transformableState = TransformableState { zoomChange, panChange, rotationChange ->
        scale *= zoomChange
        rotation += rotationChange
        offset += panChange
    }

    var scale by mutableStateOf(1f)
    var rotation by mutableStateOf(0f)
    var offset by mutableStateOf(Offset.Zero)

    override fun onScaleRatio(scaleRatio: Float) {
        this.scale *= scaleRatio
    }

    override fun onRotationDelta(rotationDelta: Float) {
        this.rotation += rotationDelta
    }

    override fun onOffsetDelta(offsetDelta: Offset) {
        this.offset += offsetDelta
    }

    fun smoothScaleTo(scale: Float) = scope.launch {
        val currScale = this@MapViewState.scale
        if (currScale > 0) {
            transformableState.animateZoomBy(scale / currScale)
        }
    }

    /**
     * TODO: Should we take pixel coordinates, or relative coordinates?
     */
    fun smoothPanTo(offset: Offset) = scope.launch {
        transformableState.animatePanBy(offset)
    }
}

internal interface ScaleRatioListener {
    fun onScaleRatio(scaleRatio: Float)
}

internal interface RotationDeltaListener {
    fun onRotationDelta(rotationDelta: Float)
}

internal interface OffsetDeltaListener {
    fun onOffsetDelta(offsetDelta: Offset)
}

class MapViewViewModel() : ViewModel() {
    val state: MapViewState by mutableStateOf(MapViewState())
}

