package com.kartavya.penny

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun ConfirmEmailScreen(
    email: String,
    onVerified: () -> Unit,
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = PennyTheme.colors.isDark
    
    // Resend countdown state
    var countdown by remember { mutableStateOf(60) }
    var isResendEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown--
        } else {
            isResendEnabled = true
        }
    }

    // Floating envelope animations
    val infiniteTransition = rememberInfiniteTransition()
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        )
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PennyTheme.colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp)
        ) {
            // Close / Back button bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "← Back to log in",
                    fontSize = 15.sp,
                    color = PennyTheme.colors.textSecondary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable { onBackToLogin() }
                        .padding(8.dp)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Beautiful Floating Envelope Graphic
                Box(
                    modifier = Modifier
                        .padding(bottom = 40.dp)
                        .offset(y = floatOffset.dp)
                        .size(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer glow ring
                    Box(
                        modifier = Modifier
                            .scale(pulseScale)
                            .size(130.dp)
                            .clip(CircleShape)
                            .background(
                                if (isDark) Color(0xFF2C2C2C).copy(alpha = 0.4f)
                                else Color(0xFFFFF5F2).copy(alpha = 0.6f)
                            )
                    )
                    
                    // Main circle
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF1E1E1E) else Color.White)
                            .border(
                                width = 1.dp,
                                color = PennyTheme.colors.border,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✉️",
                            fontSize = 42.sp
                        )
                    }

                    // Floating sparkle badges
                    Text(
                        text = "✨",
                        fontSize = 20.sp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-10).dp, y = 10.dp)
                    )
                    Text(
                        text = "🚀",
                        fontSize = 16.sp,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .offset(x = 10.dp, y = (-10).dp)
                    )
                }

                // Typography
                Text(
                    text = "confirm your email",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PennyTheme.colors.text,
                    letterSpacing = (-0.8).sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Text(
                    text = "we've sent a verification link to\n$email",
                    fontSize = 15.sp,
                    color = PennyTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = "please tap the link in that email to activate your account and start tracking.",
                    fontSize = 14.sp,
                    color = PennyTheme.colors.textMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 44.dp)
                )

                // Interactive Primary Buttons
                Button(
                    onClick = { onVerified() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PennyTheme.colors.primary,
                        contentColor = PennyTheme.colors.primaryText
                    ),
                    shape = RoundedCornerShape(30.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = "I've verified my email",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Resend Timer Label
                if (isResendEnabled) {
                    Text(
                        text = "didn't receive email? resend link",
                        color = Color(0xFF4A90E2),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable {
                                countdown = 60
                                isResendEnabled = false
                            }
                            .padding(8.dp)
                    )
                } else {
                    Text(
                        text = "resend link in ${countdown}s",
                        color = PennyTheme.colors.textMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}
