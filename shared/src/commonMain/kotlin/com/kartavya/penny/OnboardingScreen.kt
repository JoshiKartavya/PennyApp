package com.kartavya.penny

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class OnboardingSlide(
    val key: String,
    val title: String,
    val subtitleRegular: String,
    val subtitleColor: String,
    val colorHex: Color,
    val bgHex: Color,
    val centerEmoji: String,
    val ring1: List<String>,
    val ring2: List<String>
)

val OnboardingSlides = listOf(
    OnboardingSlide(
        key = "1",
        title = "penny",
        subtitleRegular = "Beautifully simple\n",
        subtitleColor = "expense tracking",
        colorHex = Color(0xFFFF7E67),
        bgHex = Color(0xFFFFF5F2),
        centerEmoji = "✦",
        ring1 = listOf("✨", "💎"),
        ring2 = listOf("💰", "📈", "🚀")
    ),
    OnboardingSlide(
        key = "2",
        title = "track locally",
        subtitleRegular = "Your data stays\n",
        subtitleColor = "on your device",
        colorHex = Color(0xFF6B8AFF),
        bgHex = Color(0xFFF2F6FF),
        centerEmoji = "👛",
        ring1 = listOf("🔒", "📱"),
        ring2 = listOf("📊", "📝", "🛡️")
    ),
    OnboardingSlide(
        key = "3",
        title = "split with friends",
        subtitleRegular = "Sync, split, and\n",
        subtitleColor = "settle up easily",
        colorHex = Color(0xFFA267FF),
        bgHex = Color(0xFFF8F2FF),
        centerEmoji = "🫂",
        ring1 = listOf("☕️", "🍻"),
        ring2 = listOf("🍕", "🎫", "✈️")
    ),
    OnboardingSlide(
        key = "4",
        title = "home widgets",
        subtitleRegular = "Quick log & view balances\n",
        subtitleColor = "directly from your home screen",
        colorHex = Color(0xFF4CAF50),
        bgHex = Color(0xFFE8F5E9),
        centerEmoji = "📱",
        ring1 = listOf("⚡", "📊"),
        ring2 = listOf("💸", "➕", "📅")
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { OnboardingSlides.size })
    val coroutineScope = rememberCoroutineScope()
    val isDark = PennyTheme.colors.isDark

    // Animate background color transition between slides in light mode
    val targetBgColor = if (isDark) {
        PennyTheme.colors.background
    } else {
        OnboardingSlides[pagerState.currentPage].bgHex
    }
    val animatedBgColor by animateColorAsState(
        targetValue = targetBgColor,
        animationSpec = tween(600)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(animatedBgColor)
    ) {
        // Decorative background texture orbs
        Box(
            modifier = Modifier
                .offset(x = (-100).dp, y = (-100).dp)
                .size(400.dp)
                .clip(CircleShape)
                .background(if (isDark) PennyTheme.colors.card.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.5f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 100.dp, y = 100.dp)
                .size(350.dp)
                .clip(CircleShape)
                .background(if (isDark) PennyTheme.colors.card.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.4f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Skip button at the top (hidden on the final slide)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pagerState.currentPage != OnboardingSlides.size - 1) {
                    Text(
                        text = "Skip",
                        color = PennyTheme.colors.textSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier
                            .clickable { onFinish() }
                            .padding(8.dp)
                    )
                }
            }

            // Pager for slides
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                val slide = OnboardingSlides[page]
                val isFocused = pagerState.currentPage == page

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Visual Orb/Ring area
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        OnboardingVisual(slide = slide, isFocused = isFocused)
                    }

                    // Content text area
                    Column(
                        modifier = Modifier
                            .weight(0.8f)
                            .fillMaxWidth()
                            .padding(horizontal = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "PENNY",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PennyTheme.colors.textSecondary,
                            letterSpacing = 2.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = slide.title,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PennyTheme.colors.text,
                            letterSpacing = (-0.5).sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Text(
                            text = buildAnnotatedString {
                                append(slide.subtitleRegular)
                                withStyle(
                                    style = SpanStyle(
                                        color = slide.colorHex,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                ) {
                                    append(slide.subtitleColor)
                                }
                            },
                            fontSize = 20.sp,
                            color = PennyTheme.colors.textSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 28.sp,
                            fontWeight = FontWeight.Medium
                        )
                        
                        if (page == 3) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { requestPinWidget() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4CAF50),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Text(
                                    text = "Add Widget to Home Screen",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Footer (Pagination and buttons)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Pagination dots
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OnboardingSlides.forEachIndexed { i, slide ->
                        val isActive = i == pagerState.currentPage
                        val dotWidth by animateDpAsState(
                            targetValue = if (isActive) 24.dp else 6.dp,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                        )
                        val dotColor = if (isActive) slide.colorHex else {
                            if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f)
                        }

                        Box(
                            modifier = Modifier
                                .padding(horizontal = 5.dp)
                                .size(width = dotWidth, height = 6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(dotColor)
                        )
                    }
                }

                // Get Started action button container
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AnimatedVisibility(
                        visible = pagerState.currentPage == OnboardingSlides.size - 1,
                        enter = fadeIn(tween(500)) + scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy), initialScale = 0.9f),
                        exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.9f)
                    ) {
                        Button(
                            onClick = { onFinish() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PennyTheme.colors.primary,
                                contentColor = PennyTheme.colors.primaryText
                            ),
                            shape = RoundedCornerShape(30.dp),
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .fillMaxHeight(),
                            contentPadding = PaddingValues(vertical = 18.dp)
                        ) {
                            Text(
                                text = "Get Started",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingVisual(
    slide: OnboardingSlide,
    isFocused: Boolean
) {
    val isDark = PennyTheme.colors.isDark
    val infiniteTransition = rememberInfiniteTransition()

    // Create a 35-second linear spinning angle
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(35000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = Modifier.size(300.dp),
        contentAlignment = Alignment.Center
    ) {
        // Rings with spin/counter-spin values
        EmojiRing(emojis = slide.ring1, radius = 85.dp, rotation = rotationAngle)
        EmojiRing(emojis = slide.ring2, radius = 150.dp, rotation = -rotationAngle)

        // Center Orb
        Card(
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            colors = CardDefaults.cardColors(containerColor = PennyTheme.colors.card),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = slide.centerEmoji,
                    fontSize = 28.sp,
                    color = slide.colorHex
                )
            }
        }
    }
}

@Composable
fun EmojiRing(
    emojis: List<String>,
    radius: androidx.compose.ui.unit.Dp,
    rotation: Float
) {
    val isDark = PennyTheme.colors.isDark
    
    Box(
        modifier = Modifier
            .size(radius * 2)
            .rotate(rotation),
        contentAlignment = Alignment.Center
    ) {
        // Draw ring border
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.dp,
                    color = if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.06f),
                    shape = CircleShape
                )
        )

        // Draw emojis standing upright (with opposite rotation applied)
        emojis.forEachIndexed { index, emoji ->
            val angleDegree = (index.toFloat() / emojis.size.toFloat()) * 360f
            val angleRadian = angleDegree * PI / 180f

            // Calculate x and y coordinates on the circle perimeter
            val x = radius * cos(angleRadian).toFloat()
            val y = radius * sin(angleRadian).toFloat()

            Box(
                modifier = Modifier
                    .offset(x = x, y = y)
                    .size(44.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .size(40.dp)
                        .rotate(-rotation), // standing upright reverse-rotation
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(containerColor = PennyTheme.colors.card),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }
    }
}
