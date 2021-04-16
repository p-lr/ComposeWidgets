package ovh.plrapps.widgets.ui.widgets

import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.TransformableState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ovh.plrapps.widgets.gestures.detectGestures
import ovh.plrapps.widgets.utils.AngleDegree
import ovh.plrapps.widgets.utils.lerp
import ovh.plrapps.widgets.utils.toRad
import kotlin.math.*

@Composable
internal fun ZoomPanRotate(
    modifier: Modifier = Modifier,
    gestureListener: GestureListener,
    layoutSizeChangeListener: LayoutSizeChangeListener,
    paddingX: Int,
    paddingY: Int,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()

    Layout(
        content = content,
        modifier
            .pointerInput(Unit) {
                detectGestures(
                    onGesture = { centroid, pan, gestureZoom, gestureRotate ->
                        gestureListener.onRotationDelta(gestureRotate)
                        gestureListener.onScaleRatio(gestureZoom, centroid)
                        gestureListener.onScrollDelta(pan)
//                        val rotRad = state.rotation * PI.toFloat() / 180f
//                        state.offsetX += (pan.x * cos(rotRad) - pan.y * sin(rotRad)) * state.scale
//                        state.offsetY += (pan.y * cos(rotRad) + pan.x * sin(rotRad)) * state.scale
                    },
                    onTouchDown = gestureListener::onTap,
                    onFling = { velocity -> gestureListener.onFling(velocity) }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { offset -> gestureListener.onDoubleTap(offset) }
                )
            }
            .onSizeChanged {
                layoutSizeChangeListener.onSizeChanged(scope, it)
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
                placeable.place(x = paddingX, y = paddingY)
            }
        }
    }
}


