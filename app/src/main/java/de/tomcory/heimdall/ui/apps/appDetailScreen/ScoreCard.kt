package de.tomcory.heimdall.ui.apps.appDetailScreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.tomcory.heimdall.persistence.database.entity.Report
import de.tomcory.heimdall.ui.chart.ScoreChart

/**
 * Composable displaying the Score Card Element.
 * Derives the score information from a given [report].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreCard(report: Report?) {

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(10.dp, 12.dp)
                .align(Alignment.CenterHorizontally)
        ) {

            Text(
                text = "Score",
                style = MaterialTheme.typography.titleLarge.merge(TextStyle(fontWeight = FontWeight.SemiBold)),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (report != null) {
                ScoreChart(score = report.mainScore * 100)
            } else {
                Text(text = "No Score found in Database. Consider re-scanning", textAlign = TextAlign.Center)
            }
        }
    }
}

@Preview
@Composable
fun ScoreCardPreview(){
    Column {
        val reportSample =
            Report(appPackageName = "test.android.com", timestamp = 3000, mainScore = .8)
        ScoreCard(report = reportSample)
        Spacer(modifier = Modifier.height(10.dp))
        ScoreCard(report = null)
    }

}

