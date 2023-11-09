package de.tomcory.heimdall.persistence.database.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import de.tomcory.heimdall.persistence.database.entity.Report
import de.tomcory.heimdall.persistence.database.entity.SubReport
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for handling [Report]-related database operations.
 * Offers single entity insertion, as well as different fetching queries returning [List] or [Flow] of all Reports or filtering by [Report.reportId] or [Report.appPackageName].
 * Also features [ReportWithSubReport] transaction, combining one Report with corresponding [SubReport]s
 */
@Dao
interface ReportDao {

    /**
     * Inserts a single [Report] into the database and returns the rowId of the entry. Overrides on conflict.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE, entity = Report::class)
    suspend fun insertReport(report: Report): Long

    /**
     * Returns a [List] of all [Report]s ordered by timestamps.
     */
    @Query("SELECT * FROM Report ORDER BY timestamp DESC")
    fun getAll(): List<Report>

    /**
     * Returns a [ReportWithSubReport], matching the [Report] with the given [reportId] with its corresponding [SubReport]s.
     */
    @Transaction
    @Query("SELECT * FROM Report WHERE reportId = :reportId ORDER BY timestamp DESC")
    fun getReportById(reportId: Int): ReportWithSubReport

    /**
     * Returns a [List] of [ReportWithSubReport], matching the [Report] with the given [packageName] with its corresponding [SubReport]s.
     */
    @Transaction
    @Query("SELECT * FROM Report WHERE appPackageName = :packageName ORDER BY timestamp DESC")
    fun getReportsByPackageName(packageName: String): List<ReportWithSubReport>

    /**
     * Returns a [ReportWithSubReport], matching the [Report] with the given [reportId] with its corresponding [SubReport]s.
     */
    @Transaction
    @Query("SELECT * FROM Report WHERE reportId = :reportId ORDER BY timestamp DESC")
    fun getReportWithSubReportsById(reportId: Int): ReportWithSubReport

    /**
     * Returns a [Flow] of all observable [ReportWithSubReport].
     */
    @Query("SELECT * FROM Report")
    fun getAllObservable(): Flow<List<Report>>
}

/**
 * Data class combining a single embedded [Report] with related [SubReport]s, matched by [Report.reportId].
 * To use this class, define it as return type of a @Transaction @Query.
 */
data class ReportWithSubReport(
    @Embedded
    val report: Report,
    @Relation(
        parentColumn = "reportId",
        entityColumn = "reportId"
    )
    val subReports: List<SubReport>
)

