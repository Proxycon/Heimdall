package de.tomcory.heimdall.persistence.database.entity

import android.graphics.drawable.Drawable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

/**
 * Data class for storing app metadata.
 * When inserting into the database, [icon] property is omitted.
 */
@Entity
data class App(
    @PrimaryKey
    @ColumnInfo(index = true)
    val packageName: String,
    val label: String,
    val versionName: String,
    val versionCode: Long,
    val isInstalled: Boolean = true,
    val isSystem: Boolean = false,
    val flags: Int = 0
) {
    @Ignore
    var icon: Drawable? = null
}