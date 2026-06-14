package com.kartavya.penny

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: (String, Boolean) -> Unit,
    onNavigateToSignup: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = PennyTheme.colors.isDark
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Google Connection Dialog State
    var showGoogleDialog by remember { mutableStateOf(false) }
    var googleEmailInput by remember { mutableStateOf("") }
    
    // Forgot Password dialog state
    var isForgotDialogVisible by remember { mutableStateOf(false) }
    var forgotEmail by remember { mutableStateOf("") }

    fun performLogin() {
        if (email.isNotEmpty() && password.isNotEmpty()) {
            isLoading = true
            errorMessage = null
            scope.launch {
                val authenticatedEmail = FirebaseService.signInWithEmail(email, password)
                isLoading = false
                if (authenticatedEmail != null) {
                    onLoginSuccess(authenticatedEmail, false)
                } else {
                    errorMessage = "Invalid email or password"
                }
            }
        }
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
                    text = "welcome back",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PennyTheme.colors.text,
                    letterSpacing = (-0.8).sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Text(
                    text = "log in to access split & sync",
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
                                        onLoginSuccess(authenticatedEmail, isNew)
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
                        .padding(vertical = 24.dp),
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

                // Email Label and Input
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

                Spacer(modifier = Modifier.height(24.dp))

                // Password Label and Input
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
                        text = "forgot password?",
                        fontSize = 13.sp,
                        color = PennyTheme.colors.danger,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable {
                            forgotEmail = email
                            isForgotDialogVisible = true
                        }
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
                            performLogin()
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
                            performLogin()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PennyTheme.colors.primary,
                            contentColor = PennyTheme.colors.primaryText
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text(
                            text = "log in",
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
                    text = "no account?",
                    color = PennyTheme.colors.textSecondary,
                    fontSize = 14.sp
                )
                Text(
                    text = "  sign up",
                    color = PennyTheme.colors.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateToSignup() }
                )
            }
        }

        // Forgot Password Modal Dialog
        if (isForgotDialogVisible) {
            ForgotPasswordDialog(
                email = forgotEmail,
                onEmailChange = { forgotEmail = it },
                onDismiss = { isForgotDialogVisible = false },
                onSend = {
                    isForgotDialogVisible = false
                }
            )
        }

    }
}

@Composable
fun CustomUnderlineTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                color = PennyTheme.colors.text,
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal
            ),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = PennyTheme.colors.textPlaceholder,
                        fontSize = 17.sp
                    )
                }
                innerTextField()
            }
        )
        // Underline boundary
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(1.dp)
                .background(PennyTheme.colors.borderSecondary)
        )
    }
}

@Composable
fun ForgotPasswordDialog(
    email: String,
    onEmailChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSend: () -> Unit
) {
    val isDark = PennyTheme.colors.isDark
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = PennyTheme.colors.card),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 32.dp)
            ) {
                Text(
                    text = "reset password",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = PennyTheme.colors.text,
                    letterSpacing = (-0.5).sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "enter your email and we'll send you a secure link to reset your password.",
                    fontSize = 14.sp,
                    color = PennyTheme.colors.textSecondary,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(bottom = 28.dp)
                )

                Text(
                    text = "EMAIL ADDRESS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PennyTheme.colors.textMuted,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                CustomUnderlineTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    placeholder = "you@example.com",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                feedbackMessage?.let { msg ->
                    Text(
                        text = msg,
                        color = if (isSuccess) PennyTheme.colors.successLight else PennyTheme.colors.danger,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isSuccess) {
                        Text(
                            text = "cancel",
                            color = PennyTheme.colors.textSecondary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clickable { onDismiss() }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                    
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = PennyTheme.colors.primary,
                            modifier = Modifier.size(24.dp).padding(horizontal = 16.dp)
                        )
                    } else {
                        Button(
                            onClick = {
                                if (isSuccess) {
                                    onDismiss()
                                } else {
                                    if (email.trim().isNotEmpty()) {
                                        scope.launch {
                                            isLoading = true
                                            feedbackMessage = null
                                            val ok = FirebaseService.sendPasswordResetEmail(email)
                                            isLoading = false
                                            if (ok) {
                                                isSuccess = true
                                                feedbackMessage = "A secure reset link has been sent to your email."
                                            } else {
                                                isSuccess = false
                                                feedbackMessage = "Failed to send link. Please verify your email is correct."
                                            }
                                        }
                                    } else {
                                        feedbackMessage = "Please enter a valid email address."
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PennyTheme.colors.primary,
                                contentColor = PennyTheme.colors.primaryText
                            ),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
                        ) {
                            Text(
                                text = if (isSuccess) "done" else "send link",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}


