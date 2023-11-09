package de.tomcory.heimdall.ui.database

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import de.tomcory.heimdall.persistence.database.entity.Request

/**
 * Debugging view showing database information.
 * Uses [DatabaseViewModel] for UI state management.
 */
@Composable
fun DatabaseScreen(
    viewModel: DatabaseViewModel = viewModel()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .wrapContentSize(Alignment.Center)
    ) {
        val requests by viewModel.requests?.collectAsState(initial = emptyList()) ?: remember {
            mutableStateOf(emptyList())
        }
        Text(text = "Database contains ${requests.size} requests.")

        val responses by viewModel.responses?.collectAsState(initial = emptyList()) ?: remember {
            mutableStateOf(emptyList())
        }
        Text(text = "Database contains ${responses.size} responses.")

        val reports by viewModel.reports?.collectAsState(initial = emptyList()) ?: remember {
            mutableStateOf(emptyList())
        }
        Text(text = "Database contains ${reports.size} score reports.")

//        LazyColumn {
//            items(requests) {
//                RequestEntry(it)
//            }
//        }
    }
}

@Composable
fun RequestEntry(request: Request) {
    Text(text = "${request.method} ${request.initiatorPkg} -> ${request.remoteHost}")
}

@Preview
@Composable
fun DatabaseScreenPreview() {
    DatabaseScreen()
}