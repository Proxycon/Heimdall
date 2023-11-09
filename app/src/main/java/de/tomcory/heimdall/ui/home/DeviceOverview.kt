package de.tomcory.heimdall.ui.home

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.tomcory.heimdall.persistence.database.dao.AppWithReports
import de.tomcory.heimdall.persistence.database.entity.App
import de.tomcory.heimdall.persistence.database.entity.Report
import de.tomcory.heimdall.ui.chart.AllAppsChart
import de.tomcory.heimdall.ui.theme.acceptableScoreColor
import de.tomcory.heimdall.ui.theme.noReportScoreColor
import de.tomcory.heimdall.ui.theme.questionableScoreColor
import de.tomcory.heimdall.ui.theme.unacceptableScoreColor
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Major component of the [HomeScreen].
 * It contains [AllAppsChart] and [FlopApps] as subcomponents.
 * The [viewModel] holds the UI state.
 * It returns this view model to the home screen, to pass the collected apps to the [PreferenceScreen].
 * When triggering the scanning process there, this composable should update as well with the newly scanned apps.
 * This could be improved by moving the viewModel to the HomeScreen
 */
@Composable
fun DeviceOverview(
    viewModel: DeviceOverviewViewModel = viewModel(),
    context: Context = LocalContext.current,
    // function for user issued app scan
    userScanApps: () -> Unit = { viewModel.scanApps(context) },
    // setting if apps with no reports should be displayed
    showNoReportApps: Boolean = false
): DeviceOverviewViewModel {
    // collect state from viewMoel
    val uiState by viewModel.uiState.collectAsState()

    // header caption
    val title = remember { "Device Privacy Score" }
    // info text
    val infoText = remember {
        "This is an overview over the apps installed on your device. They are grouped into 'unacceptable', 'questionable' and 'acceptable' in regards to their privacy impact. The large number is the total privacy score of your device, with a maximum of 100 points."
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(30.dp, 0.dp)
    ) {
        // show loading animation while viewModel fetches data
        AnimatedVisibility(visible = uiState.loadingApps, enter = fadeIn(), exit = fadeOut()) {
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Text(text = "Loading apps...")
            }
        }

        // when loading is done, show content
        AnimatedVisibility(visible = !uiState.loadingApps, enter = fadeIn(), exit = fadeOut()) {
            // get app sets from viewModel
            var appSets = remember {
                viewModel.getAppSetSizes(showNoReportApps)
            }
            // get default colors for AllAppsChart from Theme.kt, optionally for apps with no report
            val defaultColors = getDefaultColorsFromTheme(showNoReportApps)
            // addd colors to sets
            appSets = remember {
                appSets.mapIndexed { index, item ->
                    if (item.color == Color.Unspecified) item.color = defaultColors[index]
                    item
                }
            }

            LazyColumn(
                Modifier.paddingFromBaseline(top = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // header and info icon
                item {
                    TopSegmentBar(title, infoText)
                }
                // all apps chart
                item {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                        AllAppsChart(appSets = appSets, totalScore = viewModel.getTotalScore())
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(30.dp))
                }
                // if there are scanned apps
                if (appSets.sumOf { it.size } > 0) {
                    // show the flop apps
                    item {
                        FlopApps(apps = viewModel.getFlopApps(context))
                    }
                } else {
                    // if not, show text
                    item {
                        Text(text = "No App Info found. Consider scanning")
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
                // scanning button
                item {
                    Button(
                        onClick = { userScanApps() },
                        enabled = !uiState.scanningApps
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Scan Device Icon")
                        Text(text = "Scan Device")
                    }
                }
                // if scanning in progress, show progress bar
                item {
                    AnimatedVisibility(visible = uiState.scanningApps) {
                        // get scanning progress status
                        val progress by uiState.scanAppsProgressFlow.collectAsState()
                        // show linear indicator
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier.size(width = 200.dp, height = 8.dp),
                            strokeCap = StrokeCap.Round
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(70.dp)) }
            }
        }
    }
    // return viewModel (see class documentation for why this is done)
    return viewModel
}


// TODO load thresholds from preferenceStore somehow
/**
 * Data class for [DeviceOverview] UI state.
 * When given a [List] of [AppWithReports] for [apps], it divides them into categories based on score thresholds.
 * Further manages states for loading and scanning apps and the progress for these processes.
 */
data class DeviceOverviewUIState(
    val apps: List<AppWithReports> = listOf(
        AppWithReports(
            App(
                "com.test.package", "TestPackage", "0.0.1", 1
            ),
            listOf(Report(appPackageName = "com.test.package", timestamp = 1234, mainScore = 0.76))
        )
    ),
    val appsNoReport: List<AppWithReports> = apps.filter { it.getLatestReport() != null },
    val appsUnacceptable: List<AppWithReports> = apps.filter {
        (it.getLatestReport()?.mainScore ?: -1.0) in 0.0..0.49
    },
    val appsQuestionable: List<AppWithReports> = apps.filter {
        (it.getLatestReport()?.mainScore ?: -1.0) in 0.5..0.74
    },
    val appsAcceptable: List<AppWithReports> = apps.filter {
        (it.getLatestReport()?.mainScore ?: -1.0) >= 0.75
    },

    val colors: List<Color> = listOf(),
    var scanningApps: Boolean = false,
    val scanAppsProgressFlow: MutableStateFlow<Float> = MutableStateFlow(0f),
    var loadingApps: Boolean = true,
)

/**
 * Fetch default colors from [Theme].
 */
@Composable
fun getDefaultColorsFromTheme(showNoReportApps: Boolean): List<Color> {
    var colors = remember {
        listOf(
            noReportScoreColor, unacceptableScoreColor, questionableScoreColor, acceptableScoreColor
        )
    }
    if (!showNoReportApps) colors = colors.drop(1)
    return colors
}

/**
 * Wrapper for often used info texts boxes
 */
@Composable
fun HelpTextBox(infoText: String) {
    Text(
        text = infoText, style = MaterialTheme.typography.bodySmall.merge(
            TextStyle(fontStyle = FontStyle.Italic)
        )
    )
    Spacer(modifier = Modifier.height(5.dp))
}

@Preview
@Composable
fun DeviceOverviewPreview() {
    DeviceOverview()
}