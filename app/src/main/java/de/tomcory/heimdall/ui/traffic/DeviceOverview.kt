package de.tomcory.heimdall.ui.traffic

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutExpo
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.tomcory.heimdall.R

@Composable
fun DeviceOverview(
    thickness: Dp = 20.dp,
    size: Dp = 240.dp,
    bottomGap: Float = 0f
) {
    var animateFloat = remember { Animatable(0f) }
    LaunchedEffect(animateFloat) {
        animateFloat.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = EaseInOutExpo)
        )
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.scale(2f)) {
                //brush = Brush.linearGradient(colors = textGradientColors)
                //style = MaterialTheme.typography.displayLarge
                Image(painter = painterResource(R.drawable.ic_heimdall_round), contentDescription = "Heimdall App Icon")
            }
            Canvas(
                modifier = Modifier.size(size).align(Alignment.Center)
            ) {

                val arcRadius = size.toPx() - thickness.toPx()
                val gradientColors = listOf(Color.Cyan, Color(0xFF0066FF), Color(0xFFdd21d1) /*...*/)
                val brush = Brush.linearGradient(colors = gradientColors)
                drawArc(
                    brush = brush,
                    startAngle = 90f+bottomGap/2,
                    sweepAngle = (360f - bottomGap/2) * animateFloat.value,
                    useCenter = false,
                    style = Stroke(width = thickness.toPx(), cap = StrokeCap.Round),
                    size = Size(arcRadius, arcRadius),
                    topLeft = Offset(
                        x = (size.toPx() - arcRadius) / 2,
                        y = (size.toPx() - arcRadius) / 2
                    ),
                    alpha = 0.9f
                )
            }
        }
    }
}

@Preview
@Composable
fun DeviceOverviewPreview(){
    DeviceOverview()
}