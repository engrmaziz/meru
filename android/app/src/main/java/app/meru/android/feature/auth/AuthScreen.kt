package app.meru.android.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import app.meru.android.core.designsystem.components.MeruPrimaryButton
import app.meru.android.core.designsystem.components.MeruSecondaryButton
import app.meru.android.core.designsystem.theme.MeruDanger
import app.meru.android.core.designsystem.theme.MeruElevated
import app.meru.android.core.designsystem.theme.MeruMuted
import app.meru.android.core.designsystem.theme.MeruTeal
import app.meru.android.core.designsystem.theme.MeruText
import app.meru.android.core.designsystem.theme.MeruVoid

@Composable
fun AuthScreen(
    onBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MeruTeal,
        unfocusedBorderColor = MeruElevated,
        focusedTextColor = MeruText,
        unfocusedTextColor = MeruText,
        cursorColor = MeruTeal,
        focusedLabelColor = MeruTeal,
        unfocusedLabelColor = MeruMuted,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MeruVoid)
            .statusBarsPadding()
            .padding(24.dp),
    ) {
        TextButton(onClick = onBack) {
            Text("Back", color = MeruMuted)
        }
        Text(
            text = if (state.modeRegister) "Create account" else "Sign in",
            color = MeruText,
            fontSize = 28.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Meru keeps your drives and your cars in one ascent.",
            color = MeruMuted,
        )
        Spacer(Modifier.height(24.dp))

        if (state.modeRegister) {
            OutlinedTextField(
                value = state.displayName,
                onValueChange = viewModel::onDisplayName,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Display name") },
                colors = fieldColors,
                singleLine = true,
            )
            Spacer(Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = state.email,
            onValueChange = viewModel::onEmail,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Email") },
            colors = fieldColors,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = state.password,
            onValueChange = viewModel::onPassword,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Password") },
            colors = fieldColors,
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )

        state.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(text = it, color = MeruDanger)
        }

        Spacer(Modifier.height(24.dp))
        if (state.loading) {
            CircularProgressIndicator(color = MeruTeal)
        } else {
            MeruPrimaryButton(
                text = if (state.modeRegister) "Register" else "Sign in",
                onClick = viewModel::submit,
            )
            Spacer(Modifier.height(12.dp))
            MeruSecondaryButton(
                text = "Continue with Google (dev)",
                onClick = viewModel::continueWithGoogleDev,
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = viewModel::toggleMode) {
                Text(
                    text = if (state.modeRegister) "Have an account? Sign in" else "Need an account? Register",
                    color = MeruTeal,
                )
            }
        }
    }
}