class MapViewState(
    private val fullWidth: Int,
    private val fullHeight: Int
) : GestureListener, LayoutSizeChangeListener {
    private var scope: CoroutineScope? = null

    private val minimumScaleMode: MinimumScaleMode = Fit

    /* A handy tool to animate scale, rotation, and scroll */
    private val transformableState = TransformableState { zoomChange, panChange, rotationChange ->
        scale *= zoomChange
        rotation += rotationChange
        scrollX += panChange.x
        scrollY += panChange.y
    }

    internal var scale by mutableStateOf(1f)
    internal var rotation: AngleDegree by mutableStateOf(0f)
    internal var scrollX by mutableStateOf(0f)
    internal var scrollY by mutableStateOf(0f)

    private var layoutSize by mutableStateOf(IntSize(0, 0))
    private var minScale: Float by mutableStateOf(0f)

    /**
     * When scaled out beyond the scaled permitted by [Fill], these paddings are used by the layout.
     */
    internal var paddingX: Int by mutableStateOf(0)
    internal var paddingY: Int by mutableStateOf(0)

    /* Used for fling animation */
    private val scrollAnimatable: Animatable<Offset, AnimationVector2D> =
        Animatable(Offset.Zero, Offset.VectorConverter)
    private var isFlinging = false

    fun setScale(scale: Float) {
        this.scale = constrainScale(scale)
        updatePadding()
    }

    fun setScroll(scrollX: Float, scrollY: Float) {
        this.scrollX = constrainScrollX(scrollX)
        this.scrollY = constrainScrollY(scrollY)
    }

    override fun onScaleRatio(scaleRatio: Float, centroid: Offset) {
        val formerScale = scale
        setScale(scale * scaleRatio)

        /* Pinch and zoom magic */
        val effectiveScaleRatio = scale / formerScale
        setScroll(
            scrollX = getScrollAtOffsetAndScale(scrollX, centroid.x, effectiveScaleRatio),
            scrollY = getScrollAtOffsetAndScale(scrollY, centroid.y, effectiveScaleRatio)
        )
//        scrollX = (scrollX + centroid.x) * effectiveScaleRatio - centroid.x
//        scrollY = (scrollY + centroid.y) * effectiveScaleRatio - centroid.y

//        val rotRad = -rotation.toRad()
//        scrollX += (centroid.x * cos(rotRad) - centroid.y * sin(rotRad)) * (1- scaleRatio )
//        scrollY += (centroid.x * sin(rotRad) + centroid.y * cos(rotRad)) * (1 - scaleRatio )
    }

    private fun getScrollAtOffsetAndScale(scroll: Float, offSet: Float, scaleRatio: Float): Float {
        return (scroll + offSet) * scaleRatio - offSet
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
        setScroll(scrollX, scrollY)
    }

    override fun onFling(velocity: Velocity) {
        isFlinging = true

        scope?.launch {
            scrollAnimatable.snapTo(Offset(scrollX, scrollY))
            scrollAnimatable.animateDecay(
                initialVelocity = -Offset(velocity.x, velocity.y),
                animationSpec = FloatExponentialDecaySpec().generateDecayAnimationSpec(),
            ) {
                if (isFlinging) {
                    setScroll(
                        scrollX = value.x,
                        scrollY = value.y
                    )
                }
            }
        }
    }

    override fun onTap() {
        isFlinging = false
    }

    override fun onDoubleTap(offSet: Offset) {
        val startScale = scale
        val startScrollX = scrollX
        val startScrollY = scrollY

        val destScale = constrainScale(
            2.0.pow(floor(ln((scale * 2).toDouble()) / ln(2.0))).toFloat()
        )
        val destScrollX = getScrollAtOffsetAndScale(startScrollX, offSet.x, destScale / startScale)
        val destScrollY = getScrollAtOffsetAndScale(startScrollY, offSet.y, destScale / startScale)

        scope?.launch {
            Animatable(0f).animateTo(1f) {
                setScale(lerp(startScale, destScale, value))
                setScroll(
                    scrollX = lerp(startScrollX, destScrollX, value),
                    scrollY = lerp(startScrollY, destScrollY, value)
                )
            }
        }
    }

    override fun onSizeChanged(composableScope: CoroutineScope, size: IntSize) {
        println("layout changed: $size")
        scope = composableScope
        layoutSize = size
        recalculateMinScale()
        setScale(scale)
    }

//    fun smoothScaleTo(scale: Float) = scope.launch {
//        val currScale = this@MapViewState.scale
//        if (currScale > 0) {
//            transformableState.animateZoomBy(scale / currScale)
//        }
//    }

//    /**
//     * TODO: Should we take pixel coordinates, or relative coordinates?
//     */
//    fun smoothScrollTo(offset: Offset) = scope.launch {
//        transformableState.animatePanBy(offset)
//    }

    private fun constrainScrollX(scrollX: Float): Float {
        return scrollX.coerceIn(0f, max(0f, fullWidth * scale - layoutSize.width))
    }

    private fun constrainScrollY(scrollY: Float): Float {
        return scrollY.coerceIn(0f, max(0f, fullHeight * scale - layoutSize.height))
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

    private fun constrainScale(scale: Float): Float {
        return scale.coerceIn(minScale, 2f)  // scale between 0+ and 2f
    }

    private fun updatePadding() {
        paddingX = if (fullWidth * scale >= layoutSize.width) {
            0
        } else {
            layoutSize.width / 2 - (fullWidth * scale).roundToInt() / 2
        }

        paddingY = if (fullHeight * scale >= layoutSize.height) {
            0
        } else {
            layoutSize.height / 2 - (fullHeight * scale).roundToInt() / 2
        }
    }
}

internal interface GestureListener {
    fun onScaleRatio(scaleRatio: Float, centroid: Offset)
    fun onRotationDelta(rotationDelta: Float)
    fun onScrollDelta(scrollDelta: Offset)
    fun onFling(velocity: Velocity)
    fun onTap()
    fun onDoubleTap(offSet: Offset)
}

internal interface LayoutSizeChangeListener {
    fun onSizeChanged(composableScope: CoroutineScope, size: IntSize)
}

class MapViewViewModel() : ViewModel() {
    val state: MapViewState by mutableStateOf(
        MapViewState(25600, 12800)
    )
}

sealed class MinimumScaleMode
object Fit : MinimumScaleMode()
object Fill : MinimumScaleMode()
data class Forced(val scale: Float) : MinimumScaleMode()





