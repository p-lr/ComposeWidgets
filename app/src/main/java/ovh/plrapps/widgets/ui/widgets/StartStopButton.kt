package ovh.plrapps.widgets.ui.widgets

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.Path
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ovh.plrapps.widgets.ui.theme.ComposeWidgetsTheme
import ovh.plrapps.widgets.utils.lerp
import javax.inject.Inject

/**
 * A button which has two states (started, and stopped). It animates when transitioning between
 * states. If a click happens in the middle of a transition, the state holder (typically a
 * view-model) decides whether the state changes or not.
 *
 * This button is useful when a component can be started or stopped, but click events are
 * debounced to avoid starting and stopping at a too high pace.
 *
 * This button carries two information:
 * * The state started/stopped,
 * * The state transition, during which the button cannot change of state.
 */
@Composable
fun StartStopButton(
    modifier: Modifier = Modifier,
    stopped: Boolean,
    pathMorphingDurationMs: Int = 500,
    disableTimeoutMs: Int = 2000,
    onClick: () -> Unit
) {
    val animatedProgress = animateFloatAsState(
        if (stopped) 0f else 1f,
        animationSpec = tween(pathMorphingDurationMs)
    )
    val backgroundColor by animateColorAsState(
        if (stopped) Color(0xFF4CAF50) else Color(0xFFF44336),
        animationSpec = tween(pathMorphingDurationMs)
    )
    val strokeColor = if (stopped) {
        Color(0xFF4CAF50)
    } else {
        Color(0xFFF44336)
    }

    val anim = remember {
        TargetBasedAnimation(
            animationSpec = tween(disableTimeoutMs),
            typeConverter = Float.VectorConverter,
            initialValue = 0f,
            targetValue = 1f
        )
    }
    var playTime by remember { mutableStateOf(0L) }
    var timeOutProgress by remember { mutableStateOf(0f) }

    /**
     * Everytime the button is composed, the timeout animation should stop and restart. This is
     * exactly the purpose of [LaunchedEffect].
     * There's an exception though. When the button is composed for the first time, we don't want to
     * see the timeout animation.
     */
    var firstTimeComposition by remember { mutableStateOf(true) }
    LaunchedEffect(stopped) {
        if (firstTimeComposition) {
            firstTimeComposition = false
            return@LaunchedEffect
        }
        launch {
            val startTime = withFrameNanos { it }

            do {
                playTime = withFrameNanos { it } - startTime
                timeOutProgress = anim.getValueFromNanos(playTime)
            } while (timeOutProgress < 1f)
        }
    }

    Surface(
        modifier = modifier
            .clip(CircleShape)
            .clickable { onClick() }
    ) {
        TimeoutShape(
            modifier = modifier,
            backgroundColor,
            strokeColor,
            t = timeOutProgress
        )
        StartStopShape(
            modifier = modifier,
            backgroundColor,
            t = animatedProgress.value
        )
    }
}

@Composable
fun TimeoutShape(
    modifier: Modifier,
    backgroundColor: Color,
    strokeColor: Color,
    t: Float
) {
    val angle = if (t != 0f && t != 1f) {
        t * 360f
    } else 0f

    Canvas(
        modifier = modifier
            .background(backgroundColor.copy(alpha = 0.2f))
            .fillMaxSize()
            .padding(3.dp),

        ) {
        drawArc(
            color = strokeColor,
            startAngle = -90f,
            sweepAngle = angle,
            useCenter = false,
            size = Size(size.width, size.height),
            style = Stroke(width = 6.dp.toPx())
        )
    }
}

@Composable
fun StartStopShape(
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    t: Float
) {
    val playPath = remember { addPathNodes("M 19 33 L 19 15 L 33 24 L 33 24 Z") }
    val stopPath = remember { addPathNodes("M 17 31 L 17 17 L 31 17 L 31 31 Z") }

    val pathNodes = lerp(playPath, stopPath, t)

    val degree = t * 90

    Image(
        painter = rememberVectorPainter(
            defaultWidth = 48.dp,
            defaultHeight = 48.dp,
            viewportWidth = 48f,
            viewportHeight = 48f
        ) { _, _ ->
            Path(
                pathData = pathNodes,
                fill = SolidColor(backgroundColor)
            )
        },
        modifier = modifier.rotate(degree),
        contentDescription = null
    )
}

@Preview(showBackground = true)
@Composable
fun Preview0() {
    ComposeWidgetsTheme {
        StartStopShape(Modifier, Color.Yellow, 0f)
    }
}

@Preview(showBackground = true)
@Composable
fun Preview1() {
    ComposeWidgetsTheme {
        StartStopShape(Modifier, Color.Yellow, 0.25f)
    }
}


@Preview(showBackground = true)
@Composable
fun Preview2() {
    ComposeWidgetsTheme {
        StartStopShape(Modifier, Color.Yellow, 0.5f)
    }
}

@Preview(showBackground = true)
@Composable
fun Preview3() {
    ComposeWidgetsTheme {
        StartStopShape(Modifier, Color.Yellow, 0.75f)
    }
}

@Preview(showBackground = true)
@Composable
fun Preview4() {
    ComposeWidgetsTheme {
        StartStopShape(Modifier, Color.Yellow, 1f)
    }
}

@HiltViewModel
class StartStopViewModel @Inject constructor(): ViewModel() {
    /* The state of the button is controlled by the view-model (and the view-model only) */
    var isStopped by mutableStateOf(true)
        private set

    private var isButtonEnabled = true

    fun onStartStopClicked() {
        if (!isButtonEnabled) return

        viewModelScope.launch {
            isButtonEnabled = false
            isStopped = !isStopped
            delay(START_STOP_DISABLE_TIMEOUT.toLong())
            isButtonEnabled = true
        }
    }
}

const val START_STOP_DISABLE_TIMEOUT = 2000