package ovh.plrapps.widgets.ui.widgets

import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ovh.plrapps.widgets.gestures.detectGestures
import ovh.plrapps.widgets.utils.AngleDegree
import ovh.plrapps.widgets.utils.toRad
import kotlin.math.*

@Composable
internal fun ZoomPanRotate(
    modifier: Modifier = Modifier,
    scaleRatioListener: ScaleRatioListener,
    rotationDeltaListener: RotationDeltaListener,
    panDeltaListener: PanDeltaListener,
    flingListener: FlingListener,
    tapListener: TapListener,
    layoutSizeChangeListener: LayoutSizeChangeListener,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()

    Layout(
        content = content,
        modifier
            .pointerInput(Unit) {
                detectGestures(
                    onGesture = { centroid, pan, gestureZoom, gestureRotate ->
                        rotationDeltaListener.onRotationDelta(gestureRotate)
                        scaleRatioListener.onScaleRatio(gestureZoom, centroid)
                        panDeltaListener.onScrollDelta(pan)
//                        val rotRad = state.rotation * PI.toFloat() / 180f
//                        state.offsetX += (pan.x * cos(rotRad) - pan.y * sin(rotRad)) * state.scale
//                        state.offsetY += (pan.y * cos(rotRad) + pan.x * sin(rotRad)) * state.scale
                    },
                    onTouchDown = tapListener::onTap,
                    onFling = { velocity -> flingListener.onFling(scope, velocity) }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = tapListener::onDoubleTap
                )
            }
            .onSizeChanged {
                layoutSizeChangeListener.onSizeChanged(it)
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

class MapViewState : ScaleRatioListener, RotationDeltaListener, PanDeltaListener, FlingListener,
    TapListener, LayoutSizeChangeListener {
    private val scope = CoroutineScope(Dispatchers.Main)

    private val fullWidth = 25600
    private val fullHeight = 12800
    private val minimumScaleMode: MinimumScaleMode = Fit

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

    private var layoutSize by mutableStateOf(IntSize(0, 0))
    private var minScale: Float by mutableStateOf(0f)

    var scrollAnimatable: Animatable<Offset, AnimationVector2D>? = null
    var isFlinging = false

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
        println("scroll")
        val rotRad = -rotation.toRad()
//        println("scroll delta : $scrollDelta")
        var scrollX = scrollX
        var scrollY = scrollY
        scrollX -= scrollDelta.x * cos(rotRad) - scrollDelta.y * sin(rotRad)
        scrollY -= scrollDelta.x * sin(rotRad) + scrollDelta.y * cos(rotRad)
        constrainScroll(scrollX, scrollY)
    }

    override fun onFling(coroutineScope: CoroutineScope, velocity: Velocity) {
        scrollAnimatable = Animatable(Offset(scrollX, scrollY), Offset.VectorConverter)
        isFlinging = true

        coroutineScope.launch {
            scrollAnimatable?.animateDecay(
                initialVelocity = -Offset(velocity.x, velocity.y),
                animationSpec = FloatExponentialDecaySpec().generateDecayAnimationSpec(),
            ) {
                if (isFlinging) {
                    constrainScroll(value.x, value.y)
                }
            }
        }
    }

    override fun onTap() {
        isFlinging = false
    }

    override fun onDoubleTap(offSet: Offset) {
        println("double-tap")
    }

    override fun onSizeChanged(size: IntSize) {
        println("layout changed: $size")
        layoutSize = size
        recalculateMinScale()
        constrainScale(scale)
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
        constrainScrollX(scrollX)
        constrainScrollY(scrollY)
    }

    private fun constrainScrollX(scrollX: Float) {
        this.scrollX =
            scrollX.coerceIn(0f, max(0f, fullWidth * scale - layoutSize.width))
    }

    private fun constrainScrollY(scrollY: Float) {
        this.scrollY =
            scrollY.coerceIn(0f, max(0f, fullHeight * scale - layoutSize.height))
    }

    private fun recalculateMinScale() {
        val minScaleX = layoutSize.width.toFloat() / fullWidth
        val minScaleY = layoutSize.height.toFloat() / fullHeight
        minScale = when (minimumScaleMode) {
            Fit -> min(minScaleX, minScaleY)
            Fill -> max(minScaleX, minScaleY)
            is Forced -> minimumScaleMode.scale
        }
    }

    private fun constrainScale(scale: Float) {
        this.scale = scale.coerceIn(minScale, 2f)  // scale between 0+ and 2f
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

internal interface FlingListener {
    fun onFling(composableScope: CoroutineScope, velocity: Velocity)
}

internal interface TapListener {
    fun onTap()
    fun onDoubleTap(offSet: Offset)
}

internal interface LayoutSizeChangeListener {
    fun onSizeChanged(size: IntSize)
}

class MapViewViewModel() : ViewModel() {
    val state: MapViewState by mutableStateOf(MapViewState())
}

sealed class MinimumScaleMode
object Fit : MinimumScaleMode()
object Fill : MinimumScaleMode()
data class Forced(val scale: Float) : MinimumScaleMode()

private suspend fun Animatable<Float, AnimationVector1D>.fling(
    initialVelocity: Float,
    animationSpec: DecayAnimationSpec<Float>,
    adjustTarget: ((Float) -> Float)?,
    block: (Animatable<Float, AnimationVector1D>.() -> Unit)? = null,
): AnimationResult<Float, AnimationVector1D> {
    val targetValue = animationSpec.calculateTargetValue(value, initialVelocity)
    val adjustedTarget = adjustTarget?.invoke(targetValue)

    return if (adjustedTarget != null) {
        animateTo(
            targetValue = adjustedTarget,
            initialVelocity = initialVelocity,
            block = block
        )
    } else {
        animateDecay(
            initialVelocity = initialVelocity,
            animationSpec = animationSpec,
            block = block,
        )
    }
}




