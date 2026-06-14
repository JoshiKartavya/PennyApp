package com.kartavya.penny

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.compose.ui.text.style.TextAlign

@Composable
fun SignupScreen(
    onSignupSuccess: (String) -> Unit,
    onGoogleSuccess: (String, Boolean) -> Unit,
    onNavigateToLogin: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = PennyTheme.colors.isDark
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
 
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun performSignup() {
        if (name.trim().isNotEmpty() && email.trim().isNotEmpty() && password.isNotEmpty()) {
            isLoading = true
            errorMessage = null
            scope.launch {
                val authenticatedEmail = FirebaseService.signUpWithEmail(email, password)
                if (authenticatedEmail != null) {
                    FirebaseService.saveUserProfile(authenticatedEmail, name.trim())
                    FirebaseService.triggerDiscordSignupNotification(authenticatedEmail, name.trim())
                    isLoading = false
                    onSignupSuccess(authenticatedEmail)
                } else {
                    isLoading = false
                    errorMessage = "Failed to sign up. Email may already exist or password is too weak."
                }
            }
        }
    }

    // Replicate React Native password generator
    fun suggestPassword() {
        val baseName = if (name.trim().isNotEmpty()) {
            name.trim().split(" ")[0].filter { it.isLetter() }
        } else {
            "User"
        }
        val randNum = (100..999).random()
        val specialChars = listOf("@", "#", "$", "&", "!")
        val special = specialChars.random()
        val suggested = "${baseName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}$special${randNum}x"
        password = suggested
        showPassword = true
    }

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
            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center
            ) {
                // Titles
                Text(
                    text = "create account",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PennyTheme.colors.text,
                    letterSpacing = (-0.8).sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Text(
                    text = "start tracking and splitting together",
                    fontSize = 15.sp,
                    color = PennyTheme.colors.textSecondary,
                    modifier = Modifier.padding(bottom = 36.dp)
                )

                // Social SSO Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = PennyTheme.colors.border,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            isLoading = true
                            errorMessage = null
                            triggerGoogleSignIn(
                                onSuccess = { gEmail, gName, idToken ->
                                    scope.launch {
                                        val authenticatedEmail = if (idToken != null) {
                                            FirebaseService.signInWithGoogle(idToken)
                                        } else {
                                            gEmail
                                        } ?: gEmail
                                        
                                        val profileName = FirebaseService.fetchUserProfileName(authenticatedEmail)
                                        val isNew = profileName.isNullOrBlank()
                                        if (isNew) {
                                            val displayName = gName.ifEmpty { authenticatedEmail.substringBefore("@") }
                                            FirebaseService.saveUserProfile(authenticatedEmail, displayName)
                                            FirebaseService.triggerDiscordSignupNotification(authenticatedEmail, displayName)
                                        }
                                        isLoading = false
                                        onGoogleSuccess(authenticatedEmail, isNew)
                                    }
                                },
                                onFailure = { err ->
                                    isLoading = false
                                    errorMessage = err
                                }
                            )
                        }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "G",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PennyTheme.colors.text,
                        modifier = Modifier.padding(end = 10.dp)
                    )
                    Text(
                        text = "continue with Google",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = PennyTheme.colors.text
                    )
                }

                // Divider Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(PennyTheme.colors.border)
                    )
                    Text(
                        text = "or",
                        fontSize = 13.sp,
                        color = PennyTheme.colors.textMuted,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(PennyTheme.colors.border)
                    )
                }

                // Name Input
                Text(
                    text = "NAME",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PennyTheme.colors.textMuted,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                CustomUnderlineTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "Your Name",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Email Input
                Text(
                    text = "EMAIL",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PennyTheme.colors.textMuted,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                CustomUnderlineTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "you@example.com",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password Row & Suggestion Trigger
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "PASSWORD",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PennyTheme.colors.textMuted,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "suggest strong password",
                        fontSize = 11.sp,
                        color = Color(0xFF4A90E2),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { suggestPassword() }
                    )
                }

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    CustomUnderlineTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "••••••••",
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            performSignup()
                        }),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = if (showPassword) "🫣" else "👁️",
                        fontSize = 16.sp,
                        modifier = Modifier
                            .clickable { showPassword = !showPassword }
                            .padding(bottom = 10.dp, start = 10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                errorMessage?.let { err ->
                    Text(
                        text = err,
                        color = PennyTheme.colors.danger,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 16.dp).fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                // Submit Button
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = PennyTheme.colors.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            performSignup()
                        },
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
                            text = "sign up",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Footer Link
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "have an account?",
                    color = PennyTheme.colors.textSecondary,
                    fontSize = 14.sp
                )
                Text(
                    text = "  log in",
                    color = PennyTheme.colors.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateToLogin() }
                )
            }
        }
    }
}
