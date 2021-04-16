package ovh.plrapps.widgets.utils

import kotlin.math.pow

fun ease(p: Float, g: Float): Float {
    return if (p < 0.5f) {
        0.5f * (2 * p).pow(g)
    } else {
        1 - 0.5f * (2 * (1 - p)).pow(g)
    }
}

/**
 * Calculates a number between two numbers at a specific increment.
 */
fun lerp(a: Float, b: Float, t: Float): Float {
    return a + (b - a) * t
}