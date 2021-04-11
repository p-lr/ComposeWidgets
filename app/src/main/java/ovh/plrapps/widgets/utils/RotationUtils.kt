package ovh.plrapps.widgets.utils

typealias AngleDegree = Float
typealias AngleRad = Float

fun AngleDegree.toRad(): AngleRad = this * 0.017453292519943295f  // this * PI / 180.0
