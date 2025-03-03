package de.tomcory.heimdall.ui.chart

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutExpo
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.tomcory.heimdall.ui.theme.BlueGradientBrush
import de.tomcory.heimdall.ui.theme.goodScoreColor

@Preview
@Composable
fun ScoreChartPreview() {
    ScoreChart(
        score = 76.0
    )
}


/**
 * CHart for displaying App [score].
 * Includes a arc meter with a gradient depending on the [colors] given.
 *
 */
@Composable
fun ScoreChart(
    score: Double,
    colors: List<Color> = listOf(
        MaterialTheme.colorScheme.error,
        goodScoreColor
    ),
    pathColor: Color = MaterialTheme.colorScheme.background,
    max: Double = 100.0,
    size: Dp = 220.dp,
    thickness: Dp = 15.dp,
    bottomGap: Float = 60f
) {

    val animateFloat = remember { Animatable(0f) }
    // animate meter arc growing when opened
    LaunchedEffect(animateFloat) {
        animateFloat.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = EaseInOutExpo))
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {


            val arcRadius = remember {
                size - thickness
            }
            val startAngle = remember { bottomGap / 2 }
            val sweepAngle: Float = remember {
                    ((360 - bottomGap / 2 - startAngle) * (score / 100)).toFloat()
            }
            val colorArcOffset = remember { (bottomGap / 360) / 2 }
            // gradient
            val brush = Brush.sweepGradient(
                0f + colorArcOffset to colors[0],
                1f - colorArcOffset to colors[1]
            )
            Canvas(
                modifier = Modifier
                    .size(size)
            ) {

                // rotate 90 because gradient definition stats a 0f angle (right) - so everything is drawn on the side and then rotated
                rotate(90f) {
                    // draw meter "path" behind meter, showing missing potential for full score
                    drawArc(
                        color = pathColor,
                        startAngle = startAngle,
                        sweepAngle = (360 - bottomGap),
                        useCenter = false,
                        style = Stroke(width = thickness.toPx(), cap = StrokeCap.Round),
                        size = Size(arcRadius.toPx(), arcRadius.toPx()),
                        topLeft = Offset(
                            x = (size.toPx() - arcRadius.toPx()) / 2,
                            y = (size.toPx() - arcRadius.toPx()) / 2
                        ),
                        //alpha = 0f
                    )
                    // score meter arc
                    drawArc(
                        brush = brush,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle * animateFloat.value,
                        useCenter = false,
                        style = Stroke(width = thickness.toPx(), cap = StrokeCap.Round),
                        size = Size(arcRadius.toPx(), arcRadius.toPx()),
                        topLeft = Offset(
                            x = (size.toPx() - arcRadius.toPx()) / 2,
                            y = (size.toPx() - arcRadius.toPx()) / 2
                        ),
                        //alpha = 0f
                    )
                }

            }
            // score numer text
            Box {
                Text(text = score.toInt().toString(), style = MaterialTheme.typography.displayLarge.merge(
                    TextStyle(brush = BlueGradientBrush)
                ), fontWeight = FontWeight.SemiBold)
            }
            // score max text
            Box(modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(vertical = 12.dp)) {
                Text(text = "/ ${max.toInt()}", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
            }
        }
    }
}