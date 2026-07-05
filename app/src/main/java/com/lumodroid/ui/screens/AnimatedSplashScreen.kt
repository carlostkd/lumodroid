package com.lumodroid.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumodroid.R
import com.lumodroid.ui.theme.DarkBg
import kotlinx.coroutines.delay

@Composable
fun AnimatedSplashScreen(
    onFinished: () -> Unit,
) {
    var alpha by remember { mutableFloatStateOf(0f) }

    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val scale by pulseAnim.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scalePulse",
    )

    LaunchedEffect(Unit) {
        delay(100)
        animate(0f, 1f, animationSpec = tween(700, easing = FastOutSlowInEasing)) { v, _ ->
            alpha = v
        }
        delay(2200)
        animate(1f, 0f, animationSpec = tween(500, easing = FastOutSlowInEasing)) { v, _ ->
            alpha = v
        }
        delay(300)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.app_logo),
                contentDescription = "LumoDroid",
                modifier = Modifier
                    .size(140.dp)
                    .scale(scale)
                    .alpha(alpha),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "LumoDroid",
                color = Color(0xFF8B9CFF),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(alpha),
            )
            Text(
                text = "Your private Android assistant",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(alpha),
            )
        }
    }
}
