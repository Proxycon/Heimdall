package de.tomcory.heimdall.util

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Uri
import android.system.OsConstants
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.startActivity
import timber.log.Timber
import java.net.InetSocketAddress


object OsUtils {
    @JvmStatic
    @RequiresApi(29)
    fun getAidQ(local: InetSocketAddress, remote: InetSocketAddress, context: Context): Int {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        return cm?.getConnectionOwnerUid(OsConstants.IPPROTO_TCP, local, remote) ?: -1
    }

    fun getSystemApps(context: Context): List<String> {
        val packageManager: PackageManager = context.packageManager
        val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        return apps.filter { app ->
            app.flags and ApplicationInfo.FLAG_SYSTEM != 0
        }.map { app ->
            app.packageName
        }
    }

    fun largeLog(tag: String?, content: String) {
        if (content.length > 4000) {
            Timber.d("${tag ?: ""} ${content.substring(0, 3000)}")
            largeLog(tag, content.substring(3000))
        } else {
            Timber.d(content)
        }
    }

    // TODO delete from Database if successful
    fun uninstallPackage(context: Context, packageName: String) {
        val uri: Uri = Uri.fromParts("package", packageName, null)
        val uninstallIntent = Intent(Intent.ACTION_DELETE, uri)

        startActivity(context, uninstallIntent, null)
    }

    fun shareIntent(context: Context, intent: Intent) {
        val shareIntent = Intent.createChooser(intent, null)
        startActivity(context, shareIntent, null)
    }
}