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
import ovh.plrapps.widgets.utils.toRad
import kotlin.math.cos
import kotlin.math.sin

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
                    onGesture = { centroid, pan, gestureZoom, gestureRotate ->
                        rotationDeltaListener.onRotationDelta(gestureRotate)
                        scaleRatioListener.onScaleRatio(gestureZoom, centroid)
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

    private val fullWidth = 1500
    private val fullHeight = 1000

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

    override fun onScaleRatio(scaleRatio: Float, centroid: Offset) {
        val formerScale = scale
        constrainScale(scale * scaleRatio)

        val effectiveScaleRatio = scale / formerScale
        scrollX = (scrollX + centroid.x) * effectiveScaleRatio - centroid.x
        scrollY = (scrollY + centroid.y) * effectiveScaleRatio - centroid.y

//        val rotRad = -rotation.toRad()
//        scrollX += (centroid.x * cos(rotRad) - centroid.y * sin(rotRad)) * (1- scaleRatio )
//        scrollY += (centroid.x * sin(rotRad) + centroid.y * cos(rotRad)) * (1 - scaleRatio )
    }

    override fun onRotationDelta(rotationDelta: Float) {
//        this.rotation += rotationDelta
//        println("rotation : $rotation")
    }

    override fun onScrollDelta(scrollDelta: Offset) {
        val rotRad = -rotation.toRad()
//        println("scroll delta : $scrollDelta")
        var scrollX = scrollX
        var scrollY = scrollY
        scrollX -= scrollDelta.x * cos(rotRad) - scrollDelta.y * sin(rotRad)
        scrollY -= scrollDelta.x * sin(rotRad) + scrollDelta.y * cos(rotRad)
        constrainScroll(scrollX, scrollY)
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

    private fun constrainScroll(scrollX: Float, scrollY: Float) {
        this@MapViewState.scrollX = scrollX.coerceIn(0f, fullWidth * scale)
        this@MapViewState.scrollY = scrollY.coerceIn(0f, fullHeight * scale)
    }

    private fun constrainScale(scale: Float) {
        this.scale = scale.coerceIn(Float.MIN_VALUE, 2f)  // scale between 0+ and 2f
    }
}

internal interface ScaleRatioListener {
    fun onScaleRatio(scaleRatio: Float, centroid: Offset)
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


