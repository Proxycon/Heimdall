package de.tomcory.heimdall.ui.apps.AppDetailScreen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.tomcory.heimdall.persistence.database.entity.Report
import de.tomcory.heimdall.ui.chart.ScoreChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreCard(report: Report?) {

    ElevatedCard(
        onClick = { /*TODO*/ },
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
        val report_sample = Report("test.android.com", timestamp = 3000, mainScore = .8)
        ScoreCard(report =  report_sample)
        Spacer(modifier = Modifier.height(10.dp))
        ScoreCard(report =  null)
    }

}
