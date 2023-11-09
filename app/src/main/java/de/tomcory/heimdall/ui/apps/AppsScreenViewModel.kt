package de.tomcory.heimdall.ui.apps

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tomcory.heimdall.persistence.database.HeimdallDatabase
import de.tomcory.heimdall.persistence.database.dao.AppWithReports
import de.tomcory.heimdall.persistence.database.entity.App
import de.tomcory.heimdall.persistence.database.entity.Report
import de.tomcory.heimdall.scanner.code.ScanManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * ViewModel for [AppsScreen].
 * Holds the [AppsScreenUIState] and performance heavy operations.
 */
class AppsScreenViewModel : ViewModel() {
    // instantiate State as private Mutable State Flow to ensure only the ViewModel updates it - this handling is considered best practice
    private val _uiState = MutableStateFlow(AppsScreenUIState())

    // read-only state for the Composable
    val uiState: StateFlow<AppsScreenUIState> = _uiState.asStateFlow()

    // coroutine dispatcher for IO operations
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    // on creation, fetch app infos
    init {
        updateApps()
    }

    /**
     * parallelized wrapper for [fetchApps]
     */
    fun updateApps() {
        viewModelScope.launch { fetchApps() }
    }

    /**
     * dispatches coroutine to fetch apps from db and update [uiState].
     */
    private suspend fun fetchApps() = withContext(ioDispatcher) {
        Timber.d("loading apps for apps screen from DB")
        val apps =
            HeimdallDatabase.instance?.appDao?.getInstalledUserAppWithReports() ?: listOf()
        _uiState.update { AppsScreenUIState(apps, loadingApps = false) }
        Timber.d("finished loading ${apps.size} apps from DB")
    }

    /**
     * Icons fetching triggered by Composable because Context from UI is needed.
     * Context should never be statically stored in viewModel to avoid memory leaks
     */
    suspend fun loadIcons(context: Context) = withContext(ioDispatcher) {
        Timber.d("loading apps for apps screen from DB")
        var apps = uiState.value.apps
        // iterate over apps
        apps = apps.map {
            // if app entry has no icon and is installed:
            if (it.app.icon == null && it.app.isInstalled) {
                // load icon from ScanManager
                it.app.icon = ScanManager.getAppIcon(context, it.app.packageName)
            }
            it
        }
        // update state
        _uiState.update { AppsScreenUIState(apps, loadingApps = false) }
        Timber.d("finished loading ${apps.size} apps from DB")
    }
}

/**
 * State for [AppsScreen].
 * Holding list off [apps] and status [loadingApps].
 */
data class AppsScreenUIState(
    val apps: List<AppWithReports> = listOf(
        AppWithReports(
            App(
                "com.test.package",
                "TestPackage",
                "0.0.1",
                1
            ),
            listOf(Report(appPackageName = "com.test.package", timestamp = 1234, mainScore = 0.76))
        )
    ),
    var loadingApps: Boolean = true,
)