package com.kartavya.penny

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview

enum class ScreenState {
    Onboarding,
    Login,
    Signup,
    ConfirmEmail,
    Home
}

@Composable
@Preview
fun App() {
    PennyAppTheme {
        val scope = rememberCoroutineScope()
        val savedEmail = remember { getNotificationPref("logged_in_email", "") }
        // App states: restore session if email is present
        var currentScreen by remember { mutableStateOf(if (savedEmail.isNotEmpty()) ScreenState.Home else ScreenState.Login) }
        var registeredEmail by remember { mutableStateOf("you@example.com") }
        var loggedInEmail by remember { mutableStateOf(savedEmail) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PennyTheme.colors.background)
        ) {
            // Elegant crossfade transition for smooth page changes
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
                },
                label = "ScreenNavigation"
            ) { screen ->
                when (screen) {
                    ScreenState.Onboarding -> {
                        OnboardingScreen(
                            onFinish = {
                                // Onboarding finished: go straight to main home dashboard
                                currentScreen = ScreenState.Home
                            }
                        )
                    }
                    ScreenState.Login -> {
                        LoginScreen(
                            onLoginSuccess = { email, isGoogleNewUser ->
                                loggedInEmail = email
                                saveNotificationPref("logged_in_email", email)
                                if (isGoogleNewUser) {
                                    currentScreen = ScreenState.Onboarding
                                } else {
                                    scope.launch {
                                        val profileName = FirebaseService.fetchUserProfileName(email)
                                        if (profileName != null && profileName.trim().isNotEmpty()) {
                                            currentScreen = ScreenState.Home
                                        } else {
                                            currentScreen = ScreenState.Onboarding
                                        }
                                    }
                                }
                            },
                            onNavigateToSignup = {
                                currentScreen = ScreenState.Signup
                            },
                            onClose = {
                                // Close button keeps user on login or defaults gracefully
                                currentScreen = ScreenState.Login
                            }
                        )
                    }
                    ScreenState.Signup -> {
                        SignupScreen(
                            onSignupSuccess = { email ->
                                registeredEmail = email
                                loggedInEmail = email
                                saveNotificationPref("logged_in_email", email)
                                currentScreen = ScreenState.Onboarding
                            },
                            onGoogleSuccess = { email, isNew ->
                                loggedInEmail = email
                                saveNotificationPref("logged_in_email", email)
                                if (isNew) {
                                    currentScreen = ScreenState.Onboarding
                                } else {
                                    currentScreen = ScreenState.Home
                                }
                            },
                            onNavigateToLogin = {
                                currentScreen = ScreenState.Login
                            },
                            onClose = {
                                currentScreen = ScreenState.Login
                            }
                        )
                    }
                    ScreenState.ConfirmEmail -> {
                        ConfirmEmailScreen(
                            email = registeredEmail,
                            onVerified = {
                                // Verified successfully: land on dashboard
                                currentScreen = ScreenState.Home
                            },
                            onBackToLogin = {
                                currentScreen = ScreenState.Login
                            }
                        )
                    }
                    ScreenState.Home -> {
                        HomeScreen(
                            userEmail = loggedInEmail,
                            onLogout = {
                                saveNotificationPref("logged_in_email", "")
                                scheduleWorkManagerReminders(24, false)
                                currentScreen = ScreenState.Login
                            }
                        )
                    }
                }
            }
        }
    }
}