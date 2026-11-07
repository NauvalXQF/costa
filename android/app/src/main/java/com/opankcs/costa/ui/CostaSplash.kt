package com.opankcs.costa.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opankcs.costa.R
import com.opankcs.costa.ui.theme.CostaAmber
import com.opankcs.costa.ui.theme.CostaGraphite
import com.opankcs.costa.ui.theme.CostaInk
import com.opankcs.costa.ui.theme.CostaMuted
import kotlinx.coroutines.delay

@Composable
fun CostaSplash(onFinished: () -> Unit) {
    var started by remember { mutableStateOf(false) }
    var lettersVisible by remember { mutableStateOf(false) }
    var taglineVisible by remember { mutableStateOf(false) }
    var exiting by remember { mutableStateOf(false) }

    val overlayAlpha by animateFloatAsState(
        targetValue = if (exiting) 0f else 1f,
        animationSpec = tween(260),
        label = "splashAlpha",
    )
    val cScale by animateFloatAsState(
        targetValue = if (started) 1f else 0.78f,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "splashCScale",
    )
    val cAlpha by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(280),
        label = "splashCAlpha",
    )
    val taglineAlpha by animateFloatAsState(
        targetValue = if (taglineVisible) 1f else 0f,
        animationSpec = tween(500),
        label = "splashTagline",
    )

    LaunchedEffect(Unit) {
        started = true
        delay(220)
        lettersVisible = true
        delay(650)
        taglineVisible = true
        delay(450)
        exiting = true
        delay(280)
        onFinished()
    }

    val letterStyle = MaterialTheme.typography.displayLarge.copy(
        fontSize = 92.sp,
        lineHeight = 100.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-2).sp,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CostaGraphite),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(CostaAmber.copy(alpha = 0.08f), CostaGraphite.copy(alpha = 0f)),
                    ),
                ),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .graphicsLayer { alpha = overlayAlpha },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "C",
                    style = letterStyle,
                    color = CostaAmber,
                    modifier = Modifier.graphicsLayer {
                        scaleX = cScale
                        scaleY = cScale
                        alpha = cAlpha
                    },
                )

                listOf("o", "s", "t", "a").forEachIndexed { index, letter ->
                    AnimatedVisibility(
                        visible = lettersVisible,
                        enter = fadeIn(
                            animationSpec = tween(
                                durationMillis = 420,
                                delayMillis = index * 105,
                            ),
                        ) + expandHorizontally(
                            animationSpec = tween(
                                durationMillis = 420,
                                delayMillis = index * 105,
                                easing = FastOutSlowInEasing,
                            ),
                            expandFrom = Alignment.Start,
                        ),
                    ) {
                        Text(
                            text = letter,
                            style = letterStyle,
                            color = CostaInk,
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = stringResource(R.string.app_tagline).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 2.2.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = CostaMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { alpha = taglineAlpha },
            )
        }
    }
}
