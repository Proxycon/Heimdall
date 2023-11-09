package de.tomcory.heimdall.ui.apps.appDetailScreen

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import de.tomcory.heimdall.evaluator.Evaluator
import de.tomcory.heimdall.persistence.database.dao.AppWithReports
import de.tomcory.heimdall.util.OsUtils
import de.tomcory.heimdall.util.OsUtils.uninstallPackage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Factory for the [AppDetailViewModel]. Needed to pass [appWithReports] to the ViewModel on creation.
 * Unsure if this is the idiomatic solution but it worked for me.
 */
class AppDetailViewModelFactory(private val appWithReports: AppWithReports) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AppDetailViewModel(appWithReports) as T
    }
}

/**
 * ViewModel for the [AppDetailScreen] Composable.
 * Holds the UI State and performance heavy operations.
 */
class AppDetailViewModel(appWithReports: AppWithReports) : ViewModel() {
    // instantiate State as private Mutable State Flow to ensure only the ViewModel updates it - this handling is considered best practice
    private val _uiState = MutableStateFlow(AppDetailScreeUIState(appWithReports))

    // read-only state for the Composable
    val uiState: StateFlow<AppDetailScreeUIState> = _uiState.asStateFlow()


    /**
     * Rescan current app and update [uiState] with new report
     */
    fun rescanApp(context: Context) {
        // start parallel thread
        CoroutineScope(Dispatchers.IO).launch {
            Timber.d("Rescanned ${uiState.value.packageName}")
            // trigger rescan
            val report = Evaluator.instance.evaluateApp(uiState.value.packageName, context)?.first
            if (report != null) {
                // update state - does nothing if report remains unchanged
                _uiState.update {
                    it.report = report
                    it
                }
                Timber.d("Updated UI with new report")
            } else {
                Timber.d("No new report")
            }
        }
    }

    /**
     *  Triggers operating system uninstall flow
     */
    fun uninstallApp(context: Context) {
        uninstallPackage(context, uiState.value.packageName)
    }

    /**
     * Update [uiState] with new app info.
     * Should not be necessary, if ViewModel lifecycle is the same as the Composable
     */
    fun updateApp(app: AppWithReports) {
        _uiState.update {
            AppDetailScreeUIState(app)
        }
    }

    /**
     *  trigger export of report for the current app
     */
    fun exportToJson(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val json = Evaluator.instance.exportReportToJson(uiState.value.report)
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, json)
                type = "text/json"
            }
            OsUtils.shareIntent(context, sendIntent)
        }
    }

}
