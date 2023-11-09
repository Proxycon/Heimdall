package de.tomcory.heimdall.ui.apps.appDetailScreen

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import de.tomcory.heimdall.evaluator.Evaluator
import de.tomcory.heimdall.persistence.database.dao.AppWithReports
import de.tomcory.heimdall.persistence.database.entity.App
import de.tomcory.heimdall.persistence.database.entity.Report
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Fullscreen View Composable responsible for displaying app details given as [appWithReports], including
 * score and metric information.
 * It is not advices to override other parameters.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    appWithReports: AppWithReports,
    onDismissRequest: () -> Unit,
    /**
     * Facotry for the ViewModel. Needed to pass [appWithReports] to the ViewModel on creation.
     */
    factory: AppDetailViewModelFactory = AppDetailViewModelFactory(appWithReports),
    /**
     * ViewModel for this Composable. Holds the UI State and performance heavy operations.
     * If existing, the same ViewModel instance is assigned throughout recompositions.
     */
    appDetailViewModel: AppDetailViewModel = viewModel(factory = factory),
    context: Context = LocalContext.current,
    /**
     * Function to be executed when the user issues a re-scan of the current [appWithReports].
     */
    userRescanApp: () -> Unit = { appDetailViewModel.rescanApp(context) },
    /**
     * Function to be executed when the user issues uninstallation of the current [appWithReports].
     */
    userUninstallApp: () -> Unit = { appDetailViewModel.uninstallApp(context) },
    /**
     * Function to be executed when the user issues an export of the current [appWithReports].
     */
    userExportData: () -> Unit = { appDetailViewModel.exportToJson(context) }
) {
    // CoroutineScope for UI animations, like snackbar notification
    val scope = rememberCoroutineScope()

    // collecti UI state from ViewModel
    val appDetailUiState by appDetailViewModel.uiState.collectAsState()

    LaunchedEffect(key1 = 0) {
        /**
         * TODO
         * workaround to prevent the ViewModel from persisting between different app detail screens.
         * It should be destroyed when the composable is dismissed, but it doesn't.
         * That's the bug.
         * This workaround is bad vor performance and doesn't even fix the issue completely.
         */
        appDetailViewModel.updateApp(appWithReports)
    }

    // state of drop down menu
    var dropdownExpanded by remember { mutableStateOf(false) }

    // logging Composable creation
    Timber.d("Showing Details of $appWithReports")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        // header bar
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(0.dp, 0.dp, 12.dp, 0.dp),
                title = {
                    ListItem(headlineContent = { Text(text = appDetailUiState.packageLabel) },
                        supportingContent = {
                            Text(
                                text = appDetailUiState.packageName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingContent = {
                            Image(
                                painter = rememberDrawablePainter(drawable = appDetailUiState.packageIcon),
                                contentDescription = "App icon",
                                modifier = Modifier.size(40.dp)
                            )
                        })
                },
                navigationIcon = {
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.padding(0.dp, 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = "Close dialog"
                        )
                    }
                },
                // drop menu and toggle for additional actions
                actions = {
                    IconToggleButton(
                        checked = false,
                        onCheckedChange = { dropdownExpanded = !dropdownExpanded },
                        content = {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "More AppDetail Options"
                            )
                        })
                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rescan") },
                            onClick = {
                                userRescanApp()
                                // notify user via snackbar notification
                                scope.launch {
                                    appDetailUiState.snackbarHostState.showSnackbar("App re-scanned")
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Uninstall") },
                            onClick = {
                                userUninstallApp()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export") },
                            onClick = {
                                userExportData()
                                // notify user
                                scope.launch {
                                    appDetailUiState.snackbarHostState.showSnackbar("Export printed to debugging log")
                                }
                            }
                        )
                        Divider()
                        DropdownMenuItem(
                            text = { Text("Send Feedback") },
                            onClick = {
                                scope.launch {
                                    appDetailUiState.snackbarHostState.showSnackbar("Sorry, not yet implemented")
                                }
                            }
                        )
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = appDetailUiState.snackbarHostState)
        },
        ) { padding ->
        Column(Modifier.padding(padding)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp, 0.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                item {
                    // score
                    ScoreCard(report = appDetailUiState.report)
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // action buttons
                        FilledTonalButton(
                            onClick = { userUninstallApp() }) {
                            //Row {
                            Icon(Icons.Default.Delete, contentDescription = "Uninstall Icon")
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = "Uninstall")
                            // }
                        }
                        FilledTonalButton(
                            onClick = {
                                scope.launch {
                                    userExportData()
                                    appDetailUiState.snackbarHostState.showSnackbar("Report exported to debugging-log")
                                }
                            }) {
                            Icon(Icons.Default.Share, contentDescription = "Export Icon")
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(text = "Export")
                        }
                    }
                }
                // create item for each module and request their metric detail cards
                items(Evaluator.instance.getModules()) { module ->
                    module.BuildUICard(report = appDetailUiState.report)

                    // buffer padding between cards
                    Spacer(modifier = Modifier.height(9.dp))
                }
            }
        }
    }
}

/**
 * Data class for the state of the [AppDetailScreen] UI.
 * Most importantly it holds the [appWithReports] and unpacks it,
 * storing the latest [report], [packageName], [packageIcon], and [packageLabel] for easy access.
 */
data class AppDetailScreeUIState(
    var appWithReports: AppWithReports = AppWithReports(
        App(
            "com.test.package",
            "TestPackage",
            "0.0.1",
            1
        ),
        listOf(
            Report(
                reportId = 1,
                appPackageName = "com.test.package",
                timestamp = 1234,
                mainScore = 0.76
            )
        )
    ),
    val app: App = appWithReports.app,

    var report: Report? = appWithReports.getLatestReport(),

    val packageLabel: String = app.label,
    val packageName: String = app.packageName,
    val packageIcon: Drawable? = app.icon,

    // currently not used - dropdown state should eventually be outsourced to UI state
    var dropdownExpanded: MutableState<Boolean> = mutableStateOf(false),
    val snackbarHostState: SnackbarHostState = SnackbarHostState()
)


/**
 * Debugging Preview
 */
@Preview
@Composable
fun NewAppDetailScreenPreview() {
    val app = App(
        packageName = "test.package.com",
        label = "TestApp",
        versionName = "v0.1",
        versionCode = 0
    )
    val reports =
        listOf(Report(mainScore = 0.76, timestamp = 1234, appPackageName = "com.test.package"))
    AppDetailScreen(appWithReports = AppWithReports(app, reports), onDismissRequest = { })
}

