package de.tomcory.heimdall.ui.home

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrykandpatrick.vico.core.extension.sumByFloat
import de.tomcory.heimdall.persistence.database.HeimdallDatabase
import de.tomcory.heimdall.persistence.database.dao.AppWithReports
import de.tomcory.heimdall.scanner.code.ScanManager
import de.tomcory.heimdall.ui.chart.ChartData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * View model for [DeviceOverview].
 * Holds the [DeviceOverviewUIState] and performance heavy operations.
 */
class DeviceOverviewViewModel() : ViewModel() {
    // instantiate State as private Mutable State Flow to ensure only the ViewModel updates it - this handling is considered best practice
    private val _uiState: MutableStateFlow<DeviceOverviewUIState> =
        MutableStateFlow(DeviceOverviewUIState())

    // read-only state for the Composable
    val uiState: StateFlow<DeviceOverviewUIState> = _uiState.asStateFlow()

    // coroutine scope dispatcher for IO operations
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
        Timber.d("loading apps for home screen from DB")
        val apps =
            HeimdallDatabase.instance?.appDao?.getInstalledUserAppWithReports() ?: listOf()
        _uiState.update { DeviceOverviewUIState(apps, loadingApps = false) }
        Timber.d("finished loading ${apps.size} apps for home screen from DB")
    }

    /**
     * computes total "device privacy score" as the average of all scanned app scores
     */
    fun getTotalScore(): Float {
        val apps = listOf(
            uiState.value.appsUnacceptable,
            _uiState.value.appsQuestionable,
            _uiState.value.appsAcceptable
        ).flatten()
        // if no apps scanned, return -1
        if (apps.isEmpty()) return -1f
        val n = apps.size
        val totalScore = apps.sumByFloat { it.getLatestReport()?.mainScore?.toFloat() ?: 0f }

        return totalScore / n * 100
    }

    /**
     * Returns [ChartData] for sets of apps from the [uiState]. [showNoReport] indicates whether apps with no report should be included
     */
    fun getAppSetSizes(showNoReport: Boolean = false): List<ChartData> {
        val label = listOf("no evaluation found", "unacceptable", "questionable", "acceptable")
        // get app sets from state
        var sets = listOf(
            uiState.value.appsNoReport,
            uiState.value.appsUnacceptable,
            uiState.value.appsQuestionable,
            uiState.value.appsAcceptable
        ).mapIndexed { index, item ->
            // iterate over them and create chart data
            ChartData(
                label[index],
                color = uiState.value.colors.getOrElse(index) { Color.Unspecified },
                size = item.size,
                infoText = "This is the number of apps that have an ${label[index]} privacy score."
            )
        }
        // if apps without report should not be displayed, drop them from the set
        if (!showNoReport) {
            sets = sets.drop(1)
        }
        return sets
    }


    /**
     * Dispatches a coroutine for the ScanManager to scan all apps which needs a UI [context].
     */
    fun scanApps(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            _uiState.update {
                it.copy(scanningApps = true)
            }
            Timber.d("scanning all apps")
            ScanManager.create(context).scanAllApps(context, uiState.value.scanAppsProgressFlow)
            fetchApps()
        }
    }

    /**
     *  Returns a [List] of size [nFlopApps] containing [AppWithReports] with the lowes scores.
     *  If there are fewer then [nFlopApps] scanned, it returns all scanned apps
     */
    fun getFlopApps(context: Context, nFlopApps: Int = 5): List<AppWithReports> {
        var n = nFlopApps
        // when there are fewer than n apps scanned, set n to the amount of apps.
        if (uiState.value.apps.size < n) {
            n = uiState.value.apps.size
        }
        // sort scanned apps by score and pick the top n
        var apps = uiState.value.apps.sortedBy { it.getLatestReport()?.mainScore }.subList(0, n)
        // add icons to these apps
        apps = apps.map {
            if (it.app.icon == null && it.app.isInstalled) {
                it.app.icon = ScanManager.getAppIcon(context, it.app.packageName)
            }
            it
        }
        Timber.d("loaded $n flop apps: $apps")
        return apps

    }
}