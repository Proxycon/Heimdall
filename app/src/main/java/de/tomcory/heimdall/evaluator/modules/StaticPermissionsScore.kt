package de.tomcory.heimdall.evaluator.modules

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.tomcory.heimdall.evaluator.ModuleResult
import de.tomcory.heimdall.persistence.database.HeimdallDatabase
import de.tomcory.heimdall.persistence.database.entity.App
import de.tomcory.heimdall.persistence.database.entity.Report
import de.tomcory.heimdall.persistence.database.entity.SubReport
import de.tomcory.heimdall.ui.apps.DonutChart
import de.tomcory.heimdall.ui.theme.altPrimaryColor
import de.tomcory.heimdall.ui.theme.altSecondaryColor
import de.tomcory.heimdall.ui.theme.altTernaryColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import org.json.JSONObject
import timber.log.Timber

/**
 * Module that evaluates the static, potentially requested permission of apps.
 */
class StaticPermissionsScore : Module() {
    override val name: String = "StaticPermissionScore"
    val label: String = "Permissions"

    override suspend fun calculateOrLoad(
        app: App,
        context: Context,
        forceRecalculate: Boolean
    ): Result<ModuleResult> {
        // load permission info from package manager
        val pm = context.packageManager
        val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(
                app.packageName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
            )
        } else {
            pm.getPackageInfo(app.packageName, 4096)
        }

        val permProts = pkgInfo.requestedPermissions.map { perm ->
            try {
                pm.getPermissionInfo(perm, PackageManager.GET_META_DATA).protection
            } catch (e: Exception) {
                Timber.w("Unknown permission: %s", perm)
                PermissionInfo.PROTECTION_NORMAL
            }
        }
        // count different permission categories
        val countDangerous = permProts.count { perm -> perm == PermissionInfo.PROTECTION_DANGEROUS }

        val countSignature = permProts.count { perm -> perm == PermissionInfo.PROTECTION_SIGNATURE }

        val countNormal = permProts.count { perm -> perm == PermissionInfo.PROTECTION_NORMAL }

        // compute score - point subtraction for different types of permission is chosen arbitrarily
        val score =
            maxOf(1f - countDangerous * 0.4f - countSignature * 0.02f - countNormal * 0.01f, 0f)

        // parse permission counts into json for storing in db
        val details =
            Json.encodeToString(PermissionCountInfo(countDangerous, countSignature, countNormal))

        // return success result with module results, containing name, score and details
        return Result.success(ModuleResult(this.name, score, additionalDetails = details))
    }

    @Composable
    override fun BuildUICard(report: Report?) {
        // calling template UI Card builder
        super.UICard(
            title = this.label,
            infoText = "This modules inspects the permissions the app might request at some point. These are categorized into 'Dangerous', 'Signature' and 'Normal'"
        ) {
            // display own content in card
            UICardContent(report)
        }
    }

    /**
     * Given a [report], this loads and returns the [SubReport] issued by this module for the specific report from the database.
     * TODO: consider storing of report history. The are potentially multiple SubReports. Should consider using [Report.reportId].
     */
    private fun loadSubReportFromDB(report: Report): SubReport? {
        return HeimdallDatabase.instance?.subReportDao?.getSubReportsByPackageNameAndModule(
            report.appPackageName,
            name
        )
    }

    /**
     * decode json details [info] string back into [PermissionCountInfo].
     */
    private fun decode(info: String): PermissionCountInfo? {
        Timber.d("trying to decode: $info")

        return try {
            Json.decodeFromString(info)
        } catch (e: Exception) {
            Timber.w(e, "Failed to decode in module: ${this.name}")
            // return null if decoding failed
            null
        }
    }

    /**
     * loads [SubReport] matching the given [report] from database and decode the [SubReport.additionalDetails] from json to [PermissionCountInfo].
     * Uses [loadSubReportFromDB] and [decode].
     */
    private fun loadAndDecode(report: Report?): PermissionCountInfo? {
        var permissionCountInfo: PermissionCountInfo? = null
        var subReport: SubReport? = null
        // when report is not null, fetch subreport
        report?.let {
            subReport = loadSubReportFromDB(it)
        }
        // when successful, decode permission info
        subReport?.let {
            permissionCountInfo = decode(it.additionalDetails)

        }
        Timber.d("loaded and decoded from DB: $permissionCountInfo")
        return permissionCountInfo
    }


    /**
     * Inner Content of UI card for this module.
     * Visulizes the [PermissionCountInfo] in a [DonutChart].
     */
    @Composable
    fun UICardContent(report: Report?) {
        var permissionCountInfo: PermissionCountInfo? by remember { mutableStateOf(null) }
        var loadingPermissions by remember { mutableStateOf(true) }

        // load permission info
        LaunchedEffect(key1 = 1) {
            this.launch(Dispatchers.IO) {
                permissionCountInfo = loadAndDecode(report)
                loadingPermissions = false
            }
        }

        // show loading animation
        AnimatedVisibility(visible = loadingPermissions, enter = fadeIn(), exit = fadeOut()) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        // when loaded, show content
        AnimatedVisibility(
            visible = !loadingPermissions,
            enter = slideInVertically(),
            exit = slideOutVertically()
        ) {
            Column(
                modifier = Modifier.padding(12.dp, 12.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                // when permission info is set, display donut chart
                permissionCountInfo?.let {
                    DonutChart(
                        values = listOf(
                            it.dangerousPermissionCount.toFloat(),
                            it.signaturePermissionCount.toFloat(),
                            it.normalPermissionCount.toFloat()
                        ),
                        legend = listOf("Dangerous", "Normal", "Signature"),
                        size = 150.dp,
                        colors = listOf(
                            altPrimaryColor,
                            altSecondaryColor,
                            altTernaryColor
                        )
                    )

                }
            }
            // if no info found, display text
            if (permissionCountInfo == null) {
                // No Permission info found
                Text(text = "No permission information found.")
            }
        }
    }


    override fun exportToJsonObject(subReport: SubReport?): JsonObject {
        // return empty json if subreport is null
        if (subReport == null) return buildJsonObject {
            put(name, JSONObject.NULL as JsonElement)
        }
        // decode subreport
        val permissionInfo: PermissionCountInfo? = decode(subReport.additionalDetails)
        // parse permission info to Json object. This is NOT a string but a Kotlin internal Json handling object
        val permissionInfoJson = Json.encodeToJsonElement(permissionInfo).jsonObject
        // parse subreport to Json object. Similarly NOT a string
        var serializedJsonObject: JsonObject = Json.encodeToJsonElement(subReport).jsonObject
        // remove old permission info string
        serializedJsonObject = JsonObject(serializedJsonObject.minus("additionalDetails"))
        // add permission info json object and return build json subreport
        val additionalPair = Pair("permission info", permissionInfoJson)
        return JsonObject(serializedJsonObject.plus(additionalPair))
    }

    override fun exportToJson(subReport: SubReport?): String {
        // first crafts json objects, then parses to string
        return exportToJsonObject(subReport).toString()
    }
}

/**
 * Data class for storing the permission counts of an apps.
 */
@Serializable
data class PermissionCountInfo(
    val dangerousPermissionCount: Int,
    val signaturePermissionCount: Int,
    val normalPermissionCount: Int
)
