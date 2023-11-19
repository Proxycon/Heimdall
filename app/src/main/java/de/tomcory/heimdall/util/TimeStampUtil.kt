package de.tomcory.heimdall.util

import kotlinx.datetime.*
import java.text.*
import java.util.*

/**
 * Created by MJ Jacobs on 2023/11/19 at 11:44
 */

fun convertLongTimeToDateAndTimeStamp(timeStamp: Long): String? {
    val now = Clock.System.now().toEpochMilliseconds()
    val sixtySecondsInMillis = 60000
    val sixtyMinutesInMillis = 3600000
    val twentyFourHoursInMillis = 86400000
    val tenDaysInMillis = 864000000
    val difference: Int = if ((now - timeStamp) < tenDaysInMillis) (now - timeStamp).toInt() else tenDaysInMillis

    return when {
        difference in 0 until sixtySecondsInMillis -> {
            val seconds = difference / 1000
            "$seconds seconds ago"
        }

        difference in sixtySecondsInMillis until sixtyMinutesInMillis -> {
            val minutes = difference / 1000 / 60
            "$minutes minutes ago"
        }

        difference in sixtyMinutesInMillis until twentyFourHoursInMillis -> {
            val hours = difference / 1000 / 60 / 60
            "$hours hours ago"
        }

        difference in twentyFourHoursInMillis until tenDaysInMillis -> {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val timeString = sdf.format(timeStamp)
            val daysAgo = difference / 1000 / 60 / 60 / 24
            if (daysAgo == 1){
                "$daysAgo day ago at $timeString"
            }else{
                "$daysAgo days ago at $timeString"
            }

        }

        difference >= tenDaysInMillis -> {
            val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
            val time = sdfTime.format(timeStamp)
            val sdfDate = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
            val date = sdfDate.format(timeStamp)
            "$date at $time"
        }

        else -> return null
    }
}