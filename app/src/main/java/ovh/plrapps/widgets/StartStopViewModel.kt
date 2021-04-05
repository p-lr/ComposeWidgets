package ovh.plrapps.widgets

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration

class StartStopViewModel : ViewModel() {
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