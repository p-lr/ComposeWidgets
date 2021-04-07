package ovh.plrapps.widgets.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ovh.plrapps.widgets.R
import ovh.plrapps.widgets.ui.MainDestinations
import ovh.plrapps.widgets.ui.theme.ComposeWidgetsTheme

@Composable
fun Home(demoListState: LazyListState, onDemoSelected: (dest: MainDestinations) -> Unit) {
    ComposeWidgetsTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    backgroundColor = MaterialTheme.colors.surface,
                )
            }
        ) {
            LazyColumn(state = demoListState) {
                MainDestinations.values().filterNot { it == MainDestinations.HOME }.map { dest ->
                    item {
                        Text(
                            text = dest.name,
                            modifier = Modifier
                                .wrapContentSize(Alignment.Center)
                                .clickable { onDemoSelected.invoke(dest) }
                        )
                    }
                }
            }
        }
    }
}