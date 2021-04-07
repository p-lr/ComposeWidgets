package ovh.plrapps.widgets.ui.screens

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ovh.plrapps.widgets.ui.widgets.LandMark
import ovh.plrapps.widgets.ui.theme.ComposeWidgetsTheme

@Composable
fun MarkerDemo() {
    ComposeWidgetsTheme {
        var isStatic by remember { mutableStateOf(true) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(50.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LandMark(
                modifier = Modifier.size(250.dp),
                isStatic = isStatic
            )
            Button(onClick = { isStatic = !isStatic }) {
                Text(text = "Morph")
            }
        }
    }
}