package ovh.plrapps.widgets.utils

import androidx.compose.ui.graphics.vector.PathNode

/**
 * Calculates a number between two numbers at a specific increment.
 */
fun lerp(a: Float, b: Float, t: Float): Float {
    return a + (b - a) * t
}

/**
 * Linearly interpolates two lists of path nodes to simulate path morphing.
 */
fun lerp(fromPathNodes: List<PathNode>, toPathNodes: List<PathNode>, t: Float): List<PathNode> {
    return fromPathNodes.mapIndexed { i, from ->
        val to = toPathNodes[i]
        if (from is PathNode.MoveTo && to is PathNode.MoveTo) {
            PathNode.MoveTo(
                lerp(from.x, to.x, t),
                lerp(from.y, to.y, t),
            )
        } else if (from is PathNode.CurveTo && to is PathNode.CurveTo) {
            PathNode.CurveTo(
                lerp(from.x1, to.x1, t),
                lerp(from.y1, to.y1, t),
                lerp(from.x2, to.x2, t),
                lerp(from.y2, to.y2, t),
                lerp(from.x3, to.x3, t),
                lerp(from.y3, to.y3, t),
            )
        } else if (from is PathNode.LineTo && to is PathNode.LineTo) {
            PathNode.LineTo(
                lerp(from.x, to.x, t),
                lerp(from.y, to.y, t)
            )
        } else if (from is PathNode.Close && to is PathNode.Close) {
            PathNode.Close
        } else {
            // TODO: support all possible SVG path data types
            throw IllegalStateException("Unsupported SVG PathNode command")
        }
    }
}