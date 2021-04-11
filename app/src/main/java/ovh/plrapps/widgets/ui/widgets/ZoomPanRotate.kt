package ovh.plrapps.widgets.ui.widgets

import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ovh.plrapps.widgets.utils.AngleDegree

@Composable
internal fun ZoomPanRotate(
    modifier: Modifier = Modifier,
    scaleRatioListener: ScaleRatioListener,
    rotationDeltaListener: RotationDeltaListener,
    panDeltaListener: PanDeltaListener,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier
            .pointerInput(Unit) {
                detectTransformGestures(
                    onGesture = { _, pan, gestureZoom, gestureRotate ->
                        rotationDeltaListener.onRotationDelta(gestureRotate)
                        scaleRatioListener.onScaleRatio(gestureZoom)
                        panDeltaListener.onScrollDelta(pan)
//                        val rotRad = state.rotation * PI.toFloat() / 180f
//                        state.offsetX += (pan.x * cos(rotRad) - pan.y * sin(rotRad)) * state.scale
//                        state.offsetY += (pan.y * cos(rotRad) + pan.x * sin(rotRad)) * state.scale
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { println("double tap") })
            }
            .fillMaxSize(),
    ) { measurables, constraints ->
        val placeables = measurables.map { measurable ->
            // Measure each children
            measurable.measure(constraints.copy(minHeight = 0))
        }

        // Set the size of the layout as big as it can
        layout(constraints.maxWidth, constraints.maxHeight) {
            // Place children in the parent layout
            placeables.forEach { placeable ->
                // Position item on the screen
                placeable.place(x = 0, y = 0)
            }
        }
    }
}

class MapViewState : ScaleRatioListener, RotationDeltaListener, PanDeltaListener {
    private val scope = CoroutineScope(Dispatchers.Main)

    /* A handy tool to animate scale, rotation, and scroll */
    private val transformableState = TransformableState { zoomChange, panChange, rotationChange ->
        scale *= zoomChange
        rotation += rotationChange
        scrollX += panChange.x
        scrollY += panChange.y
    }

    var scale by mutableStateOf(1f)
    var rotation: AngleDegree by mutableStateOf(0f)
    var scrollX by mutableStateOf(0f)
    var scrollY by mutableStateOf(0f)

    override fun onScaleRatio(scaleRatio: AngleDegree) {
        this.scale *= scaleRatio
    }

    override fun onRotationDelta(rotationDelta: Float) {
        this.rotation += rotationDelta
//        println("rotation : $rotation")
    }

    override fun onScrollDelta(scrollDelta: Offset) {
//        val rotRad = -rotation.toRad()
//        println("scroll delta : $scrollDelta")
//        scrollX += scrollDelta.x * cos(rotRad) - scrollDelta.y * sin(rotRad)
//        scrollY += scrollDelta.x * sin(rotRad) + scrollDelta.y * cos(rotRad)
        scrollX += scrollDelta.x
        scrollY += scrollDelta.y
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
    fun smoothScrollTo(offset: Offset) = scope.launch {
        transformableState.animatePanBy(offset)
    }
}

internal interface ScaleRatioListener {
    fun onScaleRatio(scaleRatio: Float)
}

internal interface RotationDeltaListener {
    fun onRotationDelta(rotationDelta: Float)
}

internal interface PanDeltaListener {
    fun onScrollDelta(scrollDelta: Offset)
}

class MapViewViewModel() : ViewModel() {
    val state: MapViewState by mutableStateOf(MapViewState())
}


