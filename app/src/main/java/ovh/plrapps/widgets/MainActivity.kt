package ovh.plrapps.widgets

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ovh.plrapps.widgets.ui.theme.ComposeWidgetsTheme


class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<StartStopViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ComposeWidgetsTheme {
                Surface(color = MaterialTheme.colors.background) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(50.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        StartStopButton(
                            modifier = Modifier.size(200.dp),
                            disableTimeoutMs = START_STOP_DISABLE_TIMEOUT,
                            stopped = viewModel.isStopped,
                            onClick = { viewModel.onStartStopClicked() }
                        )
                    }
                }
            }
        }
    }
}



