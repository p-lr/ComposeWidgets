package ovh.plrapps.widgets.ui

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class Screens : Parcelable {
    @Parcelize
    object Home : Screens()

    @Parcelize
    data class Demo(val name: String) : Screens()
}

enum class MainDestinations {
    HOME,
    START_STOP_BTN,
    MARKER,
    ZOOM_PAN_ROTATE
}
