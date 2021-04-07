package ovh.plrapps.widgets.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ovh.plrapps.widgets.ui.widgets.START_STOP_DISABLE_TIMEOUT
import ovh.plrapps.widgets.ui.widgets.StartStopButton
import ovh.plrapps.widgets.ui.widgets.StartStopViewModel
import ovh.plrapps.widgets.ui.theme.ComposeWidgetsTheme

@Composable
fun StartStopDemo(modifier: Modifier = Modifier) {
    ComposeWidgetsTheme {
        val viewModel: StartStopViewModel = viewModel()

        Surface(modifier, color = MaterialTheme.colors.background) {
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