package ovh.plrapps.widgets.ui.widgets

import androidx.compose.animation.core.*
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ovh.plrapps.widgets.gestures.detectGestures
import ovh.plrapps.widgets.utils.AngleDegree
import ovh.plrapps.widgets.utils.lerp
import ovh.plrapps.widgets.utils.modulo
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
    val fullWidth: Int,
    val fullHeight: Int
) : GestureListener, LayoutSizeChangeListener {
    private var scope: CoroutineScope? = null

    private val minimumScaleMode: MinimumScaleMode = Fit

    internal var scale by mutableStateOf(1f)
    internal var rotation: AngleDegree by mutableStateOf(0f)
    internal var scrollX by mutableStateOf(0f)
    internal var scrollY by mutableStateOf(0f)

    internal var centroidX: Double by mutableStateOf(0.0)
    internal var centroidY: Double by mutableStateOf(0.0)

    private var layoutSize by mutableStateOf(IntSize(0, 0))
    var minScale = 0f
        set(value) {
            field = value
            setScale(scale)
        }
    var maxScale = 2f
        set(value) {
            field = value
            setScale(scale)
        }

    var shouldLoopScale = false

    /**
     * When scaled out beyond the scaled permitted by [Fill], these paddings are used by the layout.
     */
    internal var paddingX: Int by mutableStateOf(0)
    internal var paddingY: Int by mutableStateOf(0)

    /* Used for fling animation */
    private val scrollAnimatable: Animatable<Offset, AnimationVector2D> =
        Animatable(Offset.Zero, Offset.VectorConverter)
    private var isFlinging = false

    @Suppress("unused")
    fun setScale(scale: Float) {
        this.scale = constrainScale(scale)
        updatePadding()
        updateCentroid()
    }

    @Suppress("unused")
    fun setScroll(scrollX: Float, scrollY: Float) {
        this.scrollX = constrainScrollX(scrollX)
        this.scrollY = constrainScrollY(scrollY)
        updateCentroid()
    }

    /**
     * Scales the layout with animated scale, without maintaining scroll position.
     *
     * @param scale The final scale value the layout should animate to.
     * @param animationSpec The [AnimationSpec] the animation should use.
     */
    @Suppress("unused")
    fun smoothScaleTo(
        scale: Float,
        animationSpec: AnimationSpec<Float> = SpringSpec(stiffness = Spring.StiffnessLow)
    ) {
        scope?.launch {
            val currScale = this@MapViewState.scale
            if (currScale > 0) {
                Animatable(0f).animateTo(1f, animationSpec) {
                    setScale(lerp(currScale, scale, value))
                }
            }
        }
    }

    /**
     * Animates the layout to the scale provided, and centers the viewport to the supplied scroll
     * position.
     *
     * @param scrollX Horizontal scroll of the destination point.
     * @param scrollY Vertical scroll of the destination point.
     * @param destScale The final scale value the layout should animate to.
     * @param animationSpec The [AnimationSpec] the animation should use.
     */
    @Suppress("unused")
    fun slideToAndCenterWithScale(
        scrollX: Float,
        scrollY: Float,
        destScale: Float,
        animationSpec: AnimationSpec<Float> = SpringSpec(stiffness = Spring.StiffnessLow)
    ) {
        val startScrollX = this.scrollX
        val startScrollY = this.scrollY
        val destScrollX = scrollX - layoutSize.width / 2
        val destScrollY = scrollY - layoutSize.height / 2

        val startScale = this.scale

        scope?.launch {
            Animatable(0f).animateTo(1f, animationSpec) {
                setScale(lerp(startScale, destScale, value))
                setScroll(
                    scrollX = lerp(startScrollX, destScrollX, value),
                    scrollY = lerp(startScrollY, destScrollY, value)
                )
            }
        }
    }

    /**
     * Animates the layout to the scale provided, while maintaining position determined by the
     * the provided focal point.
     *
     * @param focusX The horizontal focal point to maintain, relative to the layout.
     * @param focusY The vertical focal point to maintain, relative to the layout.
     * @param destScale The final scale value the layout should animate to.
     * @param animationSpec The [AnimationSpec] the animation should use.
     */
    fun smoothScaleWithFocalPoint(
        focusX: Float,
        focusY: Float,
        destScale: Float,
        animationSpec: AnimationSpec<Float> = SpringSpec(stiffness = Spring.StiffnessLow)
    ) {
        val startScale = scale
        val startScrollX = scrollX
        val startScrollY = scrollY
        val destScrollX = getScrollAtOffsetAndScale(startScrollX, focusX, destScale / startScale)
        val destScrollY = getScrollAtOffsetAndScale(startScrollY, focusY, destScale / startScale)

        scope?.launch {
            Animatable(0f).animateTo(1f, animationSpec) {
                setScale(lerp(startScale, destScale, value))
                setScroll(
                    scrollX = lerp(startScrollX, destScrollX, value),
                    scrollY = lerp(startScrollY, destScrollY, value)
                )
            }
        }
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
    }

    private fun getScrollAtOffsetAndScale(scroll: Float, offSet: Float, scaleRatio: Float): Float {
        return (scroll + offSet) * scaleRatio - offSet
    }

    override fun onRotationDelta(rotationDelta: Float) {
        rotation = (rotation + rotationDelta).modulo()
        updateCentroid()
//        println("rotation : $rotation")
    }

    override fun onScrollDelta(scrollDelta: Offset) {
        var scrollX = scrollX
        var scrollY = scrollY

        val rotRad = -rotation.toRad()
        scrollX -= if (rotRad == 0f) scrollDelta.x else {
            scrollDelta.x * cos(rotRad) - scrollDelta.y * sin(rotRad)
        }
        scrollY -= if (rotRad == 0f) scrollDelta.y else {
            scrollDelta.x * sin(rotRad) + scrollDelta.y * cos(rotRad)
        }
        setScroll(scrollX, scrollY)
    }

    override fun onFling(velocity: Velocity) {
        isFlinging = true

        val rotRad = -rotation.toRad()
        val velocityX = if (rotRad == 0f) velocity.x else {
            velocity.x * cos(rotRad) - velocity.y * sin(rotRad)
        }
        val velocityY = if (rotRad == 0f) velocity.y else {
            velocity.x * sin(rotRad) + velocity.y * cos(rotRad)
        }

        scope?.launch {
            scrollAnimatable.snapTo(Offset(scrollX, scrollY))
            scrollAnimatable.animateDecay(
                initialVelocity = -Offset(velocityX, velocityY),
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
        val destScale = constrainScale(
            2.0.pow(floor(ln((scale * 2).toDouble()) / ln(2.0))).toFloat()
        ).let {
            if (shouldLoopScale && it >= maxScale) minScale else it
        }

        val angleRad = -rotation.toRad()
        val offSetX = if (angleRad == 0f) offSet.x else {
            layoutSize.height / 2 * sin(angleRad) + layoutSize.width / 2 * (1 - cos(angleRad)) +
                    offSet.x * cos(angleRad) - offSet.y * sin(angleRad)
        }

        val offSetY = if (angleRad == 0f) offSet.y else {
            layoutSize.height / 2 * (1 - cos(angleRad)) - layoutSize.width / 2 * sin(angleRad) +
                    offSet.x * sin(angleRad) + offSet.y * cos(angleRad)
        }

        smoothScaleWithFocalPoint(
            offSetX,
            offSetY,
            destScale,
            SpringSpec(stiffness = Spring.StiffnessMedium)
        )
    }

    override fun onSizeChanged(composableScope: CoroutineScope, size: IntSize) {
        println("layout changed: $size")
        scope = composableScope
        layoutSize = size
        recalculateMinScale()
        setScale(scale)

        scope?.launch {
            delay(3000)
//            smoothScaleTo(0f)
            slideToAndCenterWithScale(12928f, 6528f, 1f)
        }
    }

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

    private fun constrainScale(scale: Float): Float {
        return scale.coerceIn(max(minScale, Float.MIN_VALUE), maxScale)  // scale between 0+ and 2f
    }

    private fun updateCentroid() {
        centroidX = (scrollX + min(
            layoutSize.width.toDouble() / 2,
            fullWidth * scale.toDouble() / 2
        )) / (fullWidth * scale)
        centroidY = (scrollY + min(
            layoutSize.height.toDouble() / 2,
            fullHeight * scale.toDouble() / 2
        )) / (fullHeight * scale)
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
        MapViewState(25856, 13056).also {
            it.shouldLoopScale = true
        }
    )
}

sealed class MinimumScaleMode
object Fit : MinimumScaleMode()
object Fill : MinimumScaleMode()
data class Forced(val scale: Float) : MinimumScaleMode()





